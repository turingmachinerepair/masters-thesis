package org.thesis.quadomizer;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;

import com.github.dockerjava.core.exec.InspectServiceCmdExec;
import io.minio.GetObjectArgs;
import io.minio.PutObjectArgs;
import jdk.jfr.Enabled;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import org.thesis.common.Tickets.*;
import sun.misc.IOUtils;
import org.thesis.quadomizer.Node.NodeManager;
import sun.net.ResourceManager;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Enabled
@Service
public class Quadomizer {

    String serviceIdentificator;
    MinIOAdapter minioInstance;
    String topicName = "TaskFabric";
    DockerClient dockerClient;
    final NodeManager resourceManager;

    Hashtable<String,String> assignedContainers;
    Hashtable<String,String> assignedServices;
    Hashtable<String,CompilationTaskContext> taskContexts;


    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;


    public void sendMessage(String msg) {

        kafkaTemplate.send(topicName, msg);

    }

   public Quadomizer(){

       serviceIdentificator = RandomStringUtils.randomAlphabetic(10);

       minioInstance = new MinIOAdapter();
       dockerClient = DockerClientBuilder.getInstance().build();
       assignedContainers = new Hashtable<String,String>();
       assignedServices = new Hashtable<String,String>();
       taskContexts = new Hashtable<String,CompilationTaskContext>();

       List<SwarmNode> nodes = dockerClient.listSwarmNodesCmd().exec();
       resourceManager = new NodeManager(nodes);

       System.out.println( "Registered nodes:" + resourceManager.toString() );

   }

   public String getServiceIdentificator(){
        return serviceIdentificator;
   }

    /**
     * Construct compilation task digest based on type and stage necessary
     * @param ticket
     * @return
     */
    CompilationTaskDigest createDigestForTask(CompilationTaskTicket ticket){
        CompilationTaskDigest res = new CompilationTaskDigest();
        String projectPath = ticket.getProjectPath();
        String stage;
        String fpgaType;

        //Evaluate stage

        System.out.println("Infer FPGA type");
        //Evaluate FPGA type
        if( projectPath.contains("S10")) {
            fpgaType = "S10";
        } else if (projectPath.contains("A10")) {
            fpgaType = "A10";
        } else {
            fpgaType = "generic";
        }

        //Set resource constraints
        STAGES taskStageEnum = ticket.getNextStage();
        res.setStage(taskStageEnum);
        System.out.println("Target stage: " + taskStageEnum);


        if( fpgaType.equals( "S10") ){
            res.setFPGAType(fpgaType);

            if( taskStageEnum == STAGES.Fit ){
                res.setCPUs(16);
                res.setRAM(40);
            } else if( taskStageEnum == STAGES.Synthesis) {
                res.setCPUs(2);
                res.setRAM(10);
            } else {
                res.setCPUs(2);
                res.setRAM(2);
            }

        } else if( fpgaType.equals( "A10") ){
            res.setFPGAType(fpgaType);

            if( taskStageEnum == STAGES.Fit ){
                res.setCPUs(8);
                res.setRAM(20);
            } else if( taskStageEnum == STAGES.Synthesis) {
                res.setCPUs(2);
                res.setRAM(10);
            } else {
                res.setCPUs(2);
                res.setRAM(2);
            }

        } else {

            if( taskStageEnum == STAGES.Fit){
                res.setCPUs(4);
                res.setRAM(5);
            } else if( taskStageEnum == STAGES.Synthesis) {
                res.setCPUs(2);
                res.setRAM(5);
            } else {
                res.setCPUs(2);
                res.setRAM(2);
            }

        }
        return res;
    }

    /**
     * Receive method for Kafka. Either deploy task or return it to broker.
     * @param UUID
     */

    @KafkaListener(topics = "TaskFabric", groupId = "dispatchers")
   public void receiveTask(@Payload String UUID){

        System.out.println("Received task, UUID:"+UUID);
        CompilationTaskTicket ticket = minioInstance.getCompilationTaskTicket(UUID);
        CompilationTaskDigest digest = createDigestForTask(ticket);
        CompilationTaskContext taskContext = new CompilationTaskContext(ticket,digest);
        System.out.println("Full task context:"+taskContext.toString());
        String hostname = "";
        try{
            synchronized (resourceManager){

                while( resourceManager.isOpInProgress() ){
                    resourceManager.wait();
                }

                hostname = resourceManager.deployTask(taskContext);

                resourceManager.notifyAll();

            }
        } catch( Exception e){
            System.out.println( e.toString() );
            hostname = "";
        }


        boolean deploymentPossible = ( !hostname.isEmpty() );
        boolean deploymentSuccessful = false;
        if( deploymentPossible ){
            System.out.println("Deployment possible on hostname "+hostname);
            System.out.println("Resource manager snapshot:"+resourceManager.toString());
            deploymentSuccessful = this.deployTask(taskContext,hostname);
        }

        if( deploymentPossible && !deploymentSuccessful ){
            System.out.println("Resources were allocated, but deployment unsuccessful. Return resources.");

            try{
                synchronized (resourceManager){

                    while( resourceManager.isOpInProgress() ){
                        resourceManager.wait();
                    }

                    resourceManager.freeTask(taskContext);

                    resourceManager.notifyAll();

                }
            } catch( Exception e){

            }



        }

        if( deploymentPossible && deploymentSuccessful ) {
            ticket.setCurrentState(STATES.PROCESSING);
            ticket.setHostname(hostname);
            ticket.setCurrentStage( digest.getStage() );
            minioInstance.putCompilationTaskTicket(ticket);
        } else {
            System.out.println("Deployment impossible. Return ticket to fabric");
            ticket.setCurrentState(STATES.QUEUED);
            minioInstance.putCompilationTaskTicket(ticket);
            sendMessage(UUID);
        }
   }

    boolean deployTask(CompilationTaskContext task, String hostname){

        //create container
        try{
            System.out.println("Create service");
            //command
            String cmd = "bash /root/quartus_wrapper.sh \"/prototype_root"+ task.getTicket().getProjectPath()+"\" "
                            +task.getTicket().getProjectName() +" " + task.getTicket().getNextStageIndex() + " " + task.getTicket().getUUID();

            //networks
            List<NetworkAttachmentConfig> nets = new ArrayList<>(
                    Arrays.asList( new NetworkAttachmentConfig().withTarget("host")  )
            );

            String serviceName = "service-"+task.getTicket().getUUID();

            //placement
            ServicePlacement sp = new ServicePlacement().withConstraints(Collections.singletonList("node.hostname==" + hostname));

            //container spec
            ContainerSpec ct = new ContainerSpec().withImage("phdinintegrals/quartus-masters:19.1-wrapper").
                    withCommand(Collections.singletonList(cmd)).withTty(true);
            //task spec
            TaskSpec tt = new TaskSpec().withContainerSpec(ct).withPlacement(sp);
            //service spec
            ServiceSpec ss = new ServiceSpec().withTaskTemplate(tt).withName( serviceName ).withNetworks(nets);

            //deploy service
            CreateServiceResponse serviceResponse = dockerClient.createServiceCmd(ss).exec();
            String serviceID = serviceResponse.getId();

            //infer containerID from service (guaranteed to contain service's name)
            String containerID = "";
            List<Task> tasks = dockerClient.listTasksCmd().withServiceFilter(serviceID).exec();

            if( tasks.size() >0 ){
                System.out.println("Container ready.");
                tasks.get(0).getStatus().getContainerStatus().getExitCodeLong();
                containerID = tasks.get(0).getId();
            } else {
                System.out.println("No container.");
                throw new Exception("Container for service not created");
            }

            System.out.println("Store data. Hostname:"+ hostname + " Container ID:"+ containerID + " Service ID:" + serviceID );
            taskContexts.put( task.getTicket().getUUID() , task);
            assignedContainers.put( task.getTicket().getUUID(), containerID );
            assignedServices.put( task.getTicket().getUUID(), serviceID );
            System.out.println("Task reference snapshot:" + taskContexts.toString());
            System.out.println("Container reference snapshot:" + assignedContainers.toString());
            System.out.println("Services reference snapshot:" + assignedContainers.toString());

            //TODO: how to register finished service?


            //Implement event watcher for new task and register callback
            System.out.println("Register callback. Master ID:"+this.getServiceIdentificator());
            DockerContainerCallback resultCallback = new DockerContainerCallback(this, task.getTicket().getUUID() );

            dockerClient.eventsCmd().withContainerFilter("com.docker.swarm.task.id=="+tasks.get(0).getId()).
                    withEventFilter("die","stop","kill").exec(resultCallback);
            System.out.println("Task deployed");

        } catch( Exception e){
            System.out.println(e.toString() );
            return false;
        }

        return true;
    }


    /**
     * Upon getting the compilation result - update ticket
     */
   public void updateTaskStatus(String UUID){
       System.out.println("Task "+UUID+" finished");
       //String containerID = assignedContainers.get(UUID);

       String serviceID = assignedServices.get(UUID);
       List<Task> tasks = dockerClient.listTasksCmd().withServiceFilter(serviceID).exec();
       Task targetTask = tasks.get(0);

       System.out.println("On service:"+serviceID );

       Long result = targetTask.getStatus().getContainerStatus().getExitCodeLong();
       System.out.println("Result: "+result.toString() + " Reason:" + targetTask.getStatus().getState().getValue());

       CompilationTaskContext context = taskContexts.get(UUID);
       CompilationTaskTicket ticket = context.getTicket();

       int lastStage = ticket.getLastStage();
       boolean ticketRetranslate = false;

       if( result == 0L){

           boolean[] stageVector = ticket.getCurrentStages();
           boolean[] targetVector = ticket.getNecessaryStages();

           System.out.println("Success.New stage:"+ Integer.toString(lastStage + 1));
           System.out.println("Task vectors:"+ Arrays.toString(stageVector));
           System.out.println("Target vector:"+ Arrays.toString(targetVector));
           int lastStageIndex = ticket.getNextStageIndex();
           STAGES taskStage = context.getDigest().getStage();

            stageVector[lastStageIndex] = true;

            ticket.setLastStage( lastStageIndex );
            ticket.setCurrentStages( stageVector );
            ticket.setError(false);

            if( !Arrays.equals(targetVector, stageVector)){
                System.out.println("Ticket not finished, retranslation necessary.");
                ticketRetranslate = true;
                ticket.setCurrentState(STATES.QUEUED);
                ticket.setHostname("");
            } else {
                System.out.println("Ticket fully finished.");
                ticket.setCurrentState(STATES.FINISHED);
                ticket.setHostname("");
            }

        } else {

           System.out.println("Error on processing ticket, refer to logs.");
           ticket.setError(true);
           ticket.setCurrentState(STATES.FINISHED);
           ticket.setHostname("");

        }

       System.out.println("Update ticket in MinIO");
       minioInstance.putCompilationTaskTicket(ticket);
       System.out.println("Free resources");
       try{
           synchronized (resourceManager){

               while( resourceManager.isOpInProgress() ){
                   resourceManager.wait();
               }

               resourceManager.freeTask(context);

               resourceManager.notifyAll();

           }
       } catch( Exception e){

       }

       System.out.println("Res manager snapshot:" + resourceManager.toString() );

       System.out.println("Killing service.");
       dockerClient.removeServiceCmd(serviceID);

       System.out.println("Remove container reference");
       assignedContainers.remove(UUID);
       assignedServices.remove(UUID);
       System.out.println("Remove task reference");
       taskContexts.remove(UUID);

       if( ticketRetranslate ){
           sendMessage(UUID);
       }

   }

}

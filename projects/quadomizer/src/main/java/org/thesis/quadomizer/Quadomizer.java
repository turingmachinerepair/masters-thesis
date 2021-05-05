package org.thesis.quadomizer;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.core.DockerClientBuilder;

import io.minio.GetObjectArgs;
import io.minio.PutObjectArgs;
import jdk.jfr.Enabled;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

@Enabled
@Service
public class Quadomizer {

    String serviceIdentificator;
    MinIOAdapter minioInstance;
    String topicName = "TaskFabric";
    DockerClient dockerClient;
    final NodeManager resourceManager;
    HashMap<String,String> assignedContainers;
    HashMap<String,CompilationTaskContext> taskContexts;


    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;


    public void sendMessage(String msg) {

        kafkaTemplate.send(topicName, msg);

    }

   public Quadomizer(){

       serviceIdentificator = RandomStringUtils.randomAlphabetic(10);

        minioInstance = new MinIOAdapter();
       dockerClient = DockerClientBuilder.getInstance().build();
       assignedContainers = new HashMap<String,String>();
       taskContexts = new HashMap<String,CompilationTaskContext>();
       resourceManager = new NodeManager();
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

            if( taskStageEnum == STAGES.Place || taskStageEnum == STAGES.Route ){
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

            if( taskStageEnum == STAGES.Place || taskStageEnum == STAGES.Route ){
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

            if( taskStageEnum == STAGES.Place || taskStageEnum == STAGES.Route ){
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
        /*TODO: quartus container logic with:
        1) s3fs mount src and project buckets
        2) set env variables with paths to mounted buckets
        3) form command.
        */
        //create container
        try{
            System.out.println("Create container");
            CreateContainerResponse container = dockerClient.createContainerCmd("hello-world").exec();
            dockerClient.startContainerCmd(container.getId()).exec();

            System.out.println("Store data. Hostname:"+ hostname + " Container ID:"+container.getId() );
            taskContexts.put( task.getTicket().getUUID() , task);
            assignedContainers.put( task.getTicket().getUUID(), container.getId() );
            System.out.println("Task reference snapshot:" + taskContexts.toString());
            System.out.println("Container reference snapshot:" + assignedContainers.toString());

            System.out.println("Register callback. Master ID:"+this.getServiceIdentificator());
            DockerContainerCallback resultCallback = new DockerContainerCallback(this, task.getTicket().getUUID() );
            dockerClient.waitContainerCmd( container.getId() ).exec(resultCallback);
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
       String containerID = assignedContainers.get(UUID);
       System.out.println("On container"+containerID);

       InspectContainerResponse res = dockerClient.inspectContainerCmd(containerID).exec();
       Long result = res.getState().getExitCodeLong();
       System.out.println("Result: "+result.toString());

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

       System.out.println("Killing container.");
       dockerClient.killContainerCmd(containerID);

       System.out.println("Remove container reference");
       assignedContainers.remove(UUID);
       System.out.println("Remove task reference");
       taskContexts.remove(UUID);

       if( ticketRetranslate ){
           sendMessage(UUID);
       }

   }

}

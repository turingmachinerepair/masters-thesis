package org.thesis.quadomizer;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;

import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.exec.InspectServiceCmdExec;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
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

/**
 * Класс - ядро сервиса, реализующий прикладную логику микросервиса
 */
@Enabled
@Service
public class Quadomizer {

    /**
     * Имя сервиса
     */
    String serviceIdentificator;

    /**
     * Экземпляр S3 клиента
     */
    MinIOAdapter minioInstance;

    /**
     * Имя темы Kafka откуда читаются UUID задач и куда возвращаются UUID задач
     */
    String topicName = "TaskFabric";

    /**
     * Экземпляр клиента docker
     */
    DockerClient dockerClient;

    /**
     * Экземпляр диспетчера ресурсов кластера
     */
    final NodeManager resourceManager;

    /**
     * ID контейнеров назначенных на задачу.
     * @deprecated
     */
    Hashtable<String,String> assignedContainers;

    /**
     * ID сервисов назначенных на задачи. Ключ = UUID, значение = ID service на swarm
     */
    Hashtable<String,String> assignedServices;
    /**
     * Справочник контекстов активных задач
     */
    Hashtable<String,CompilationTaskContext> taskContexts;

    /**
     * Экземпляр издателя Kafka
     */
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Публикация UUID в очередь брокера Kafka
     * Применяется если запуск задачи на кластере невозможен или этап успешно завершен, но нужно выполнить следующий этап.
     * @param msg UUID задачи на компиляцию ПЛИС
     */
    public void sendMessage(String msg) {

        kafkaTemplate.send(topicName, msg);

    }

    /**
     * Конструктор по-умолчанию
     */
    public Quadomizer(){

       serviceIdentificator = RandomStringUtils.randomAlphabetic(10);

       minioInstance = new MinIOAdapter();

       DockerClientConfig custom = DefaultDockerClientConfig.createDefaultConfigBuilder()
               .withDockerHost("unix:///var/run/docker.sock")
               .build();

       DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
               .dockerHost(custom.getDockerHost())
               .sslConfig(custom.getSSLConfig())
               .maxConnections(100)
               .build();

       dockerClient = DockerClientImpl.getInstance(custom, httpClient);


       assignedContainers = new Hashtable<String,String>();
       assignedServices = new Hashtable<String,String>();
       taskContexts = new Hashtable<String,CompilationTaskContext>();

       List<SwarmNode> nodes = dockerClient.listSwarmNodesCmd().exec();
       resourceManager = new NodeManager(nodes);

       System.out.println( "Registered nodes:" + resourceManager.toString() );

   }

    /**
     * Получить уникальное имя экземпляра сервиса
     * @return имя сервиса
     */
   public String getServiceIdentificator(){
        return serviceIdentificator;
   }

    /**
     * Оценить ресурсы необходимые для нужного этапа задачи на компиляцию
     * @param ticket задача этап которой надо выполнить
     * @return экземпляр оценки ресурсов для этапа
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
     * Метод-подписчик брокера Kafka, тема TaskFabric
     * Чтение UUID задач которые находятся в очереди на исполнение
     * @param UUID UUID задачи которую нужно выполнить
     */
    @KafkaListener(topics = "TaskFabric", groupId = "dispatchers")
   public void receiveTask(@Payload String UUID){

        System.out.println("-------------------------------------------");
        System.out.println("Received task, UUID:"+UUID);
        CompilationTaskTicket emptyTicket = new CompilationTaskTicket();
        CompilationTaskTicket ticket = minioInstance.getCompilationTaskTicket(UUID);
        if( ticket.getUUID().equals(emptyTicket.getUUID() ) ){
            System.out.println("Kafka has obsolete task with UUID:"+UUID+", discard it.");
            return;
        }
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



        } else if( deploymentPossible && deploymentSuccessful ) {
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
        System.out.println("Task processing finished.");
        System.out.println("-------------------------------------------");
   }

    /**
     * Запустить задачу на кластере
     * @param task экземпляр контекста задачи
     * @param hostname имя узла
     * @return флаг успеха запуска задачи. true=получилось, false=возникла ошибка
     */
    boolean deployTask(CompilationTaskContext task, String hostname){

        //create container
        try{
            STAGES stage = task.getTicket().getNextStage();
            int nextStage = 0;
            if( stage == STAGES.Synthesis)
                nextStage = 1;
            else if (stage == STAGES.Fit )
                nextStage = 2;
            else if (stage == STAGES.TimingAnalysis )
                nextStage = 3;
            else if (stage == STAGES.Assembler)
                nextStage = 4;
            String strStage = String.valueOf(nextStage);

            System.out.println("Create service");
            //command
            String cpuStr =  String.valueOf( task.getDigest().getCPUs() );
            String flowID = strStage;
            List<String> cmd = new ArrayList<>(
                    Arrays.asList(
                    "/bin/bash",
                    "/root/quartus_wrapper.sh",
                    "/prototype_root"+ task.getTicket().getProjectPath(),
                    task.getTicket().getProjectName(),
                    cpuStr,
                            flowID,
                    task.getTicket().getUUID()
                    ) );

            //networks
            List<NetworkAttachmentConfig> nets = new ArrayList<>(
                    Collections.singletonList(new NetworkAttachmentConfig().withTarget("host"))
            );

            String serviceName = "service-"+task.getTicket().getUUID();

            //placement
            ServicePlacement sp = new ServicePlacement().withConstraints(Collections.singletonList("node.hostname==" + hostname));
            ServiceRestartPolicy restartPolicy = new ServiceRestartPolicy().withMaxAttempts(0L);
            ContainerSpecConfig contConfig = new ContainerSpecConfig();

            //mount
            Mount mnt = new Mount().
                    withType( MountType.BIND ).
                    withSource("/tmp/s3mount0").
                    withTarget("/prototype_root");
            List<Mount> mounts = new LinkedList<Mount>(Collections.singletonList(mnt));
            Map<String,String> lbls = new HashMap<String,String>( );
            lbls.put(  "taskID", task.getTicket().getUUID() );
            //container spec
            ContainerSpec ct = new ContainerSpec().
                    withImage("phdinintegrals/quartus-masters:19.1-wrapper2").
                    withCommand( cmd ).
                    withTty(true).
                    withMounts(mounts).
                    withLabels(lbls);

            //task spec
            TaskSpec tt = new TaskSpec().
                    withContainerSpec(ct).
                    withPlacement(sp).withRestartPolicy(restartPolicy);

            //service spec
            ServiceSpec ss = new ServiceSpec().
                    withTaskTemplate(tt).
                    withName( serviceName ).
                    withNetworks(nets);

            //deploy service
            CreateServiceResponse serviceResponse = dockerClient.createServiceCmd(ss).withServiceSpec(ss).exec();
            String serviceID = serviceResponse.getId();
            Thread.sleep(5000);
            //infer containerID from service (guaranteed to contain service's name)
            String containerID = "";
            List<Task> tasks = dockerClient.listTasksCmd().withServiceFilter(serviceID).exec();

            if( tasks.size() >0 ){
                System.out.println("Container ready.");
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


            //Implement event watcher for new task and register callback
            System.out.println("Register callback. Master ID:"+this.getServiceIdentificator());
            DockerContainerCallback resultCallback = new DockerContainerCallback(this, task.getTicket().getUUID() );
            dockerClient.eventsCmd().
                    withLabelFilter("taskID="+task.getTicket().getUUID()).
                            exec( resultCallback );
            System.out.println("Task deployed");

        } catch( Exception e){
            System.out.println(e.toString() );
            return false;
        }

        return true;
    }


    /**
     * Обновить состояние задачи и загрузить его в S3
     * @param UUID UUID задачи
     */
   public void updateTaskStatus(String UUID) {
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
       dockerClient.removeServiceCmd(serviceID).exec();
       List<String> serviceIDList = new LinkedList<String>(Collections.singleton(serviceID));
       while( !dockerClient.listServicesCmd().withIdFilter(serviceIDList).exec().isEmpty() ){
           System.out.println("Service not killed yet. Waiting.");
           try{
               Thread.sleep(100L);
           } catch( Exception e){
               System.out.println(e.toString() );
           }
       }

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

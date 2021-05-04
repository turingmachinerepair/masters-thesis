package org.thesis.functionary;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import io.minio.MakeBucketArgs;
import jdk.jfr.Enabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import org.thesis.functionary.Kafka.KafkaProducerConfig;
import org.thesis.common.Tickets.CompilationTaskTicket;
import org.thesis.functionary.Tickets.*;
import org.thesis.functionary.Tickets.TaskTicketValidator;

import org.apache.commons.lang3.RandomStringUtils;


/**
 * Static data necessary:
 * 1) List of prototype templates
 * 2) Project compilation digest - resources necessary, time estimate calculation
 */

/**
 * request ( TaskTicket instance )
 *  -> task tickets one per project ( CompilationTaskTicket instances )
 *  -> resolve project paths and names in minio ( Fill CompilationTaskTicket project paths )
 *  -> put tasks in designated minio bucket
 *  -> commit to kafka ( UUID string, no alterations to default  );
 *
 * request -> get task status from storage - return JSON
 *
 * kafka producer - produces messages for task queue
 */
@Enabled
@RestController
public class Functionary {
    String serviceIdentificator;
    String topicName = "TaskFabric";
    MinIOAdapter minioAdapter = new MinIOAdapter();
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public Functionary() {
        minioAdapter.init();
    }


    public void sendMessage(String msg) {

        kafkaTemplate.send(topicName, msg);
    }

    /**
     * REST controller
     */
    private final AtomicLong counter = new AtomicLong();

    @Autowired
    TaskTicketValidator taskValidator = new TaskTicketValidator();

    @PostMapping(path = "/commit_task", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public TaskTicket commitTask(@RequestBody TaskTicket ticket, BindingResult result) {
        serviceIdentificator = RandomStringUtils.randomAlphabetic(10);

        System.out.println("New task request");
        taskValidator.validate(ticket, result);
        ticket.setTaksID(counter.getAndIncrement());
        System.out.println(ticket.toString());

        String[] compilationTaskNames = minioAdapter.resolveProjectTemplate(ticket.getTaskName());
        ExtendedTaskTicket extendedTT = new ExtendedTaskTicket(ticket);

        for (int i = 0; i < compilationTaskNames.length; i++) {
            String projectName = compilationTaskNames[i];
            String projectPath = minioAdapter.resolveProjectPath(ticket.getTaskName(), projectName);
            CompilationTaskTicket compilationTask = new CompilationTaskTicket(
                    serviceIdentificator,
                    counter.getAndIncrement(),
                    projectName,
                    projectPath, 7
            );

            System.out.println("Created " + compilationTask.toString() + " UUID:" + compilationTask.getUUID());
            minioAdapter.putCompilationTaskTicket(compilationTask);
            System.out.println("Put task to minio");
            sendMessage(compilationTask.getUUID());
            extendedTT.associateTask(compilationTask.getUUID());
            System.out.println("Created kafka message");

        }
        minioAdapter.putExtendedTaskTicket(extendedTT);

        if (result.hasErrors()) {
            System.out.println(result.toString());
            throw new IllegalArgumentException("Error in properties");
        } else {
            System.out.println("Task successfully created.");
        }


        return ticket;
        //return new TaskTicket(counter.incrementAndGet(), String.format(template, name));
    }

    @GetMapping(path = "/list_tasks", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object[] listTasks() {
        List<String> res = minioAdapter.listExtendedTaskTickets();
        return res.toArray();

    }

    @PostMapping(path = "/task_status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ExtendedTaskTicketResponse listFullTaskStatus(@RequestBody String taskUUID, BindingResult result) {
        System.out.println("Get task status:"+taskUUID);
        ExtendedTaskTicket taskTicket = minioAdapter.getExtendedTaskTicket(taskUUID);
        HashSet<String> UUIDs = taskTicket.getTaskUUIDs();

        ExtendedTaskTicketResponse res = new ExtendedTaskTicketResponse();
        HashSet<CompilationTaskTicket> tickets = new HashSet<CompilationTaskTicket>();
        res.setMainTask(taskTicket);
        for (String UUID : UUIDs) {
            System.out.println("Get subtask:"+UUID);
            CompilationTaskTicket tt = minioAdapter.getCompilationTaskTicket(UUID);
            tickets.add(tt);
        }

        res.setSubtasks(tickets);
        return res;

    }
}

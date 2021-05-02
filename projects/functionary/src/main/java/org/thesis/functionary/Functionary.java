package org.thesis.functionary;

import java.lang.reflect.Member;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

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
import org.thesis.functionary.Tickets.CompilationTaskTicket;
import org.thesis.functionary.Tickets.TaskTicketValidator;
import org.thesis.functionary.Tickets.TaskTicket;
import org.thesis.functionary.Tickets.TaskTicketValidator;

import javax.xml.ws.Binding;

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
    String topicName = "TaskFabric";
    MinIOAdapter minioAdapter = new MinIOAdapter();
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

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
    public TaskTicket commitTask(@RequestBody TaskTicket ticket, BindingResult result ) {
        System.out.println("New task request");
        taskValidator.validate(ticket, result);
        ticket.setTaksID(counter.getAndIncrement());
        System.out.println( ticket.toString() );

        String[] compilationTaskNames = minioAdapter.resolveProjectTemplate( ticket.getTaskName() );

        for( int i=0;i<compilationTaskNames.length; i++){
            String projectName = compilationTaskNames[i];
            String projectPath = minioAdapter.resolveProjectPath( ticket.getTaskName(), projectName );
            CompilationTaskTicket compilationTask = new CompilationTaskTicket(counter.getAndIncrement(),
                    projectName,
                    projectPath,7
                    );
            System.out.println("Created "+compilationTask.toString() +" UUID:"+compilationTask.getUUID() );
            minioAdapter.putCompilationTaskTicket(compilationTask);
            System.out.println("Put task to minio" );
            sendMessage(compilationTask.getUUID());
            System.out.println("Created kafka message");

        }


        if (result.hasErrors()) {
            System.out.println(result.toString());
            throw new IllegalArgumentException("Error in properties");
        } else {
            System.out.println("Task successfully created.");
        }


        return ticket;
        //return new TaskTicket(counter.incrementAndGet(), String.format(template, name));
    }


}

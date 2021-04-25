package org.thesis.functionary.Functionary;

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

import org.thesis.functionary.Functionary.KafkaProducerConfig;

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

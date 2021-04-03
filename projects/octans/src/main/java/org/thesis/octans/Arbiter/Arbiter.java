package org.thesis.octans.Arbiter;

import java.lang.reflect.Member;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import jdk.jfr.Enabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import javax.xml.ws.Binding;

/**
 * request -> task ticket -> resolve project paths and names in minio -> commit to kafka
 * request -> get task status from storage - return JSON
 * kafka producer - in independent thread - produces messages from task queue
 * kafka consumer - in independent thread - consumes messages about task updates
 * kafka consumer - heartbeat
 */
@Enabled
@RestController
public class Arbiter {

    ConcurrentLinkedQueue pendingTasks = new ConcurrentLinkedQueue();

    /**
     * REST controller
     */
    private final AtomicLong counter = new AtomicLong();

    @Autowired
    TaskTicketValidator taskValidator = new TaskTicketValidator();

    //@GetMapping("/heartbeat")

    @PostMapping(path = "/commit_task", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public TaskTicket commitTask(@RequestBody TaskTicket ticket, BindingResult result ) {
        System.out.println("New task request");
        taskValidator.validate(ticket,result);
        ticket.setTaksID( counter.getAndIncrement() );


        if( result.hasErrors() ){
            System.out.println( result.toString() );
            throw new IllegalArgumentException("Error in properties");
        } else {
            System.out.println("Task successfully created.");
        }
        pendingTasks.add(ticket);

        System.out.print( "Current pending queue dump:" + pendingTasks.toString() );

        return ticket;
        //return new TaskTicket(counter.incrementAndGet(), String.format(template, name));
    }

    /**
     * Kafka heartbeat producer
     */

    /**
     * Kafka heartbeat consumer
     */

    /**
     * Kafka task commit producer
     */

}

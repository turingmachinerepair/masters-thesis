package org.thesis.functionary;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import jdk.jfr.Enabled;
import org.apache.kafka.common.metrics.stats.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import org.thesis.common.Tickets.CompilationTaskTicket;
import org.thesis.functionary.Tickets.*;
import org.thesis.functionary.Tickets.TaskTicketValidator;
import org.thesis.common.Tickets.ExtendedTaskTicketResponse;

import org.apache.commons.lang3.RandomStringUtils;

import org.thesis.common.Tickets.ExtendedTaskTicket;
import org.thesis.common.Tickets.TaskTicket;

import javax.annotation.PostConstruct;

/**
 * Класс REST-контроллер сервиса управления системой
 */
@Enabled
@RestController
public class Functionary {

    /**
     * Строка идентификатор микросервиса
     */
    String serviceIdentificator;

    /**
     * Имя темы в брокере Kafka для публикации UUID задач для компиляции ПЛИС
     */
    String topicName = "TaskFabric";

    @Value("${minio.addr}")
    String MinIOAddr;

    /**
     * Экземпляр адаптера для MinIO
     */
    MinIOAdapter minioAdapter ;

    /**
     * Экземпляр издателя для брокера Kafka, темы TaskFabric
     * Имеет аннотацию @Autowired, создается автоматически фреймворком Spring.
     */
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Конструктор по-умолчанию.
     * Формирует случайный идентификатор сервиса и инициализирует адаптер MinIO.
     */
    public Functionary() {
        serviceIdentificator = RandomStringUtils.randomAlphabetic(10);
    }

    @PostConstruct
    void init(){
        System.out.println("Init minio");
        minioAdapter = new MinIOAdapter(MinIOAddr);
        minioAdapter.init();
        System.out.println("Init finished");
    }

    /**
     * Отправить сообщение брокеру Kafka, тема TaskFabric
     * @param msg сообщение которое надо передавать
     */
    public void sendMessage(String msg) {
        kafkaTemplate.send(topicName, msg);
    }

    /**
     * Счетчик для назначения номеров задач
     */
    private final AtomicLong counter = new AtomicLong();

    /**
     * Экземпляр валидатора входящей JSON строки для создания задачи
     */
    @Autowired
    TaskTicketValidator taskValidator = new TaskTicketValidator();

    private String currentTimestamp(){
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        return timestamp.toString();
    }
    /**
     * Метод, реализующий REST endpoint для создания новой задачи
     * Метод HTTP POST, в теле запроса JSON-строка с данными для задачи
     * @param ticket экземпляр задачи компиляции прототипа сформированный из переданной с запросом JSON-строки
     * @param result результат валидации HTTP-запроса
     * @return экземпляр входящей задачи для валидации что задача принята
     */
    @PostMapping(path = "/commit_task", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public TaskTicket commitTask(@RequestBody TaskTicket ticket, BindingResult result) {

        System.out.println("New task request");
        taskValidator.validate(ticket, result);
        ticket.setTaskId(counter.getAndIncrement());
        System.out.println(ticket.toString());

        String[] compilationTaskNames = minioAdapter.resolveProjectTemplate(ticket.getTaskName());
        ExtendedTaskTicket extendedTT = new ExtendedTaskTicket(ticket);

        System.out.println("#EVENT\tTYPE:CREATE_PROTO\tUUID:"+extendedTT.getUUID()+"\tTIMESTAMP:"+currentTimestamp());

        for (String projectName : compilationTaskNames) {
            String projectPath = minioAdapter.resolveProjectPath(ticket.getTaskName(), projectName);
            CompilationTaskTicket compilationTask = new CompilationTaskTicket(
                    serviceIdentificator,
                    counter.getAndIncrement(),
                    projectName,
                    projectPath, 4
            );

            System.out.println("#EVENT\tTYPE:CREATE_FPGA\tUUID:"+compilationTask.getUUID()+"\tTIMESTAMP:"+currentTimestamp()+"\tASSOC_UUID:"+extendedTT.getUUID());

            System.out.println("Created " + compilationTask.toString() + " UUID:" + compilationTask.getUUID());
            minioAdapter.putCompilationTaskTicket(compilationTask);
            System.out.println("Put task to minio");
            sendMessage(compilationTask.getUUID());
            System.out.println("#EVENT\tTYPE:QUEUE_FPGA\tUUID:"+compilationTask.getUUID());
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

    /**
     * Метод, реализующий REST endpoint для вывода списка задач компиляции
     * Метод HTTP GET
     * @return экземпляр входящей задачи для валидации что задача принята
     */
    @GetMapping(path = "/list_tasks", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object[] listTasks() {
        List<String> res = minioAdapter.listExtendedTaskTickets();
        return res.toArray();

    }

    /**
     * Метод, реализующий REST endpoint для вывода списка задач компиляции
     * Метод HTTP POST
     * @param taskUUID UUID задачи из тела запроса
     * @param result результат валидации HTTP-запроса
     * @return экземпляр расширенной задачи для компиляции прототипа {@link org.thesis.common.Tickets.ExtendedTaskTicket}
     */
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

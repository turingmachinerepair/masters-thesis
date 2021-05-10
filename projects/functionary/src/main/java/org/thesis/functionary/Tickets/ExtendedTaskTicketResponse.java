package org.thesis.functionary.Tickets;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.thesis.common.Tickets.CompilationTaskTicket;

import java.util.HashSet;

/**
 * Класс с экземпляром дескриптора задачи компиляции прототипа {@link org.thesis.functionary.Tickets.TaskTicket} и множество дескрипторов задач компиляции проектов FPGA {@link org.thesis.common.Tickets.CompilationTaskTicket}
 * Для ответа на запрос task_status {@link org.thesis.functionary.Functionary}
 */
@Data
public class ExtendedTaskTicketResponse  {
    ExtendedTaskTicket mainTask;
    HashSet<CompilationTaskTicket> subtasks;

}

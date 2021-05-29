package org.thesis.common.Tickets;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.thesis.common.Tickets.CompilationTaskTicket;

import java.util.HashSet;

/**
 * Класс с экземпляром дескриптора задачи компиляции прототипа {@link org.thesis.common.Tickets.TaskTicket} и множество дескрипторов задач компиляции проектов FPGA {@link org.thesis.common.Tickets.CompilationTaskTicket}
 */
@Data
public class ExtendedTaskTicketResponse  {
    ExtendedTaskTicket mainTask;
    HashSet<CompilationTaskTicket> subtasks;

}

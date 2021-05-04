package org.thesis.functionary.Tickets;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.thesis.common.Tickets.CompilationTaskTicket;

import java.util.HashSet;


@Data
public class ExtendedTaskTicketResponse  {
    ExtendedTaskTicket mainTask;
    HashSet<CompilationTaskTicket> subtasks;

}
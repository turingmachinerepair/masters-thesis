package org.thesis.functionary.Tickets;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;


@Component
public class TaskTicketValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return TaskTicket.class.equals(clazz);
    }

    @Override
    public void validate(Object obj, Errors errors) {
        TaskTicket task = (TaskTicket) obj;
        String name = task.getTaskName();
        //long taskId = task.getTaskId();

        if(StringUtils.isEmpty(name)) errors.rejectValue("name", "Can't be null or Empty");

    }

}

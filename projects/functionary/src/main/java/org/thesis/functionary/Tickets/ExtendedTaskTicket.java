package org.thesis.functionary.Tickets;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;

import java.util.HashSet;


@Data
public class ExtendedTaskTicket extends  TaskTicket implements java.io.Serializable  {

    private HashSet<String> taskUUIDs;

    public ExtendedTaskTicket(){
        taskUUIDs = new HashSet<String>();
    }

    public ExtendedTaskTicket( TaskTicket tt){
        super.setFootnote( tt.getFootnote() );
        super.setTaskId( tt.getTaskId() );
        super.setTaskName( tt.getTaskName() );
        super.setTaskSemanticName( tt.getTaskSemanticName() );
        taskUUIDs = new HashSet<String>();
    }

    public void associateTask(String UUID){
        taskUUIDs.add(UUID);
    }

    public String getUUID(){
        String res = this.getTaskId() + "-" + this.getTaskName() + "-" + taskUUIDs.toString();
        return res;
    }

}

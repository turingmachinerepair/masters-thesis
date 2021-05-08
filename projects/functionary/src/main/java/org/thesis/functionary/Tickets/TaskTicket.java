package org.thesis.functionary.Tickets;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;

@Data
public class TaskTicket implements java.io.Serializable  {

    private long taskId;
    private String taskName;
    private String taskSemanticName;
    private String footnote;

    public TaskTicket(){
        taskId = 0L;
        taskName = "";
        taskSemanticName = "";
        footnote = "";
    }

    public TaskTicket(long _taskId, String _taskName, String _taskSemanticName,String footnote){
        taskId = _taskId;
        taskName = _taskName;
        taskSemanticName = _taskSemanticName;
        this.footnote = footnote;
    }

    public void setTasksID(long _id){
        taskId = _id;
    }

    @JsonSetter
    public void setTaskName(String name){
        if(name == null || name == "") throw new IllegalArgumentException("Error while setting name. Can't be null and empty");
        this.taskName = name;
    }

    @JsonSetter
    public void setTaskSemanticName(String semanticName){
        if(semanticName == null || semanticName == "") throw new IllegalArgumentException("Error while setting semantic name. Can't be null and empty");
        this.taskSemanticName = semanticName;
    }

    @JsonSetter
    public void setFootnote(String fnote){

        footnote = fnote;
    }

    public long getTaskId(){
        return taskId;
    }

    public String getTaskName(){
        return taskName;
    }



}

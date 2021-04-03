import com.fasterxml.jackson.annotation.JsonSetter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;

public class TaskTicket {

    private long taskId;
    private String taskName;
    private String taskSemanticName;

    private String protoProjectName;

    public TaskTicket(long _taskId, String _taskName, String _taskSemanticName){
        taskId = _taskId;
        taskName = _taskName;
        taskSemanticName = _taskSemanticName;
        protoProjectName = "";
    }

    public void setTaksID(long _id){
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


    public void setProtoProjectName(String _protoProjectName){
        protoProjectName = _protoProjectName;
    }

    public long getTaskId(){
        return taskId;
    }

    public String getTaskName(){
        return taskName;
    }



}

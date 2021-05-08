package org.thesis.common.Tickets;

import lombok.Data;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

public @Data
class CompilationTaskTicket implements java.io.Serializable {
    //Desciptive attributes
    String processingServerName;
    long ID;
    String projectName;
    String projectPath;
    boolean[] necessaryStages;
    boolean[] currentStages;
    
    //Result attributes
    boolean error;
    int lastStage;
    
    //State attributes
    STAGES currentStage;
    STATES currentState;
    String hostname;
    int estTime;

     public CompilationTaskTicket(){
         ID = 0L;
         processingServerName="";
         projectName="";
         projectPath="";
         necessaryStages = new boolean[4];
         currentStages = new boolean[4];
         Arrays.fill(necessaryStages,false);
         Arrays.fill(currentStages,false);
         error = false;
         lastStage = 0;
         
         currentStage = STAGES.NAN;
         currentState = STATES.IDLE;
         hostname = "";
         estTime = 0;
     }

    public CompilationTaskTicket(String _processingServerName,long _ID, String _projectName, String _projectPath, int lastNecessaryStage){
        processingServerName = _processingServerName;
        ID = _ID;
        projectName=_projectName;
        projectPath= _projectPath;
        necessaryStages = new boolean[4];
        currentStages = new boolean[4];
        Arrays.fill(necessaryStages,0,lastNecessaryStage,true);
        Arrays.fill(currentStages,false);
        error = false;
        lastStage = 0;
        
         currentStage = STAGES.NAN;
         currentState = STATES.IDLE;
         hostname = "";
         estTime = 0;
    }

     public String getUUID(){
         Long lID = ID;
         String res = processingServerName +"-" + projectName + "-" + lID.toString();
         return res;
     }
     
     public int getNextStageIndex(){
        int taskStage = -1;
        for(int i=0; i< currentStages.length && taskStage == -1; i++){
            System.out.println( "\t" + currentStages[i] + "/"+ necessaryStages[i]);
            if( !currentStages[i] && necessaryStages[i]){
                taskStage = i;
            }
        }
        return taskStage;
     }
     
     public STAGES getNextStage(){
        int taskStage = getNextStageIndex();
        if( taskStage == -1){
            return STAGES.NAN;
        }
        return STAGES.values()[taskStage];
        
     }

}

package org.thesis.common.Tickets;

import lombok.Data;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

public @Data
class CompilationTaskTicket implements java.io.Serializable {
    String processingServerName;
    long ID;
    String projectName;
    String projectPath;
    boolean[] necessaryStages;
    boolean[] currentStages;
    
    boolean error;
    int lastStage;

     public CompilationTaskTicket(){
         ID = 0L;
         processingServerName="";
         projectName="";
         projectPath="";
         necessaryStages = new boolean[8];
         currentStages = new boolean[8];
         Arrays.fill(necessaryStages,false);
         Arrays.fill(currentStages,false);
         error = false;
         lastStage = 0;
     }

    public CompilationTaskTicket(String _processingServerName,long _ID, String _projectName, String _projectPath, int lastNecessaryStage){
        processingServerName = _processingServerName;
        ID = _ID;
        projectName=_projectName;
        projectPath= _projectPath;
        necessaryStages = new boolean[8];
        currentStages = new boolean[8];
        Arrays.fill(necessaryStages,0,lastNecessaryStage,true);
        Arrays.fill(currentStages,false);
        error = false;
        lastStage = 0;
    }

     public String getUUID(){
         Long lID = ID;
         String res = processingServerName +"-" + projectName + "-" + lID.toString();
         return res;
     }

}

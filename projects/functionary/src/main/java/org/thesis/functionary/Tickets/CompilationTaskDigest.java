package org.thesis.functionary.Tickets;

import lombok.Data;

@Data
public class CompilationTaskDigest {
 
    String FPGAType;
    int FPGAUtilization;

    int CPUs;
    int RAM;
    int bogomips;

    public CompilationTaskDigest(){
        FPGAType="";
        FPGAUtilization=0;
        CPUs=4;
        RAM=5;
        bogomips=2500;
    }

    Long getStageEstimate(int stageIndex){
        return 0L;
    }


    
 }

package org.thesis.common.Tickets;

import lombok.Data;
import org.thesis.common.Tickets.STAGES;

@Data
public class CompilationTaskDigest {
 
    String FPGAType;
    int FPGAUtilization;

    int CPUs;
    int RAM;
    int bogomips;

    STAGES stage;

    public CompilationTaskDigest(){
        FPGAType="";
        FPGAUtilization=0;
        CPUs=4;
        RAM=5;
        bogomips=2500;
        stage = STAGES.NAN;
    }

    Long getStageEstimate(int stageIndex){
        return 0L;
    }


    
 }

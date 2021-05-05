package org.thesis.quadomizer.Node;

import lombok.Data;
import org.thesis.common.Tickets.CompilationTaskContext;
import org.thesis.common.Tickets.CompilationTaskDigest;

import java.util.HashSet;

@Data
public class NodeDescriptor{
    String name;
    int RAM;
    int CPU;
    
    int ReservedRAM;
    int ReservedCPU;

    
    public NodeDescriptor(String name,int CPU,int RAM){
        this.name = name;
        this.CPU = CPU;
        this.RAM = RAM;

        ReservedCPU = 0;
        ReservedRAM = 0;
    }
    
    boolean evaluateDeploymentPossibility( CompilationTaskContext task){
        int avalRAM = RAM - ReservedRAM;
        int avalCPU = CPU - ReservedCPU;
        if( avalRAM >= task.getDigest().getRAM() && avalCPU >= task.getDigest().getCPUs() ){
            return true;
        }
        return false;
    }

    void deployTask( CompilationTaskContext task){
        CompilationTaskDigest digest = task.getDigest();
        ReservedCPU += digest.getCPUs();
        ReservedRAM += digest.getRAM();

    }

    void freeTask( CompilationTaskContext task){
        CompilationTaskDigest digest = task.getDigest();
        ReservedCPU -= digest.getCPUs();
        ReservedRAM -= digest.getRAM();
    }
    
}

package org.thesis.quadomizer.Node;

import lombok.Data;
import org.thesis.quadomizer.Node.NodeDescriptor;
import org.thesis.common.Tickets.CompilationTaskContext;
import org.thesis.common.Tickets.CompilationTaskDigest;

import javax.xml.soap.Node;
import java.util.*;

@Data
public class NodeManager{

    List<NodeDescriptor> nodes;
    HashMap<String, Integer> tasks;

    //TODO: Replace with autofill from dockerClient
    public NodeManager(){
        NodeDescriptor node1 = new NodeDescriptor("native-d8", 24,64);
        NodeDescriptor node2 = new NodeDescriptor("var2-vm5",6,20);
        NodeDescriptor node3 = new NodeDescriptor("var2-vm6",6,20);
        nodes = Arrays.asList(node1,node2,node3);
        tasks = new HashMap<String,Integer>();

    }
    
    public boolean evaluateDeploymentPossibility( CompilationTaskContext task){
        ListIterator<NodeDescriptor> it = nodes.listIterator();
        for (NodeDescriptor node : nodes) {
            if( node.evaluateDeploymentPossibility(task) ){
                return true;
            }
        }
        return true;
    }

    public String deployTask( CompilationTaskContext task){
        String nodeName = "";
        int nodeIndex=0;
        for (NodeDescriptor node : nodes) {
            if( node.evaluateDeploymentPossibility(task) ){
                node.deployTask(task);
                tasks.put(task.getTicket().getUUID(), nodeIndex );
                nodeName = node.getName();
            }
            nodeIndex++;
        }

        return nodeName;
    }

    public void freeTask( CompilationTaskContext task){
        int nodeIndex = tasks.get(task.getTicket().getUUID());
        nodes.get(nodeIndex).freeTask(task);
        tasks.remove(task.getTicket().getUUID() );
    }
    
}

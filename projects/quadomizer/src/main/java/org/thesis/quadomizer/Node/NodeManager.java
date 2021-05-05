package org.thesis.quadomizer.Node;

import lombok.Data;
import org.thesis.quadomizer.Node.NodeDescriptor;
import org.thesis.common.Tickets.CompilationTaskContext;
import org.thesis.common.Tickets.CompilationTaskDigest;

import javax.xml.soap.Node;
import java.util.*;

@Data
public class NodeManager{

    boolean opInProgress;
    List<NodeDescriptor> nodes;
    HashMap<String, Integer> tasks;

    //TODO: Replace with autofill from dockerClient
    public NodeManager(){
        NodeDescriptor node1 = new NodeDescriptor("native-d8", 24,64);
        NodeDescriptor node2 = new NodeDescriptor("var2-vm5",6,20);
        NodeDescriptor node3 = new NodeDescriptor("var2-vm6",6,20);
        nodes = Arrays.asList(node1,node2,node3);
        tasks = new HashMap<String,Integer>();
        opInProgress = false;
    }
    
    public boolean evaluateDeploymentPossibility( CompilationTaskContext task){

        ListIterator<NodeDescriptor> it = nodes.listIterator();
        for (NodeDescriptor node : nodes) {
            if( node.evaluateDeploymentPossibility(task) ){
                return true;
            }
        }
        return false;
    }

    public String deployTask( CompilationTaskContext task){
        opInProgress = true;
        System.out.println("Pre-deploy tasks: "+ tasks.toString() );
        System.out.println("Pre-deploy nodes: "+ nodes.toString() );
        String nodeName = "";
        boolean deployed = false;
        int nodeIndex=0;
        ListIterator<NodeDescriptor> it = nodes.listIterator();

        while( it.hasNext() && !deployed){
            NodeDescriptor node = it.next();

            if( node.evaluateDeploymentPossibility(task) ){
                node.deployTask(task);
                tasks.put(task.getTicket().getUUID(), nodeIndex );

                nodeName = node.getName();
                deployed = true;
            }
            
        }

        System.out.println("Post-deploy tasks: "+ tasks.toString() );
        System.out.println("Post-deploy nodes: "+ nodes.toString() );
        opInProgress = false;
        return nodeName;
    }

    public void freeTask( CompilationTaskContext task){
        opInProgress = true;
        System.out.println("Freeing task "+task.getTicket().getUUID() );
        System.out.println("Pre-free tasks: "+ tasks.toString() );
        System.out.println("Pre-free nodes: "+ nodes.toString() );

        int nodeIndex = tasks.get(task.getTicket().getUUID());
        System.out.println("On "+ Integer.toString(nodeIndex) );
        nodes.get(nodeIndex).freeTask(task);
        tasks.remove(task.getTicket().getUUID() );

        System.out.println("Post-free nodes: "+ nodes.toString() );
        System.out.println("Post-free tasks: "+ tasks.toString() );
        opInProgress =false;
    }
    
}

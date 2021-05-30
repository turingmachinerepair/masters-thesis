package org.thesis.quadomizer.Node;

import com.github.dockerjava.api.model.SwarmNode;
import lombok.Data;
import org.thesis.quadomizer.Node.NodeDescriptor;
import org.thesis.common.Tickets.CompilationTaskContext;
import org.thesis.common.Tickets.CompilationTaskDigest;

import javax.xml.soap.Node;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.*;

/**
 * Диспетчер вычислительных ресурсов кластера.
 */
@Data
public class NodeManager{

    /**
     * Флаг блокировки контекста класса
     */
    boolean opInProgress;

    /**
     * Список дескрипторов вычислительных узлов в кластере Swarm
     */
    List<NodeDescriptor> nodes;

    /**
     *
     */
    HashMap<String, Integer> tasks;

    /**
     * Конструктор по-умолчанию
     */
    public NodeManager(){
        nodes = new ArrayList<>();
        tasks = new HashMap<String,Integer>();
        opInProgress = false;
    }

    /**
     * Конструктор из списка узлов Swarm
     * @param nodesList список экземпляров дескрипторов вычислитенльых узлов
     */
    public NodeManager(List<SwarmNode> nodesList ){
        tasks = new HashMap<String,Integer>();
        nodes = new LinkedList<NodeDescriptor>();
        opInProgress = false;

        long nanoCpuDivisor = 1000000000L;
        long RAMDivisor = 1073741824;
        for( SwarmNode node: nodesList){
            try{

                String hostname = node.getDescription().getHostname();
                long nanoCPU = node.getDescription().getResources().getNanoCPUs();
                long ramBytes = node.getDescription().getResources().getMemoryBytes();
                System.out.println( hostname
                        + " " +  nanoCPU
                        + " " +  ramBytes );


                int CPUs = Math.toIntExact( nanoCPU / nanoCpuDivisor );
                System.out.print("+");
                int RAM = Math.toIntExact(ramBytes / RAMDivisor);
                RAM += 1;
                System.out.print("+");
                NodeDescriptor localNode = new NodeDescriptor( hostname, CPUs, RAM );
                System.out.println("New node:" +  localNode.toString() );
                nodes.add(localNode);
            } catch( Exception e) {
                System.out.println(e.toString() );
            }
        }
    }

    /**
     * Оценить возможность выполнения задачи на кластере
     * @param task экземпляр контекста задачи на компиляцию ПЛИС
     * @return возможность выполнения на кластере. true=возможно, false=нет
     * @deprecated
     */
    public boolean evaluateDeploymentPossibility( CompilationTaskContext task){

        ListIterator<NodeDescriptor> it = nodes.listIterator();
        for (NodeDescriptor node : nodes) {
            if( node.evaluateDeploymentPossibility(task) ){
                return true;
            }
        }
        return false;
    }

    /**
     * Оценить возможность выполнения задачи на кластере и если можно - запустить
     * @param task экземпляр полного контекста задачи компиляции ПЛИС
     * @return имя узла на котором запущена задача. Пустая строка если выполнение невозможно.
     */
    public String deployTask( CompilationTaskContext task){
        opInProgress = true;
        System.out.println("Pre-deploy tasks: "+ tasks.toString() );
        System.out.println("Pre-deploy nodes: "+ nodes.toString() );
        String nodeName = "";
        boolean deployed = false;
        int nodeIndex=0;
        ListIterator<NodeDescriptor> it = nodes.listIterator();
        double minRamRatio = 1;

        System.out.println("\tRunning deployment analysis..." );
        while( it.hasNext() && !deployed){
            NodeDescriptor node = it.next();

            if( node.evaluateDeploymentPossibility(task) ){
                float reservedRamRatio = ((float)node.getReservedRAM())/((float)node.getRAM());
                System.out.println("\tRes ratio:" + reservedRamRatio +" Current minimal ratio:"+minRamRatio );
                if( reservedRamRatio < minRamRatio ){
                    deployed = true;
                }
            } else
                nodeIndex++;
        }

        if( deployed ){
            NodeDescriptor node = nodes.get(nodeIndex);

            node.deployTask(task);
            tasks.put(task.getTicket().getUUID(), nodeIndex );
            nodeName = node.getName();
            System.out.println("Can be deployed on "+nodeName );

            System.out.println("#RES_EVENT\tTYPE:RESERVE_RESOURCES\tHOSTNAME:" + node.getName() +
                    "\tCPU:"+node.getReservedCPU() +
                    "\tRAM:" + node.getReservedRAM() +
                    "\tTIMESTAMP:"+  (new Timestamp(System.currentTimeMillis()) ).toString());

        } else {

            System.out.println("No resources to deploy tasks." );
        }
        System.out.println("Post-deploy tasks: "+ tasks.toString() );
        System.out.println("Post-deploy nodes: "+ nodes.toString() );

        opInProgress = false;
        return nodeName;
    }

    /**
     * Освободить зарезервированные для задачи ресурсы на узле
     * @param task контекст задачи ресурсы которой освобождаются
     */
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
        NodeDescriptor node = nodes.get(nodeIndex);
        System.out.println("#RES_EVENT\tTYPE:FREE_RESOURCES\tHOSTNAME:" + node.getName() +
                "\tCPU:"+node.getReservedCPU() +
                "\tRAM:" + node.getReservedRAM() +
                "\tTIMESTAMP:"+  (new Timestamp(System.currentTimeMillis()) ).toString());
        opInProgress =false;
    }
    
}

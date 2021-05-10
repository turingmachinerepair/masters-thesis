package org.thesis.quadomizer.Node;

import lombok.Data;
import org.thesis.common.Tickets.CompilationTaskContext;
import org.thesis.common.Tickets.CompilationTaskDigest;

import java.util.HashSet;

/**
 * Класс - описатель вычислительного узла Swarm
 */
@Data
public class NodeDescriptor{
    /**
     * Имя узла
     */
    String name;

    /**
     * Объем оперативной памяти, в ГБ
     */
    int RAM;
    /**
     * Количество потоков узла
     */
    int CPU;

    /**
     * Количество зарезервированной ОП
     */
    int ReservedRAM;
    /**
     * Количество зарезервированных потоков ЦП
     */
    int ReservedCPU;

    /**
     * Конструктор по значениям
     * @param name имя вычислительного узла
     * @param CPU количество потоков ЦП
     * @param RAM количество ОП, в ГБ
     */
    public NodeDescriptor(String name,int CPU,int RAM){
        this.name = name;
        this.CPU = CPU;
        this.RAM = RAM;

        ReservedCPU = 0;
        ReservedRAM = 0;
    }

    /**
     * Оценить достаточно ли вычислительных ресурсов на узле для выполнения задачи
     * @param task полный контекст задачи
     * @return возможность запуска. true = ресурсов достаточно
     */
    boolean evaluateDeploymentPossibility( CompilationTaskContext task){
        int avalRAM = RAM - ReservedRAM;
        int avalCPU = CPU - ReservedCPU;
        return avalRAM >= task.getDigest().getRAM() && avalCPU >= task.getDigest().getCPUs();
    }

    /**
     * Зарезервировать ресурсы для задачи на узле
     * @param task задача для которой занимаются ресурсы
     */
    void deployTask( CompilationTaskContext task){
        CompilationTaskDigest digest = task.getDigest();
        ReservedCPU += digest.getCPUs();
        ReservedRAM += digest.getRAM();

    }

    /**
     * Освободить ресурсы выделенные для задачи
     * @param task задача ресурсы которой освобождаются
     */
    void freeTask( CompilationTaskContext task){
        CompilationTaskDigest digest = task.getDigest();
        ReservedCPU -= digest.getCPUs();
        ReservedRAM -= digest.getRAM();
    }
    
}

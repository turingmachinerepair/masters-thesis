package org.thesis.common.Tickets;

import lombok.Data;
import org.thesis.common.Tickets.STAGES;

/**
 * Класс описывающий необходимые для выполнения задачи вычислительные ресурсы.
 */
@Data
public class CompilationTaskDigest {
    /**
     * Тип ПЛИС
     */
    String FPGAType;
    /**
     * Процент использования логики ПЛИС.
     * Зарезервировано
     */
    int FPGAUtilization;

    /**
     * Количество потоков для выполнения задачи
     */
    int CPUs;
    /**
     * Количество оперативной памяти для выполнения задачи
     */
    int RAM;
    /**
     * Значение bogomips, зарезервированно
     */
    int bogomips;

    /**
     * Этап задачи компиляции для которого валидна оценка
     */
    STAGES stage;

    /**
     * Конструктор по-умолчанию
     */
    public CompilationTaskDigest(){
        FPGAType="";
        FPGAUtilization=0;
        CPUs=4;
        RAM=5;
        bogomips=2500;
        stage = STAGES.NAN;
    }

    /**
     * Оценить время на выполнение задачи
     * @param stageIndex индекс этапа
     * @return время на выполнение задачи, unix time
     * @deprecated
     */
    Long getStageEstimate(int stageIndex){
        return 0L;
    }


    
 }

package org.thesis.common.Tickets;

import lombok.Data;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

/**
 * Класс описатель задачи компиляции проекта ПЛИС FPGA
 * Реализует интерфейс java.io.Serializable
 */
public @Data
class CompilationTaskTicket implements java.io.Serializable {
    //Desciptive attributes
    /**
     * Имя сервиса, сформировавшего экземпляр
     */
    String processingServerName;

    /**
     * Числовой идентификатор задачи
     */
    long ID;

    /**
     * Имя проекта
     */
    String projectName;

    /**
     * Путь в S3 бакете до проекта
     */
    String projectPath;

    /**
     * Массив флагов этапов компиляции которые нужно выполнить
     * Индекс соответствуют объявлениям в {@link org.thesis.common.Tickets.STAGES}
     */
    boolean[] necessaryStages;

    /**
     * Массив флагов этапов компиляции которые выполнены.
     * Индекс соответствуют объявлениям в {@link org.thesis.common.Tickets.STAGES}
     */
    boolean[] currentStages;
    
    //Result attributes
    /**
     * Флаг признака что последний этап не был завершен.
     * Индекс проваленного этапа можно получить через
     */
    boolean error;
    /**
     * Последний успешно выполненный этап
     */
    int lastStage;
    
    //State attributes
    /**
     * Текущий выполняемый этап
     */
    STAGES currentStage;
    /**
     * Текущее состояние задачи
     */
    STATES currentState;
    /**
     * Имя сервера на котором выполняется (выполнялась) задача
     */
    String hostname;

    /**
     * Оценка времени
     */
    int estTime;

    /**
     * Конструктор по-умолчанию
     */
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

    /**
     * Конструктор из значений
     * @param _processingServerName сервер формирования экземпляра
     * @param _ID числовой идентификатор
     * @param _projectName имя проекта
     * @param _projectPath путь проекта
     * @param lastNecessaryStage последний этап который надо выполнить
     */
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

    /**
     * Получить уникальный идентификатор задачи
     * @return строка UUID
     */
     public String getUUID(){
         long lID = ID;
         return processingServerName +"-" + projectName + "-" + Long.toString(lID);
     }

    /**
     * Получить индекс следующего этапа компиляции проекта.
     * @return индекс следующего этапа компиляции. -1, если задача готова.
     */
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

    /**
     * Получить значение типа STAGES следующего этапа компиляции проекта
     * @return значение перечисление этапов соответствующее следующему этапу компиляции
     */
    public STAGES getNextStage(){
        int taskStage = getNextStageIndex();
        if( taskStage == -1){
            return STAGES.NAN;
        }
        return STAGES.values()[taskStage];
        
     }

}

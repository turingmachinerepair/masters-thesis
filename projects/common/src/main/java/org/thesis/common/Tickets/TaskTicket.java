package org.thesis.common.Tickets;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;

/**
 * Класс описывающий задачу компиляции встроенного ПО прототипа
 * Реализует интерфейс java.io.Serializable для возможности записи и чтения в S3.
 */
@Data
public class TaskTicket implements java.io.Serializable  {

    /**
     * Идентификатор задачи. Целое число, применяется для формирования UUID
     */
    private long taskId;

    /**
     * Имя задачи. Соответствует имени прототипа и имени директории в processor_test, которая содержит проекты ПЛИС для прототипа.
     */
    private String taskName;

    /**
     * Семантическое имя задачи. Для простоты восприятия.
     */
    private String taskSemanticName;

    /**
     * Пользовательское примечание. Для простоты восприятия.
     */
    private String footnote;

    /**
     * Конструктор по-умолчанию
     */
    public TaskTicket(){
        taskId = 0L;
        taskName = "";
        taskSemanticName = "";
        footnote = "";
    }

    /**
     * Конструктор из значений
     * @param _taskId - идентификатор задачи
     * @param _taskName - имя проекта
     * @param _taskSemanticName - семантическое имя проекта
     * @param footnote - пользовательское примечание
     */
    public TaskTicket(long _taskId, String _taskName, String _taskSemanticName,String footnote){
        taskId = _taskId;
        taskName = _taskName;
        taskSemanticName = _taskSemanticName;
        this.footnote = footnote;
    }

    /**
     * Задать имя задачи.
     * Нужен для фреймворка spring boot с аннотацией JsonSetter для автоматического формирования экземпляра класса из json строки.
     * @param name - целевое имя задачи
     */
    @JsonSetter
    public void setTaskName(String name){
        if(name == null || name == "") throw new IllegalArgumentException("Error while setting name. Can't be null and empty");
        this.taskName = name;
    }

    /**
     * Задать семантическое имя задачи.
     * Нужен для фреймворка spring boot с аннотацией JsonSetter для автоматического формирования экземпляра класса из json строки.
     * @param semanticName - целевое семантическое имя задачи
     */
    @JsonSetter
    public void setTaskSemanticName(String semanticName){
        if(semanticName == null || semanticName == "") throw new IllegalArgumentException("Error while setting semantic name. Can't be null and empty");
        this.taskSemanticName = semanticName;
    }

    /**
     * Задать примечание задачи
     * Нужен для фреймворка spring boot с аннотацией JsonSetter для автоматического формирования экземпляра класса из json строки.
     * @param fnote - целевое примечание задачи
     */
    @JsonSetter
    public void setFootnote(String fnote){

        footnote = fnote;
    }

}

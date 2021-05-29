package org.thesis.common.Tickets;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;

import java.util.HashSet;

/**
 * Класс расширенной задачи компиляции прототипа.
 * Содержит в себе исходный экземпляр задачи компиляции и множество UUID задач для компиляции проектов ПЛИС
 */
@Data
public class ExtendedTaskTicket extends  TaskTicket implements java.io.Serializable  {

    /**
     * Множество UUID задач для компиляции проектов ПЛИС, выполняющихся в рамках задачи компиляции прототипа
     */
    private HashSet<String> taskUUIDs;

    /**
     * Конструктор по-умолчанию
     */
    public ExtendedTaskTicket(){
        taskUUIDs = new HashSet<String>();
    }

    /**
     * Конструктор из экземпляра суперкласса
     * @param tt - базовый экземпляр задачи для компиляции прототипа
     */
    public ExtendedTaskTicket( TaskTicket tt){
        super.setFootnote( tt.getFootnote() );
        super.setTaskId( tt.getTaskId() );
        super.setTaskName( tt.getTaskName() );
        super.setTaskSemanticName( tt.getTaskSemanticName() );
        taskUUIDs = new HashSet<String>();
    }

    /**
     * Добавить UUID задачу в множество ассоциированных задач компиляции
     * @param UUID - UUID задачи компиляции FPGA, связанной с текущей задачей компиляции прототипа.
     */
    public void associateTask(String UUID){
        taskUUIDs.add(UUID);
    }

    /**
     * Получить UUID задачи компиляции прототипа
     * @return UUID задачи компиляции прототипа. Формируется из ID задачи, имени задачи и хеша от строки UUID ассоциированных задач
     */
    public String getUUID(){
        return this.getTaskId() + "-" + this.getTaskName() + "-" + String.valueOf( taskUUIDs.toString().hashCode() );
    }

}

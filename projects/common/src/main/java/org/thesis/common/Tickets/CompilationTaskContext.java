package org.thesis.common.Tickets;

import lombok.Data;

/**
 * Класс полного контекста задачи компиляции. Содержит экземпляр задачи и экземпляр оценки ресурсов на её выполнение
 */
@Data
public class CompilationTaskContext{
 
   CompilationTaskDigest digest;
   CompilationTaskTicket ticket;

   public CompilationTaskContext(CompilationTaskTicket ticket,
                                 CompilationTaskDigest digest
                                 ){
       this.digest = digest;
       this.ticket = ticket;
   }
    
 }

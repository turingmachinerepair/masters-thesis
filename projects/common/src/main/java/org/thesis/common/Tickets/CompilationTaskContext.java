package org.thesis.common.Tickets;

import lombok.Data;

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

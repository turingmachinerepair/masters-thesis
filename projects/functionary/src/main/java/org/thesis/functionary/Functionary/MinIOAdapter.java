 

 class MinIOAdapter{
    MinioClient minioClient;
    
    MinIOAdapter(){
        minioClient =    MinioClient.builder()
            .endpoint("http://localhost:9000")
            .credentials("minioadmin", "minioadmin")
            .build();
    }
    
    /**
    *   Resolve path to project from projectTemplateName and projectName
    */
    String resolveProjectPath( String projectTemplateName, String projectName) {
        String bucket = "src";
        
    }
    
    /**
    *   resolve list of project names for prototype name, e.g.
    *   e16c = hmu, xmu, tile, eioh
    *   e2c3 = hmu, uncore, core, eioh
    */
    String[] resolveProjectTemplate( String projectTemplateName){
    
    }
 
    /**
    *   Put CompilationTaskTicket to "tickets" bucket
    */
    String putCompilationTaskTicket( CompilationTaskTicket object){
        /*ObjectWriteResponse res = minioClient.putObject(
                                        PutObjectArgs.builder().bucket("tickets").object("my-objectname").stream(
                                                inputStream, size, -1)
                                            .contentType("video/mp4")
                                            .build());
        return res.getUUID();*/
    }
    
    /**
    *   Get CompilationTaskTicket from "tickets" bucket
    */
    CompilationTaskTicket getCompilationTaskTicket( String UUID){
    
    }
    
 }

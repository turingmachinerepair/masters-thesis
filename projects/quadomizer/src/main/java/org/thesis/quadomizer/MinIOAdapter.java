package org.thesis.quadomizer;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.util.SerializationUtils;
import org.springframework.util.StreamUtils;
import org.thesis.common.Tickets.CompilationTaskTicket;
import sun.misc.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class MinIOAdapter{
    MinioClient minioClient;
    
    MinIOAdapter(){
        minioClient =    MinioClient.builder()
            .endpoint("http://localhost:9000")
            .credentials("minioadmin", "minioadmin")
            .build();
    }
    
    /**
    *   @deprecated
    */
    String resolveProjectPath( String projectTemplateName, String projectName) {
        String bucket = "src";
        String path = bucket +"/firmware/parcing/result/"+projectTemplateName+"/"+projectName;
        return path;
    }
    
    /**
    *  @deprecated
    */
    String[] resolveProjectTemplate( String projectTemplateName){
        String[] strings = {};
        switch( projectTemplateName ){

            case("e16c"):
            case("e12c"): {
                strings = new String[]{
                        "tile", "xmu", "hmu", "xmu_com", "eioh"
                };
                break;
            }
            case("e2c3"):{
                strings = new String[]{
                        "tile", "uncore", "eioh"
                };
            }
            default:{
                strings = new String[]{};
            }
        }

        return strings;
    }
 
    /**
    *   Put CompilationTaskTicket to "tickets" bucket
    */
    String putCompilationTaskTicket( CompilationTaskTicket object){
        String uuid = object.getUUID();

        byte[] data = SerializationUtils.serialize(object);

        // Create a InputStream for object upload.
        ByteArrayInputStream bais = new ByteArrayInputStream( data );

        // Create object 'my-objectname' in 'my-bucketname' with content from the input stream.
        try{
            minioClient.putObject(
                    PutObjectArgs.builder().bucket("tickets").object(uuid).stream(
                            bais, bais.available(), -1)
                            .build());
            bais.close();
            System.out.println(uuid+" is uploaded successfully");
        } catch (Exception e){
            System.out.println( "MinIOClient:putCompilationTaskTicket" + e.toString() );
        }


        return uuid;
    }
    
    /**
    *   Get CompilationTaskTicket from "tickets" bucket
    */
    CompilationTaskTicket getCompilationTaskTicket( String UUID){
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket("tickets")
                        .object(UUID)
                        .build()) ) {
            // Read data from stream
            byte[] data = StreamUtils.copyToByteArray(stream);
            CompilationTaskTicket res = (CompilationTaskTicket) SerializationUtils.deserialize(data);
            return res;
        } catch(Exception e){
            System.out.println(e.toString() );
        }
        return new CompilationTaskTicket();
    }
    
 }

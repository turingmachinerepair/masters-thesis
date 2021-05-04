package org.thesis.functionary;

import io.minio.*;
import io.minio.messages.Item;
import org.springframework.util.SerializationUtils;
import org.springframework.util.StreamUtils;
import org.thesis.common.Tickets.CompilationTaskTicket;
import org.thesis.functionary.Tickets.ExtendedTaskTicket;
import sun.misc.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

class MinIOAdapter{
    MinioClient minioClient;
    
    MinIOAdapter(){
        minioClient =    MinioClient.builder()
            .endpoint("http://localhost:9000")
            .credentials("minioadmin", "minioadmin")
            .build();
    }

    public void init(){
        try{
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket("tickets")
                            .build());
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket("tasks")
                            .build());
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket("src")
                            .build());
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket("projects")
                            .build());
        } catch(Exception e){
            System.out.println(e.toString());
        }

    }
    
    /**
    *   Resolve path to project from projectTemplateName and projectName
    */
    String resolveProjectPath( String projectTemplateName, String projectName) {
        String bucket = "src";
        String path = bucket +"/firmware/parcing/result/"+projectTemplateName+"/"+projectName;
        return path;
    }
    
    /**
    *   resolve list of project names for prototype name, e.g.
    *   e16c = hmu, xmu, tile, eioh, xmu_com
    *   e2c3 = uncore, tile, eioh
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

    String putExtendedTaskTicket(ExtendedTaskTicket obj){
        String uuid = obj.getUUID();

        byte[] data = SerializationUtils.serialize(obj);

        // Create a InputStream for object upload.
        ByteArrayInputStream bais = new ByteArrayInputStream( data );

        // Create object 'my-objectname' in 'my-bucketname' with content from the input stream.
        try{
            minioClient.putObject(
                    PutObjectArgs.builder().bucket("tasks").object(uuid).stream(
                            bais, bais.available(), -1)
                            .build());
            bais.close();
            System.out.println(uuid+" is uploaded successfully");
        } catch (Exception e){
            System.out.println( "MinIOClient:putCompilationTaskTicket" + e.toString() );
        }


        return uuid;
    }

    ExtendedTaskTicket getExtendedTaskTicket( String UUID ){
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket("tasks")
                        .object(UUID)
                        .build()) ) {
            // Read data from stream
            byte[] data = StreamUtils.copyToByteArray(stream);
            ExtendedTaskTicket res = (ExtendedTaskTicket) SerializationUtils.deserialize(data);
            return res;
        } catch(Exception e){
            System.out.println(e.toString() );
        }
        return new ExtendedTaskTicket();
    }

    List<String> listExtendedTaskTickets(){
        LinkedList<String> res = new LinkedList<String>();
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder().bucket("tasks").build());

        for( Result<Item> obj : results){
            try{

                String subres = obj.get().objectName();
                res.add(subres);
            } catch( Exception e) {
                System.out.println(e.toString() );
            }
        }
        return res;
    }

 }

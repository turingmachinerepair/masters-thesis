package org.thesis.functionary;

import io.minio.*;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;
import org.springframework.util.StreamUtils;
import org.thesis.common.Tickets.CompilationTaskTicket;
import org.thesis.common.Tickets.ExtendedTaskTicket;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * Адаптер MinIO, реализующий прикладную логику работы с S3
 */
class MinIOAdapter{
    /**
     * Экземпляр клиента MinIO
     */
    String addr;
    MinioClient minioClient;

    /**
     * Конструктор по-умолчанию.
     */
    MinIOAdapter( String addr){
        this.addr = addr;
    }

    /**
     * Инициализировать S3 бакеты для системы
     */
    public void init(){
        System.out.println("Minio server addr:"+addr +" effective addr:"+"http://"+addr);
        try {
            minioClient = MinioClient.builder()
                    .endpoint("http://" + addr)
                    .credentials("minioadmin", "minioadmin")
                    .build();
        } catch(Exception e){
                System.out.println(e.toString());
        }

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
        } catch(Exception e){
            System.out.println(e.toString());
        }

    }

    /**
     * Сформировать путь до директории проекта компиляции FPGA
     * @param projectTemplateName имя прототипа
     * @param projectName имя проекта FPGA
     * @return путь к директории с проектом FPGA
     */
    String resolveProjectPath( String projectTemplateName, String projectName) {
        return "/processor_test/"+projectTemplateName+"/"+projectName;
    }

    /**
     * Сформировать список проектов ПЛИС FPGA по имени прототипа
     * В директории прототипа (/processor_test/имя прототипа) расположены директории проектов FPGA. Список имен этих директорий надо вернуть.
     * @param projectTemplateName имя прототипа
     * @return список проектов ПЛИС FPGA входящих в проект прототипа
     */
    String[] resolveProjectTemplate( String projectTemplateName){
        ArrayList<String> resPrototype = new ArrayList<>();
        String prefix = "processor_test/" + projectTemplateName+"/";
        System.out.println("Resolve project:" + projectTemplateName +" Prefix:"+prefix);
        ListObjectsArgs args = ListObjectsArgs.builder().bucket("src").prefix(prefix).build();
        System.out.println("Projects detected:");
        Iterable<Result<Item>> results = minioClient.listObjects(args);
        System.out.println(results.toString() );
        for( Result<Item> obj : results){
            try{
                if( obj.get().isDir() ){
                    String subres = obj.get().objectName();
                    String[] folders = subres.split("/");
                    subres = folders[ folders.length - 1 ];

                    resPrototype.add(subres);
                    System.out.println(subres);
                }

            } catch( Exception e) {
                System.out.println(e.toString() );
            }
        }
        String[] res = new String[resPrototype.size()];
        res = resPrototype.toArray(res);

        return res;
    }

    /**
     * Загрузка в S3 бакет tickets экземпляр класса задачи компиляции FPGA
     * @param object - экземпляр который нужно сохранить
     */
    void putCompilationTaskTicket(CompilationTaskTicket object){
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


    }

    /**
     * Получить экземпляр класса задачи компиляции проекта FPGA
     * @param UUID UUID задачи которую надо получить
     * @return экземпляр задачи
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

    /**
     * Загрузка в S3 бакет tasks экземпляр класса задачи компиляции прототипа
     * @param obj - экземпляр который нужно сохранить
     */
    void putExtendedTaskTicket(ExtendedTaskTicket obj){
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


    }

    /**
     * Получить экземпляр класса задачи компиляции прототипа из S3 бакета tasks
     * @param UUID UUID задачи которую надо получить
     * @return экземпляр задачи
     */
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

    /**
     * Получить список задач компиляции прототипов полученных системой
     * @return список UUID задач компиляции прототипов
     */
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

package org.thesis.Declarant;

import com.google.gson.Gson;
import de.vandermeer.asciitable.AsciiTable;
import jdk.nashorn.internal.runtime.JSONFunctions;
import okhttp3.*;
import org.thesis.common.Tickets.CompilationTaskTicket;
import org.thesis.common.Tickets.TaskTicket;
import picocli.CommandLine;

import java.io.IOException;
import java.sql.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.Callable;

import org.thesis.common.Tickets.ExtendedTaskTicketResponse;


@CommandLine.Command(name = "declarant", mixinStandardHelpOptions = true, version = "0.1",
        description = "Отправить запрос серверу \"Функционарий\"")
class Declarant implements Callable<Integer> {

    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    @CommandLine.Option(names = {"-a", "--address"}, description = "Адрес сервера")
    private String IP = "MD5";

        @CommandLine.Option(names = {"-c", "--commit"}, description = "Создать задачу компиляции прототипа. Принимает файл с JSON-объектом описания задачи.")
        private String JSONFile;

        @CommandLine.Option(names = {"-l", "--list"}, description = "Вывести список задач компиляции прототипов в системе.")
        boolean listRequested;

        @CommandLine.Option(names = {"-i", "--inspect"}, description = "Проверить задачу компиляции прототипа. Принимает UUID задачи.")
        private String taskUUID;

    String list() throws IOException {
        String URL = IP+"/list_tasks";
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(URL)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            //get
            String data = response.body().string();
            //deser
            Gson gson = new Gson();
            String[] tt = gson.fromJson(data, String[].class);
		System.out.println(tt.toString());
            //format
            AsciiTable at = new AsciiTable();

            at.addRule();
            for(String UUID:tt){
                at.addRule();
                at.addRow(
                        UUID
                );
            }
            at.addRule();

            return at.render();
        }
    }

    String inspect(String json) throws IOException {
        String URL = IP + "/task_status ";
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(URL)
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            String data = response.body().string();
            Gson gson = new Gson();
            ExtendedTaskTicketResponse tt = gson.fromJson(data, ExtendedTaskTicketResponse.class);
            //format
            AsciiTable at = new AsciiTable();
            HashSet<CompilationTaskTicket> tasks = tt.getSubtasks();
            at.addRule();
            at.addRow(
                    "UUID",
                    "Состояние",
                    "Имя проекта",
                    "Путь",
                    "Выч узел",
                    "Текущий этап",
                    "Следующий этап"
            );
            for(CompilationTaskTicket task:tasks){
                at.addRule();
                at.addRow(
                        task.getUUID(),
                        task.getCurrentState(),
                        task.getProjectName(),
                        task.getProjectName(),
                        task.getHostname(),
                        task.getCurrentStage(),
                        task.getNextStage()
                );
            }
            at.addRule();
            return at.render();
        }
    }

    String commit(String json) throws IOException {
        String URL = IP + "/commit_task";
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(URL)
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }

    @Override
    public Integer call() throws Exception {
        String res = "";
        if(listRequested) {
            res = list();
        } else if ( !JSONFile.isEmpty() ){
            res = commit( JSONFile );
        } else if ( !taskUUID.isEmpty() ){
            res = inspect(taskUUID);
        }

        System.out.println("Результат:\n"+res);
        return 0;
    }

    public static void main(String... args) {
        CommandLine cmd = new CommandLine(new Declarant());

        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }
}

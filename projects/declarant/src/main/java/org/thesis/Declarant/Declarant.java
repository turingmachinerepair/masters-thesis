package org.thesis.Declarant;

import jdk.nashorn.internal.runtime.JSONFunctions;
import okhttp3.*;
import picocli.CommandLine;

import java.io.IOException;
import java.util.concurrent.Callable;


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
            return response.body().string();
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
            return response.body().string();
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

        System.out.printf("Результат: "+res);
        return 0;
    }

    public static void main(String... args) {
        CommandLine cmd = new CommandLine(new Declarant());

        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }
}

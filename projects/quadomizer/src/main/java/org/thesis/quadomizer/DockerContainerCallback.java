package org.thesis.quadomizer;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.core.command.EventsResultCallback;

import java.io.Closeable;
import java.io.IOException;

/**
 * Класс обратного вызова при окончании работы над задачей Swarm
 * Наследник {@link com.github.dockerjava.core.command.EventsResultCallback}
 */

public class DockerContainerCallback extends EventsResultCallback {
    /**
     * Ссылка на экземпляр ядра микросервиса
     */
    Quadomizer masterReference;

    /**
     * UUID задачи состояние которой надо обновить по изменению её статуса
     */
    String UUID;

    public DockerContainerCallback( Quadomizer master, String _UUID){
        UUID = _UUID;
        masterReference = master;

        System.out.println( master.getServiceIdentificator() );
    }

    @Override
    public void onStart(Closeable closeable) {
        System.out.println("Callback triggered close");

        //masterReference.updateTaskStatus(UUID);
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("Callback triggered error");

        masterReference.updateTaskStatus(UUID);
    }

    @Override
    public void onComplete() {
        System.out.println("Callback triggered complete");

        masterReference.updateTaskStatus(UUID);
    }

    @Override
    public void close() throws IOException {
        System.out.println("Callback triggered close");
        masterReference.updateTaskStatus(UUID);
    }
}

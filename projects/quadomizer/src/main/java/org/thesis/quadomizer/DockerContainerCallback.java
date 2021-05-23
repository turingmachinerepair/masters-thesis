package org.thesis.quadomizer;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.EventType;
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

        System.out.println( "Created callback. Master ID:" + master.getServiceIdentificator() + " task ID:" + UUID );
    }

    @Override
    public void onStart(Closeable closeable) {
        System.out.println("Callback "+UUID+" triggered start");
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("Callback "+UUID+" triggered error");
        masterReference.updateTaskStatus(UUID);
    }


    @Override
    public void onNext(Event item) {
        System.out.println("Callback "+UUID+" triggered onNext. Action:" + item.getAction());
        if(item.getType() == EventType.CONTAINER && item.getAction().equals("die")){
            masterReference.updateTaskStatus(UUID);
        }
    }


    @Override
    public void onComplete() {
        System.out.println("Callback "+UUID+" triggered complete");
        masterReference.updateTaskStatus(UUID);
    }

    @Override
    public void close() throws IOException {
        System.out.println("Callback "+UUID+" triggered close");
        masterReference.updateTaskStatus(UUID);
    }
}

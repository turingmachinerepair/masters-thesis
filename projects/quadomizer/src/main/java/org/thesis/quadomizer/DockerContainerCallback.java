package org.thesis.quadomizer;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.WaitContainerResultCallback;

import java.io.Closeable;
import java.io.IOException;


public class DockerContainerCallback implements ResultCallback {
    Quadomizer masterReference;
    String UUID;

    public DockerContainerCallback( Quadomizer master, String _UUID){
        UUID = _UUID;
        masterReference = master;

        System.out.println( master.getServiceIdentificator() );
    }

    @Override
    public void onStart(Closeable closeable) {

    }

    @Override
    public void onNext(Object o) {

    }

    @Override
    public void onError(Throwable throwable) {

        masterReference.updateTaskStatus(UUID);
    }

    @Override
    public void onComplete() {

        masterReference.updateTaskStatus(UUID);
    }

    @Override
    public void close() throws IOException {

    }
}

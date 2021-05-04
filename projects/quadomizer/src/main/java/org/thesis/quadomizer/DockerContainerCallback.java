package org.thesis.quadomizer;

import com.github.dockerjava.api.command.WaitContainerResultCallback;

public class DockerContainerCallback extends WaitContainerResultCallback {
    Quadomizer masterReference;
    String UUID;

    public DockerContainerCallback( Quadomizer master, String _UUID){
        UUID = _UUID;
        masterReference = master;

        System.out.println( master.getServiceIdentificator() );
    }

    @Override
    public void onComplete() {
        masterReference.updateTaskStatus(UUID);
    }

}

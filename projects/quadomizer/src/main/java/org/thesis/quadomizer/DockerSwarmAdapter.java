package org.thesis.quadomizer;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.util.SerializationUtils;
import org.springframework.util.StreamUtils;
import org.thesis.common.Tickets.CompilationTaskDigest;
import org.thesis.common.Tickets.CompilationTaskTicket;
import sun.misc.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @deprecated
 */
public class DockerSwarmAdapter{

    public boolean deployJob(CompilationTaskTicket task, CompilationTaskDigest taskDigest, String nodeConstraint){

     return false;
    };

    public CompilationTaskTicket endedJob(){

     return new CompilationTaskTicket();
    }

}

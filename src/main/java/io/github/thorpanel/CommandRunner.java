// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
package io.github.thorpanel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Drain output concurrently so a stuck system command cannot hold the service forever. */
final class CommandRunner {
    private static final int MAX_OUTPUT=1024*1024;
    static String run(long timeoutMs,String... args) throws Exception {
        Process process=new ProcessBuilder(args).redirectErrorStream(true).start();
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        AtomicReference<IOException> readFailure=new AtomicReference<>();
        Thread reader=new Thread(()->{
            try(InputStream input=process.getInputStream()){
                byte[] buffer=new byte[4096];int count;
                while((count=input.read(buffer))!=-1){
                    int keep=Math.min(count,MAX_OUTPUT-output.size());
                    if(keep>0)output.write(buffer,0,keep);
                }
            }catch(IOException e){readFailure.set(e);}
        },"Thor-command-output");
        reader.setDaemon(true);reader.start();
        long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        try{
            if(!process.waitFor(timeoutMs,TimeUnit.MILLISECONDS))throw new IOException("系统输入服务响应超时，请重试");
            long remaining=deadline-System.nanoTime();
            if(remaining>0)reader.join(Math.max(1,TimeUnit.NANOSECONDS.toMillis(remaining)));
            if(reader.isAlive())throw new IOException("读取系统输入状态超时，请重试");
            if(readFailure.get()!=null)throw readFailure.get();
            String result=output.toString("UTF-8");
            if(process.exitValue()!=0)throw new IOException(result.trim());
            return result;
        }finally{
            // Also terminate on interruption. Never join an unresponsive reader
            // without a deadline (a child may have inherited the output pipe).
            if(process.isAlive())process.destroyForcibly();
        }
    }
}

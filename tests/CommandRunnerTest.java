// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
package io.github.thorpanel;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public final class CommandRunnerTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static String[] child(String mode){return new String[]{System.getProperty("java.home")+"/bin/java","-cp",System.getProperty("java.class.path"),CommandRunnerTest.class.getName(),mode};}
    public static void main(String[] args) throws Exception {
        if(args.length>0){
            switch(args[0]){
                case "ok":System.out.print("输入服务 ready");return;
                case "error":System.err.print("service unavailable");System.exit(7);return;
                case "large":for(int i=0;i<1200;i++)System.out.print("x".repeat(1024));return;
                case "sleep":Thread.sleep(30000);return;
            }
        }
        check("输入服务 ready".equals(CommandRunner.run(3000,child("ok"))),"UTF-8 output survives");
        try{CommandRunner.run(3000,child("error"));throw new AssertionError("exit failure lost");}
        catch(IOException expected){check(expected.getMessage().contains("service unavailable"),"stderr explains failure");}
        check(CommandRunner.run(3000,child("large")).length()==1024*1024,"large output drains without blocking and stays bounded");
        long start=System.nanoTime();
        try{CommandRunner.run(250,child("sleep"));throw new AssertionError("hung command accepted");}
        catch(IOException expected){check(expected.getMessage().contains("超时"),"timeout has actionable error");}
        check(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start)<2500,"hung command cannot occupy the service indefinitely");
        check("输入服务 ready".equals(CommandRunner.run(3000,child("ok"))),"a timed-out command does not block the next one");
        java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
        Thread interrupted=new Thread(()->{
            try{CommandRunner.run(30000,child("sleep"));throw new AssertionError("interruption lost");}
            catch(InterruptedException expected){}catch(Exception e){throw new AssertionError(e);}
        });
        interrupted.setUncaughtExceptionHandler((t,e)->failure.set(e));
        interrupted.start();Thread.sleep(100);interrupted.interrupt();interrupted.join(2000);
        check(!interrupted.isAlive(),"interruption must promptly terminate waiting");
        check(failure.get()==null,"interruption must propagate without being swallowed");
        System.out.println("Passed "+checks+" system-command checks");
    }
}

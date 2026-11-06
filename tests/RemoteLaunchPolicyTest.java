package io.github.thorpanel;
import java.util.Set;
public final class RemoteLaunchPolicyTest {
 static int checks;
 static void eq(Object want,Object got){checks++;if(!java.util.Objects.equals(want,got))throw new AssertionError(want+" != "+got);}
 public static void main(String[] args){
  RemoteLaunchPolicy p=new RemoteLaunchPolicy();
  eq(false,p.enabled());eq(null,p.sample("uu",true));
  p.configure(Set.of("uu","moon"));eq(true,p.enabled());eq("uu",p.sample("uu",true));
  eq(null,p.sample("uu",true));eq(null,p.sample(null,true));eq(null,p.sample("uu",true));
  eq(null,p.sample("launcher",false));eq(null,p.sample("uu",true));
  eq(null,p.sample("launcher",true));eq("uu",p.sample("uu",true));
  eq("moon",p.sample("moon",true));eq(null,p.sample("moon",true));
  p.configure(Set.of("uu","moon"));eq(null,p.sample("moon",true));
  p.configure(Set.of());eq(false,p.enabled());eq(null,p.sample("uu",true));
  p.configure(Set.of("uu"));eq("uu",p.sample("uu",true));
  eq(null,p.sample("home",true));eq(null,p.sample("uu",false));eq("uu",p.sample("uu",true));
  System.out.println("Passed "+checks+" remote foreground checks");
 }
}

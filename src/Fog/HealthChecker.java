package Fog;

import Config.ProjectProperties;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class HealthChecker {
    public static void main(String[] args){
        boolean fallo = false;
        try (ZContext context = new ZContext()){
            ZMQ.Socket requesterProxyPrincipal = context.createSocket(SocketType.REQ);
            requesterProxyPrincipal.connect(ProjectProperties.proxyHC);
            requesterProxyPrincipal.setReceiveTimeOut(1000);
            ZMQ.Socket requesterAuxProxy = context.createSocket(SocketType.REQ);
            requesterAuxProxy.connect(ProjectProperties.auxProxyHC);
            requesterAuxProxy.setReceiveTimeOut(1000);

            while(!Thread.currentThread().isInterrupted()){
                String reply = null;
                try{
                    requesterProxyPrincipal.send("PING");
                    reply = requesterProxyPrincipal.recvStr();
                } catch (Exception ignored) {

                }
                if (reply==null && !fallo){
                    fallo = true;
                    System.out.println("Proxy Principal no responde, procediendo a cambiar a proxy de respaldo...");
                    requesterAuxProxy.send("Fallo detectado");
                    if (requesterAuxProxy.recvStr()==null){
                        System.out.println("Proxy auxiliar no responde, falla total");
                        Thread.currentThread().interrupt();
                    }else{
                        System.out.println("Proxy auxiliar puesto en funcionamiento");
                    }
                }
                else if (reply!=null && fallo){
                    System.out.println("Proxy Principal ha vuelto a funcionar, procediendo a desactivar el auxiliar...");
                    requesterAuxProxy.send("Proxy principal recuperado");
                }
            }
        }
    }
}

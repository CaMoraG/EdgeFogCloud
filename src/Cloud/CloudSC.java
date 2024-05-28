package Cloud;

import Config.ProjectProperties;
import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class CloudSC {
    public static void main(String[] args) {
        try (ZContext context = new ZContext()) {
            ZMQ.Socket receiver = context.createSocket(SocketType.REP);
            receiver.bind(ProjectProperties.cloudSCIp);
            System.out.println("Sistema de Calidad capa Cloud esperando alertas...");

            while (!Thread.currentThread().isInterrupted()){
                String jsonString = receiver.recvStr();

                JSONObject message = new JSONObject(jsonString);
                String tipoAlerta = message.getString("TipoAlerta");
                String mensajeAlerta = message.getString("Cuerpo");

                System.out.println("Tipo de alerta: "+tipoAlerta+ ", "+mensajeAlerta);
                receiver.send("OK");
            }
        }
    }
}

package Cloud;

import Config.ProjectProperties;
import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class CloudSC {
    private static int numMensajesRecibidos = 0;
    public static void main(String[] args) {
        configurarTerminacion();
        try (ZContext context = new ZContext()) {
            ZMQ.Socket receiver = context.createSocket(SocketType.REP);
            receiver.bind(ProjectProperties.cloudSCIp);
            System.out.println("Sistema de Calidad capa Cloud esperando alertas...");

            while (!Thread.currentThread().isInterrupted()){
                String jsonString = receiver.recvStr();
                numMensajesRecibidos++;

                JSONObject message = new JSONObject(jsonString);
                String tipoAlerta = message.getString("TipoAlerta");
                String mensajeAlerta = message.getString("Cuerpo");

                System.out.println("Tipo de alerta: "+tipoAlerta+ ", "+mensajeAlerta);
                receiver.send("OK");
            }
        }catch (Exception ignored) {

        }finally {
            System.out.println("Total de mensajes procesados por el SC de la capa cloud: "+numMensajesRecibidos);
        }
    }
    private static void configurarTerminacion(){
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Total de mensajes procesados por el SC de la capa cloud: "+numMensajesRecibidos);
        }));
    }
}

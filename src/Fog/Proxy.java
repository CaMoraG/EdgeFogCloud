package Fog;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class Proxy {
    public static void main(String[] args) {
        try (ZContext context = new ZContext()) {
            // Socket to receive messages on
            ZMQ.Socket receiver = context.createSocket(SocketType.PULL);
            receiver.bind("tcp://*:12345");
            System.out.println("Proxy esperando mensajes de la capa edge...");

            while (!Thread.currentThread().isInterrupted()) {
                // Receive message as a JSON string
                String jsonString = receiver.recvStr(0);

                // Parse the JSON string into a JSON object
                JSONObject message = new JSONObject(jsonString);

                // Extract data from the JSON object
                int id = message.getInt("Id");
                String tipoSensor = message.getString("TipoSensor");
                double medicion = message.getDouble("Medicion");
                String fecha = message.getString("Fecha");

                // Process the received data
                System.out.println("Se recibió del sensor de "+tipoSensor+" "+Integer.toString(id)+" la medición: "+Double.toString(medicion)+" en la fecha"+fecha );
            }
        }
    }
}

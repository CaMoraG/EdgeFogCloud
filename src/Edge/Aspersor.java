package Edge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Aspersor {
    private ServerSocket serverSocket;
    public Aspersor(int puerto) {
        try {
            serverSocket = new ServerSocket(puerto);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void run() {
        System.out.println("El aspersor está listo para recibir alertas.");
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String idSensor = reader.readLine();
                System.out.println("Alerta recibida del Sensor de Humo " + idSensor);
                System.out.println("Aspersores encendidos");
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

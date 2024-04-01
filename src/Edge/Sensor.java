package Edge;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.Scanner;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.json.JSONObject;

public class Sensor extends Thread{
    private int id;
    private String tipoSensor;
    private String archConfig;

    private final double minTemp = 11.0;
    private final double maxTemp = 29.4;
    private double minHumedad = 0.7;
    private double maxHumedad = 1.0;
    private int timeHumo = 3;
    private int timeTemp = 6;
    private int timeHumedad = 5;
    private String ipAspersor = "localhost";
    private int puertoAspersor = 12346;

    public Sensor(int id, String tipoSensor, String archConfig){
        this.id=id;
        this.tipoSensor=tipoSensor;
        this.archConfig=archConfig;
    }

    public void run(){
        try (ZContext context = new ZContext()){
            File archivo = new File(archConfig);
            Scanner scanner = new Scanner(archivo);
            double probDentro = Double.parseDouble(scanner.nextLine());
            double probFuera = Double.parseDouble(scanner.nextLine());
            double probError = Double.parseDouble(scanner.nextLine());
            scanner.close();

            ZMQ.Socket sender = context.createSocket(SocketType.PUSH);
            sender.connect("tcp://localhost:12345");

            while (!Thread.currentThread().isInterrupted()) {
                if (tipoSensor.equals("Humo")) {
                    SensorHumo(probDentro, probFuera, probError, sender);
                } else if (tipoSensor.equals("Temperatura")) {
                    SensorTemperatura(probDentro, probFuera, probError, sender);
                } else {
                    SensorHumedad(probDentro, probFuera, probError, sender);
                }
            }


        } catch (FileNotFoundException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }



    public void SensorTemperatura(double probDentro, double probFuera, double probError, ZMQ.Socket sender) throws InterruptedException {
        Random random = new Random();
        Random randMedicion = new Random();
        double prob, medicion;
        while(true){
            prob = random.nextDouble();

            if(prob<probDentro){
                medicion = randMedicion.nextDouble() * minTemp+(maxTemp-minTemp);
            }else if(prob<probDentro+probFuera){
                medicion = randomConExcepciones(0, 100, minTemp, maxTemp);
            }else{
                medicion = -100.0 + (99.0) * randMedicion.nextDouble();
            }
            EnviarMensaje(medicion, sender);
            System.out.println("Sensor de "+tipoSensor+" "+Integer.toString(id)+" midió "+Double.toString(medicion));
            Thread.sleep(timeTemp * 1000);
        }

    }

    public void SensorHumedad(double probDentro, double probFuera, double probError, ZMQ.Socket sender) throws InterruptedException {
        Random random = new Random();
        Random randMedicion = new Random();
        double prob, medicion;
        while(true){
            prob = random.nextDouble();

            if(prob<probDentro){
                medicion = randMedicion.nextDouble() * minHumedad+(maxHumedad-minHumedad);
            }else if(prob<probDentro+probFuera){
                medicion = randomConExcepciones(0, 200, minHumedad, maxHumedad);
            }else{
                medicion = -100.0 + (99.0) * randMedicion.nextDouble();
            }
            EnviarMensaje(medicion, sender);
            System.out.println("Sensor de "+tipoSensor+" "+Integer.toString(id)+" midió "+Double.toString(medicion));
            Thread.sleep(timeHumedad * 1000);
        }
    }

    public void SensorHumo(double probDentro, double probFuera, double probError, ZMQ.Socket sender) throws InterruptedException {
        Random random = new Random();
        Random randMedicion = new Random();
        double prob;
        int medicion;
        String valor;
        while(true){
            prob = random.nextDouble();

            if(prob<probDentro){
                medicion = random.nextInt(2);
            }else if(prob<probDentro+probFuera){
                medicion = random.nextInt(2);
            }else{
                medicion = 2;
            }
            if(medicion==0){
                valor = "Falso";
            }else if (medicion==1){
                valor = "Verdadero";
                AlertarAspersor(id);
            }else{
                valor = "Error";
            }
            EnviarMensaje(medicion, sender);
            System.out.println("Sensor de "+tipoSensor+" "+Integer.toString(id)+" midió "+ valor);
            Thread.sleep(timeHumo * 1000);
        }

    }

    private void EnviarMensaje(double medicion, ZMQ.Socket sender){
        JSONObject mensaje = new JSONObject();
        mensaje.put("Id", id);
        mensaje.put("TipoSensor", tipoSensor);
        mensaje.put("Medicion", medicion);
        LocalDateTime fechaHoraActual = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS");
        String fechaHoraFormateada = fechaHoraActual.format(formatter);
        mensaje.put("Fecha", fechaHoraFormateada);
        sender.send(mensaje.toString());
    }

    private void AlertarAspersor(int id){
        try {
            Socket socket = new Socket(ipAspersor, puertoAspersor);
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(Integer.toString(id).getBytes());
            outputStream.flush();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double randomConExcepciones(double min, double max, double excMin, double excMax){
        Random random = new Random();
        double medicion;

        do {
            medicion = min + (max - min) * random.nextDouble();
        } while (medicion >= excMin && medicion <= excMax);

        return medicion;
    }
}

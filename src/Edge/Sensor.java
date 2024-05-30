package Edge;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.Scanner;

import Config.ProjectProperties;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.json.JSONObject;


public class Sensor extends Thread{
    private int id;
    private String tipoSensor;
    private String archConfig;

    private final double minTemp = ProjectProperties.temperatureMin;
    private final double maxTemp = ProjectProperties.temperatureMax;
    private double minHumedad = ProjectProperties.humedadMin;
    private double maxHumedad = ProjectProperties.humedadMax;
    private int timeHumo = ProjectProperties.timeHumo;
    private int timeTemp = ProjectProperties.timeTemp;
    private int timeHumedad = ProjectProperties.timeHumedad;
    private String ipAspersor = "localhost";
    private int puertoAspersor = 12346;
    private String proxyIp = ProjectProperties.proxyIp;
    private String auxProxyIp = ProjectProperties.auxProxyIp;

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

            ZMQ.Socket senderProxyPrincipalProxyPrincipal = context.createSocket(SocketType.PUSH);
            senderProxyPrincipalProxyPrincipal.connect(proxyIp);
            ZMQ.Socket senderAuxProxy = context.createSocket(SocketType.PUSH);
            senderAuxProxy.connect(auxProxyIp);
            ZMQ.Socket requester = context.createSocket(SocketType.REQ);
            requester.connect(ProjectProperties.edgeSCIp);

            while (!Thread.currentThread().isInterrupted()) {
                if (tipoSensor.equals("Humo")) {
                    SensorHumo(probDentro, probFuera, probError, senderProxyPrincipalProxyPrincipal, requester, senderAuxProxy);
                } else if (tipoSensor.equals("Temperatura")) {
                    SensorTemperatura(probDentro, probFuera, probError, senderProxyPrincipalProxyPrincipal, requester, senderAuxProxy);
                } else {
                    SensorHumedad(probDentro, probFuera, probError, senderProxyPrincipalProxyPrincipal, requester, senderAuxProxy);
                }
            }


        } catch (FileNotFoundException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }



    public void SensorTemperatura(double probDentro, double probFuera, double probError, ZMQ.Socket senderProxyPrincipal, ZMQ.Socket requester, ZMQ.Socket senderAuxProxy) throws InterruptedException {
        Random random = new Random();
        Random randMedicion = new Random();
        double prob, medicion;
        boolean alerta = false;
        while(!Thread.currentThread().isInterrupted()){
            alerta = false;
            prob = random.nextDouble();

            if(prob<probDentro){
                medicion = minTemp+(maxTemp-minTemp) * randMedicion.nextDouble();
            }else if(prob<probDentro+probFuera){
                medicion = randomConExcepciones(0, 100, minTemp, maxTemp);
                alerta = true;
            }else{
                medicion = -1;
            }
            BigDecimal bd = new BigDecimal(medicion);
            bd = bd.setScale(2, RoundingMode.HALF_UP);
            medicion = bd.doubleValue();
            if (alerta){EnviarAlerta(senderProxyPrincipal, requester, medicion, senderAuxProxy);}
            EnviarMensaje(medicion, senderProxyPrincipal, senderAuxProxy);
            System.out.println("Sensor de "+tipoSensor+" "+Integer.toString(id)+" midió "+Double.toString(medicion));
            Thread.sleep(timeTemp * 1000);
        }

    }

    public void SensorHumedad(double probDentro, double probFuera, double probError, ZMQ.Socket senderProxyPrincipal, ZMQ.Socket requester, ZMQ.Socket senderAuxProxy) throws InterruptedException {
        Random random = new Random();
        Random randMedicion = new Random();
        double prob, medicion;
        boolean alerta = false;
        while(!Thread.currentThread().isInterrupted()){
            alerta = false;
            prob = random.nextDouble();

            if(prob<probDentro){
                medicion = minHumedad+(maxHumedad-minHumedad) * randMedicion.nextDouble() ;
            }else if(prob<probDentro+probFuera){
                medicion = randomConExcepciones(0, 200, minHumedad, maxHumedad);
                alerta = true;
            }else{
                medicion = -1;
            }
            BigDecimal bd = new BigDecimal(medicion);
            bd = bd.setScale(2, RoundingMode.HALF_UP);
            medicion = bd.doubleValue();
            if (alerta){EnviarAlerta(senderProxyPrincipal, requester, medicion, senderAuxProxy);}
            EnviarMensaje(medicion, senderProxyPrincipal, senderAuxProxy);
            System.out.println("Sensor de "+tipoSensor+" "+Integer.toString(id)+" midió "+Double.toString(medicion));
            Thread.sleep(timeHumedad * 1000);
        }
    }

    public void SensorHumo(double probDentro, double probFuera, double probError, ZMQ.Socket senderProxyPrincipal, ZMQ.Socket requester, ZMQ.Socket senderAuxProxy) throws InterruptedException {
        Random random = new Random();
        double prob;
        int medicion;
        String valor;
        while(!Thread.currentThread().isInterrupted()){
            prob = random.nextDouble();

            if(prob<probDentro){
                medicion = 0;
            }else if(prob<probDentro+probFuera){
                medicion = 1;
            }else{
                medicion = -1;
            }
            if(medicion==0){
                valor = "Falso";
            }else if (medicion==1){
                valor = "Verdadero";
                AlertarAspersor(id);
                EnviarAlerta(senderProxyPrincipal, requester, medicion, senderAuxProxy);
            }else{
                valor = "Error";
            }
            EnviarMensaje(medicion, senderProxyPrincipal, senderAuxProxy);
            System.out.println("Sensor de "+tipoSensor+" "+Integer.toString(id)+" midió "+ valor);
            Thread.sleep(timeHumo * 1000);
        }

    }

    private void EnviarMensaje(double medicion, ZMQ.Socket senderProxyPrincipal, ZMQ.Socket senderAuxProxy){
        JSONObject mensaje = new JSONObject();
        mensaje.put("TipoMensaje", "Medicion");
        mensaje.put("Id", id);
        mensaje.put("TipoSensor", tipoSensor);
        mensaje.put("Medicion", medicion);
        LocalDateTime fechaHoraActual = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS");
        String fechaHoraFormateada = fechaHoraActual.format(formatter);
        mensaje.put("Fecha", fechaHoraFormateada);
        senderProxyPrincipal.send(mensaje.toString());
        senderAuxProxy.send(mensaje.toString());
    }

    private void EnviarAlerta(ZMQ.Socket senderProxyPrincipal, ZMQ.Socket requester, double medicion, ZMQ.Socket senderAuxProxy){
        LocalDateTime fechaHoraActual = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS");
        String fecha = fechaHoraActual.format(formatter);
        String tipoAlerta = "";
        String cuerpoMensaje = "";
        if (this.tipoSensor.equals("Humedad")){
            tipoAlerta = "Humedad fuera del rango";
            cuerpoMensaje = "Humedad del "+medicion+"% registrada el "+fecha;
        }else if (this.tipoSensor.equals("Temperatura")){
            tipoAlerta = "Temperatura fuera del rango";
            cuerpoMensaje = "Temperatura de "+medicion+"° registrada el "+fecha;
        }
        else{
            tipoAlerta = "Señal de Humo";
            cuerpoMensaje = "Señal detectada el "+fecha;
        }
        JSONObject mensaje = new JSONObject();
        mensaje.put("TipoAlerta", tipoAlerta);
        mensaje.put("Medicion", medicion);
        mensaje.put("Fecha", fecha);
        mensaje.put("Cuerpo", cuerpoMensaje);
        requester.send(mensaje.toString());
        requester.recvStr();

        mensaje.put("TipoMensaje", "Alerta");
        senderProxyPrincipal.send(mensaje.toString());
        senderAuxProxy.send(mensaje.toString());
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

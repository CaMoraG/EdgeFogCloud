package Fog;

import Config.ProjectProperties;
import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicInteger;

public class Proxy {
    private static AtomicInteger temperaturaMsjsRecibidos = new AtomicInteger();
    private static double medicionesTemperatura = 0;
    private static String fechaTemperaturas = "";
    private static AtomicInteger humedadMsjsRecibidos = new AtomicInteger();
    private static double medicionesHumedad = 0;
    private static String fechaHumedad = "";
    private static int numMensajesRecibidos = 0;
    private static int numMensajesEnviados = 0;
    public static void main(String[] args) {
        configurarTerminacion();
        temperaturaMsjsRecibidos.set(0);
        humedadMsjsRecibidos.set(0);
        try (ZContext context = new ZContext()) {
            hiloHumedad(context);
            hiloTemperatura(context);
            hiloHealthChecker(context);
            // Socket to receive messages on
            ZMQ.Socket receiver = context.createSocket(SocketType.PULL);
            receiver.bind(ProjectProperties.proxyIp);
            ZMQ.Socket requester = context.createSocket(SocketType.REQ);
            requester.connect(ProjectProperties.CloudIp);
            System.out.println("Proxy esperando mensajes de la capa edge...");

            while (!Thread.currentThread().isInterrupted()) {
                // Receive message as a JSON string
                String jsonString = receiver.recvStr(0);
                numMensajesRecibidos++;

                // Parse the JSON string into a JSON object
                JSONObject message = new JSONObject(jsonString);
                String tipoMensaje = message.getString("TipoMensaje");

                if (tipoMensaje.equals("Medicion")){
                    // Extract data from the JSON object
                    int id = message.getInt("Id");
                    String tipoSensor = message.getString("TipoSensor");
                    double medicion = message.getDouble("Medicion");
                    String fecha = message.getString("Fecha");

                    // Process the received data
                    System.out.println("Se recibió del sensor de "+tipoSensor+" "+Integer.toString(id)+" la medición: "+Double.toString(medicion)+" en la fecha "+fecha );
                    TratarMensaje(tipoSensor, medicion, fecha, jsonString, requester);
                }else{
                    requester.send(agregarTiempo(jsonString));
                    requester.recvStr();
                    setNumMensajesEnviados(getNumMensajesEnviados()+1);
                }
            }
        }
    }

    private static void TratarMensaje(String tipoSensor, double medicion, String fecha, String msjOriginal, ZMQ.Socket requester){
        if (medicion >= 0){
            if (tipoSensor.equals("Temperatura")){
                if (medicion<ProjectProperties.temperatureMin){medicion = ProjectProperties.temperatureMin;}
                else if (medicion > ProjectProperties.temperatureMax){medicion = ProjectProperties.temperatureMax;}
                setMedicionesTemperatura(getMedicionesTemperatura()+medicion);
                temperaturaMsjsRecibidos.set(temperaturaMsjsRecibidos.get()+1);
                setFechaTemperaturas(fecha);
            }
            else if (tipoSensor.equals("Humedad")){
                if (medicion<ProjectProperties.humedadMin){medicion = ProjectProperties.humedadMin;}
                else if (medicion > ProjectProperties.humedadMax){medicion = ProjectProperties.humedadMax;}
                setMedicionesHumedad(getMedicionesHumedad()+medicion);
                humedadMsjsRecibidos.set(humedadMsjsRecibidos.get()+1);
                setFechaHumedad(fecha);
            }
            requester.send(agregarTiempo(msjOriginal));
            requester.recvStr();
            setNumMensajesEnviados(getNumMensajesEnviados()+1);
        }
    }

    private static String agregarTiempo(String jsonString){
        JSONObject message = new JSONObject(jsonString);
        message.put("TiempoEnvio", System.nanoTime());
        return message.toString();
    }

    private static void hiloTemperatura(ZContext context){
        ZMQ.Socket requesterCloud = context.createSocket(SocketType.REQ);
        requesterCloud.connect(ProjectProperties.CloudIp);
        ZMQ.Socket requesterSC = context.createSocket(SocketType.REQ);
        requesterSC.connect(ProjectProperties.fogSCIp);
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run(){
                while(!Thread.currentThread().isInterrupted()){
                    monitorearTemperatura(requesterCloud, requesterSC);
                    try {
                        Thread.sleep(ProjectProperties.timeTemp*1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    private static void monitorearTemperatura(ZMQ.Socket requesterCloud, ZMQ.Socket requesterSC){
        int cantidadMensajes = temperaturaMsjsRecibidos.get();
        double mediciones = getMedicionesTemperatura();
        String fecha = getFechaTemperaturas();
        if (cantidadMensajes != 0){
            double tempRelativa = mediciones/cantidadMensajes;
            temperaturaMsjsRecibidos.set(0);
            setMedicionesTemperatura(0);

            BigDecimal bd = new BigDecimal(tempRelativa);
            bd = bd.setScale(2, RoundingMode.HALF_UP);
            tempRelativa = bd.doubleValue();
            String mensajeAlerta = "Temperatura Promedio en la fecha "+fecha+ " fue de "+tempRelativa+"°C";
            System.out.println(mensajeAlerta);

            if (tempRelativa > ProjectProperties.temperatureMax){
                JSONObject mensaje = new JSONObject();
                mensaje.put("TipoMensaje", "Alerta");
                mensaje.put("TipoAlerta", "Temperatura promedio alta");
                mensaje.put("Medicion", tempRelativa);
                mensaje.put("Fecha", fecha);
                mensaje.put("Cuerpo", mensajeAlerta);
                requesterSC.send(mensaje.toString());
                requesterSC.recvStr();
                setNumMensajesEnviados(getNumMensajesEnviados()+1);

                requesterCloud.send(agregarTiempo(mensaje.toString()));
                requesterCloud.recvStr();
                setNumMensajesEnviados(getNumMensajesEnviados()+1);
            }
        }
    }

    private static void hiloHumedad(ZContext context){
        ZMQ.Socket requester = context.createSocket(SocketType.REQ);
        requester.connect(ProjectProperties.CloudIp);
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run(){
                while(!Thread.currentThread().isInterrupted()){
                    monitorearHumedad(requester);
                    try {
                        Thread.sleep(ProjectProperties.timeHumedad*1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    private static void monitorearHumedad(ZMQ.Socket requester){
        int cantidadMensajes = humedadMsjsRecibidos.get();
        double mediciones = getMedicionesHumedad();
        String fecha = getFechaHumedad();
        if (cantidadMensajes != 0){
            double humedadRelativa = mediciones/cantidadMensajes;
            humedadMsjsRecibidos.set(0);
            setMedicionesHumedad(0);
            BigDecimal bd = new BigDecimal(humedadRelativa);
            bd = bd.setScale(2, RoundingMode.HALF_UP);
            humedadRelativa = bd.doubleValue();

            JSONObject mensaje = new JSONObject();
            mensaje.put("TipoMensaje", "MedicionHumedad");
            mensaje.put("Medicion", humedadRelativa);
            mensaje.put("Fecha", fecha);
            requester.send(agregarTiempo(mensaje.toString()));
            requester.recvStr();
            setNumMensajesEnviados(getNumMensajesEnviados()+1);
        }
    }

    private static void hiloHealthChecker(ZContext context){
        ZMQ.Socket replierHealthChecker = context.createSocket(SocketType.REP);
        replierHealthChecker.bind(ProjectProperties.proxyHC);
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run(){
                while(!Thread.currentThread().isInterrupted()){
                    replierHealthChecker.recvStr();
                    replierHealthChecker.send("OK");
                }
            }
        });
        thread.start();
    }

    private static void configurarTerminacion(){
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Total de mensajes procesados por el Proxy: "+numMensajesRecibidos);
            System.out.println("Total de mensajes enviados por el Proxy: "+numMensajesEnviados);
        }));
    }

    private static synchronized void setMedicionesTemperatura(double nuevoValor){
        medicionesTemperatura = nuevoValor;
    }
    private static synchronized double getMedicionesTemperatura(){
        return medicionesTemperatura;
    }
    private static synchronized void setMedicionesHumedad(double nuevoValor){
        medicionesHumedad = nuevoValor;
    }
    private static synchronized double getMedicionesHumedad(){
        return medicionesHumedad;
    }

    public static synchronized String getFechaTemperaturas() {
        return fechaTemperaturas;
    }

    public static synchronized void setFechaTemperaturas(String nuevaFecha) {
        fechaTemperaturas = nuevaFecha;
    }

    public static synchronized String getFechaHumedad() {
        return fechaHumedad;
    }

    public static synchronized void setFechaHumedad(String nuevaFecha) {
        fechaHumedad = nuevaFecha;
    }
    public static synchronized int getNumMensajesEnviados() {
        return numMensajesEnviados;
    }

    public static synchronized void setNumMensajesEnviados(int nuevoValor) {
        numMensajesEnviados = nuevoValor;
    }
}

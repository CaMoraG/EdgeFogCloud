package Cloud;

import Config.ProjectProperties;
import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicInteger;

public class CloudData {
    private static AtomicInteger humedadDiasRecibidos = new AtomicInteger();
    private static double medicionesHumedad = 0;
    private static String fechaHumedad = "";

    public static void main(String[] args) {
        humedadDiasRecibidos.set(0);
        try (ZContext context = new ZContext()) {
            hiloHumedad(context);
            // Socket to receive messages on
            ZMQ.Socket receiver = context.createSocket(SocketType.REP);
            receiver.bind(ProjectProperties.CloudIp);
            System.out.println("Proxy esperando mensajes de la capa proxy...");

            while (!Thread.currentThread().isInterrupted()) {
                // Receive message as a JSON string
                String jsonString = receiver.recvStr();

                // Parse the JSON string into a JSON object
                JSONObject message = new JSONObject(jsonString);

                TratarMensaje(message);
                receiver.send("OK");
            }
        }
    }

    private static void TratarMensaje(JSONObject mensaje){
        String tipoMensaje = mensaje.getString("TipoMensaje");

        if (tipoMensaje.equals("MedicionHumedad")){
            humedadDiasRecibidos.set(humedadDiasRecibidos.get()+1);
            setMedicionesHumedad(getMedicionesHumedad()+mensaje.getDouble("Medicion"));
            setFechaHumedad(mensaje.getString("Fecha"));
        }else if (tipoMensaje.equals("Medicion")){
            TratarMedicion(mensaje);
        }else{
            TratarAlerta(mensaje);
        }
    }

    private static void TratarMedicion(JSONObject mensaje){
        String tipoSensor = mensaje.getString("TipoSensor");
        Double medicion = mensaje.getDouble("Medicion");
        String fecha = mensaje.getString("Fecha");

        String nombreArchivo = getArchivo(tipoSensor);

        try{
            registrarMensaje(nombreArchivo, "", fecha, medicion);
        }catch (Exception e){
            System.out.println("Error al escribir en el archivo "+nombreArchivo+" para el sensor "+tipoSensor);
        }
    }

    private static void TratarAlerta(JSONObject mensaje){
        String tipoAlerta = mensaje.getString("TipoAlerta");
        Double medicion = mensaje.getDouble("Medicion");
        String fecha = mensaje.getString("Fecha");
        String cuerpoMensaje = mensaje.getString("Cuerpo");

        String nombreArchivo = getArchivo(tipoAlerta);

        try{
            registrarMensaje(nombreArchivo, cuerpoMensaje, fecha, medicion);
        }catch (Exception e){
            System.out.println("Error al escribir en el archivo "+nombreArchivo+" para la alerta "+tipoAlerta);
        }
    }

    private static String getArchivo(String criterio){
        return "src/Cloud/"+ switch (criterio) {
            case "Humedad fuera del rango" -> "AlertasHumedad.txt";
            case "Temperatura fuera del rango" -> "AlertasTemperatura.txt";
            case "Señal de Humo" -> "AlertasHumo.txt";
            case "Temperatura promedio alta" -> "AlertasTemperaturaPromedio.txt";
            case "Humedad" -> "MedicionesHumedad.txt";
            case "Temperatura" -> "MedicionesTemperatura.txt";
            case "Humo" -> "MedicionesHumo.txt";
            default -> "";
        };
    }

    private static void registrarMensaje(String nombreArchivo, String cuerpoMensaje, String fecha, double medicion) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(nombreArchivo, true))) {
            writer.write(fecha+","+medicion);
            if(!cuerpoMensaje.isEmpty()){
                writer.write(","+cuerpoMensaje);
            }
            writer.newLine();
        }
    }

    private static void hiloHumedad(ZContext context){
        ZMQ.Socket requester = context.createSocket(SocketType.REQ);
        requester.connect(ProjectProperties.cloudSCIp);
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
        int n = ProjectProperties.diasHumedadCalculoMes;
        int diasRecibidos = humedadDiasRecibidos.get();
        double mediciones = getMedicionesHumedad();
        if (diasRecibidos == n){
            double humedadRelativa = mediciones/n;
            BigDecimal bd = new BigDecimal(humedadRelativa);
            bd = bd.setScale(2, RoundingMode.HALF_UP);
            humedadRelativa = bd.doubleValue();

            System.out.println("Humedad Relativa Mensual fue de "+humedadRelativa+"%");
            if (humedadRelativa < ProjectProperties.humedadMin){
                JSONObject mensaje = new JSONObject();
                mensaje.put("TipoAlerta", "Humedad mensual baja");
                mensaje.put("Medicion", humedadRelativa);
                mensaje.put("Fecha", getFechaHumedad());
                mensaje.put("Cuerpo", "Humedad mensual relativa media bajo a "+humedadRelativa+"%");
                requester.send(mensaje.toString());
                requester.recvStr();
            }
            humedadDiasRecibidos.set(0);
            setMedicionesHumedad(0);
        }
    }
    private static synchronized void setMedicionesHumedad(double nuevoValor){
        medicionesHumedad = nuevoValor;
    }
    private static synchronized double getMedicionesHumedad(){
        return medicionesHumedad;
    }
    private static synchronized void setFechaHumedad(String nuevaFecha){
        fechaHumedad = nuevaFecha;
    }
    private static synchronized String getFechaHumedad(){
        return fechaHumedad;
    }
}

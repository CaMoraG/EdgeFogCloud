package Edge;

public class Sensores {
    private static int instHumo = 10;
    private static int instTemp = 10;
    private static int instHumedad = 10;
    private static String archHumo = "src/Edge/ArchivosConfiguracion/ArchConfigHumedad.txt";
    private static String archTemp = "src/Edge/ArchivosConfiguracion/ArchConfigTemperatura.txt";
    private static String archHumedad = "src/Edge/ArchivosConfiguracion/ArchConfigHumedad.txt";

    public static void main(String[] args){
        Aspersor aspersor = new Aspersor(12346);
        int i;
        for(i=0;i<instHumo;i++){
            Sensor sensor = new Sensor(i+1,"Humo", archHumo);
            sensor.start();
        }
        for(i=0;i<instTemp;i++){
            Sensor sensor = new Sensor(i+1,"Temperatura", archTemp);
            sensor.start();
        }
        for(i=0;i<instHumedad;i++){
            Sensor sensor = new Sensor(i+1,"Humedad", archHumedad);
            sensor.start();
        }
        aspersor.run();

    }
}

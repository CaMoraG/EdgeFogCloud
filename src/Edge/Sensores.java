package Edge;

import Config.ProjectProperties;

public class Sensores {
    private static String archHumo = "src/Edge/ArchivosConfiguracion/ArchConfigHumedad.txt";
    private static String archTemp = "src/Edge/ArchivosConfiguracion/ArchConfigTemperatura.txt";
    private static String archHumedad = "src/Edge/ArchivosConfiguracion/ArchConfigHumedad.txt";

    public static void main(String[] args){
        Aspersor aspersor = new Aspersor(12346);
        int i;
        for(i=0; i< ProjectProperties.numSensoresHumo; i++){
            Sensor sensor = new Sensor(i+1,"Humo", archHumo);
            sensor.start();
        }
        for(i=0;i<ProjectProperties.numSensoresTemperatura;i++){
            Sensor sensor = new Sensor(i+1,"Temperatura", archTemp);
            sensor.start();
        }
        for(i=0;i<ProjectProperties.numSensoresHumedad;i++){
            Sensor sensor = new Sensor(i+1,"Humedad", archHumedad);
            sensor.start();
        }
        aspersor.run();

    }
}

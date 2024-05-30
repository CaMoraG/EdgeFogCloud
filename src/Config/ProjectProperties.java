package Config;

public class ProjectProperties {
    public static final double temperatureMin = 11.0;
    public static final double temperatureMax = 29.4;
    public static final double humedadMin = 70;
    public static final double humedadMax = 100;
    public static final int timeHumo = 3;
    public static final int timeTemp = 6;
    public static final int timeHumedad = 5;
    public static final int numSensoresTemperatura = 10;
    public static final int numSensoresHumo = 10;
    public static final int numSensoresHumedad = 10;
    public static final String ipAspersor = "localhost";
    public static final int puertoAspersor = 12345;
    public static final String edgeSCIp = "tcp://localhost:11111";
    public static final String proxyIp = "tcp://localhost:22222";
    public static final String auxProxyIp = "tcp://localhost:22223";
    public static final String fogSCIp = "tcp://localhost:22224";
    public static final String proxyHC = "tcp://localhost:22225";
    public static final String auxProxyHC = "tcp://localhost:22226";
    public static final String CloudIp = "tcp://localhost:33333";
    public static final String cloudSCIp = "tcp://localhost:33334";
    public static final int diasHumedadCalculoMes = 4;
}

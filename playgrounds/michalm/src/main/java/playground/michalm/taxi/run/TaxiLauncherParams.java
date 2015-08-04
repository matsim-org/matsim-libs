package playground.michalm.taxi.run;

import java.io.File;
import java.util.Map;

import playground.michalm.util.ParameterFileReader;


class TaxiLauncherParams
{

    static TaxiLauncherParams readParams(String paramFile)
    {
        Map<String, String> params = ParameterFileReader.readParametersToMap(paramFile);
        String dir = new File(paramFile).getParent() + '/';
        params.put("inputDir", dir);
        params.put("outputDir", dir);
        return new TaxiLauncherParams(params);
    }


    static TaxiLauncherParams readParams(String paramFile, String inputDir, String outputDir)
    {
        Map<String, String> params = ParameterFileReader.readParametersToMap(paramFile);
        params.put("inputDir", inputDir);
        params.put("outputDir", outputDir);
        return new TaxiLauncherParams(params);
    }


    private Map<String, String> params;

    String inputDir;

    String netFile;
    String plansFile;

    String taxiCustomersFile;
    String taxisFile;
    String ranksFile;
    
    String zonesXmlFile;
    String zonesShpFile;

    String eventsFile;
    String changeEventsFile;
    AlgorithmConfig algorithmConfig;

    Integer nearestRequestsLimit;
    Integer nearestVehiclesLimit;

    Boolean onlineVehicleTracker;
    Boolean advanceRequestSubmission;
    Boolean destinationKnown;
    Boolean vehicleDiversion;

    Double pickupDuration;
    Double dropoffDuration;

    Boolean otfVis;

    String outputDir;
    String vrpOutDir;
    String histogramOutDir;
    String eventsOutFile;

    public static final String INPUT_DIR = "inputDir";
    public static final String NET_FILE = "netFile";
    public static final String PLANS_FILE = "plansFile";

    public static final String TAXI_CUSTOMERS_FILE = "taxiCustomersFile";


    TaxiLauncherParams(Map<String, String> params)
    {
        this.params = params;

        inputDir = params.get(INPUT_DIR);
        netFile = getInputPath(NET_FILE);
        plansFile = getInputPath(PLANS_FILE);

        taxiCustomersFile = getInputPath(TAXI_CUSTOMERS_FILE);
        ranksFile = getInputPath("ranksFile");
        taxisFile = getInputPath("taxisFile");
        
        zonesXmlFile = getInputPath("zonesXmlFile");
        zonesShpFile = getInputPath("zonesShpFile");

        eventsFile = getInputPath("eventsFile");
        changeEventsFile = getInputPath("changeEventsFile");

        algorithmConfig = AlgorithmConfig.valueOf(params.get("algorithmConfig"));

        nearestRequestsLimit = getInteger("nearestRequestsLimit");
        nearestVehiclesLimit = getInteger("nearestVehiclesLimit");

        onlineVehicleTracker = getBoolean("onlineVehicleTracker");
        advanceRequestSubmission = getBoolean("advanceRequestSubmission");
        destinationKnown = getBoolean("destinationKnown");
        vehicleDiversion = getBoolean("vehicleDiversion");

        if (vehicleDiversion && !onlineVehicleTracker) {
            throw new IllegalArgumentException("Diversion requires online tracking");
        }

        pickupDuration = getDouble("pickupDuration");
        dropoffDuration = getDouble("dropoffDuration");

        otfVis = getBoolean("otfVis");

        outputDir = params.get("outputDir");
        vrpOutDir = getOutputPath("vrpOutDir");
        histogramOutDir = getOutputPath("histogramOutDir");
        eventsOutFile = getOutputPath("eventsOutFile");
    }


    private String getInputPath(String key)
    {
        return getPath(inputDir, key);
    }


    private String getOutputPath(String key)
    {
        return getPath(outputDir, key);
    }


    private String getPath(String dir, String key)
    {
        String file = params.get(key);
        return file == null ? null : dir + file;
    }


    private Boolean getBoolean(String key)
    {
        return params.containsKey(key);
    }


    private Integer getInteger(String key)
    {
        return Integer.valueOf(params.get(key));
    }


    private Double getDouble(String key)
    {
        return Double.valueOf(params.get(key));
    }
}
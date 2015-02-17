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
    
    String eventsFile;
    String changeEventsFile;
    AlgorithmConfig algorithmConfig;

    Integer nearestRequestsLimit;
    Integer nearestVehiclesLimit;

    Boolean onlineVehicleTracker;
    Boolean advanceRequestSubmission;
    Boolean destinationKnown;
    
    Double pickupDuration;
    Double dropoffDuration;
    
    Boolean otfVis;
    
    String outputDir;
    String vrpOutDir;
    String histogramOutDir;
    String eventsOutFile;


    TaxiLauncherParams(Map<String, String> params)
    {
        this.params = params;

        inputDir = params.get("inputDir");
        netFile = getInputPath("netFile");
        plansFile = getInputPath("plansFile");

        taxiCustomersFile = getInputPath("taxiCustomersFile");
        ranksFile = getInputPath("ranksFile");
        taxisFile = getInputPath("taxisFile");

        eventsFile = getInputPath("eventsFile");
        changeEventsFile = getInputPath("changeEventsFile");

        algorithmConfig = AlgorithmConfig.valueOf(params.get("algorithmConfig"));

        nearestRequestsLimit = getInteger("nearestRequestsLimit");
        nearestVehiclesLimit = getInteger("nearestVehiclesLimit");

        onlineVehicleTracker = getBoolean("onlineVehicleTracker");
        advanceRequestSubmission = getBoolean("advanceRequestSubmission");

        destinationKnown = getBoolean("destinationKnown");
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
        String fileName = params.get(key);
        return fileName == null ? null : dir + fileName;
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
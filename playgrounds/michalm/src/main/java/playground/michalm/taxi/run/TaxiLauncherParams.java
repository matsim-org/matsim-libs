package playground.michalm.taxi.run;

import java.io.File;
import java.util.Map;

import pl.poznan.put.util.ParameterFileReader;


class TaxiLauncherParams
{
    static TaxiLauncherParams readParams(String paramFile)
    {
        Map<String, String> params = ParameterFileReader.readParametersToMap(new File(paramFile));
        params.put("dir", new File(paramFile).getParent() + '/');
        return new TaxiLauncherParams(params);
    }

    Map<String, String> params;
    
    String dir;
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
    boolean otfVis;
    String vrpOutDir;
    String histogramOutDir;
    String eventsOutFile;


    TaxiLauncherParams(Map<String, String> params)
    {
        this.params = params;
        
        dir = params.get("dir");
        netFile = getFilePath("netFile");
        plansFile = getFilePath("plansFile");

        taxiCustomersFile = getFilePath("taxiCustomersFile");
        ranksFile = getFilePath("ranksFile");
        taxisFile = getFilePath("taxisFile");

        eventsFile = getFilePath("eventsFile");
        changeEventsFile = getFilePath("changeEventsFile");

        algorithmConfig = AlgorithmConfig.valueOf(params.get("algorithmConfig"));

        nearestRequestsLimit = getInteger("nearestRequestsLimit");
        nearestVehiclesLimit = getInteger("nearestVehiclesLimit");

        onlineVehicleTracker = getBoolean("onlineVehicleTracker");
        advanceRequestSubmission = getBoolean("advanceRequestSubmission");

        destinationKnown = getBoolean("destinationKnown");
        pickupDuration = getDouble("pickupDuration");
        dropoffDuration = getDouble("dropoffDuration");

        otfVis = getBoolean("otfVis");

        vrpOutDir = getFilePath("vrpOutDir");
        histogramOutDir = getFilePath("histogramOutDir");
        eventsOutFile = getFilePath("eventsOutFile");
    }


    private String getFilePath(String key)
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
package playground.lsieber.networkshapecutter;

import java.io.File;
import java.io.IOException;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import playground.clruch.ScenarioOptions;
import playground.clruch.data.LocationSpec;
import playground.clruch.utils.PropertiesExt;

public class PrepSettings {
    
    /* Directories and Paths */ 
    public final File workingDirectory;
    public final File configFile;
    
    public final String VIRTUALNETWORKFOLDERNAME;
    public final String VIRTUALNETWORKFILENAME;
    public final String MINIMUMFLEETSIZEFILENAME;
    public final String TRAVELDATAFILENAME;
    public final String PERFORMANCEFLEETSIZEFILENAME;
    
    public String NETWORKUPDATEDNAME = "networkConverted";
    public String POPULATIONUPDATEDNAME = "populationConverted";
    
    /* Booleans */
    public final boolean populationeliminateWalking;
    public final boolean populationeliminateFreight;
    public final boolean populationchangeModeToAV;
    public final boolean completeGraph;
    public final boolean calculatePerformanceFleetSize;
    public final boolean centerNetwork;
    
    public final int numVirtualNodes;
    public final int maxPopulationSize;
    public final int dtTravelData;
    
    public final LocationSpec locationSpec;
    
    public PrepSettings() throws IOException {
        workingDirectory = MultiFileTools.getWorkingDirectory();
        PropertiesExt simOptions = PropertiesExt.wrap(ScenarioOptions.load(workingDirectory));

        configFile = new File(workingDirectory, simOptions.getString("fullConfig"));
        System.out.println("loading config file to get data " + configFile.getAbsoluteFile());

        populationeliminateFreight = simOptions.getBoolean("populationeliminateFreight");
        populationeliminateWalking = simOptions.getBoolean("populationeliminateWalking");
        populationchangeModeToAV = simOptions.getBoolean("populationchangeModeToAV");
        locationSpec = simOptions.getLocationSpec();
        numVirtualNodes = simOptions.getInt("numVirtualNodes");
        completeGraph = simOptions.getBoolean("completeGraph");
        maxPopulationSize = simOptions.getInt("maxPopulationSize");
        dtTravelData = simOptions.getInt("dtTravelData");
        calculatePerformanceFleetSize = simOptions.getBoolean("calculatePerformanceFleetSize");
        centerNetwork = simOptions.getBoolean("centerNetwork");
        VIRTUALNETWORKFOLDERNAME = simOptions.getString("virtualNetworkDir");
        VIRTUALNETWORKFILENAME = simOptions.getString("virtualNetworkName");
        TRAVELDATAFILENAME = simOptions.getString("travelDataName");
        MINIMUMFLEETSIZEFILENAME = simOptions.getString("minimumFleetSizeFileName");
        PERFORMANCEFLEETSIZEFILENAME = simOptions.getString("performanceFleetSizeFileName");
        
        NETWORKUPDATEDNAME = simOptions.getString("NetworkUpdateName");
        POPULATIONUPDATEDNAME = simOptions.getString("PopulationUpdateName");
    }

}

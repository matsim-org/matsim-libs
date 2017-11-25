package playground.lsieber.networkshapecutter;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import playground.clruch.ScenarioOptions;
import playground.clruch.data.LocationSpec;
import playground.clruch.utils.PropertiesExt;

public class PrepSettings {

    private final PropertiesExt simOptions;

    /* Directories and Paths */
    public final File workingDirectory;
    public final File configFile;
    public final File preparedScenarioDirectory;

    public final String VIRTUALNETWORKFOLDERNAME;
    public final String VIRTUALNETWORKFILENAME;
    public final String MINIMUMFLEETSIZEFILENAME;
    public final String TRAVELDATAFILENAME;
    public final String PERFORMANCEFLEETSIZEFILENAME;

    // public final File shapefile = null;

    public String NETWORKUPDATEDNAME;
    public String POPULATIONUPDATEDNAME;

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
    public final LinkModes modes;
    public final boolean networkCleaner;

    public PrepSettings() throws IOException {
        workingDirectory = MultiFileTools.getWorkingDirectory();
        simOptions = PropertiesExt.wrap(ScenarioOptions.load(workingDirectory));
        preparedScenarioDirectory = new File(workingDirectory,simOptions.getString("preparedScenarioDirectory", "preparedScenario"));
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

        NETWORKUPDATEDNAME = simOptions.getString("NetworkUpdateName", "networkConverted");
        POPULATIONUPDATEDNAME = simOptions.getString("PopulationUpdateName", "populationConverted");

        // Cutting Attribtes
        // shapefile = new File(simOptions.getString("shapefile"));

        modes = new LinkModes(simOptions.getString("modes", "ALLMODES"));

        networkCleaner = simOptions.getBoolean("networkCleaner", true);
    }

    public File getFile(String string) {
        return new File(workingDirectory, simOptions.getString(string));
    }

    public NetworkCutters createNetworkCutter() {
        return NetworkCutters.valueOf(simOptions.getString("networkCutter"));
    }

    public PopulationCutters createPopulationCutter() {
        return PopulationCutters.valueOf(simOptions.getString("populationCutter"));
    }

    public VirtualNetworkCreators createVirtualNetworkCreator() {
        return VirtualNetworkCreators.valueOf(simOptions.getString("virtualNetworkCreator"));
    }
    // TODO @Lukas create function which checks if the propertie value exists and returns an error otherwhise

}

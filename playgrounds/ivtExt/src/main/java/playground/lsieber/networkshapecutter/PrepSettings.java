package playground.lsieber.networkshapecutter;

import java.io.File;
import java.io.IOException;

import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import playground.clruch.ScenarioOptions;
import playground.clruch.data.LocationSpec;
import playground.clruch.data.ReferenceFrame;
import playground.clruch.utils.PropertiesExt;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;

public class PrepSettings {
    private final PropertiesExt simOptions;

    /* Directories and Paths */
    public final File workingDirectory;
    public final File configFileName;
    public final Config config;
    public final File preparedScenarioDirectory;
    public final String preparedConfigName;

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

    public final boolean waitForClients;
    public final ReferenceFrame referenceFrame;
    public final LocationSpec locationSpec;
    public final LinkModes modes;
    public final boolean networkCleaner;
    
    public enum SettingsType {Server, Preparer};

    public PrepSettings(SettingsType settingsType) throws IOException {
        workingDirectory = MultiFileTools.getWorkingDirectory();
        simOptions = PropertiesExt.wrap(ScenarioOptions.load(workingDirectory));
        preparedScenarioDirectory = new File(workingDirectory, simOptions.getString("preparedScenarioDirectory", "preparedScenario"));
        configFileName = new File(workingDirectory, simOptions.getString("fullConfig"));
        preparedConfigName = simOptions.getString("preparedConfigName", "prepared_config.xml");
        if (SettingsType.Server == settingsType) {
            DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
            dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);
            config = ConfigUtils.loadConfig(preparedConfigName.toString(), new AVConfigGroup(), dvrpConfigGroup);
        }else if (SettingsType.Preparer == settingsType) {
            config = ConfigUtils.loadConfig(configFileName.toString());
        }else {
            System.out.println("ERROR, Something went Wrong with the Loading of the Config Files");
            config = ConfigUtils.loadConfig(configFileName.toString());
        }
        System.out.println("loading config file to get data " + configFileName.getAbsoluteFile());

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

        NETWORKUPDATEDNAME = simOptions.getString("NetworkUpdateName", "networkPrepared");
        POPULATIONUPDATEDNAME = simOptions.getString("PopulationUpdateName", "populationPrepared");

        /** set to true in order to make server wait for at least 1 client, for
         * instance viewer client */
        waitForClients = simOptions.getBoolean("waitForClients");
        referenceFrame = simOptions.getReferenceFrame();

        // TOTO Lukas Cutting Attribtes
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

package playground.clruch;

import java.io.File;
import java.io.IOException;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import playground.clruch.data.LocationSpec;
import playground.clruch.utils.PropertiesExt;

public class PrepSettings {
    public final boolean populationeliminateWalking;

    public PrepSettings() throws IOException {
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        PropertiesExt simOptions = PropertiesExt.wrap(ScenarioOptions.load(workingDirectory));

        File configFile = new File(workingDirectory, simOptions.getString("fullConfig"));
        System.out.println("loading config file to get data " + configFile.getAbsoluteFile());

        // TODO can some of these be directly read from another source? Do all have to be user settings?
        boolean populationeliminateFreight = simOptions.getBoolean("populationeliminateFreight");
        populationeliminateWalking = simOptions.getBoolean("populationeliminateWalking");
        boolean populationchangeModeToAV = simOptions.getBoolean("populationchangeModeToAV");
        LocationSpec ls = simOptions.getLocationSpec();
        int numVirtualNodes = simOptions.getInt("numVirtualNodes");
        boolean completeGraph = simOptions.getBoolean("completeGraph");
        int maxPopulationSize = simOptions.getInt("maxPopulationSize");
        int dtTravelData = simOptions.getInt("dtTravelData");
        boolean calculatePerformanceFleetSize = simOptions.getBoolean("calculatePerformanceFleetSize");
        boolean centerNetwork = simOptions.getBoolean("centerNetwork");
        String VIRTUALNETWORKFOLDERNAME = simOptions.getString("virtualNetworkDir");
        String VIRTUALNETWORKFILENAME = simOptions.getString("virtualNetworkName");
        String TRAVELDATAFILENAME = simOptions.getString("travelDataName");
        String MINIMUMFLEETSIZEFILENAME = simOptions.getString("minimumFleetSizeFileName");
        String PERFORMANCEFLEETSIZEFILENAME = simOptions.getString("performanceFleetSizeFileName");

    }

}

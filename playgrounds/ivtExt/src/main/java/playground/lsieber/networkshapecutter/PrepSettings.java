//package playground.lsieber.networkshapecutter;
//
//import java.io.File;
//import java.io.IOException;
//
//import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
//import org.matsim.core.config.Config;
//import org.matsim.core.config.ConfigUtils;
//
//import playground.clruch.data.LocationSpec;
//import playground.clruch.data.ReferenceFrame;
//import playground.clruch.options.ScenarioOptions;
//import playground.sebhoerl.avtaxi.framework.AVConfigGroup;
//
//@Deprecated // TODO delete this class
//public class PrepSettings {
//    private final ScenarioOptions simOptions;
//
//    /* Directories and Paths */
//    private final File workingDirectory;
//    private final File configFileName;
//    private final Config config;
//    private final File preparedScenarioDirectory;
//    private final File preparedConfigFile;
//    private final String preparedConfigName;
//
//    /* Booleans */
//    private final boolean centerNetwork;
//
//    private final boolean waitForClients;
//    private final ReferenceFrame referenceFrame;
//    private final LocationSpec locationSpec;
//    private final boolean networkCleaner;
//
//    public PrepSettings(File workingDirectory) throws IOException {
//        this.workingDirectory = workingDirectory;
//        simOptions = ScenarioOptions.load(workingDirectory);
//        preparedScenarioDirectory = new File(workingDirectory, simOptions.getString("preparedScenarioDirectory", ""));
//        configFileName = new File(workingDirectory, simOptions.getString("fullConfig"));
//        preparedConfigName = simOptions.getString("simuConfig", "prepared_config.xml");
//        preparedConfigFile = new File(workingDirectory, preparedConfigName);
//
//        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
//        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);
//        config = ConfigUtils.loadConfig(preparedConfigFile.toString(), new AVConfigGroup(), dvrpConfigGroup);
//
//        locationSpec = simOptions.getLocationSpec();
//
//        centerNetwork = simOptions.getBoolean("centerNetwork");
//
//        /** set to true in order to make server wait for at least 1 client, for
//         * instance viewer client */
//        waitForClients = simOptions.getBoolean("waitForClients");
//        referenceFrame = simOptions.getReferenceFrame();
//
//        networkCleaner = simOptions.getBoolean("networkCleaner", true);
//    }
//
//}

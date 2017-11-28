/**
 * 
 */
package playground.clruch.html;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.Tensor;
import playground.clruch.analysis.minimumfleetsize.MinimumFleetSizeCalculator;
import playground.clruch.analysis.minimumfleetsize.MinimumFleetSizeGet;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.options.ScenarioOptions;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVConfig;
import playground.sebhoerl.avtaxi.config.AVConfigReader;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVOperatorConfig;

/** @author Claudio Ruch */
public enum ScenarioParameters {
    INSTANCE;

    public final int populationSize;
    public final int iterations;
    public final int redispatchPeriod;
    public final int rebalancingPeriod;
    public final int virtualNodes;

    public final String dispatcher;
    public final String networkName;
    public final String user;
    public final String date;

    public final Tensor EMDks;
    public final Tensor minFleet;
    public final double minimumFleet;

    private ScenarioParameters() {
        File workingDirectory = null;
        ScenarioOptions scenOptions = null;
        try {
            workingDirectory = new File("").getCanonicalFile();
            scenOptions = ScenarioOptions.load(workingDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        }
        File configFile = new File(workingDirectory, scenOptions.getSimulationConfigName());
        Config config = ConfigUtils.loadConfig(configFile.toString());

        user = System.getProperty("user.name");
        date = new SimpleDateFormat("yyyy/MM/dd - HH:mm:ss").format(new Date());

        File basePath = new File(config.getContext().getPath()).getParentFile();
        File configPath = new File(basePath, "av.xml");
        AVConfig avConfig = new AVConfig();
        AVConfigReader reader = new AVConfigReader(avConfig);
        reader.readFile(configPath.getAbsolutePath());
        AVOperatorConfig oc = avConfig.getOperatorConfigs().iterator().next();
        AVDispatcherConfig avdispatcherconfig = oc.getDispatcherConfig();
        SafeConfig safeConfig = SafeConfig.wrap(avdispatcherconfig);

        redispatchPeriod = safeConfig.getInteger("dispatchPeriod", -1);
        rebalancingPeriod = safeConfig.getInteger("rebalancingPeriod", -1);
        dispatcher = avdispatcherconfig.getStrategyName();
        Scenario scenario = ScenarioUtils.loadScenario(config);

        populationSize = scenario.getPopulation().getPersons().values().size();

        Network network = scenario.getNetwork();
        networkName = network.getName();

        VirtualNetwork<Link> virtualNetwork = null;
        MinimumFleetSizeCalculator minimumFleetSizeCalculator = null;
        try {
            virtualNetwork = VirtualNetworkGet.readDefault(network);
            minimumFleetSizeCalculator = MinimumFleetSizeGet.readDefault();
        } catch (IOException e) {
            e.printStackTrace();
        }

        GlobalAssert.that(virtualNetwork != null);

        virtualNodes = virtualNetwork.getvNodesCount();
        minFleet = minimumFleetSizeCalculator.getMinFleet();
        EMDks = minimumFleetSizeCalculator.getEMDk();
        minimumFleet = minimumFleetSizeCalculator.minimumFleet;

        iterations = config.controler().getLastIteration();

    }

}

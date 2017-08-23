package playground.clruch.html;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.matsim.core.config.Config;

import ch.ethz.idsc.tensor.Tensor;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVConfig;
import playground.sebhoerl.avtaxi.config.AVConfigReader;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVOperatorConfig;

/** Created by Joel on 28.06.2017. */
public class ScenarioParameters implements Serializable {
    public int populationSize;
    public int iterations;
    public int redispatchPeriod;
    public int rebalancingPeriod;
    public int virtualNodes;

    public String dispatcher;
    public String networkName;
    public String user;
    public String date;

    public Tensor EMDks;
    public Tensor minFleet;
    public double minimumFleet;

    public Tensor availabilities;

    public ScenarioParameters(Config config) {
        user = System.getProperty("user.name");
        date = new SimpleDateFormat("yyyy/MM/dd - HH:mm:ss").format(new Date());
        fillDispatcherInfo(config);
    }

    private void fillDispatcherInfo(Config config) {
        File basePath = new File(config.getContext().getPath()).getParentFile();
        File configPath = new File(basePath, "av.xml");
        AVConfig avConfig = new AVConfig();
        AVConfigReader reader = new AVConfigReader(avConfig);
        reader.readFile(configPath.getAbsolutePath());
        AVOperatorConfig oc = avConfig.getOperatorConfigs().iterator().next();
        AVDispatcherConfig avdispatcherconfig = oc.getDispatcherConfig();
        SafeConfig safeConfig = SafeConfig.wrap(avdispatcherconfig);

        int redispatchPeriod = safeConfig.getInteger("dispatchPeriod", -1);
        int rebalancingPeriod = safeConfig.getInteger("rebalancingPeriod", -1);
        String dispatcher = avdispatcherconfig.getStrategyName();

        this.redispatchPeriod = redispatchPeriod;
        this.rebalancingPeriod = rebalancingPeriod;
        this.dispatcher = dispatcher;

    }

}

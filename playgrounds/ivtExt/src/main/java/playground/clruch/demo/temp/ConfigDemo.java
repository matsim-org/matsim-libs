package playground.clruch.demo.temp;

import java.io.File;

import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import playground.sebhoerl.avtaxi.framework.AVConfigGroup;

/**
 * DOES NOT WORK YET
 */
public class ConfigDemo {
    public static void main(String[] args) {
        File configFile = new File(args[0]);

        Config config = ConfigUtils.loadConfig(configFile.toString(), //
                new AVConfigGroup(), new ApocalypseConfigGroup(), new DvrpConfigGroup());

        config.getModules().get("av").getParams().entrySet().forEach(System.out::println);
        config.getModules().get("apocalypse").getParams().entrySet().forEach(System.out::println);

    }
}

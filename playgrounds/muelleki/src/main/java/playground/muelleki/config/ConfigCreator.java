package playground.muelleki.config;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;

public class ConfigCreator {
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		ConfigWriter w = new ConfigWriter(config);
		w.write("config.xml");
	}
}

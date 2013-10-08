package playground.balmermi;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.SimulationConfigGroup;

public class ConfigurationWriter {

	public static void main(String[] args) {
		Config c = new Config();
		c.addCoreModules();
		c.addModule( new SimulationConfigGroup() );
		new ConfigWriter(c).write("../../../config_default.xml");
	}
}

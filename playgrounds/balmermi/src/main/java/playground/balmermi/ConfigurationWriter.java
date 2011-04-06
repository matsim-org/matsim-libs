package playground.balmermi;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup;

public class ConfigurationWriter {

	public static void main(String[] args) {
		Config c = new Config();
		c.addCoreModules();
		c.addQSimConfigGroup(new QSimConfigGroup());
		new ConfigWriter(c).write("../../config.xml");
	}
}

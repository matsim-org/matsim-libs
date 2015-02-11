package playground.balac.utils;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;

public class ConfigTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Config config = ConfigUtils.createConfig();
		
		ConfigWriter configwriter = new ConfigWriter(config);
		
		configwriter.writeFileV2("C:/Users/balacm/Desktop/config_out.xml");

	}

}

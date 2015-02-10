package playground.dhosse.qgis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class MainForQGisWriter {
	
	public static void main(String args[]){
		
		Config config = ConfigUtils.createConfig();
//		ConfigUtils.loadConfig(config, "C:/Users/Daniel/Desktop/noiseTest/config.xml");
		ConfigUtils.loadConfig(config, "C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/config.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		QGisWriter writer = new QGisWriter(config.global().getCoordinateSystem());
		writer.addNetworkLayer(scenario.getNetwork());
		writer.write("C:/Users/Daniel/Desktop/MATSimQGisIntegration/test.qgs");

	}

}
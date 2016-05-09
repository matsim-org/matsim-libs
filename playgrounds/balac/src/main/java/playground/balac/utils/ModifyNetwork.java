package playground.balac.utils;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
/**
 * @author balacm
 */
public class ModifyNetwork {


	MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
	double freeSpeedFactor = 1.5;
	String outputFilePath = null;
	String networkFilePath = null;
	public ModifyNetwork(String networkFilePath, String outputFilePath) {
		
		this.outputFilePath = outputFilePath;
		this.networkFilePath = networkFilePath;
	}
	
	
	
	public void changeFreeSpeed() {
		networkReader.readFile(networkFilePath);
		
		for(Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getAllowedModes().contains("car")) {
				
				//if (link.getFreespeed() < 5)
				//	link.setFreespeed(link.getFreespeed());
				
			//	else
					link.setFreespeed(link.getFreespeed() * freeSpeedFactor);
				
			}
			
		}
		
		new NetworkWriter(scenario.getNetwork()).write(outputFilePath + "/network" + "_increased_50perc.xml.gz");
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		ModifyNetwork mn = new ModifyNetwork(args[0], args[1]);
		mn.changeFreeSpeed();
		
	}

}

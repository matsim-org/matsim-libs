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
public class ModifyNetworkHomogenized {


	MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
	double freeSpeedFactor = 0.7;
	String outputFilePath = null;
	String networkFilePath = null;
	public ModifyNetworkHomogenized(String networkFilePath, String outputFilePath) {
		
		this.outputFilePath = outputFilePath;
		this.networkFilePath = networkFilePath;
	}
	
	
	
	public void changeFreeSpeed() {
		networkReader.readFile(networkFilePath);
		
		for(Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getAllowedModes().contains("car")) {
			if (link.getFreespeed() < 5)
				link.setFreespeed(link.getFreespeed());
			else if (link.getFreespeed() * 3.6 < 21)
				link.setFreespeed(link.getFreespeed());
			else if (link.getFreespeed() * 3.6 < 31)
				link.setFreespeed(link.getFreespeed());
			else if (link.getFreespeed() * 3.6 < 36)
				link.setFreespeed(40.0/3.6);
			else  if (link.getFreespeed() * 3.6 < 41)
				link.setFreespeed(45.0/3.6);
			else  if (link.getFreespeed() * 3.6 < 46)
				link.setFreespeed(50.0/3.6);
			else  if (link.getFreespeed() * 3.6 < 51)
				link.setFreespeed(50.0/3.6);
			else  if (link.getFreespeed() * 3.6 < 56)
				link.setFreespeed(50.0/3.6);
			else  if (link.getFreespeed() * 3.6 < 61)
				link.setFreespeed(55.0/3.6);
			else  if (link.getFreespeed() * 3.6 < 66)
				link.setFreespeed(55.0/3.6);
			else  if (link.getFreespeed() * 3.6 < 71)
				link.setFreespeed(60.0/3.6);
			else  if (link.getFreespeed() * 3.6 < 81)
				link.setFreespeed(65.0/3.6);
			else  if (link.getFreespeed() * 3.6 < 91)
				link.setFreespeed(70.0/3.6);
			else  if (link.getFreespeed() * 3.6 < 101)
				link.setFreespeed(70.0/3.6);
			else  if (link.getFreespeed() * 3.6 < 121)
				link.setFreespeed(80.0/3.6);
			}
		}
		
		new NetworkWriter(scenario.getNetwork()).write(outputFilePath + "/network_" + "homogenization.xml");
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		ModifyNetworkHomogenized mn = new ModifyNetworkHomogenized(args[0], args[1]);
		mn.changeFreeSpeed();
		
	}

}

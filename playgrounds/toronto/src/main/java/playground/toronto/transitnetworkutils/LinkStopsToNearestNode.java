package playground.toronto.transitnetworkutils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;

public class LinkStopsToNearestNode {

	public static void main(String[] args) throws IOException{
		
		String stop_infile = args[0];
		String network_infile = args[1];
		String stop_outfile = args[2];
		String network_outfile = args[3];
		
		Config blankConfig = new Config();
		blankConfig.setParam("network", "inputNetworkFile", args[1]);
		Network network = ScenarioUtils.loadScenario(blankConfig).getNetwork();
		
		BufferedReader reader = new BufferedReader(new FileReader(args[0]));
		
		String header = reader.readLine();
		int stopnumber = Arrays.asList(header.split(",")).indexOf("stop_id");
		int stop_lon = Arrays.asList(header.split(",")).indexOf("stop_lon");
		int stop_lat = Arrays.asList(header.split(",")).indexOf("stop_lat");
		
		String line;
		while ((line = reader.readLine()) != null){
			
		}
		
	}
	
	
}

package playground.sergioo.NetworkPropertiesChanger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class NetworkPropertiesChanger {
	
	private static final String CSV_SEPARATOR = ",";
	
	public static void changePropertiesCSVFile(Network network, String fileName) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));
		String line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(CSV_SEPARATOR);
			LinkImpl link = (LinkImpl) network.getLinks().get(new IdImpl(parts[0]));
			link.setLength(Double.parseDouble(parts[1]));
			link.setFreespeed(Double.parseDouble(parts[2]));
			link.setCapacity(Double.parseDouble(parts[3]));
			link.setNumberOfLanes(Double.parseDouble(parts[4]));
			line = reader.readLine();
		}
		reader.close();
	}
	
	/**
	 * Makes properties changes to a network read from a file (second argument) according to a .CSV file (first argument) and writes the result in other file (third argument)
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(scenario);
		matsimNetworkReader.readFile(args[1]);
		Network network = scenario.getNetwork();
		changePropertiesCSVFile(network, args[0]);
		NetworkWriter networkWriter =  new NetworkWriter(network);
		networkWriter.write(args[2]);
	}
	
}

package playground.sergioo.NetworkPropertiesChanger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class NetworkPropertiesChanger {
	
	private static final String CSV_SEPARATOR = ",";
	
	public static void changePropertiesPerLinkCSVFile(Network network, String fileName) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));
		String line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(CSV_SEPARATOR);
			LinkImpl link = (LinkImpl) network.getLinks().get(new IdImpl(parts[0]));
			if(!parts[1].equals(""))
				link.setLength(Double.parseDouble(parts[1]));
			if(!parts[2].equals(""))
				link.setFreespeed(Double.parseDouble(parts[2]));
			if(!parts[3].equals(""))
				link.setCapacity(Double.parseDouble(parts[3]));
			if(!parts[4].equals(""))
				link.setNumberOfLanes(Double.parseDouble(parts[4]));
			line = reader.readLine();
		}
		reader.close();
	}
	
	public static void changePropertiesGroupLinksCSVFile(Network network, String fileName) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));
		reader.readLine();
		String line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(CSV_SEPARATOR);
			double[] values = new double[parts.length];
			for(int i=0; i<parts.length; i++)
				values[i] = Double.parseDouble(parts[i]);
			for(Link link:network.getLinks().values())
				if(isLinkOfType(link,values[0],values[1],values[2])) {
					link.setFreespeed(values[3]);
					link.setCapacity(values[4]);
					link.setNumberOfLanes(values[5]);
				}
			line = reader.readLine();
		}
		reader.close();
	}
	
	private static boolean isLinkOfType(Link link, double freeSpeed, double capacity, double numLanes) {
		return Math.abs(link.getFreespeed()-freeSpeed)<0.01 && Math.abs(link.getCapacity()-capacity)<0.01 && link.getNumberOfLanes()==numLanes;
	}

	/**
	 * Makes properties changes to a network read from a file (second argument) according to a .CSV file (first argument) and writes the result in other file (third argument)
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if(args.length==4) {
			Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(scenario);
			matsimNetworkReader.readFile(args[0]);
			Network network = scenario.getNetwork();
			
			if(args[1].equals("-l"))
				changePropertiesPerLinkCSVFile(network, args[2]);
			else if(args[1].equals("-g"))
				changePropertiesGroupLinksCSVFile(network, args[2]);
			NetworkWriter networkWriter =  new NetworkWriter(network);
			networkWriter.write(args[3]);
		}
	}
	
}

/**
 * 
 */
package matrix;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.run.NetworkCleaner;

/**
 * @author sfuerbas
 * 
 */
public class NetworkScaler {

	/**
	 * @param args
	 * @throws IOException 
	 */
	
	final static double BETW_MAX = 1500.;
	final static double CAPACITY_MAX = 600;
	
	public static void main(String[] args) throws IOException {
		Scenario scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(args[0]);	//provide MATSim network
		NetworkLayer network = (NetworkLayer) scenario.getNetwork();
		HashMap<String, Double> linkBetw = new HashMap<String, Double>();
		Double betw = 0.;
		String matsimLinkId = null;
		BufferedReader br = new BufferedReader(new FileReader(args[1]));	//as args[1] provide file with results from MatrixCentrality in format: Matsim Id "TAB" Betweenness
		
		while (br.ready()) {
			String aLine = br.readLine();
			String splitLine[] = aLine.split("\t");
			matsimLinkId = splitLine[0];
			betw = Double.parseDouble(splitLine[1]);
			linkBetw.put(matsimLinkId, betw);
		}
		
		for (Link link : network.getLinks().values()) {
			betw = linkBetw.get(link.getId().toString());
			System.out.println(betw);
			if (betw<=BETW_MAX && link.getCapacity()<=CAPACITY_MAX) {
				network.removeLink(link.getId());
			}
		}
		
		new NetworkCleaner().run(new String[] {args[0], args[0]});

		

	}

}

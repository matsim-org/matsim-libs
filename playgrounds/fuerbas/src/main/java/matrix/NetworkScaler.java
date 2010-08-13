/**
 * 
 */
package matrix;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
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
	
	final static double BETW_MIN = 1500000.;	//minimum betweenness required to remain in network
	final static double CAPACITY_MIN = 600;	//minimim capacity required to remain in network
	
	public static void main(String[] args) throws IOException {
		Scenario scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(args[0]);	//provide MATSim network
		Network network = scenario.getNetwork();
		HashMap<String, Double> linkBetw = new HashMap<String, Double>();
		HashMap<Id, Link> linkDeletion = new HashMap<Id, Link>();	//HashMap containing all links up for deletion from network
//		List<Link> linkDeletion = new ArrayList<Link>();
		Double betw = 0.;
		String matsimLinkId = null;
		BufferedReader br = new BufferedReader(new FileReader(args[1]));	//as args[1] provide file with results from MatrixCentrality in format: Matsim Id "TAB" Betweenness
		
		while (br.ready()) {
			String aLine = br.readLine();
			String splitLine[] = aLine.split("\t");
			matsimLinkId = splitLine[0].trim();
			betw = Double.parseDouble(splitLine[1]);
			linkBetw.put(matsimLinkId, betw);
		}
		
		for (Link link : network.getLinks().values()) {
			betw = linkBetw.get(link.getId().toString());
			if (betw==null) continue;	//bricht teilweise ab, weil betw "null" wird. ursache habe ich noch nicht gefunden!
			System.out.println(betw);
			if (betw<=BETW_MIN && link.getCapacity()<=CAPACITY_MIN) {
				linkDeletion.put(link.getId(), link);
//				System.out.println(linkDeletion.toString());
			}
		}
		
        for (Entry<Id, Link> entry : linkDeletion.entrySet()) {        
//            System.out.println(linkDeletion.get(entry.getKey()));
            network.removeLink(entry.getKey());	//tut leider nicht das, was ich will, nämlich den link löschen. gibt es dafür eine methode?
        }
		
		new NetworkCleaner().run(new String[] {args[0], args[2]});

		

	}

}

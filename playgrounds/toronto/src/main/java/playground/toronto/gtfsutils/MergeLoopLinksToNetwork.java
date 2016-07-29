package playground.toronto.gtfsutils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkFactoryImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * A simple utility which reads in two networks, a base network with or without loop links,
 * and an 'updated' network which contains loop links to add.
 * 
 * @author pkucirek
 *
 */
public class MergeLoopLinksToNetwork {

	public static void main(String[] args) throws IOException{
		
		if (args.length != 4) return;
		
		String baseNetworkFile = args[0];
		String updatedNetworkFile = args[1];
		String outputNetworkFile = args[2];
		String reportOutputFile = args[3];
		
		MutableScenario scenario1 = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario1.getNetwork()).readFile(baseNetworkFile);
		Network baseNetwork = scenario1.getNetwork();
		
		MutableScenario scenario2 = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario2.getNetwork()).readFile(updatedNetworkFile);
		Network updatedNetwork = scenario2.getNetwork();
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(reportOutputFile));
		bw.write("Nodes with Loops:");
		
		int loopsAdded = 0;
		int errorCount = 0;
		LinkFactoryImpl factory = NetworkUtils.createLinkFactory();
		for (Link l : updatedNetwork.getLinks().values()){
			Link L = (Link) l;
			if (NetworkUtils.getType(L).equals("LOOP")){
				
				Node fn = baseNetwork.getNodes().get(L.getFromNode().getId());
				Node tn = baseNetwork.getNodes().get(L.getToNode().getId());
				
				try {
					Link newLink = (Link) NetworkUtils.createLink(L.getId(), fn, tn, baseNetwork, L.getLength(), L.getFreespeed(), L.getCapacity(), L.getNumberOfLanes());
					NetworkUtils.setType( newLink, (String) "LOOP");
					baseNetwork.addLink(newLink);
					bw.write("\n" + fn.getId().toString());
					loopsAdded++;
				} catch (NullPointerException e) {
					System.err.println("Error with link \"" + L.getId().toString() + "\"");
					e.printStackTrace();
					errorCount++;
				} catch (IllegalArgumentException e){
					System.err.println("Link \"" + L.getId().toString() + "\" already exists!");
					errorCount++;
				}
			}
		}
		NetworkWriter nw = new NetworkWriter(baseNetwork);
		nw.write(outputNetworkFile);
		
		bw.close();
		
		System.out.println("Done. " + loopsAdded + " loop links added, " + errorCount + " errors.");
		

	}
}

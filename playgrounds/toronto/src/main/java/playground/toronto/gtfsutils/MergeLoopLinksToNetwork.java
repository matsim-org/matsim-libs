package playground.toronto.gtfsutils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkFactoryImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
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
		new MatsimNetworkReader(scenario1).readFile(baseNetworkFile);
		Network baseNetwork = scenario1.getNetwork();
		
		MutableScenario scenario2 = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario2).readFile(updatedNetworkFile);
		Network updatedNetwork = scenario2.getNetwork();
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(reportOutputFile));
		bw.write("Nodes with Loops:");
		
		int loopsAdded = 0;
		int errorCount = 0;
		LinkFactoryImpl factory = new LinkFactoryImpl();
		for (Link l : updatedNetwork.getLinks().values()){
			LinkImpl L = (LinkImpl) l;
			if (L.getType().equals("LOOP")){
				
				Node fn = baseNetwork.getNodes().get(L.getFromNode().getId());
				Node tn = baseNetwork.getNodes().get(L.getToNode().getId());
				
				try {
					LinkImpl newLink = (LinkImpl) factory.createLink(L.getId(), 
							fn, 
							tn, 
							baseNetwork, 
							L.getLength(), 
							L.getFreespeed(),
							L.getCapacity(), 
							L.getNumberOfLanes());
					newLink.setType("LOOP");
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

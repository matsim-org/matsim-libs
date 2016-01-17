package playground.staheale.preprocess;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class CombineNetworks {
	private static Logger log = Logger.getLogger(CombineNetworks.class);

	public static void main(String[] args) {
		final ScenarioImpl streetScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final ScenarioImpl transitScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	
		
//	    new MatsimNetworkReader(transitScenario).parse("./input/02-OeV_2030+_DWV_Ref_Mit_IterationGerman_adapted.xml");
//	    Network transitNetwork = transitScenario.getNetwork();
//	    Set<String> ptMode = new HashSet<String>();
//	    ptMode.add("pt");
//	    
//	    for (Link link : transitNetwork.getLinks().values()) {
//	    	link.setAllowedModes(ptMode);
//		}
	    
//	    new NetworkWriter(transitNetwork).write("./output/uvek2030_oev_final.xml.gz");
	    
//	    TransitSchedule schedule = transitScenario.getTransitSchedule();
//	    for (TransitStopFacility facility : schedule.getFacilities().values()) {
//	    	Id stopId = facility.getId();
//		    boolean unused = false;
//
//	    	for (TransitLine line : schedule.getTransitLines().values()) {
//
//	    		for (TransitRoute route : line.getRoutes().values()) {
//	    			if (route.getStops().contains(stopId)) {
//	    				unused = true;
//	    			}
//	    		}
//	    	}
//	    	if (unused = true) {
//	    		schedule.removeStopFacility(facility);
//	    	}
//	    	unused = false;
//	    }
	    
	    log.info("Reading teleatlas network xml file...");
	    new MatsimNetworkReader(streetScenario.getNetwork()).parse("./input/teleatlas2030networkSingleMode_Cleaned.xml.gz"); //teleatlas2010networkSingleMode.xml.gz
	    Network streetNetwork = streetScenario.getNetwork();
	    log.info("Reading teleatlas network xml file...done");
	    log.info("Reading pt network xml file...");
	    new MatsimNetworkReader(transitScenario.getNetwork()).parse("./input/uvek2030network_adjusted.xml.gz"); //uvek2005network_adjusted.xml.gz
	    Network transitNetwork = transitScenario.getNetwork();
	    log.info("Reading pt network xml file...done");

	    MergeNetworks.merge(streetNetwork, "", transitNetwork);
	    new NetworkWriter(streetNetwork).write("./output/multimodalNetwork2030final.xml.gz"); //multimodalNetwork2010final.xml.gz
	    
//	    MatsimNetworkReader NetworkReader = new MatsimNetworkReader(scenario);
//		NetworkReader.readFile("./input/teleatlas2010network.xml.gz");
//		Network network = scenario.getNetwork();
//	    log.info("Reading teleatlas network xml file...done.");
//	    log.info("Number of nodes: " +network.getNodes().size());
//	    log.info("Number of links: " +network.getLinks().size());
//	    
//	    log.info("Reading public transport network xml file...");
//	    MatsimNetworkReader NetworkReader2 = new MatsimNetworkReader(scPT);
//		NetworkReader2.readFile("./input/publicTransportNetwork.xml.gz");
//		Network PTnetwork = scPT.getNetwork();
//	    log.info("Reading public transport network xml file...done.");
//	    log.info("Number of nodes: " +PTnetwork.getNodes().size());
//	    log.info("Number of links: " +PTnetwork.getLinks().size());
//	    
//		double capacityFactor = PTnetwork.getCapacityPeriod() / network.getCapacityPeriod();
//		NetworkFactory factory = PTnetwork.getFactory();
//	    
//		for (Link l : PTnetwork.getLinks().values()) {
//			Id fromNodeId = new IdImpl(l.getFromNode().getId().toString());
//			Id toNodeId = new IdImpl(l.getToNode().getId().toString());
//			Link link2 = factory.createLink(new IdImpl(l.getId().toString()),
//					fromNodeId, toNodeId);
//			link2.setAllowedModes(l.getAllowedModes());
//			link2.setCapacity(l.getCapacity() * capacityFactor);
//			link2.setFreespeed(l.getFreespeed());
//			link2.setLength(l.getLength());
//			link2.setNumberOfLanes(l.getNumberOfLanes());
//			network.addLink(link2);
//		}
//		for (Node n : PTnetwork.getNodes().values()) {
//			NodeImpl node2 = (NodeImpl) factory.createNode(new IdImpl(n.getId().toString()), n.getCoord());
//			network.addNode(node2);
//		}
//
//		new NetworkWriter(network).write("./output/multimodalNetwork.xml.gz");
		
	}

}

package playground.toronto.transitnetworkutils;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkFactory;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class RenameLoopLinks {

	public static void main(String[] args){
		
		String networkFile = args[0];
		String scheduleFile = args[1];
		String outputScheduleFile = args[2];
		String outputNetworkFile = args[3];
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		Network baseNetwork = scenario.getNetwork();
		
		new TransitScheduleReaderV1(scenario).readFile(scheduleFile);
		TransitSchedule inSchedule = scenario.getTransitSchedule();
		
		Network outNetwork = NetworkUtils.createNetwork();
		for (Node n : baseNetwork.getNodes().values()) outNetwork.addNode(n);
		
		HashMap<Id, Id> loopNamesMap = new HashMap<Id, Id>();
		
		LinkFactory factory = NetworkUtils.createLinkFactory();
		for (Link l : baseNetwork.getLinks().values()){
			Link L = (Link) l;
			
			Id<Link> linkId;
			if (NetworkUtils.getType(L).equals("LOOP")){
				linkId = Id.create(L.getFromNode().getId().toString() + "_LOOP", Link.class);
				loopNamesMap.put(L.getId(), linkId);
			}else linkId = L.getId();
			Node fn = outNetwork.getNodes().get(L.getFromNode().getId());
			Node tn = outNetwork.getNodes().get(L.getToNode().getId());
			
			Link newLink = (Link) NetworkUtils.createLink(linkId, fn, tn, outNetwork, L.getLength(), L.getFreespeed(), L.getCapacity(), L.getNumberOfLanes());
			NetworkUtils.setType( newLink, (String) NetworkUtils.getType(L));
			outNetwork.addLink(newLink);
		}
		TransitScheduleFactoryImpl tsFactory = new TransitScheduleFactoryImpl();
		TransitSchedule outSchedule = tsFactory.createTransitSchedule();
		
		//update stop references
		for (TransitStopFacility stop : inSchedule.getFacilities().values()){
			Id newLinkRedId = loopNamesMap.get(stop.getLinkId());
			outSchedule.addStopFacility(tsFactory.createTransitStopFacility(newLinkRedId, stop.getCoord(), stop.getIsBlockingLane()));
		}
		
		//update route references
		for (TransitLine line : inSchedule.getTransitLines().values()){
			TransitLine newLine = tsFactory.createTransitLine(line.getId());
			
			for (TransitRoute route : line.getRoutes().values()){
				
				

				
			}
			
			outSchedule.addTransitLine(newLine);
		}
		
	}
}

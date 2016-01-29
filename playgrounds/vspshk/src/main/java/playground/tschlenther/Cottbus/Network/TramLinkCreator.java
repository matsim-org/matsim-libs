package playground.tschlenther.Cottbus.Network;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

public class TramLinkCreator {


	public static void main(String[] args){
	
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		String netfile = "C:/Users/Tille/WORK/Cottbus/Cottbus-pt/Demand_input/network_pt_cap60.xml";
		new MatsimNetworkReader(scenario.getNetwork()).readFile(netfile);
		Network network = scenario.getNetwork();

		Node knotenRechts = network.getNodes().get(Id.createNodeId(31559430));
		Node knotenLinks = network.getNodes().get(network.getLinks().get(Id.createLinkId(9803)).getFromNode().getId());
		
		Node knotenKreuzung = network.getNodes().get(Id.createNodeId("pt121"));
		
		NetworkFactory fact = network.getFactory();
		
		List<Link> newLinks = new ArrayList<Link>();
		
		Link lRechtsR = fact.createLink(Id.createLinkId("ptrr"), knotenKreuzung, knotenRechts);
		newLinks.add(lRechtsR);
		Link lLinksR = fact.createLink(Id.createLinkId("ptlr"), knotenLinks, knotenKreuzung);
		newLinks.add(lLinksR);
		Link lLinksL = fact.createLink(Id.createLinkId("ptll"), knotenKreuzung, knotenLinks);
		newLinks.add(lLinksL);
		Link lRechtsL = fact.createLink(Id.createLinkId("ptrl"), knotenRechts, knotenKreuzung);
		newLinks.add(lRechtsL);
		
		Set<String> modes = new HashSet<String>();
		modes.add("tram");
		for (Link l : newLinks){
			Double d = CoordUtils.calcDistance(l.getFromNode().getCoord(), l.getToNode().getCoord());
			l.setCapacity(60);
			l.setAllowedModes(modes);
			l.setFreespeed(14);
			l.setLength(d);
			network.addLink(l);
		}
		
		new NetworkWriter(network).write("C:/Users/Tille/WORK/Cottbus/Cottbus-pt/Demand_input/ADDEDLINKS_cap60.xml");
		
	}
	
}

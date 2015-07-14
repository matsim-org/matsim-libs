package playground.sergioo.singapore2012;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

public class NetworkStructureSolver {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		(new MatsimNetworkReader(scenario)).readFile(args[0]);
		(new TransitScheduleReader(scenario)).readFile(args[1]);
		List<List<Link>> paths = new ArrayList<List<Link>>();
		Set<Link> links = new HashSet<Link>();
		Set<Node> nodes = new HashSet<Node>();
		BufferedReader reader = new BufferedReader(new FileReader(args[2]));
		String line = reader.readLine();
		int n=0;
		while(line!=null) {
			String[] parts = line.split(",");
			List<Link> path = new ArrayList<Link>();
			List<Link> path2 = new ArrayList<Link>();
			Node node = null;
			for(String part:parts) {
				Link link = scenario.getNetwork().getLinks().get(Id.createLinkId(part));
				path.add(link);
				if(!links.add(link))
					throw new Exception("Error: "+part);
				node = link.getToNode();
				nodes.add(node);
				if(n>5) {
					link = scenario.getNetwork().getLinks().get(Id.createLinkId("cl"+part));
					path2.add(link);
					if(!links.add(link))
						throw new Exception("Error: "+part);
					nodes.add(link.getToNode());
				}
			}
			nodes.remove(node);
			paths.add(path);
			if(n>5) {
				paths.add(path2);
				Node node2 = scenario.getNetwork().getNodes().get(Id.createNodeId("cl"+node.getId().toString()));
				if(node2!=null)
					nodes.remove(node2);
			}
			line = reader.readLine();
			n++;
		}
		reader.close();
		Map<Id<Node>, Integer> nodesCount = new HashMap<Id<Node>, Integer>();
		NetworkFactory factory = new NetworkFactoryImpl(scenario.getNetwork());
		Set<Node> specialNodes = new HashSet<Node>();
		specialNodes.add(scenario.getNetwork().getNodes().get(Id.createNodeId("1380007282")));
		specialNodes.add(scenario.getNetwork().getNodes().get(Id.createNodeId("1380001447")));
		specialNodes.add(scenario.getNetwork().getNodes().get(Id.createNodeId("1380001450")));
		specialNodes.add(scenario.getNetwork().getNodes().get(Id.createNodeId("1380001459")));
		for(List<Link> path:paths) {
			Node node = null;
			for(Link link:path) {
				if(node!=null) {
					link.setFromNode(node);
					node = null;
				}
				if(!link.equals(path.get(path.size()-1))) {
					Node toNode = link.getToNode();
					for(Link link2:toNode.getInLinks().values())
						if(!links.contains(link2) && nodes.contains(link2.getFromNode())) {
							if(isInTransit(link2.getId(), scenario.getTransitSchedule()))
								System.out.println(link2.getId()+" in Yes transit");
							else {
								System.out.println(link2.getId()+" in No transit");
								scenario.getNetwork().removeLink(link2.getId());
								if(link2.getFromNode().getInLinks().size()==0 && link2.getFromNode().getOutLinks().size()==0) {
									System.out.println(link2.getFromNode()+" in Node");
									scenario.getNetwork().removeNode(link2.getFromNode().getId());
								}
							}
						}
					for(Link link2:toNode.getOutLinks().values())
						if(!links.contains(link2) && nodes.contains(link2.getToNode())) {
							if(isInTransit(link2.getId(), scenario.getTransitSchedule()))
								System.out.println(link2.getId()+" out Yes transit");
							else {
								System.out.println(link2.getId()+" out No transit");
								scenario.getNetwork().removeLink(link2.getId());
								if(link2.getToNode().getInLinks().size()==0 && link2.getToNode().getOutLinks().size()==0) {
									System.out.println(link2.getToNode()+" out Node");
									scenario.getNetwork().removeNode(link2.getToNode().getId());
								}
							}
						}
					Integer c = nodesCount.get(toNode.getId());
					if(c==null)
						c = 0;
					else if (c>1 || !specialNodes.contains(toNode)) {
						node = factory.createNode(Id.createNodeId(toNode.getId()+"_"+c++), toNode.getCoord());
						scenario.getNetwork().addNode(node);
						link.setToNode(node);
					}
					else
						c++;
					nodesCount.put(toNode.getId(), c);
				}
			}
		}
		(new NetworkWriter(scenario.getNetwork())).write(args[3]);
	}

	private static boolean isInTransit(Id<Link> linkId, TransitSchedule transitSchedule) {
		for(TransitLine line:transitSchedule.getTransitLines().values())
			for(TransitRoute route:line.getRoutes().values()) {
				if(linkId.equals(route.getRoute().getStartLinkId()) || linkId.equals(route.getRoute().getEndLinkId()))
					return true;
				for(Id<Link> id:route.getRoute().getLinkIds())
					if(linkId.equals(id))
						return true;
			}
		return false;
	}

}

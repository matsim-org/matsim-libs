package playground.nmviljoen.network.salience;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.misc.Counter;

import playground.nmviljoen.network.NmvLink;
import playground.nmviljoen.network.NmvNode;
import playground.southafrica.projects.complexNetworks.pathDependence.DigicorePathDependentNetworkReader_v1;
import playground.southafrica.projects.complexNetworks.pathDependence.PathDependentNetwork;
import playground.southafrica.projects.complexNetworks.pathDependence.PathDependentNetwork.PathDependentNode;
import playground.southafrica.utilities.Header;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;

public class DigicoreNetworkConverter {
	final private static Logger LOG = Logger.getLogger(DigicoreNetworkConverter.class);

	public static void main(String[] args) {
		Header.printHeader(DigicoreNetworkConverter.class.toString(), args);
		
		String inputFile = args[0];
		String outputFile = args[1];
		
		Graph<NmvNode, NmvLink> graph = DigicoreNetworkConverter.convertGraph(inputFile);
		SampleNetworkBuilder.writeGraphML(graph, outputFile);
		
		Header.printFooter();
	}
	
	public static Graph<NmvNode, NmvLink> convertGraph(String filename){
		Graph<NmvNode, NmvLink> graph = new DirectedSparseGraph<NmvNode, NmvLink>();
		
		LOG.info("Parsing path-dependent network...");
		DigicorePathDependentNetworkReader_v1 pdnr = new DigicorePathDependentNetworkReader_v1();
		pdnr.parse(filename);
		PathDependentNetwork pdn = pdnr.getPathDependentNetwork();
		pdn.writeNetworkStatisticsToConsole();

		LOG.info("Adding all nodes...");
		Map<Id<Node>, NmvNode> nodeMap = new HashMap<Id<Node>, NmvNode>(pdn.getNumberOfNodes());
		
		Counter nodeCounter = new Counter("   nodes # ");
		for(PathDependentNode node : pdn.getPathDependentNodes().values()){
			NmvNode n = new NmvNode(node.getId().toString(), node.getId().toString(), node.getCoord().getX(), node.getCoord().getY());
			graph.addVertex(n);
			nodeMap.put(node.getId(), n);
			nodeCounter.incCounter();
		}
		nodeCounter.printCounter();
		
		LOG.info("Adding all edges...");
		
		Counter edgeCounter = new Counter("   edges # ");
		int linkId = 0;
		for(PathDependentNode o : pdn.getPathDependentNodes().values()){
			for(PathDependentNode d : pdn.getPathDependentNodes().values()){
				if(!o.equals(d)){
					double weight = pdn.getWeight(o.getId(), d.getId());
					
					if(weight > 0){
						NmvLink link = new NmvLink(String.valueOf(linkId++), weight);
						graph.addEdge(link, nodeMap.get(o.getId()), nodeMap.get(d.getId()), EdgeType.DIRECTED);
						edgeCounter.incCounter();
					}
				}
			}
		}
		edgeCounter.printCounter();
		
		return graph;
	}

}

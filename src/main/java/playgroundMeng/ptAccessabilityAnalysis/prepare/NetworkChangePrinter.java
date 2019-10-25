//package playgroundMeng.ptAccessabilityAnalysis.prepare;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import org.locationtech.jts.noding.NodableSegmentString;
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.network.Link;
//import org.matsim.api.core.v01.network.Network;
//import org.matsim.api.core.v01.network.Node;
//import org.matsim.core.network.NetworkChangeEvent;
//import org.matsim.core.network.NetworkUtils;
//import org.matsim.core.network.io.NetworkWriter;
//
//
//
//public class NetworkChangePrinter {
//	public static void main(String[] args) {
//		//String networkFile = "W:\\08_Temporaere_Mitarbeiter\\082_K-GGSN\\0822_Praktikanten\\Meng\\VIA\\via-sampledata\\via-sampledata\\transit-tutorial\\network.xml";
//		String networkFile = "W:/08_Temporaere_Mitarbeiter/082_K-GGSN/0822_Praktikanten/Meng/vw243_cadON_ptSpeedAdj.0.1/vw243_cadON_ptSpeedAdj.0.1.output_network.xml";
//		String outputNetwork = "outputNetworkFileFinal.xml";
//		String newOutputNetwork = "outputNetworkFileNew.xml";
//		String shapeFile = "C:/Users/VW3RCOM/Documents/shp/Hannover_Stadtteile.shp";
//		Network network = NetworkUtils.readNetwork(networkFile);
////		
//		ShapeFileFilter shapeFileFilter = new ShapeFileFilter(shapeFile, network);
//		shapeFileFilter.filter();
//		List<Id<Link>> linkId2s = new ArrayList<Id<Link>>();
//		
//		Network newNetwork = NetworkUtils.createNetwork();
//		for(String string: shapeFileFilter.getDistrict2LinkId().keySet()) {
//			linkId2s.addAll(shapeFileFilter.getDistrict2LinkId().get(string));
//		}
//		for(Id<Link> linkId : linkId2s) {
//			if(!linkId.toString().contains("pt")) {
//				if(!newNetwork.getNodes().keySet().contains(network.getLinks().get(linkId).getFromNode().getId())) {
//					Node fromNode = NetworkUtils.createNode(network.getLinks().get(linkId).getFromNode().getId(), network.getLinks().get(linkId).getFromNode().getCoord());
//					newNetwork.addNode(fromNode);
//				}
//				if(!newNetwork.getNodes().keySet().contains(network.getLinks().get(linkId).getToNode().getId())) {
//					Node toNode = NetworkUtils.createNode(network.getLinks().get(linkId).getToNode().getId(), network.getLinks().get(linkId).getToNode().getCoord());
//					newNetwork.addNode(toNode);
//				}
//				newNetwork.addLink(network.getLinks().get(linkId));
//				newNetwork.getLinks().get(linkId).setCapacity(0.);
//			}
////		}
//		System.out.println(shapeFileFilter.getDistrict2LinkId().keySet());
//		new NetworkWriter(newNetwork).write(newOutputNetwork);
//		
//		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
//		for (Link link : network.getLinks().values()) {
//			link.setCapacity(0.);
//			if(link.getId().toString().startsWith("pt")) {
//				linkIds.add(link.getId());
//			}
//		}
//		for(Id<Link> linkId : linkIds) {
//			network.removeLink(linkId);
//		}
//		
//		new NetworkWriter(network).write(outputNetwork);
//		
//	}
//}

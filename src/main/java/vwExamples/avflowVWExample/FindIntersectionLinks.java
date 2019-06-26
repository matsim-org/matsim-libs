package vwExamples.avflowVWExample;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.GeometryUtils;

import com.opencsv.CSVReader;

public class FindIntersectionLinks {
	static String trafficLightsCSV;
	static String networkFile;
	static List<Coord> Coords = new ArrayList<Coord>();
	static Set<Id<Link>> Links = new HashSet<Id<Link>>();
	static Network network;
	static Network networkIntersectionLinks;

	public static void main(String[] args) {
		trafficLightsCSV = "D:\\Matsim\\Axer\\Hannover\\ZIM\\input\\network\\osmTrafficSignals_clustered.csv";
		networkFile = "D:\\Matsim\\Axer\\Hannover\\ZIM\\input\\network\\network.xml.gz";
		List<String[]> csvContent = processByOpenCsv(trafficLightsCSV);
		System.out.println(csvContent.get(0)[0]);

		network = NetworkUtils.createNetwork();
		networkIntersectionLinks = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkFile);
		new MatsimNetworkReader(networkIntersectionLinks).readFile(networkFile);
		getTrafficLightLinksFast(network, Coords);
		adjustLinkCapacity(1.28);
		new NetworkWriter(networkIntersectionLinks)
				.write("C:\\Users\\VWBIDGN\\Desktop\\network_intersectionLinks_1.28_.xml.gz");
	}

	static void adjustLinkCapacity(double flowIncreaseFactor) {

		for (Id<Link> linkId : Links) {
			double capacity = networkIntersectionLinks.getLinks().get(linkId).getCapacity();
			Link link = networkIntersectionLinks.getLinks().get(linkId);
			link.setCapacity(capacity * flowIncreaseFactor);
			System.out.println("ID: "+link.getId() + " || " + capacity+ " |>| " + link.getCapacity());
		}

	}

	static void cleanNonCarLinksFromLinkSet() {
		int linkSetSizeBefore = Links.size();

		for (Iterator<Id<Link>> iter = Links.iterator(); iter.hasNext();) {
			Link link = network.getLinks().get(iter.next());
			if (link.getAllowedModes().contains(TransportMode.pt)) {
				iter.remove();
			}

		}

		int linkSetSizeAfter = Links.size();
		int diff = linkSetSizeBefore - linkSetSizeAfter;

		System.out.println("Filtered pt links: " + diff);

	}

	static void getTrafficLightLinksFast(Network network, List<Coord> Coords) {
		GeometryFactory f = new GeometryFactory();
		double n = Coords.size();
		double i = 0;

		for (Coord coord : Coords) {

			Collection<Node> nodes = NetworkUtils.getNearestNodes(network, coord, 25);

			for (Node node : nodes) {
				Map<Id<Link>, ? extends Link> relevantLinks1 = node.getInLinks();
				Map<Id<Link>, ? extends Link> relevantLinks2 = node.getOutLinks();

				Links.addAll(relevantLinks1.keySet());
				Links.addAll(relevantLinks2.keySet());

			}
			// i = i + 1;
			// System.out.println(Links.size());
		}
		
		//Drop non car links from link set
		cleanNonCarLinksFromLinkSet();

		//Export only modified links
//		{
//			for (Entry<Id<Link>, ? extends Link> link : network.getLinks().entrySet()) {
//				if (!Links.contains(link.getValue().getId())) {
//					networkIntersectionLinks.removeLink(link.getValue().getId());
//				}
//	
//			}
//		}

	}

	static List<String[]> processByOpenCsv(String trafficLightsCSV) {
		CSVReader reader = null;
		List<String[]> lines = new ArrayList<String[]>();
		try {
			reader = new CSVReader(new FileReader(trafficLightsCSV));
			lines = reader.readAll();
			for (int i = 1; i < lines.size(); i++) {
				String[] lineContents = lines.get(i);
				Coord trafficLightCoord = new Coord(Double.parseDouble(lineContents[55]),
						Double.parseDouble(lineContents[56]));
				Coords.add(trafficLightCoord);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return lines;
	}

}

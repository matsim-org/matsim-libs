package vwExamples.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;

public class serviceAreaToAllCarLinks {
	private static final Logger LOG = Logger.getLogger(serviceAreaToAllCarLinks.class);
	Set<String> zones = new HashSet<>();
	Map<String, Geometry> zoneMap = new HashMap<>();
	static String networkFilePath = null;
	static String shapeFilePath = null;
	static String drtTag = null;
	static String shapeFeature = null;
	File networkFile = null;
	File shapeFile = null;

	String networkfolder = null;
	String outputNetworkFile = null;

	Network network = NetworkUtils.createNetwork();

	List<String> zoneList = new ArrayList<String>();
	double bufferRange = 700;
	Map<Id<Link>, String> linkToZoneMap = new HashMap<>();

	public serviceAreaToAllCarLinks(String networkFilePath, String shapeFilePath, String shapeFeature, String drtTag) {
		this.networkFile = new File(networkFilePath);
		// this.shapeFile = new File(shapeFilePath);
		this.networkfolder = networkFile.getParent();
		this.outputNetworkFile = networkfolder + "\\drtServiceAreaNetwork.xml.gz";
		// readShape(shapeFile, shapeFeature);
	}

	// Main function creates the class and runs it!
	public static void main(String[] args) {
		serviceAreaToAllCarLinks.run(args[0], args[1]);
		//serviceAreaToAllCarLinks.run("D:\\Matsim\\Axer\\BSWOB2.0_Scenarios\\network\\vw219_SpeedCal.xml", "drt");

	}

	public static void run(String networkFilePath, String drtTag) {
		// Run constructor and initialize shape file
		LOG.info("Creating DRT Service area by assigning " + drtTag + " to network links that are within shape "
				+ shapeFilePath);
		serviceAreaToAllCarLinks serviceArea = new serviceAreaToAllCarLinks(networkFilePath, shapeFilePath,
				shapeFeature, drtTag);
		serviceArea.assignServiceAreatoNetwork(drtTag);
	}

	private void initalizeLinkMap() {

		new MatsimNetworkReader(this.network).readFile(networkFile.toString());

	}

	private void assignServiceAreatoNetwork(String drtTag) {
		// //Load Network
		// new MatsimNetworkReader(this.network).readFile(networkFile.toString());
		initalizeLinkMap();

		int i = 0;
		for (Link l : this.network.getLinks().values()) {

			if (l.getAllowedModes().contains("car"))

			{
				Set<String> modes = new HashSet<>();
				modes.addAll(l.getAllowedModes());
				modes.add(drtTag);
				l.setAllowedModes(modes);
				i++;
			}
		}

		NetworkFilterManager nfm = new NetworkFilterManager(this.network);
		nfm.addLinkFilter(new NetworkLinkFilter() {

			@Override
			public boolean judgeLink(Link l) {
				if (l.getAllowedModes().contains(drtTag))
					return true;
				else
					return false;
			}
		});

		Network avNetwork = nfm.applyFilters();
		NetworkFilterManager nfm2 = new NetworkFilterManager(avNetwork);
		nfm2.addLinkFilter(new NetworkLinkFilter() {
			@Override
			public boolean judgeLink(Link l) {
				return true;
			}
		});

		Network uncleanedAvNetwork = nfm2.applyFilters();
		new NetworkCleaner().run(avNetwork);
		for (Link l : uncleanedAvNetwork.getLinks().values()) {
			if (!avNetwork.getLinks().containsKey(l.getId())) {
				Link netLink = network.getLinks().get(l.getId());
				Set<String> modes = new HashSet<>();
				modes.addAll(l.getAllowedModes());
				modes.remove(drtTag);
				netLink.setAllowedModes(modes);
			}
		}

		System.out.println("Touched " + i + " Links within total network");
		new NetworkWriter(network).write(outputNetworkFile);

	}

	// private boolean isServiceAreaLink(Link l, String[] zoneList) {
	// //Construct a LineSegment from link coordinates
	// Coordinate start = new Coordinate(l.getFromNode().getCoord().getX(),
	// l.getFromNode().getCoord().getY());
	// Coordinate end = new Coordinate(l.getToNode().getCoord().getX(),
	// l.getToNode().getCoord().getY());
	// LineSegment lineSegment = new LineSegment(start, end);
	//
	// GeometryFactory f = new GeometryFactory();
	//
	// //1. Link needs to be in geographical area
	//
	// boolean relevantLink = false;
	// for (String z : Arrays.asList(zoneList)) {
	// //System.out.println("Check zone: "+z);
	// //Get geometry for zone
	// Geometry zone = zoneMap.get(z);
	//
	// if (zone.buffer(this.bufferRange).intersects(lineSegment.toGeometry(f))) {
	// //2. Link needs to be already available for car
	// if (l.getAllowedModes().contains("car")) {
	//
	// relevantLink = true;
	// return relevantLink;
	//
	// }
	//
	// }
	//
	//
	// }
	//
	// return relevantLink;
	//
	// }

	// private boolean isServiceAreaLinkMap(Link l, List<String> zoneList) {
	//
	// String linkZone = this.linkToZoneMap.get(l.getId());
	// if (zoneList.contains(linkZone)) return true;
	//
	// return false;
	//
	// }

}

package playground.toronto.demand.dprecated;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkFactoryImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;

import playground.toronto.maneuvers.NetworkAddEmmeManeuverRestrictions;

/**
 * A class which converts an EMME .211 file into a MATSim XML network
 * file. Performs several fixes, and also allows several options.
 * DOES NOT HANDLE TRANSIT (not enough detail in the EMME transit 
 * network definition to convert one-to-one).
 * 
 * ACTIONS PERFORMED BY THIS CLASS:
 * 		(in order)
 * 	0. Read in an EMME network file (.211), converting the EMME modes to
 * 		a comma-separated list for MATSim and flagging special links. It
 * 		also converts the speed limits from km/hr to m/s and changes link
 * 		length from km to m.
 *  1. Re-draw special lanes [optional]: Identifies links that are part 
 *  	of special lanes and re-draws them in their proper position. 
 *  	Applies to streetcar ROWs like Spadina, as well as HOV
 *  	lanes. HOV lanes are given a type "HOV" for future handling
 *  	of HOV lanes to be implemented. 
 *  2. Exclude virtual links [optional]: Removes centroid connectors and 
 *  	walk-transfer links from the network. 
 *  3. Add turn restrictions [optional]:  Calls procedures from the ManeuverCreation
 *  	package (which also has a stand-alone main() method)
 *  4. Convert coordinates [optional]: Converts the coordinates (of NODES ONLY) 
 *  	from one coordinate system to another (MUST BE SPECIFIED IN
 *  	MGC.java). Currently works FROM WGS84 to NAD17N, but does NOT
 *  	work in the other direction.
 *  5. Choke network capacity [optional]: Reduces the network capacity by a user
 *  	specified factor.
 *  6. Finally, it outputs the MATSim network to a user-speicifed XML file.
 * 
 * ARGS: 
 * 	- [0] inputFileName = the file location of the .211 file to be converted
 * 	- [1] outputFolder = the folder location to export the file to. File will
 * 		be named to [args[0]].xml
 * 
 * 	The remaining args can be in any order.
 * 
 *	'-a [String inSystem] [String outSystem]' = Allows converting from one 
 *		coordinate system to another. [inSystem] = the coordinate system to 
 *		convert form. [outSystem] = the coordinate system to convert to. 
 *		Coordinate systems must match those defined in MGC.java. This procedure
 *		is stable (and working) but create a large number of errors and warnings.
 *		These can safely be ignored.
 *
 *	'-b' = Flag for re-drawing special lanes.
 *
 *	'-c' = Flag for excluding virtual links.
 *
 *	'-d [Double factor]' = Allows 'choking' the capacity of roads on the
 *		network. [factor] = a factor to multiply the capacity of each link
 *		by.
 *
 *	'-e [turnsFileName] [removeUTurns] [linkSep] [expRad]' = Adds turn restrictions 
 *		to the network. [turnsFileName] is the name of the file which specifies the 
 *		EMME turn restrictions file. [removeUTurns] is a boolean flag to remove
 *		U-turns from the network. [linkSep] is the desired link separation (m).
 *		[expRad] is the expansion radius (m).
 *
 *	'-f [shapeFileName]' = Exports links to ArcGIS shapefile format using the Link2ESRIShape
 *		method. Does not export nodes at the moment.
 *
 * @author pkucirek
 *
 */
@Deprecated
public class oldEmme2MatsimConverter {

	private static CoordinateTransformation coordinateTransformation;
	private static NetworkImpl network;
	
	
	public static void main(String[] args) throws Exception{
		List<String> args1 =  Arrays.asList(args);
		
		if(!(args1.size() >= 2 ) || (args1.contains("-h") || (args1.contains("help")))){ 
			printHelp();
			return;
		}
		boolean isConvertingCoords = args1.contains("-a");
		boolean isReDrawingLanes = args1.contains("-b");
		boolean isRemovingVirtualLinks = args1.contains("-c");
		boolean isChokingNetwork = args1.contains("-d");
		boolean isAddingManeuvers = args1.contains("-e");
		boolean isExportingLinksToESRI = args1.contains("-f");
		
		readNetwork(args[0]);
		
		if(isReDrawingLanes)reDrawLinks();
		
		if (isRemovingVirtualLinks) removeVirtualLinks();
		
		if (isAddingManeuvers){
			int i = args1.indexOf("-e");
			String filename = args1.get(i + 1);
			boolean removeUTurns = Boolean.parseBoolean(args1.get(i+2));
			double linkSep = Double.parseDouble(args1.get(i+3));
			double expRad = Double.parseDouble(args1.get(i+4));
			
			restrictTurns(filename, removeUTurns, linkSep, expRad);
		}
		
		if (isConvertingCoords) {
			int i = args1.indexOf("-a");
			String fromSystem = args1.get(i + 1);
			String toSystem = args1.get(i + 2);
			
			try {
				coordinateTransformation = TransformationFactory.getCoordinateTransformation(fromSystem, toSystem);
				convertCoordinates();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
		
		if (isChokingNetwork){
			int i = args1.indexOf("-d");
			double factor = Double.parseDouble(args[i + 1]);
			
			chokeNetwork(factor);
		}
		
		if (isExportingLinksToESRI){
			int i = args1.indexOf("-f");
			String filename = args1.get(i+1);
			writeLinks2ESRI(filename);
		}
		
		String[] path = args[0].split("\\\\|/");
		String outputfile = args[1] + "/" + path[path.length - 1].replace(".211",".xml");
		writeNetworkXML(outputfile);
		
	}
	
	private static Set<String> convertMode(String modes){		
		
		ArrayList<String> a = new ArrayList<String>();
		
		char[] c = modes.toCharArray();
		
		for(int i = 0; i < c.length; i++){
			switch (c[i]) {
			case 'c' : a.add("Car"); break;
			case 'w' : a.add("Walk"); break;
			case 't' : a.add("Transfer"); break;
			case 'h' : a.add("Car"); break;
			case 'b' : a.add("Bus"); break;
			case 'm' : a.add("Subway"); break;
			case 'r' : a.add("Train"); break;
			case 'g' : a.add("Bus"); break;
			case 's' : a.add("Streetcar"); break;
			case 'l' : a.add("LRT"); break;
			case 'i' : a.add("Car"); break;
			case 'f' : a.add("Truck"); break;
			case 'e' : a.add("Truck"); break;
			case 'd' : a.add("Truck"); break;
			case 'j' : a.add("Car"); break;
			case 'a' : a.add("Transfer"); break;
			case 'p' : a.add("Bus"); break;
			case 'q' : a.add("Bus"); break;
			case 'u' : a.add("Transfer"); break;
			case 'v' : a.add("Transfer"); break;
			case 'y' : a.add("Walk"); break;
			}
		}
		
		HashSet<String> out = new HashSet<String>(a);
		
		return out;
	}

	private static String createType(String[] cells){
		//0 1    2     3      4      5  6 7  8 9  10
		//a 251 10274 0.12 chifedjv 106 2 90 0 40 9999
		
		//HOV links
		if (cells[7].equals("41") && Double.parseDouble(cells[3]) > 0) 
			if (Double.parseDouble(cells[3]) > 0) return "HOV";
			else return "HOV transfer";
		
		//Centroid Connectors
		if ((Integer.parseInt(cells[1]) < 10000) || (Integer.parseInt(cells[2]) < 10000)) return "CC";
		
		//Highway 407 toll road
		if (cells[7].equals("14")) return "Toll Highway";
		
		if (cells[7].equals("11")) return "Highway";
		
		if(cells[7].equals("13") || cells[7].equals("15")) return "On/Off Ramp";
		
		//Exclusive streetcar ROW
		if (cells[4].equals("sl") || cells[4].equals("s" )| cells[4].equals("l")) return "Streetcar ROW";
		
		//Transfer to transit (only contains walk/transfer modes)
		if ((cells[4].contains("t") || cells[4].contains("u") || cells[4].contains("v") || cells[4].contains("w") || cells[4].contains("a") || cells[4].contains("y")) 
				&& ( !cells[4].contains("c") && !cells[4].contains("h")  && !cells[4].contains("b") && !cells[4].contains("m") && !cells[4].contains("r") 
						&& !cells[4].contains("g") && !cells[4].contains("s") && !cells[4].contains("l") && !cells[4].contains("i") && !cells[4].contains("f")  
						&& !cells[4].contains("e") && !cells[4].contains("d") && !cells[4].contains("j")  && !cells[4].contains("p")  && !cells[4].contains("q"))) 
			return "Transfer";
		
		return "";
	}
	
	private static void readNetwork(String filename) throws IOException{
		
		network = NetworkImpl.createNetwork();
		//capperiod="1:00:00"
		network.setCapacityPeriod(60 * 60); //1 hour, in sec
		
		System.out.println("Reading file \"" + filename + "\"...");
		
		BufferedReader emmeReader =  new BufferedReader(new FileReader(filename));
		boolean isReadingNodes = false;
		boolean isReadingLinks = false;
		
		String line;
		while((line = emmeReader.readLine()) != null){
			if(line.startsWith("t nodes")){
				isReadingNodes = true;
				isReadingLinks = false;
				continue;
			}
			else if(line.startsWith("t links")){
				isReadingLinks = true;
				isReadingNodes = false;
				continue;
			}
			
			if(isReadingNodes){
				String[] cells = line.split("\\s+");
				if (cells.length != 8){
					if (!line.trim().isEmpty()) System.err.println("WARN: Skipped line \"" + line + "\", invalid number of arguments.");
					
					continue;
				}
				boolean isZone = cells[0].contains("*");
				
				NodeImpl n = new NodeImpl(Id.create(cells[1], Node.class));
				final double y = Double.parseDouble(cells[3].length() == 6 ? "4" + cells[3] : cells[3]);
				n.setCoord(new Coord(Double.parseDouble(cells[2]), y));
				//Some EMME networks are restricted to using only 6 characters for the y-coordinate. This appends a '4' to the start if this is the case.
				
				n.setType(isZone ? "Zone" : "");
				
				network.addNode(n);
			}
			else if (isReadingLinks){
				//0 1    2     3      4      5  6 7  8 9  10
				//a 251 10274 0.12 chifedjv 106 2 90 0 40 9999
				
				String[] cells = line.split("\\s+");
				if (cells.length != 11){
					if (!line.trim().isEmpty()) System.err.println("WARN: Skipped line \"" + line + "\", invalid number of arguments.");
					
					continue;
				}
				
				Node i = network.getNodes().get(Id.create(cells[1], Node.class));
				Node j = network.getNodes().get(Id.create(cells[2], Node.class));
				double length = Double.parseDouble(cells[3]) * 1000; //converts km to m
				double speed = Double.parseDouble(cells[9]) / 3.6; //converts km/hr to m/s
				double cap = Double.parseDouble(cells[10]);
				double lanes = Double.parseDouble(cells[6]);
				if (lanes == 0.0) lanes = 1.0; //ensures that transit-only links have at least one lane.
				
				LinkFactoryImpl factory = new LinkFactoryImpl();
				
				LinkImpl l = (LinkImpl) factory.createLink(Id.create(cells[1] + ">" + cells[2], Link.class), 
						i, j, network, length, speed, cap, lanes);
				Link L = factory.createLink(Id.create(cells[1] + ">" + cells[2], Link.class), 
						i, j, network, length, speed, cap, lanes);
				
				L.setAllowedModes(convertMode(cells[4]));
				((LinkImpl) L).setType(createType(cells));
				
				
				l.setAllowedModes(convertMode(cells[4]));
				l.setType(createType(cells));

				network.addLink(l);
				
			}
			
		}
		
		System.out.print("done.");
		System.out.println("Network contains " + network.getNodes().size() + " nodes and " + network.getLinks().size() + " links.");
	}
	
	private static void writeNetworkXML(String filename){
		
		NetworkWriter writer = new NetworkWriter(network);
		writer.write(filename);
		
	}
	
	private static void writeLinks2ESRI(String filename){
		
		Links2ESRIShape e = new Links2ESRIShape(network, filename, TransformationFactory.NAD83_UTM17N);
		
		e.write();
		
	}
	
	private static void convertCoordinates(){
		for(Node n : network.getNodes().values()){
			coordinateTransformation.transform(n.getCoord());
		}
		
		//CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.NAD83_UTM17N);
		//Coord c = transformation.transform(new CoordImpl(-81, 53.5));

	}
	
	private static void reDrawLinks(){
		//Takes links which have been drawn in offset positions
		//for special EMME handling (because EMME only permits
		//two links for each node pair). Since MATSim is free
		//from this restriction, this algorithm re-draws these
		//links in their 'proper' locations.
		//
		//Currently only applies to two types of links: 
		// - HOV links, connected to the network with links of 
		//		'0' length. These have been previously
		//		identified by the createType method and have
		//		a 'Type' property = "HOV".
		// - Dedicated Streetcar links (such as the Spadina
		//		line or the St. Clair line). These links 
		//		only allow streetcars on them ('Modes' = 
		//		"Streetcar") and are connected to the network
		//		by Walk/Transfer links.
		
		System.out.println("Re-drawing HOV and streetcar ROWs...");
		
		//HOV links
		ArrayList<Id> hovs = new ArrayList<Id>();
		for (Link i : network.getLinks().values()) {
			LinkImpl L = (LinkImpl) i;
			if(L.getType() == "HOV") hovs.add(L.getId());
		}
		
		System.out.println(hovs.size() + " links are flagged as HOV");
		
		//	._______._______________.___________._______.
		//	|		|				|			|		|
		//==*=======*===============*===========*=======*===
		//
		//Algorithm 'follows' transfer links to the 'main' road,
		//re-attaches the HOV link to the proper points. Transfer
		//links are removed.
		
		ArrayList<Id> linksToRemove = new ArrayList<Id>();
		
		for (Id i : hovs){
			Link hovLane = network.getLinks().get(i);
			
			//Get incoming transfer link
			Link incomingTransfer = null;
			for (Link L : hovLane.getFromNode().getInLinks().values()) {
				if (L.getLength() == 0.0) {
					if (incomingTransfer != null) 
						System.out.println("Check here.");
					incomingTransfer = L; 
				}
			}
			linksToRemove.add(incomingTransfer.getId());
			
			//Get outgoing transfer link
			Link outgoingTransfer = null;
			for (Link L : hovLane.getToNode().getOutLinks().values()){
				if (L.getLength() == 0.0) {
					if (outgoingTransfer != null) 
						System.out.println("Check here.");
					outgoingTransfer = L; 
				}
			}
			linksToRemove.add(outgoingTransfer.getId());
			
			//Migrate start/end nodes to correct position.
			hovLane.setFromNode(incomingTransfer.getFromNode());
			hovLane.setToNode(outgoingTransfer.getToNode());
		}
		
		System.out.println("HOVs done. Starting Streetcar ROWs");
		
		//Streetcar ROWs
		ArrayList<Id> rows = new ArrayList<Id>();
		for (Link i : network.getLinks().values()) {
			LinkImpl L = (LinkImpl) i;
			if(L.getType().equals("Streetcar ROW")) rows.add(L.getId());
		}
		
		for (Id i : rows){
			
			//   M----._________._______._______.----M
			//		  |			|		|		|
			//========*=========*=======*=======*=======
			// 
			// Similar to HOV lanes, except that the start/end points
			// of the entire line are subway/metro stations.
			// In general uses the same algorithm as above, except
			// that it allows for multiple incoming/outgoing
			// transfer links.
			
			Link lrtLane = network.getLinks().get(i);
			boolean hasIncomingTransitLink = false;
			boolean hasOutgoingTransitLink = false;
			
			//Get incoming transfer link
			ArrayList<Link> incomingTransfers = new ArrayList<Link>();
			for (Link L : lrtLane.getFromNode().getInLinks().values()) {
				LinkImpl l = (LinkImpl) L;
				if (l.getType().equals("Transfer")) incomingTransfers.add(L);
				if (l.getType().equals("Streetcar ROW")) hasIncomingTransitLink = true;
			}
			for (Link l : incomingTransfers) linksToRemove.add(l.getId());
			
			//Get outgoing transfer link
			ArrayList<Link> outgoingTransfers = new ArrayList<Link>();
			for (Link L : lrtLane.getToNode().getOutLinks().values()){
				LinkImpl l = (LinkImpl) L;
				if (l.getType().equals("Transfer")) outgoingTransfers.add(L);
				if (l.getType().equals("Streetcar ROW")) hasOutgoingTransitLink = true;
			}
			for (Link l : outgoingTransfers) linksToRemove.add(l.getId());
	
			
			//Migrate start/end nodes to correct position.
			//Four situations are handled:
			// 1. Start/end points of the ROW with multiple transfers 
			// 2. Mid-route links with only one transfer
			// 3. Mid-route links which have no transfers at one or both ends (connect to ROW-C streetcar network)
			// 4. Mid-route links with multiple transfers are flagged for checking,
			//		as I don't intend to deal with them now (requires splitting of links).
			
			ArrayList<Id> tweakedNodes = new ArrayList<Id>();
			
			//from Node
			if (!hasIncomingTransitLink){//start point
				double sumX = 0.0;
				double sumY = 0.0;
				
				if (incomingTransfers.size() == 0) break;
				
				for (Link L : incomingTransfers) {
					sumX += L.getFromNode().getCoord().getX();
					sumY += L.getFromNode().getCoord().getY();
				}
				
				if (!tweakedNodes.contains(lrtLane.getFromNode().getId())){
					NodeImpl N = (NodeImpl) lrtLane.getFromNode();
					N.setCoord(new Coord(sumX / incomingTransfers.size(), sumY / incomingTransfers.size()));
					tweakedNodes.add(N.getId());
				}
			}
			else if (incomingTransfers.size() == 1){
				lrtLane.setFromNode(incomingTransfers.get(0).getFromNode());
			}
			
			//to node
			if (!hasOutgoingTransitLink){
				double sumX = 0.0;
				double sumY = 0.0;
				
				if (outgoingTransfers.size() == 0) break;
				
				for (Link L : outgoingTransfers) {
					sumX += L.getToNode().getCoord().getX();
					sumY += L.getToNode().getCoord().getY();
				}
				
				if (!tweakedNodes.contains(lrtLane.getToNode().getId())){
					NodeImpl N = (NodeImpl) lrtLane.getToNode();
					N.setCoord(new Coord(sumX / outgoingTransfers.size(), sumY / outgoingTransfers.size()));
					tweakedNodes.add(N.getId());
				}
			}
			else if (outgoingTransfers.size() == 1){
				lrtLane.setToNode(outgoingTransfers.get(0).getToNode());
			}
			
		}
		//Clear out unconnected transfer nodes.
		for (Id i : linksToRemove){
			network.removeLink(i);
		}
		
		System.out.println("Done. " + linksToRemove.size() + " transfer links removed from the network.");
	}
	
	private static void removeVirtualLinks(){
		//Removes CCs and walk/transfer links
		//Also removes zones.
		
		ArrayList<Id> linksToRemove = new ArrayList<Id>();
		
		for(Link a : network.getLinks().values()){
			LinkImpl link = (LinkImpl) a;
			
			if((link.getType() == "CC") || (link.getType() == "Transfer"))
				linksToRemove.add(link.getId()); 
		}
		
		for (Id i : linksToRemove) network.removeLink(i);
		
		ArrayList<Id> nodesToRemove = new ArrayList<Id>();
		for (Node n : network.getNodes().values()){
			NodeImpl N = (NodeImpl) n;
			if (N.getType().equals("Zone")) nodesToRemove.add(N.getId());
		}
		
		for (Id i : nodesToRemove) network.removeNode(i);
		
	}
	
	private static void chokeNetwork(double factor){
		
		for (Link l : network.getLinks().values()){
			l.setCapacity(l.getCapacity() * factor);
		}
		
	}
	
	private static void restrictTurns(String filename, boolean removeUTurns, double linkSep, double expRad) throws Exception{
		
		NetworkAddEmmeManeuverRestrictions naemr = new NetworkAddEmmeManeuverRestrictions(filename);
		naemr.removeUTurns = removeUTurns;
		naemr.linkSeparation = linkSep;
		naemr.expansionRadius = expRad;
		
		naemr.run(network);
		new NetworkCleaner().run(network);
	}
	
	private static void printHelp(){
		System.out.println(" This class which converts an EMME .211 file into a MATSim XML network");
		System.out.println(" file. Performs several fixes, and also allows several options.");
		System.out.println(" DOES NOT HANDLE TRANSIT (not enough detail in the EMME transit ");
		System.out.println(" network definition to convert one-to-one).");
		System.out.println(" ");
		System.out.println(" ACTIONS PERFORMED BY THIS CLASS:");
		System.out.println(" 		(in order)");
		System.out.println(" 	0. Read in an EMME network file (.211), converting the EMME modes to");
		System.out.println(" 		a comma-separated list for MATSim and flagging special links. It");
		System.out.println(" 		also converts the speed limits from km/hr to m/s and changes link");
		System.out.println(" 		length from km to m.");
		System.out.println("  1. Re-draw special lanes [optional]: Identifies links that are part ");
		System.out.println("  	of special lanes and re-draws them in their proper position. ");
		System.out.println("  	Applies to streetcar ROWs like Spadina, as well as HOV");
		System.out.println("  	lanes. HOV lanes are given a type \"HOV\" for future handling");
		System.out.println("  	of HOV lanes to be implemented. ");
		System.out.println("  2. Exclude virtual links [optional]: Removes centroid connectors and ");
		System.out.println("  	walk-transfer links from the network. ");
		System.out.println("  3. Add turn restrictions [optional]:  Calls procedures from the ManeuverCreation");
		System.out.println("  	package (which also has a stand-alone main() method)");
		System.out.println("  4. Convert coordinates [optional]: Converts the coordinates (of NODES ONLY) ");
		System.out.println("  	from one coordinate system to another (MUST BE SPECIFIED IN");
		System.out.println("  	MGC.java). Currently works FROM WGS84 to NAD17N, but does NOT");
		System.out.println("  	work in the other direction.");
		System.out.println("  5. Choke network capacity [optional]: Reduces the network capacity by a user");
		System.out.println("  	specified factor.");
		System.out.println("  6. Finally, it outputs the MATSim network to a user-speicifed XML file.");
		System.out.println(" ");
		System.out.println(" ARGS: ");
		System.out.println(" 	- [0] inputFileName = the file location of the .211 file to be converted");
		System.out.println(" 	- [1] outputFolder = the folder location to export the file to. File will");
		System.out.println(" 		be named to [args[0]].xml");
		System.out.println(" ");
		System.out.println(" 	The remaining args can be in any order.");
		System.out.println(" ");
		System.out.println("	'-a [String inSystem] [String outSystem]' = Allows converting from one ");
		System.out.println("		coordinate system to another. [inSystem] = the coordinate system to ");
		System.out.println("		convert form. [outSystem] = the coordinate system to convert to. ");
		System.out.println("		Coordinate systems must match those defined in MGC.java. This procedure");
		System.out.println("		is stable (and working) but create a large number of errors and warnings.");
		System.out.println("		These can safely be ignored.");
		System.out.println("");
		System.out.println("	'-b' = Flag for re-drawing special lanes.");
		System.out.println("");
		System.out.println("	'-c' = Flag for excluding virtual links.");
		System.out.println("");
		System.out.println("	'-d [Double factor]' = Allows 'choking' the capacity of roads on the");
		System.out.println("		network. [factor] = a factor to multiply the capacity of each link");
		System.out.println("		by.");
		System.out.println("");
		System.out.println("	'-e [turnsFileName] [removeUTurns] [linkSep] [expRad]' = Adds turn restrictions ");
		System.out.println("		to the network. [turnsFileName] is the name of the file which specifies the ");
		System.out.println("		EMME turn restrictions file. [removeUTurns] is a boolean flag to remove");
		System.out.println("		U-turns from the network. [linkSep] is the desired link separation (m).");
		System.out.println("		[expRad] is the expansion radius (m).");
		System.out.println("");
		System.out.println("	'-f [shapeFileName]' = Exports links to ArcGIS shapefile format using the Link2ESRIShape");
		System.out.println("		method. Does not export nodes at the moment.");
		System.out.println("");
		System.out.println(" @author pkucirek");
	}
	
	
	
}

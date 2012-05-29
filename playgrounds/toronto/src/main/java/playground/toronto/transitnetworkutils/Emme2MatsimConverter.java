package playground.toronto.transitnetworkutils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkFactoryImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.LinkImplTest;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * A class which converts an EMME .211 file into a MATSim XML network
 * file. Performs several fixes, and also allows several options.
 * DOES NOT HANDLE TRANSIT (not enough detail in the EMME transit 
 * network definition to convert one-to-one).
 * 
 * MANDATORY CHANGES
 * 	- Re-format as XML
 * 	- Converts freeflow speed from km/hr to m/s
 * 	- Converts from EMME modes to comma-separated list of MATSim modes
 *
 * OPTIONS
 *  - Convert coordinates: Converts the coordinates (of NODES ONLY) 
 *  	from one coordinate system to another (MUST BE SPECIFIED IN
 *  	MGC.java)
 *  - Re-draw special lanes: Identifies links that are part of 
 *  	special lanes and re-draws them in their proper position. 
 *  	Applies to streetcar ROWs like Spadina, as well as HOV
 *  	lanes. HOV lanes are given a type "HOV" for future handling
 *  	of HOV lanes to be implemented.
 *  - Exclude virtual links: Removes centroid connectors and 
 *  	walk-transfer links from the network. 
 * 
 * ARGS: 
 * 	- [0] inputFileName = the file location of the .211 file to be converted
 * 	- [1] outputFolder = the folder location to export the file to. File will
 * 		be named to [args[0]].xml
 * 
 *	'-a [String inSystem] [String outSystem]' = Allows converting from one 
 *		coordinate system to another. [inSystem] = the coordinate system to 
 *		convert form. [outSystem] = the coordinate system to convert to. 
 *		Coordinate systems must match those defined in MGC.java.
 *
 *	'-b' = Flag for re-drawing special lanes.
 *
 *	'-c' = Flag for excluding virtual links.
 *
 *	'-d [Double factor]' = Allows 'choking' the capacity of roads on the
 *		network. [factor] = a factor to multiply the capacity of each link
 *		by.
 *
 *	'-e [turnsFileName]' = Adds turn restrictions to the network. [turnsFileName] 
 *		is the name of the file which specifies the EMME turn restrictions file.
 *
 * @author pkucirek
 *
 */
public class Emme2MatsimConverter {

	private static CoordinateTransformation coordinateTransformation;
	private static NetworkImpl network;
	
	
	public static void main(String[] args) throws IOException{
		List<String> args1 =  Arrays.asList(args);
		
		if(!(args1.size() >= 2 ) || (args1.contains("-h") || (args1.contains("help")))) printHelp();
		boolean isConvertingCoords = args1.contains("-a");
		boolean isReDrawingLanes = args1.contains("-b");
		boolean isRemovingVirtualLinks = args1.contains("-c");
		boolean isChokingNetwork = args1.contains("-d");
		
		readNetwork(args[0]);
		
		if(isReDrawingLanes)reDrawLinks();
		
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
		
		if (isRemovingVirtualLinks) removeVirtualLinks();
		
		if (isChokingNetwork){
			int i = args1.indexOf("-a");
			double factor = Double.parseDouble(args[i + 1]);
			
			chokeNetwork(factor);
		}
		
		writeNetworkXML(args[1]);
		
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
		if (cells[7] == "41") return "HOV";
		
		//Centroid Connectors
		if ((Integer.parseInt(cells[1]) < 10000) || (Integer.parseInt(cells[2]) < 10000)) return "CC";
		
		//Highway 407 toll road
		if (cells[7] == "14") return "Toll Highway";
		
		//Exclusive streetcar ROW
		if (cells[4] == "sl" || cells[4] == "s" || cells[4] == "l") return "Streetcar ROW";
		
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
				String[] cells = line.split(" ");
				if (cells.length != 8){
					if (!line.trim().isEmpty()) System.err.println("WARN: Skipped line \"" + line + "\", invalid number of arguments.");
					
					continue;
				}
				boolean isZone = cells[0].contains("*");
				
				NodeImpl n = new NodeImpl(new IdImpl(cells[1]));
				n.setCoord(new CoordImpl(cells[2], 
						cells[3].length() == 6 ? "4" + cells[3] : cells[3])); 
				//Some EMME networks are restricted to using only 6 characters for the y-coordinate. This appends a '4' to the start if this is the case.
				
				n.setType(isZone ? "Zone" : "");
				
				network.addNode(n);
			}
			else if (isReadingLinks){
				//0 1    2     3      4      5  6 7  8 9  10
				//a 251 10274 0.12 chifedjv 106 2 90 0 40 9999
				
				String[] cells = line.split(" ");
				if (cells.length != 11){
					if (!line.trim().isEmpty()) System.err.println("WARN: Skipped line \"" + line + "\", invalid number of arguments.");
					
					continue;
				}
				
				Node i = network.getNodes().get(new IdImpl(cells[1]));
				Node j = network.getNodes().get(new IdImpl(cells[2]));
				double length = Double.parseDouble(cells[3]) * 1000; //converts km to m
				double speed = Double.parseDouble(cells[9]) / 3.6; //converts km/hr to m/s
				double cap = Double.parseDouble(cells[10]);
				double lanes = Double.parseDouble(cells[6]);
				
				LinkFactoryImpl factory = new LinkFactoryImpl();
				
				LinkImpl l = (LinkImpl) factory.createLink(new IdImpl(cells[1] + ">" + cells[2]), 
						i, j, network, length, speed, cap, lanes);
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
	
	/**
	 * Currently broken, something with my WKT definition of NAD83_UTM17N doesn't work. But should work for other jurisdictions.
	 */
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
		
		//HOV links
		ArrayList<Id> hovs = new ArrayList<Id>();
		for (Link i : network.getLinks().values()) {
			LinkImpl L = (LinkImpl) i;
			if(L.getType() == "HOV") hovs.add(L.getId());
		}
		//	._______._______________.___________._______.
		//	|		|				|			|		|
		//==*=======*===============*===========*=======*===
		//
		//Algorithm 'follows' transfer links to the 'main' road,
		//re-attaches the HOV link to the proper points. Transfer
		//links are removed.
		
		for (Id i : hovs){
			Link hovLane = network.getLinks().get(i);
			
			
		}
		
		
		
		//Streetcar ROWs
		ArrayList<Id> rows = new ArrayList<Id>();
		for (Link i : network.getLinks().values()) {
			LinkImpl L = (LinkImpl) i;
			if(L.getType() == "Streetcar ROW") rows.add(L.getId());
		}
		
		
	}
	
	private static void removeVirtualLinks(){
		//Removes CCs and walk/transfer links
		
		ArrayList<Id> linksToRemove = new ArrayList<Id>();
		
		for(Link a : network.getLinks().values()){
			LinkImpl link = (LinkImpl) a;
			
			if((link.getType() == "CC") || 
					((link.getAllowedModes().contains("Walk") || link.getAllowedModes().contains("Transfer")) 
					&& !(link.getAllowedModes().contains("Car")) && !(link.getAllowedModes().contains("Bus")) 
					&& !(link.getAllowedModes().contains("Subway")) && !(link.getAllowedModes().contains("Streetcar"))
					&& !(link.getAllowedModes().contains("LRT")))) 
				linksToRemove.add(link.getId()); 
		}
		
		for (Id i : linksToRemove) network.removeLink(i);
		
	}
	
	private static void chokeNetwork(double factor){
		
		for (Link l : network.getLinks().values()){
			l.setCapacity(l.getCapacity() * factor);
		}
		
	}
	
	private static void printHelp(){
		 System.out.println(" A class which converts an EMME .211 file into a MATSim XML network");
		 System.out.println(" file. Performs several fixes, and also allows several options.");
		 System.out.println(" ");
		 System.out.println(" MANDATORY CHANGES");
		 System.out.println(" 	- Re-format as XML");
		 System.out.println(" 	- Converts freeflow speed from km/hr to m/s");
		 System.out.println(" 	- Converts from EMME modes to comma-separated list of MATSim modes");
		 System.out.println("");
		 System.out.println(" OPTIONS");
		 System.out.println("  - Convert coordinates: Converts the coordinates (of NODES ONLY) ");
		 System.out.println("  	from one coordinate system to another (MUST BE SPECIFIED IN");
		 System.out.println("  	MGC.java)");
		 System.out.println("  - Re-draw special lanes: Identifies links that are part of ");
		 System.out.println("  	special lanes and re-draws them in their proper position. ");
		 System.out.println("  	Applies to streetcar ROWs like Spadina, as well as HOV");
		 System.out.println("  	lanes. HOV lanes are given a type \"HOV\" for future handling");
		 System.out.println("  	of HOV lanes to be implemented.");
		 System.out.println("  - Exclude virtual links: Removes centroid connectors and ");
		 System.out.println("  	walk-transfer links from the network. ");
		 System.out.println(" ");
		 System.out.println(" ARGS: ");
		 System.out.println(" 	- [0] inputFileName = the file location of the .211 file to be converted");
		 System.out.println(" 	- [1] outputFolder = the folder location to export the file to. File will");
		 System.out.println(" 		be named to [args[0]].xml");
	}
	
	
	
}

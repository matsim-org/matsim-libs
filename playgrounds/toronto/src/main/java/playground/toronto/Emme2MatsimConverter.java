package playground.toronto;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.LineStringBasedFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.Nodes2ESRIShape;

import playground.toronto.maneuvers.NetworkAddEmmeManeuverRestrictions;

public class Emme2MatsimConverter {

	// ////////////////////////////////////////////////////////////////////
	// member variables
	// ////////////////////////////////////////////////////////////////////
	
	private static CoordinateTransformation coordinateTransformation = null;
	private static NetworkImpl network;
	
	private static final Logger log = Logger.getLogger(Emme2MatsimConverter.class);
	
	private static FileFilter d211Filter = new FileFilter() {
		@Override
		public String getDescription() {return "EMME base network file *.211, *.in";}
		@Override
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith( ".211" ) || f.getName().toLowerCase(Locale.ROOT).endsWith( ".in" );
		}
	};
	private static FileFilter turnsFilter = new FileFilter() {
		@Override
		public String getDescription() {return "EMME turns file *.txt";}
		@Override
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith( ".txt" );
		}
	};
	private static FileFilter shapefileFilter = new FileFilter() {
		@Override
		public String getDescription() {return "ESRI shapefile *.shp";}
		@Override
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith( ".shp" );
		}
	};
	private static FileFilter xmlFilter = new FileFilter() {
		@Override
		public String getDescription() {return "XML file *.xml";}
		@Override
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith( ".xml" );
		}
	};
	
	
	// ////////////////////////////////////////////////////////////////////
	// main method
	// ////////////////////////////////////////////////////////////////////
	
	public static void main(String[] args){
		
		JOptionPane.showMessageDialog(null, getHelp(), "Info", JOptionPane.INFORMATION_MESSAGE);
		
		if (args.length > 2){
			if(checkArgs(args)){
				runArgs(args);
			}else log.error("Terminated prematurely.");
		}else{
			try {
				if(runGUI()){
					
				}else log.info("Cancelled / Exited.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static boolean checkArgs(String[]args){
		//TODO add old handling of args. Or maybe not?
		return true;
	}
	
	private static void runArgs(String[] args){
		
	}
	
	private static boolean runGUI() throws IOException{
		
		//TODO migrate the GUI to the separate function calls
		
		String info = "";
		int choice;
		
		String EMMEfilename = "";
		JFileChooser fc = new JFileChooser(); 
		fc.setDialogType(JFileChooser.OPEN_DIALOG);
		fc.setDialogTitle("Please select an EMME network batchout file");
		fc.setFileFilter(d211Filter);
		choice = fc.showOpenDialog(null);
		if (choice == JFileChooser.APPROVE_OPTION){
			EMMEfilename = fc.getSelectedFile().getAbsolutePath();
		}else if (choice == JFileChooser.CANCEL_OPTION) return false;
		if (EMMEfilename == "" || EMMEfilename == null) return false;
		
		File EMMEFile = new File(EMMEfilename);
		readNetwork(EMMEFile);
		
		JCheckBox opt1 = new JCheckBox("Coordinate Transformation");
		JCheckBox opt2 = new JCheckBox("Re-Draw Special Lanes");
		JCheckBox opt3 = new JCheckBox("Exclude Virtual Links");
		JCheckBox opt4 = new JCheckBox("Apply Capacity Factor");
		JCheckBox opt5 = new JCheckBox("Add turn restrictions");
		JCheckBox opt6 = new JCheckBox("Export to ESRI Shapefile");
		JCheckBox opt7 = new JCheckBox("Filter modes");
		
		String message = "The Emme2Matsim network converter can also perform several additional operations on the network before exporting it. Additional information will be provided once your selection has been\nmade; you will also be allowed to cancel. Please select 0-6 of the following options. ";
		
		choice = 0;
		choice = JOptionPane.showOptionDialog(null, message, "test title", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{opt1, opt2, opt3, opt4, opt5, opt6, opt7, "OK", "Cancel"} , null);
		
		if (choice == JOptionPane.CLOSED_OPTION || choice == 8) return false;
		else if (choice == JOptionPane.OK_OPTION || choice == 7){
			
			if (opt2.isSelected()){
				//Re-draw links
				info = "RE-DRAW SPECIAL LINKS\n" +
						"----------------------\n\n" +
						"This option re-draws certain special links, namely HOV-lanes and Streetcar ROW\n" +
						"links which have been drawn in as running parallel to the network. Because\n" +
						"MATSim allows multiple links to connect two nodes, this algorithm identifies\n" +
						"these links, tracing their virtual (zero-length) connectors back to their\n" +
						"'proper' start/end nodes. No additional arguments are required.\n\n" +
						"Update July 2012: Some problems exist re-drawing HOVs on highways. For now,\n" +
						"this algorithm simply removes all HOV lanes from the network.\n\n" +
						"Proceed?";
				choice = JOptionPane.showConfirmDialog(null, info, "Info", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
				if (choice == JOptionPane.OK_OPTION){
					reDrawLinks();
				}
			}
			if (opt5.isSelected()){
				//Add turn restrictions
				info = "ADD TURN RESTRICTIONS\n" +
						"---------------------\n\n" +
						"This option calls the ManueverCreation class which parses the EMME turns restrictions file\n"
						+ "and re-draws the network to restrict certain turns. Four additional arguments are required:\n\n"
						+ "File filename: The EMME turn-restrictions file\n"
						+ "Boolean removeUTurns: Flag for allowing/restricting u-turns.\n" +
						"Double linkSep: Link separation paramter (m)\n" +
						"Double expRad: Expansion radius parameter (m)\n\n" +
						"Proceed?";
				choice = JOptionPane.showConfirmDialog(null, info, "Info", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
				if (choice == JOptionPane.OK_OPTION) {
					String turnsFile = "";
					fc.setDialogTitle("Please select an EMME turns file");
					fc.removeChoosableFileFilter(d211Filter);
					fc.setFileFilter(turnsFilter);
					choice = fc.showOpenDialog(null);
					if (choice == JFileChooser.APPROVE_OPTION){
						turnsFile = fc.getSelectedFile().getAbsolutePath();
						if (!(turnsFile == "") && (turnsFile != null)){
							boolean removeUTurns;
							choice = JOptionPane.showConfirmDialog(null, "Remove u-turns?", "Question", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
							if (choice != JOptionPane.CANCEL_OPTION){
								if(choice == JOptionPane.YES_OPTION) removeUTurns = true;
								else if (choice == JOptionPane.NO_OPTION) removeUTurns = false;
								else throw new IOException("Choice not recognized!");
																
								Double linkSep = null;
								boolean isParseable;
								message = "Please enter the link separation parameter (m)";
								do{
									try {
										String s = JOptionPane.showInputDialog(message);
										if (s == null) break;
										linkSep = Double.parseDouble(s);
										isParseable = true;
									} catch (NumberFormatException e) {
										message = "Incorrect number format!\nPlease enter the link separation parameter (m)";
										isParseable = false;
									}
								} while (!isParseable);
								
								if (linkSep != null){
									Double expRad = null;
									message = "Please enter the expansion radius parameter (m)";
									do{
										try {
											String s = JOptionPane.showInputDialog(message);
											if (s == null) break;
											expRad = Double.parseDouble(s);
											isParseable = true;
										} catch (NumberFormatException e) {
											message = "Incorrect number format!\nPlease enter the expansion radius parameter (m)";
											isParseable = false;
										}
									} while (!isParseable);
									
									if (expRad != null){									
										try {
											restrictTurns(turnsFile, removeUTurns, linkSep, expRad);
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								}
							}
						}
					}
				}
			}
			if (opt3.isSelected()){
				//Remove virtual links
				info = "REMOVE VIRTUAL LINKS\n" +
						"----------------------\n\n" +
						"This option removes centroid connectors and walk/transfer links from the\n" +
						"network. Useful in map-matching, but perhaps not for simulation.\n\n" +
						"Proceed?";
				choice = JOptionPane.showConfirmDialog(null, info, "Info", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
				if (choice == JOptionPane.OK_OPTION){
					removeVirtualLinks();
				}
			}	
			if (opt7.isSelected()){
				//Filter network by modes
				info = "FILTER NETWORK BY MODES\n" +
						"---------------------------------\n\n" +
						"This option filter the network by modes.\n\n" +
						"Proceed?";
				
				choice = JOptionPane.showConfirmDialog(null, info, "Info", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
				if (choice == JOptionPane.OK_OPTION){
					HashSet<String> modes = new HashSet<String>();
					for (Link L : network.getLinks().values()) {
						for (String s : L.getAllowedModes()) modes.add(s);
					}
					
					Set<String> S = CollectionUtils.stringToSet(JOptionPane.showInputDialog("Please enter a comma-delimited set of modes to filter\nModes available in network: " + modes.toString()));
					filterModes(S);
				}
			}
			
			if (opt1.isSelected()){
				//Coordinate Transformations
				info = "COORDINATE TRANSFORMATION\n" +
						"-----------------------------\n\n" +
						"This option transforms the node coordinates of the network from/to\n" +
						"user-specified coordinate systems. The coordinate systems are\n" +
						"defined by name, so be sure to consult MGC.java for some default\n" +
						"system names. Typically, the Toronto projection is defined as\n" +
						"\"EPSG:26917\" or as \"NAD83_UTM17N\" (both work), while the\n" +
						"sphere-based projection (ie, long/lat) is referred to as\n" +
						"\"WGS84\".\n\n" +
						"Proceed?";
				choice = JOptionPane.showConfirmDialog(null, info, "Info", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
				if (choice == JOptionPane.OK_OPTION){
					String inSystem = JOptionPane.showInputDialog("Please input the coordinate system to transform FROM (Input coordinate system)");
					String outSystem = JOptionPane.showInputDialog("Please input the coordinate system to transform TO (Output coordinate system)");
					
					try {
						coordinateTransformation = TransformationFactory.getCoordinateTransformation(inSystem, outSystem);
						convertCoordinates();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					}
				}	
			}
			
			if (opt4.isSelected()){
				//Capacity Factor
				info = "APPLY CAPACITY FACTOR\n" +
						"-------------------------\n\n" +
						"This option applies a user-specified factor to the base network\n" +
						"capacity (ie, 0.05 for a 5% sample).\n\n" +
						"Proceed?";
				choice = JOptionPane.showConfirmDialog(null, info, "Info", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
				if (choice == JOptionPane.OK_OPTION){
					double factor = 0;
					boolean isParseable;
					message = "Pleas enter a capacity factor ( > 0)";
					do{
						try {
							String s = JOptionPane.showInputDialog(message);
							if (s == null) break;
							factor = Double.parseDouble(s);
							if (factor > 0){
								isParseable = true;
							}else{
								message = "Factor must be positive!\nPleas enter a capacity factor ( > 0)";
								isParseable = false;
							}	
						} catch (NumberFormatException e) {
							message = "Incorrect number format!\nPleas enter a capacity factor ( > 0)";
							isParseable = false;
						}
					} while (!isParseable);
					
					chokeNetwork(factor);
				}
			}
			
			if (opt6.isSelected()){
				//Export to ESRI shapefile
				
				info = "EXPORT TO ESRI SHAPEFILE\n" +
						"----------------------------\n\n" +
						"This option exports the network to ESRI points & polylines. Uses the utilites\n" +
						"provided in org.matsim.utils.gis.matsim2esri.network package, and as such,\n" +
						"does not export modes or link types. Ah well.\n\n" +
						"Proceed?";
				choice = JOptionPane.showConfirmDialog(null, info, "Info", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
				if (choice == JOptionPane.OK_OPTION){
					String shpefileName = "";
					fc.setDialogTitle("Save ESRI shapefile");
					fc.removeChoosableFileFilter(turnsFilter);
					fc.removeChoosableFileFilter(d211Filter);
					fc.setFileFilter(shapefileFilter);
					fc.setDialogType(JFileChooser.SAVE_DIALOG);
					//EMMEFile.getName().replace(".211", "_links.shp");
					//EMMEFile.getName().replace(".211", "_ndoes.shp");
					fc.setSelectedFile(new File(EMMEFile.getName().replace(".211", ".shp"))); 
					choice = fc.showSaveDialog(null);
					if (choice == JFileChooser.APPROVE_OPTION){
						shpefileName = fc.getSelectedFile().getAbsolutePath();
						if (!(shpefileName == "") && (shpefileName != null)){
							if (!shpefileName.endsWith(".shp")) shpefileName += ".shp";
							String system = JOptionPane.showInputDialog("Please input the coordinate system of the network or blank for default (default = \"EPSG:26917\")");
							if (system.equals("")) system = "EPSG:26917";
							writeLinks2ESRI(shpefileName.replace(".shp", "_links.shp"), shpefileName.replace(".shp", "_nodes.shp"), system);
						}
					}
				}
			}
			
			String outFileName = "";
			fc.setDialogTitle("Save network XML file");
			fc.removeChoosableFileFilter(turnsFilter);
			fc.removeChoosableFileFilter(d211Filter);
			fc.removeChoosableFileFilter(shapefileFilter);
			fc.setFileFilter(xmlFilter);
			fc.setDialogType(JFileChooser.SAVE_DIALOG);
			choice = fc.showSaveDialog(null);
			if (choice == JFileChooser.APPROVE_OPTION){
				outFileName = fc.getSelectedFile().getAbsolutePath();
				if (!(outFileName == "") && (outFileName != null)){
					if (!outFileName.endsWith(".xml")) outFileName += ".xml";
					writeNetworkXML(outFileName);
				}
			}else if(choice == JFileChooser.CANCEL_OPTION) return false;
		}
		else{
			throw new IOException("Choice not recognized!");
		}
		
		return true;
		
	}
	
	
	// ////////////////////////////////////////////////////////////////////
	// sub functions
	// ////////////////////////////////////////////////////////////////////
	
	private static void filterModes(Set<String> modes){
		
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
		NetworkImpl filteredNetwork = NetworkImpl.createNetwork();
		filter.filter(filteredNetwork, modes);
		
		network = filteredNetwork;
	}
	
	private static String getHelp(){
		String s = "EMME 2 MATSIM CONVERTER\n" +
				"**********************************\n\n" +
				"This program converts an EMME .211 file into a MATSim XML network\n" +
				"file. Performs several fixes, and also allows several options.Assumes\n" +
				"that link freeflow speeds are stored in ul2 and capacities in ul3.\n" +
				"DOES NOT HANDLE TRANSIT (not enough detail in the EMME transit\n" +
				"network definition to convert one-to-one)\n\n" +
				"-----------------------------------\n" +
				"written by Peter Kucirek June 2012";
		
		return s;
	}
	
	private static void readNetwork(File f) throws IOException{
		
		network = NetworkImpl.createNetwork();
		//capperiod="1:00:00"
		network.setCapacityPeriod(60 * 60); //1 hour, in sec
		NetworkFactoryImpl factory = network.getFactory();
		
		
		log.info("Reading file \"" + f + "\"...");
		
		BufferedReader emmeReader =  new BufferedReader(new FileReader(f));
		boolean isReadingNodes = false;
		boolean isReadingLinks = false;
		
		String line;
		while((line = emmeReader.readLine()) != null){
			if (line.startsWith("c")) continue;
			
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
				/*if (cells.length != 7){
					if (!line.trim().isEmpty()) log.warn("Skipped line \"" + line + "\", invalid number of arguments.");

					
					continue;
				}*/
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
					if (!line.trim().isEmpty()) log.warn("Skipped line \"" + line + "\", invalid number of arguments.");
					
					continue;
				}
				
				Node i = network.getNodes().get(Id.create(cells[1], Node.class));
				Node j = network.getNodes().get(Id.create(cells[2], Node.class));
				double length = Double.parseDouble(cells[3]) * 1000; //converts km to m
				double speed = Double.parseDouble(cells[9]) / 3.6; //converts km/hr to m/s
				double lanes = Double.parseDouble(cells[6]);
				double cap = Double.parseDouble(cells[10]) * lanes;
				String modes = cells[4];
				if (lanes == 0.0) lanes = 1.0; //ensures that transit-only links have at least one lane.
				
				LinkImpl l = (LinkImpl) factory.createLink(Id.create(cells[1] + "-" + cells[2], Link.class), 
						i, j, network, length, speed, cap, lanes);
								
				l.setAllowedModes(convertMode(modes));
				l.setType(createType(cells));

				network.addLink(l);
				
				//Special handling of "l" and "q" modes (LRT/BRT ROW-B)
				if (modes.contains("l") || modes.contains("q")){
					l = (LinkImpl) factory.createLink(Id.create(cells[1] + "-" + cells[2] + "_TrROW", Link.class),
							i, j, network, length, speed, 9999, 1.0); //Duplicate link for the ROW
					
					HashSet<String> modeSet = new HashSet<String>();
					if (modes.contains("l")) modeSet.add("Streetcar");
					if (modes.contains("q")) modeSet.add("Bus");
					l.setAllowedModes(modeSet);
					
					l.setType("TransitROW");
					
					network.addLink(l);
				}
				
			}
			
		}
		
		emmeReader.close();
		
		log.info("Network contains " + network.getNodes().size() + " nodes and " + network.getLinks().size() + " links.");
	}
	
	private static void writeNetworkXML(String filename){
		
		NetworkWriter writer = new NetworkWriter(network);
		writer.write(filename);
		
	}
	
	private static void writeLinks2ESRI(String linksfile, String nodesfile, String system){
				
		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(network, system);
		builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
		Links2ESRIShape l = new Links2ESRIShape(network, linksfile, builder);
		l.write();
		
		Nodes2ESRIShape n = new Nodes2ESRIShape(network, nodesfile, system);
		n.write();
		
	}
	
	private static void convertCoordinates(){
		for(Node n : network.getNodes().values()){
			NodeImpl N = (NodeImpl) n;
			N.setCoord(coordinateTransformation.transform(n.getCoord()));
		}
		
		//CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.NAD83_UTM17N);
		//Coord c = transformation.transform(new CoordImpl(-81, 53.5));

	}
	
	/**
	 * 	Takes links which have been drawn in offset positions
		for special EMME handling (because EMME only permits
		two links for each node pair). Since MATSim is free
		from this restriction, this algorithm re-draws these
		links in their 'proper' locations. This is
		necessary to ensure that matsim doesn't pick these
		links for trip origins/destinations
		
		Currently only applies to two types of links:
		 - HOV links, connected to the network with links of 
				'0' length. These have been previously
				identified by the createType method and have
				a 'Type' property = "HOV".
		 - Dedicated Streetcar links (such as the Spadina
				line or the St. Clair line). These links 
				only allow streetcars on them ('Modes' = 
				"Streetcar") and are connected to the network
				by Walk/Transfer links.
	 */
	private static void reDrawLinks(){		
		
		//reDrawHOVs();
		removeHOVs();
		
		reDrawStreetcarROWs();
		
	}
	
	/**
	 * Simply clears the network of all HOV lanes.
	 */
	private static void removeHOVs(){
		
		
		HashSet<Id> linksToRemove = new HashSet<Id>(); 
		for (Link i : network.getLinks().values()) {
			LinkImpl L = (LinkImpl) i;
			if(L.getType().equals("HOV") || L.getType().equals("HOV transfer")){
				linksToRemove.add(L.getId());
			}
		}
		
		for (Id i : linksToRemove){
			network.removeLink(i);
		}
		//Clear unconnected nodes
		HashSet<Id> nodesToRemove = new HashSet<Id>();
		for (Node n : network.getNodes().values()) 
			if (n.getInLinks().size() == 0 && n.getOutLinks().size() == 0)
				nodesToRemove.add(n.getId());
		for (Id i : nodesToRemove) network.removeNode(i);
		
	}
	
	/**
	 *  Update July 2012: Originally, this algorithm applied
			arterial HOVs (ie, did not flag highway HOvs).
			Fixing highway HOVs however requires dealing
			with 'special case' lanes where the HOV lane
			'skips' over several main road links. 
	 */
	@Deprecated
	private static void reDrawHOVs(){
		
		//TODO implement HOV re-drawing.
		
		HashSet<Id> hovs = new HashSet<Id>(); 
		for (Link i : network.getLinks().values()) {
			LinkImpl L = (LinkImpl) i;
			if(L.getType().equals(TorontoLinkTypes.hov)) hovs.add(L.getId());
		}
		
		log.info("Re-drawing HOV lanes. " + hovs.size() + " links are flagged as HOV");
		
		//	._______.______.____.___.___________._______.
		//	|		|  <-sp.case->	|			|		|
		//==*=======*====*====*=====*===========*=======*===
		//
		//Algorithm 'follows' transfer links to the 'main' road,
		//re-attaches the HOV link to the proper points. Transfer
		//links are removed.
		
		HashSet<Id> linksToRemove = new HashSet<Id>();
				
		for (Id i : hovs){
			Link hovLane = network.getLinks().get(i);
			
			//Get incoming transfer link. 
			Link incomingTransfer = null;
			for (Link L : hovLane.getFromNode().getInLinks().values()) {
				LinkImpl l = (LinkImpl) L;
				if (l.getType().equals(TorontoLinkTypes.hovTransfer)) {
					if (incomingTransfer != null) 
						System.out.println("Check here.");
					incomingTransfer = L; 
				}
			}
			linksToRemove.add(incomingTransfer.getId());
			
			//Get outgoing transfer link
			Link outgoingTransfer = null;
			for (Link L : hovLane.getToNode().getOutLinks().values()){
				LinkImpl l = (LinkImpl) L;
				if (l.getType().equals(TorontoLinkTypes.hovTransfer)) {
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
		
		//Clear out unconnected transfer links.
		for (Id i : linksToRemove){
			network.removeLink(i);
		}
		//Clear unconnected nodes
		HashSet<Id> nodesToRemove = new HashSet<Id>();
		for (Node n : network.getNodes().values()) 
			if (n.getInLinks().size() == 0 && n.getOutLinks().size() == 0)
				nodesToRemove.add(n.getId());
		for (Id i : nodesToRemove) network.removeNode(i);
		
		log.info("Done. " + linksToRemove.size() + " transfer links removed from the network.");
	}
	
	private static void reDrawStreetcarROWs(){
		HashSet<Id> linksToRemove = new HashSet<Id>();
		
		ArrayList<Id> rows = new ArrayList<Id>();
		for (Link i : network.getLinks().values()) {
			LinkImpl L = (LinkImpl) i;
			if(L.getType().equals(TorontoLinkTypes.streetcarROW)) rows.add(L.getId());
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
				if (l.getType().equals(TorontoLinkTypes.transfer)) incomingTransfers.add(L);
				if (l.getType().equals(TorontoLinkTypes.streetcarROW)) hasIncomingTransitLink = true;
			}
			for (Link l : incomingTransfers) linksToRemove.add(l.getId());
			
			//Get outgoing transfer link
			ArrayList<Link> outgoingTransfers = new ArrayList<Link>();
			for (Link L : lrtLane.getToNode().getOutLinks().values()){
				LinkImpl l = (LinkImpl) L;
				if (l.getType().equals(TorontoLinkTypes.transfer)) outgoingTransfers.add(L);
				if (l.getType().equals(TorontoLinkTypes.streetcarROW)) hasOutgoingTransitLink = true;
			}
			for (Link l : outgoingTransfers) linksToRemove.add(l.getId());
	
			
			//Migrate start/end nodes to correct position.
			//Four situations are handled:
			// 1. Start/end points of the ROW with multiple transfers 
			// 2. Mid-route links with only one transfer
			// 3. Mid-route links which have no transfers at one or both ends (connect to ROW-C streetcar network)
			// 4. Mid-route links with multiple transfers are flagged for checking,
			//		as I don't intend to deal with them now (requires splitting of links).
			
			HashSet<Id> tweakedNodes = new HashSet<Id>();
			
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
		//Clear unconnected nodes
		HashSet<Id> nodesToRemove = new HashSet<Id>();
		for (Node n : network.getNodes().values()) 
			if (n.getInLinks().size() == 0 && n.getOutLinks().size() == 0)
				nodesToRemove.add(n.getId());
		for (Id i : nodesToRemove) network.removeNode(i);
		
		log.info("Done. " + linksToRemove.size() + " transfer links removed from the network.");
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
			if (N.getType() == null) continue;
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
	
	// ////////////////////////////////////////////////////////////////////
	// utility functions
	// ////////////////////////////////////////////////////////////////////
	
	private static Set<String> convertMode(String modes){		
		
		ArrayList<String> a = new ArrayList<String>();
		
		char[] c = modes.toCharArray();
		
		for(int i = 0; i < c.length; i++){
			switch (c[i]) {
			case 'c' : a.add("Car"); break;
			case 'w' : a.add("Walk"); break;
			case 'h' : a.add("Car"); break;
			case 'b' : a.add("Bus"); break;
			case 'm' : a.add("Subway"); break;
			case 'r' : a.add("Train"); break;
			case 'g' : a.add("Bus"); break;
			case 's' : a.add("Streetcar"); break;
			case 'f' : a.add("Truck"); break;
			case 'e' : a.add("Truck"); break;
			case 'd' : a.add("Truck"); break;
			case 'p' : a.add("Bus"); break;
			case 'u' : a.add("Transfer"); break;
			default : break;
			}
		}
		
		HashSet<String> out = new HashSet<String>(a);
		
		return out;
	}

	private static String createType(String[] cells){
		//0 1    2     3      4      5  6 7  8 9  10
		//a 251 10274 0.12 chifedjv 106 2 90 0 40 9999
		
		final List<String> hovVDFs = Arrays.asList(new String[]{"41","16","17"});
		
		//HOV links
		if (hovVDFs.contains(cells[7])) {
			if (cells[7].equals("17") || (Double.parseDouble(cells[3]) == 0.0 )) return TorontoLinkTypes.hovTransfer;
			else return TorontoLinkTypes.hov;
		}
		
		//Centroid Connectors
		if ((Integer.parseInt(cells[1]) < 10000) || (Integer.parseInt(cells[2]) < 10000)) 
			return TorontoLinkTypes.centroidConnector;
		
		//Highway 407 toll road
		if (cells[7].equals("14")) return TorontoLinkTypes.ETR407;
		
		if (cells[7].equals("11")) return TorontoLinkTypes.highway;
		
		if(cells[7].equals("13") || cells[7].equals("15")) return TorontoLinkTypes.ramp;
		
		//Exclusive streetcar ROW
		//if (cells[4].equals("sl") || cells[4].equals("s" )| cells[4].equals("l")) return TorontoLinkTypes.streetcarROW;
		
		//Transfer to transit (only contains walk/transfer modes)
		if ((cells[4].contains("t") || cells[4].contains("u") || cells[4].contains("v") || cells[4].contains("w") || cells[4].contains("a") || cells[4].contains("y")) 
				&& ( !cells[4].contains("c") && !cells[4].contains("h")  && !cells[4].contains("b") && !cells[4].contains("m") && !cells[4].contains("r") 
						&& !cells[4].contains("g") && !cells[4].contains("s") && !cells[4].contains("l") && !cells[4].contains("i") && !cells[4].contains("f")  
						&& !cells[4].contains("e") && !cells[4].contains("d") && !cells[4].contains("j")  && !cells[4].contains("p")  && !cells[4].contains("q"))) 
			return TorontoLinkTypes.transfer;
		
		return "";
	}
	

	

	
}

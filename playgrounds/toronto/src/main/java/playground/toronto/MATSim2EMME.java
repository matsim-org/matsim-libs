package playground.toronto;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordinateTransformation;

/**
 * A class for outputting a MATSim network to EMME. In the general case, this works, because both
 * EMME and MATSim use a simplified network of nodes and links, with similar properties. Some
 * specific issues, however:
 * 	- Since Matsim doesn't use zones explicitly, no zones will be created in the EMME network.
 *  - A GTA Model formulation is assumed in EMME, that is, ul2 = speed limit, ul3 = capacity
 *  - VDF and type are left at default values, since these have no equivalent in Matsim.
 * 
 * TODO Add coordinate transformation functionality (ie, WGS84 to UTM_NAD17N).
 * TODO Tweak the node-Id mapping to only b used when int.parse(Node.getId()) fails.
 * 
 * @author pkucirek
 *
 */
public class MATSim2EMME {

	private final static Logger log = Logger.getLogger(MATSim2EMME.class);
	private String mapFolder = "C:/Users/Peter Admin/Desktop/NETWORK DATA/MATSIM NETWORK/with turns";
	private Network network;
	private HashMap<String, Character> modeMap;
	private CoordinateTransformation coordinateTransformation;
	
	public MATSim2EMME(Network net){
		this.network = net;
		this.modeMap = null;
		this.coordinateTransformation = null;
	}
	public MATSim2EMME(Network net, HashMap<String, Character> map){
		this.network = net;
		this.modeMap = map;
		this.coordinateTransformation = null;
	}
	public MATSim2EMME(Network net, CoordinateTransformation ct){
		this.network = net;
		this.modeMap = null;
		this.coordinateTransformation = ct;
	}
	public MATSim2EMME(Network net, HashMap<String, Character> map, CoordinateTransformation ct){
		this.network = net;
		this.modeMap = map;
		this.coordinateTransformation = ct;
	}
	
	
	/**
	 * 
	 * @param filename - The name of the file to write.
	 */
	public void write(String filename) throws IOException{
	
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		final double MAX_LINK_LENGTH = 1 * (10^20);

		log.info("Writing to file " + filename);
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		
		writer.write("c MATSim Network Export\n");
		writer.write("c written on " + sdf.format(Calendar.getInstance().getTime()) + "\n");
		//writer.write("c Netowrk Name = " + network.);
		writer.write("c ------------------------------------------\n");
		
		writer.write("\nt nodes init");
		
		//Just in case the node IDs are not parse-able integers
		HashMap<Id, Integer> nodeIdMap = new HashMap<Id, Integer>();
		int currentNode = 1;
				
		for(Node n : network.getNodes().values()){
			if(currentNode > 999999){
				log.error("Too many nodes in network! EMME allows a maxmimum of 999999 nodes. No additional nodes will be exported.");
				break;
			}
			
			nodeIdMap.put(n.getId(), currentNode++);
			String i = nodeIdMap.get(n.getId()).toString();
			Double xi = n.getCoord().getX();
			Double yi = n.getCoord().getY();
			
			if (xi.isNaN() || yi.isNaN()){
				log.warn("Node " + n.getId().toString() + " has coordinates with NaN values! Skipping...");
				continue;
			}
			else if (this.coordinateTransformation != null){
				//TODO add code for coordinate transformation
			}
			
			String label = n.getId().toString();
			label = label.substring(0, label.length() < 4 ? label.length() : 4);

			// [action] [i] [xi] [yi] [ui1] [ui2] [ui3] [label]
			writer.write("\na " + i + " " + xi + " " + yi + " 0 0 0 " + label);
		}
		
		writer.write("\nt links init");
		for(Link l : network.getLinks().values()){
			
			Integer i = nodeIdMap.get(l.getFromNode().getId());
			Integer j = nodeIdMap.get(l.getToNode().getId());
			
			if(i==null || j==null){
				log.warn("From/to node missing for link " + l.getId() + " (" + l.getFromNode().getId().toString() + " to " + l.getToNode().getId().toString() + "). Link was skipped.");
				continue;
			}
			
			double len = l.getLength();
			len = len > MAX_LINK_LENGTH ? MAX_LINK_LENGTH : len;
			
			String modes = "";
			if(this.modeMap != null){
				for(String s : l.getAllowedModes()) modes += modeMap.get(s);
			}
			else{
				modes = "c";
			}
				
			int type = 1;
			double lanes = l.getNumberOfLanes();
			lanes = (lanes > 9.9) ? 9.9 : lanes;
			lanes = lanes < 0 ? 0 : lanes;
			int vdf = 0;
			double spd = l.getFreespeed();
			double cap = l.getCapacity();
			
			
			// [action] [i] [j] [length] [modes] [type] [lanes] [vdf] [ul1] [ul2 = speed] [ul3 = capacity]
			writer.write("\na " + i + " " + j + " " + len + " " + modes + " " + type + " " + lanes + " " + vdf + " 0 " + spd + " " + cap);
		}
		
		writer.close();
		
		writer = new BufferedWriter(new FileWriter(this.mapFolder + "/map.txt"));
		writer.write("EMME inode <-> MATSim Node.Id Map");
		writer.write("\nCreated on " + sdf.format(Calendar.getInstance().getTime()));
		writer.write("\n\nEMME_Node,MATSim_Node.Id");
		for(Id i : nodeIdMap.keySet()) writer.write("\n" + nodeIdMap.get(i).toString() + "," + i.toString());
		writer.close();
		
		log.info("Node mapping written to " + this.mapFolder + "/map.txt");
		
		log.info("MATSim2EMME complete.");
	}

	public void write(String filename, HashMap<String, Character> modeMap)  throws IOException {
		
	}
	
	public void write(String filename, HashMap<String, Character> modeMap, String outCoordinateSystem)  throws IOException {
		
	}
}


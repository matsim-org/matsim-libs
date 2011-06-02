package playground.sergioo.VISUM;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkFactoryImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.CoordImpl;

public class VisumFile2MatsimNetwork {
	
	
	//Constants
	/**
	 * Visum network file
	 */
	private static final String VISUM_FILE = "C:/Users/sergioo/Documents/2011/Work/FCL/Operations/Data/Navteq/Network.net";
	/**
	 * Table beginnings
	 */
	private enum TABLE_BEGINNINGS {
		VERSION(new String[]{"$VERSION:"},"readVersion",new String[]{"LANGUAGE"}),
		NETPARA(new String[]{"$NETPARA:", "$NETPARA:"},"readNetworkParameters",new String[]{}),
		TSYS(new String[]{"$TSYS:", "$TSYS:"},"readTransportSystems",new String[]{}),
		MODE(new String[]{"$MODE:", "$MODE:"},"readModes",new String[]{}),
		NODE(new String[]{"$NODE:", "$NODE:"},"readNodes",new String[]{"NO","XCOORD","YCOORD"}),
		LINKTYPE(new String[]{"$LINKTYPE:", "$STRECKENTYP:"},"readLinkTypes",new String[]{}),
		LINK(new String[]{"$LINK:", "$LINK:"},"readLinks",new String[]{"NO","FROMNODENO","TONODENO","LENGTH","V0PRT","CAPPRT","NUMLANES","TSYSSET"}),
		TURN(new String[]{"$TURN:", "$TURN:"},"readTurns",new String[]{});
		public String[] languages;
		public String function;
		public String[] columns;
		private TABLE_BEGINNINGS(String[] languages, String function, String[] columns) {
			this.languages = languages;
			this.function = function;
			this.columns = columns;
		}
	}
	
	//Attributes
	/**
	 * Repeated nodes
	 */
	private Hashtable<Id, List<Id>> nodesRep =  new Hashtable<Id, List<Id>>();
	/**
	 * Language of the VISUM file
	 */
	private int language;
	/**
	 * MATSim network generated from the VISUM file
	 */
	private Network network;
	
	//Methods
	/**
	 * @return the network
	 */
	public Network getNetwork() {
		return network;
	}
	/**
	 * Creates the MATSim network from the VISUM file
	 * @param file
	 * @throws IOException
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public void createNetworkFromVISUMFile(File file) throws IOException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		network = NetworkImpl.createNetwork();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = reader.readLine();
		if (!"$VISION".equals(line))
			throw new IOException("File does not start with '$VISION'. Are you sure it is a VISUM network file?");
		line = reader.readLine();
		while (line != null) {
			for(TABLE_BEGINNINGS beginning:TABLE_BEGINNINGS.values())
				if (line.startsWith(beginning.languages[language])) {
					Method m = VisumFile2MatsimNetwork.class.getMethod(beginning.function, new Class[] {int[].class,BufferedReader.class});
					String[] attributes = line.substring(beginning.languages[language].length()).split(";");
					int[] indices = new int[beginning.columns.length];
					for(int i=0; i<beginning.columns.length; i++)
						indices[i] = getAttributeIndex(beginning.columns[i], attributes);
					m.invoke(this, new Object[]{indices,reader});
					System.out.println("Finished: "+beginning.name());
				}
			line=reader.readLine();
		}
	}
	/**
	 * Reads the version of the VISUM file
	 * @param tableAttributes
	 * @param reader
	 * @throws IOException
	 */
	public void readVersion(final int[] columnsIndices, final BufferedReader reader) throws IOException {
		String line = reader.readLine();
		if (line==null)
			throw new RuntimeException("Language definition cannot be found.");
		final String[] parts = line.split(";");
		if (parts[columnsIndices[0]].equals("ENG"))
			language = 0;
		else if (parts[columnsIndices[0]].equals("DEU"))
			language = 1;
		else
			throw new RuntimeException("Unknown language: " + parts[columnsIndices[0]]);
		reader.readLine();
	}
	/**
	 * Reads the version of the VISUM file
	 * @param columnsIndices
	 * @param reader
	 * @throws IOException
	 */
	public void readNetworkParameters(final int[] columnsIndices, final BufferedReader reader) throws IOException {
		String line = reader.readLine();
		while (line!=null && line.length()>0) {
			line=reader.readLine();
		}
	}
	/**
	 * Reads the transport systems of the VISUM file
	 * @param columnsIndices
	 * @param reader
	 * @throws IOException
	 */
	public void readTransportSystems(final int[] columnsIndices, final BufferedReader reader) throws IOException {
		String line = reader.readLine();
		while (line!=null && line.length()>0) {
			line=reader.readLine();
		}
	}
	/**
	 * Reads the modes of the VISUM file
	 * @param columnsIndices
	 * @param reader
	 * @throws IOException
	 */
	public void readModes(final int[] columnsIndices, final BufferedReader reader) throws IOException {
		String line = reader.readLine();
		while (line!=null && line.length()>0) {
			line=reader.readLine();
		}
	}
	/**
	 * Reads the nodes of the VISUM file
	 * @param columnsIndices
	 * @param reader
	 * @throws IOException
	 */
	public void readNodes(final int[] columnsIndices, final BufferedReader reader) throws IOException {
		String line = reader.readLine();
		while (line!=null && line.length() > 0) {
			final String[] parts = line.split(";");
			NodeImpl node=new NodeImpl(new IdImpl(parts[columnsIndices[0]]));
			node.setCoord(new CoordImpl(Double.parseDouble(parts[columnsIndices[1]]),Double.parseDouble(parts[columnsIndices[2]])));
			network.addNode(node);
			line=reader.readLine();
		}
	}
	/**
	 * Reads the nodes of the VISUM file analysing repeated ones
	 * @param columnsIndices
	 * @param reader
	 * @throws IOException
	 */
	public void readNodesRepeated(final int[] columnsIndices, final BufferedReader reader) throws IOException {
		String line = reader.readLine();
		while (line!=null && line.length() > 0) {
			final String[] parts = line.split(";");
			NodeImpl node=new NodeImpl(new IdImpl(parts[columnsIndices[0]]));
			node.setCoord(new CoordImpl(Double.parseDouble(parts[columnsIndices[1]]),Double.parseDouble(parts[columnsIndices[2]])));
			Id repeated = null;
			for(Id idB:nodesRep.keySet())
				if(network.getNodes().get(idB).getCoord().equals(node.getCoord()))
					repeated=idB;
			if(repeated!=null)
				nodesRep.get(repeated).add(node.getId());
			else {
				List<Id> reps = new ArrayList<Id>();
				reps.add(node.getId());
				nodesRep.put(node.getId(), reps);
				network.addNode(node);
			}
			line=reader.readLine();
		}
		/* For printing the repetitions
		for(List<Id> ids:nodesRep.values()) {
			if(ids.size()>1) {
				for(Id id:ids)
					System.out.print(id+",");
				System.out.println();
			}
		}
		*/
	}
	/**
	 * Reads the link types of the VISUM file
	 * @param columnsIndices
	 * @param reader
	 * @throws IOException
	 */
	public void readLinkTypes(final int[] columnsIndices, final BufferedReader reader) throws IOException {
		String line = reader.readLine();
		while (line!=null && line.length()>0) {
			line=reader.readLine();
		}
	}
	/**
	 * Reads the links of the VISUM file
	 * @param columnsIndices
	 * @param reader
	 * @throws IOException
	 */
	public void readLinks(final int[] columnsIndices, final BufferedReader reader) throws IOException {
		String line = reader.readLine();
		long id=0;
		List<String> zeroCapacity=new ArrayList<String>();
		List<String> loops=new ArrayList<String>();
		while (line!=null && line.length()>0) {
			final String[] parts = line.split(";");
			String origId = parts[columnsIndices[0]];
			Node from = network.getNodes().get(new IdImpl(parts[columnsIndices[1]]));
			Node to = network.getNodes().get(new IdImpl(parts[columnsIndices[2]]));
			double length = Double.parseDouble(parts[columnsIndices[3]]);
			double freeSpeed = Double.parseDouble(parts[columnsIndices[4]]);
			double capacity = Double.parseDouble(parts[columnsIndices[5]]);
			double nOfLanes = Double.parseDouble(parts[columnsIndices[6]]);
			if(capacity!=0 && !from.getId().equals(to.getId())) {
				Link link = new LinkFactoryImpl().createLink(new IdImpl(id), from, to, network, length, freeSpeed, capacity, nOfLanes);
				((LinkImpl)link).setOrigId(origId);
				Set<String> modes = new HashSet<String>();
				modes.add("Car");
				link.setAllowedModes(modes);
				network.addLink(link);
				id++;
			}
			else if(capacity==0)
				zeroCapacity.add(origId);
			else
				loops.add(origId);
			line=reader.readLine();
		}
		/* For printing the replays
		System.out.println("Zero capacity");
		for(String i:zeroCapacity)
			System.out.println(i);
		System.out.println("Loops");
		for(String i:loops)
			System.out.println(i);
		*/
	}
	/**
	 * Reads the links of the VISUM file analysing replayed nodes
	 * @param columnsIndices
	 * @param reader
	 * @throws IOException
	 */
	public void readLinksRepeated(final int[] columnsIndices, final BufferedReader reader) throws IOException {
		String line = reader.readLine();
		long id=0;
		List<String> zeroCapacity=new ArrayList<String>();
		List<String> loops=new ArrayList<String>();
		while (line!=null && line.length()>0) {
			final String[] parts = line.split(";");
			String origId = parts[columnsIndices[0]];
			Node from = network.getNodes().get(getPrincipalNode(new IdImpl(parts[columnsIndices[1]])));
			Node to = network.getNodes().get(getPrincipalNode(new IdImpl(parts[columnsIndices[2]])));
			double length = Double.parseDouble(parts[columnsIndices[3]]);
			double freeSpeed = Double.parseDouble(parts[columnsIndices[4]]);
			double capacity = Double.parseDouble(parts[columnsIndices[5]]);
			double nOfLanes = Double.parseDouble(parts[columnsIndices[6]]);
			if(capacity!=0 && !from.getId().equals(to.getId())) {
				Link link = new LinkFactoryImpl().createLink(new IdImpl(id), from, to, network, length, freeSpeed, capacity, nOfLanes);
				((LinkImpl)link).setOrigId(origId);
				Set<String> modes = new HashSet<String>();
				modes.add("car");
				link.setAllowedModes(modes);
				network.addLink(link);
				id++;
			}
			else if(capacity==0)
				zeroCapacity.add(origId);
			else
				loops.add(origId);
			line=reader.readLine();
		}
		PrintWriter pw = new PrintWriter(new File("./data/networks/badLinks.txt"));
		pw.println("Zero capacity");
		for(String i:zeroCapacity)
			pw.println(i);
		pw.println("Loops");
		for(String i:loops)
			pw.println(i);
		pw.close();
	}
	private Id getPrincipalNode(Id idN) {
		for(Entry<Id, List<Id>> idE:nodesRep.entrySet())
			for(Id id:idE.getValue())
				if(id.equals(idN))
					return idE.getKey();
		return idN;
	}
	/**
	 * Reads the turns of the VISUM file
	 * @param columnsIndices
	 * @param reader
	 * @throws IOException
	 */
	public void readTurns(final int[] columnsIndices, final BufferedReader reader) throws IOException {
		String line = reader.readLine();
		while (line!=null && line.length()>0) {
			line=reader.readLine();
		}
	}
	/**
	 * Determines the position of an attribute in an array
	 * @param attribute
	 * @param attributes
	 * @return
	 */
	public int getAttributeIndex(final String attribute, final String[] attributes) {
		for (int i = 0; i < attributes.length; i++)
			if (attributes[i].equals(attribute))
				return i;
		return -1;
	}
	
	public static void main(String[] args) throws Exception {
		VisumFile2MatsimNetwork v2m = new VisumFile2MatsimNetwork();
		v2m.createNetworkFromVISUMFile(new File(VISUM_FILE));
		Network network = v2m.getNetwork();
		new NetworkCleaner().run(network);
		NetworkWriter networkWriter =  new NetworkWriter(network);
		networkWriter.write("./data/networks/singapore3.xml");
	}
		
}

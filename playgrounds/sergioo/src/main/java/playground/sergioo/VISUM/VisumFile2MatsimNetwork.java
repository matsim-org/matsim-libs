package playground.sergioo.VISUM;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkFactoryImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.geometry.CoordImpl;

public class VisumFile2MatsimNetwork {
	
	//Constants
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
		while (line!=null && line.length()>0) {
			final String[] parts = line.split(";");
			String origId = parts[columnsIndices[0]];
			Node from = network.getNodes().get(new IdImpl(parts[columnsIndices[1]]));
			Node to = network.getNodes().get(new IdImpl(parts[columnsIndices[2]]));
			double length = Double.parseDouble(parts[columnsIndices[3]]);
			double freeSpeed = Double.parseDouble(parts[columnsIndices[4]]);
			double capacity = Double.parseDouble(parts[columnsIndices[5]]);
			double nOfLanes = Double.parseDouble(parts[columnsIndices[6]]);
			if(capacity!=0) {
				Link link = new LinkFactoryImpl().createLink(new IdImpl(id), from, to, network, length, freeSpeed, capacity, nOfLanes);
				((LinkImpl)link).setOrigId(origId);
				Set<String> modes = new HashSet<String>();
				modes.add("Car");
				link.setAllowedModes(modes);
				network.addLink(link);
				id++;
			}
			line=reader.readLine();
		}
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
	
}

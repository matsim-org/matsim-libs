package josmMatsimPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * a layer which contains MATSim-network data to differ from normal OSM layers
 * @author nkuehnel
 * 
 */
public class NetworkLayer extends OsmDataLayer
{
	private Map<String, Node> nodes = new HashMap<String, Node>();
	private Map<String, Link> links = new HashMap<String, Link>();

	private	Network matsimNetwork;
	private String coordSystem; 
	
	

	public String getCoordSystem()
	{
		return coordSystem;
	}


	public NetworkLayer(DataSet data, String name, File associatedFile, Network network, String coordSystem)
	{
		super(data, name, associatedFile);
		this.matsimNetwork=network;
		this.coordSystem=coordSystem;
	}


	public Network getMatsimNetwork()
	{
		return matsimNetwork;
	}


	public Map<String, Link> getLinks()
	{
		// TODO Auto-generated method stub
		return links;
	}
	
	public Map<String, Node> getNodes()
	{
		return nodes;
	}
	
}

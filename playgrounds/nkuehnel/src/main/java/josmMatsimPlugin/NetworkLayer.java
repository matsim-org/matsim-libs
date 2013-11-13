package josmMatsimPlugin;

import java.io.File;

import org.matsim.api.core.v01.network.Network;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * a layer which contains MATSim-network data to differ from normal OSM layers
 * @author nkuehnel
 * 
 */
public class NetworkLayer extends OsmDataLayer
{
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

	
}

package josmMatsimPlugin;

import java.io.File;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * a layer which contains MATSim-network data to differ from normal OSM layers
 * @author nkuehnel
 * 
 */
public class NetworkLayer extends OsmDataLayer
{

	public NetworkLayer(DataSet data, String name, File associatedFile)
	{
		super(data, name, associatedFile);
	}

}

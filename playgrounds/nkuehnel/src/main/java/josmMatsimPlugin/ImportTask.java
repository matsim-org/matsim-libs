package josmMatsimPlugin;

import java.io.File;
import java.io.IOException;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.UncheckedIOException;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

/**
 * The task which is executed after confirming the MATSimImportDialog. Creates a new layer showing the network data.
 * @author nkuehnel
 * 
 */
public class ImportTask extends PleaseWaitRunnable 
{
	private Layer layer;
	static String originSystem="WGS84";
	
	public ImportTask()
	{
		super("matsimImport");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openstreetmap.josm.gui.PleaseWaitRunnable#cancel()
	 */
	@Override
	protected void cancel()
	{
		// TODO Auto-generated method stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openstreetmap.josm.gui.PleaseWaitRunnable#finish()
	 */
	@Override
	protected void finish()
	{
		Main.main.addLayer(layer);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openstreetmap.josm.gui.PleaseWaitRunnable#realRun()
	 */
	@Override
	protected void realRun() throws SAXException, IOException,
			OsmTransferException, UncheckedIOException
	{
		String filename = MATSimImportDialog.path.getText();
		DataSet dataSet = new DataSet();
		
		CoordinateTransformation ct = TransformationFactory
				.getCoordinateTransformation(originSystem,
						TransformationFactory.WGS84);
		
		JosmNetworkReader reader = new JosmNetworkReader(ct);
		reader.parse(filename);
		
		for (Node node : reader.getNodes().values())
		{
			dataSet.addPrimitive(node);
		}
		
		for (Way way : reader.getWays())
		{
			dataSet.addPrimitive(way);
		}
		layer = new NetworkLayer(dataSet, filename, new File(filename));
	}

	

}

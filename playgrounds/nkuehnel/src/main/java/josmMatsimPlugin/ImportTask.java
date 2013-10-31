package josmMatsimPlugin;

import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.UncheckedIOException;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

/**
 * The task which is executed after confirming the MATSimImportDialog. Creates a new layer showing the network data.
 * @author nkuehnel
 * 
 */
public class ImportTask extends PleaseWaitRunnable 
{
	private NetworkLayer layer;
	
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
		Main.map.mapView.setActiveLayer(layer);
		JOptionPane.showMessageDialog(Main.main.parent, "Import finished.");
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
		DataSet dataSet = new DataSet();
		
		CoordinateTransformation ct = TransformationFactory
				.getCoordinateTransformation(Defaults.originSystem,
						TransformationFactory.WGS84);
		
		Config config= ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile(Defaults.importPath);
		
		for (Node node: scenario.getNetwork().getNodes().values())
		{
			Coord tmpCoor= node.getCoord();
			LatLon coor;
			
			if(Defaults.originSystem.equals("WGS84"))
			{
				coor = new LatLon(tmpCoor.getY(), tmpCoor.getX());
			}
			else
			{
				tmpCoor =ct.transform(new CoordImpl(tmpCoor.getX(), tmpCoor.getY()));
				coor = new LatLon(tmpCoor.getY(), tmpCoor.getX());
			}
			org.openstreetmap.josm.data.osm.Node nodeOsm = new org.openstreetmap.josm.data.osm.Node(coor);
			nodeOsm.setOsmId(Long.parseLong(node.getId().toString()), 1);
			dataSet.addPrimitive(nodeOsm);
		}
		
		for (Link link: scenario.getNetwork().getLinks().values())
		{
			Way way = new Way(Long.parseLong(link.getId().toString()), 1);
			
			String fromNode=link.getFromNode().getId().toString();
			String toNode=link.getToNode().getId().toString();
			
			OsmPrimitiveType type=OsmPrimitiveType.NODE;
			way.addNode((org.openstreetmap.josm.data.osm.Node) dataSet.getPrimitiveById(Long.parseLong(toNode), type));
			way.addNode((org.openstreetmap.josm.data.osm.Node) dataSet.getPrimitiveById(Long.parseLong(fromNode), type));
			
			way.put("freespeed", String.valueOf(link.getFreespeed()));
			way.put("capacity", String.valueOf(link.getCapacity()));
			way.put("length", String.valueOf(link.getLength()));
			way.put("permlanes", String.valueOf(link.getNumberOfLanes()));
			way.put("modes", String.valueOf(link.getAllowedModes()));
			
			dataSet.addPrimitive(way);
		}

		layer = new NetworkLayer(dataSet, Defaults.importPath, new File(Defaults.importPath), scenario.getNetwork(), Defaults.originSystem);
		dataSet.addDataSetListener(new NetworkListener(layer, scenario.getNetwork(), Defaults.originSystem));
	}

	

}

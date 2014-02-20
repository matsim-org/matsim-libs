package josmMatsimPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.UncheckedIOException;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

/**
 * The task which is executed after confirming the MATSimImportDialog. Creates a
 * new layer showing the network data.
 * 
 * @author nkuehnel
 * 
 */
public class ImportTask extends PleaseWaitRunnable {
	public static final String NODE_TAG_ID = "id";
	public static final String WAY_TAG_ID = "id";
	private NetworkLayer layer;
	private String path;

	public ImportTask(String path) {
		super("MATSim Import");
		this.path = path;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openstreetmap.josm.gui.PleaseWaitRunnable#cancel()
	 */
	@Override
	protected void cancel() {
		// TODO Auto-generated method stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openstreetmap.josm.gui.PleaseWaitRunnable#finish()
	 */
	@Override
	protected void finish() {
		// layer = null happens if Exception happens during import,
		// as Exceptions are handled only after this method is called.
		if (layer != null) {
			Main.main.addLayer(layer);
			Main.map.mapView.setActiveLayer(layer);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openstreetmap.josm.gui.PleaseWaitRunnable#realRun()
	 */
	@Override
	protected void realRun() throws SAXException, IOException,
			OsmTransferException, UncheckedIOException {
		this.progressMonitor.setTicksCount(4);
		this.progressMonitor.setTicks(0);

		DataSet dataSet = new DataSet();
		String importSystem = (String) ImportDialog.importSystem
				.getSelectedItem();
		CoordinateTransformation ct = TransformationFactory
				.getCoordinateTransformation(importSystem,
						TransformationFactory.WGS84);
		
		this.progressMonitor.setTicks(1);
		this.progressMonitor.setCustomText("creating scenario..");
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		this.progressMonitor.setTicks(2);
		this.progressMonitor.setCustomText("reading network xml..");
		new MatsimNetworkReader(scenario).readFile(path);
		Network network = NetworkImpl.createNetwork();

		HashMap<Way, List<Link>> way2Links = new HashMap<Way, List<Link>>();
		HashMap<Node, org.openstreetmap.josm.data.osm.Node> node2OsmNode = new HashMap<Node, org.openstreetmap.josm.data.osm.Node>();
		this.progressMonitor.setTicks(3);
		this.progressMonitor.setCustomText("creating nodes..");
		for (Node node : scenario.getNetwork().getNodes().values()) {
			Coord tmpCoor = node.getCoord();
			LatLon coor;

			if (importSystem.equals("WGS84")) {
				coor = new LatLon(tmpCoor.getY(), tmpCoor.getX());
			} else {
				tmpCoor = ct.transform(new CoordImpl(tmpCoor.getX(), tmpCoor
						.getY()));
				coor = new LatLon(tmpCoor.getY(), tmpCoor.getX());
			}
			org.openstreetmap.josm.data.osm.Node nodeOsm = new org.openstreetmap.josm.data.osm.Node(
					coor);
			nodeOsm.put(NODE_TAG_ID, node.getId().toString());
			node2OsmNode.put(node, nodeOsm);
			dataSet.addPrimitive(nodeOsm);
			Node newNode = network.getFactory().createNode(
					new IdImpl(Long.toString(nodeOsm.getUniqueId())),
					node.getCoord());
			((NodeImpl) newNode).setOrigId(node.getId().toString());
			network.addNode(newNode);
		}

		this.progressMonitor.setTicks(4);
		this.progressMonitor.setCustomText("creating ways..");
		for (Link link : scenario.getNetwork().getLinks().values()) {
			Way way = new Way();
			org.openstreetmap.josm.data.osm.Node fromNode = node2OsmNode
					.get(link.getFromNode());
			way.addNode(fromNode);
			org.openstreetmap.josm.data.osm.Node toNode = node2OsmNode.get(link
					.getToNode());
			way.addNode(toNode);
			way.put(WAY_TAG_ID, link.getId().toString());
			way.put("freespeed", String.valueOf(link.getFreespeed()));
			way.put("capacity", String.valueOf(link.getCapacity()));
			way.put("length", String.valueOf(link.getLength()));
			way.put("permlanes", String.valueOf(link.getNumberOfLanes()));
			StringBuilder modes = new StringBuilder();

			for (String mode : link.getAllowedModes()) {
				modes.append(mode);
				if (link.getAllowedModes().size() > 1) {
					modes.append(";");
				}
			}
			way.put("modes", modes.toString());

			dataSet.addPrimitive(way);
			Link newLink = network.getFactory().createLink(
					new IdImpl(Long.toString(way.getUniqueId())),
					network.getNodes().get(
							new IdImpl(Long.toString(fromNode.getUniqueId()))),
					network.getNodes().get(
							new IdImpl(Long.toString(toNode.getUniqueId()))));
			newLink.setFreespeed(link.getFreespeed());
			newLink.setCapacity(link.getCapacity());
			newLink.setLength(link.getLength());
			newLink.setNumberOfLanes(link.getNumberOfLanes());
			newLink.setAllowedModes(link.getAllowedModes());
			((LinkImpl) newLink).setOrigId(link.getId().toString());
			network.addLink(newLink);
			way2Links.put(way, Collections.singletonList(newLink));
		}

		this.progressMonitor.setTicks(5);
		this.progressMonitor.setCustomText("creating layer..");

		layer = new NetworkLayer(dataSet, ImportDialog.path.getText(),
				new File(path), network, importSystem, way2Links);
	}
}

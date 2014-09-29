package org.matsim.contrib.josm;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

/**
 * The task which is executed after confirming the ImportDialog. Creates a new
 * layer showing the network data.
 * 
 * @author Nico
 */
class ImportTask extends PleaseWaitRunnable {

	/**
	 * The String representing the id tagging-key for nodes.
	 */
	public static final String NODE_TAG_ID = "id";
	/**
	 * The String representing the id tagging-key for ways.
	 */
	public static final String WAY_TAG_ID = "id";
	private MATSimLayer layer;
	private String path;
	private DataSet dataSet;
	private Scenario scenario;
	private String importSystem;
	private HashMap<Way, List<Link>> way2Links;
	private HashMap<Link, List<WaySegment>> link2Segment;
	private HashMap<Relation, TransitRoute> relation2Route = new HashMap<Relation, TransitRoute>();

	/**
	 * Creates a new Import task with the given <code>path</code>.
	 * 
	 * @param path
	 *            The path to be imported from
	 */
	public ImportTask(String path) {
		super("MATSim Import");
		this.path = path;
	}

	/**
	 * @see org.openstreetmap.josm.gui.PleaseWaitRunnable#cancel()
	 */
	@Override
	protected void cancel() {
		// TODO Auto-generated method stub
	}

	/**
	 * @see org.openstreetmap.josm.gui.PleaseWaitRunnable#finish()
	 */
	@Override
	protected void finish() {
		// layer = null happens if Exception happens during import,
		// as Exceptions are handled only after this method is called.
		layer = new MATSimLayer(dataSet, ImportDialog.path.getText(),
				new File(path), scenario, importSystem, way2Links, link2Segment, relation2Route);
		if (layer != null) {
			Main.main.addLayer(layer);
			Main.map.mapView.setActiveLayer(layer);
		}
	}

	/**
	 * @see org.openstreetmap.josm.gui.PleaseWaitRunnable#realRun()
	 */
	@Override
	protected void realRun() throws SAXException, IOException,
			OsmTransferException, UncheckedIOException {
		this.progressMonitor.setTicksCount(4);
		this.progressMonitor.setTicks(0);

		dataSet = new DataSet();
		importSystem = (String) ImportDialog.importSystem.getSelectedItem();
		CoordinateTransformation ct = TransformationFactory
				.getCoordinateTransformation(importSystem,
						TransformationFactory.WGS84);

		this.progressMonitor.setTicks(1);
		this.progressMonitor.setCustomText("creating scenario..");
		Config config = ConfigUtils.createConfig();
		Scenario tempScenario = ScenarioUtils.createScenario(config);
		this.progressMonitor.setTicks(2);
		this.progressMonitor.setCustomText("reading network xml..");
		new MatsimNetworkReader(tempScenario).readFile(path);
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		way2Links = new HashMap<Way, List<Link>>();
		link2Segment = new HashMap<Link, List<WaySegment>>();
		
		HashMap<Node, org.openstreetmap.josm.data.osm.Node> node2OsmNode = new HashMap<Node, org.openstreetmap.josm.data.osm.Node>();
		this.progressMonitor.setTicks(3);
		this.progressMonitor.setCustomText("creating nodes..");
		for (Node node : tempScenario.getNetwork().getNodes().values()) {
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
			Node newNode = scenario.getNetwork().getFactory().createNode(
					Id.create(nodeOsm.getUniqueId(), Node.class),
					node.getCoord());
			((NodeImpl) newNode).setOrigId(node.getId().toString());
			scenario.getNetwork().addNode(newNode);
		}

		this.progressMonitor.setTicks(4);
		this.progressMonitor.setCustomText("creating ways..");
		for (Link link : tempScenario.getNetwork().getLinks().values()) {
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
			Link newLink = scenario.getNetwork().getFactory().createLink(
					Id.create(way.getUniqueId(), Link.class),
					scenario.getNetwork().getNodes().get(
							Id.create(fromNode.getUniqueId(), Node.class)),
					scenario.getNetwork().getNodes().get(
							Id.create(toNode.getUniqueId(), Node.class)));
			newLink.setFreespeed(link.getFreespeed());
			newLink.setCapacity(link.getCapacity());
			newLink.setLength(link.getLength());
			newLink.setNumberOfLanes(link.getNumberOfLanes());
			newLink.setAllowedModes(link.getAllowedModes());
			((LinkImpl) newLink).setOrigId(link.getId().toString());
			scenario.getNetwork().addLink(newLink);
			way2Links.put(way, Collections.singletonList(newLink));
			link2Segment.put(newLink,
					Collections.singletonList(new WaySegment(way, 0)));
		}

		this.progressMonitor.setTicks(5);
		this.progressMonitor.setCustomText("creating layer..");
	}
}

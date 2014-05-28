package org.matsim.contrib.josm;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * The Task that handles the convert action
 * 
 * 
 */

class ConvertTask extends PleaseWaitRunnable {

	private NetworkLayer newLayer;

	public ConvertTask() {
		super("Convert to MATSim network");
	}

	@Override
	protected void cancel() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void realRun() throws SAXException, IOException,
			OsmTransferException {
		this.progressMonitor.setTicksCount(6);
		this.progressMonitor.setTicks(0);

		Layer layer = Main.main.getActiveLayer();

		Network tempNetwork = NetworkImpl.createNetwork();
		Network network = NetworkImpl.createNetwork();

		this.progressMonitor.setTicks(1);
		this.progressMonitor.setCustomText("converting osm data..");
		
		NewConverter.convertOsmLayer(((OsmDataLayer) layer).data, tempNetwork, new HashMap<Way, List<Link>>(), new HashMap<Link, List<WaySegment>>());
		if (Main.pref.getBoolean("matsim_cleanNetwork")) {
			this.progressMonitor.setTicks(2);
			this.progressMonitor.setCustomText("cleaning network..");
			new NetworkCleaner().run(tempNetwork);
		}

		this.progressMonitor.setTicks(3);
		this.progressMonitor.setCustomText("preparing data set..");
		DataSet dataSet = new DataSet();

		HashMap<Way, List<Link>> way2Links = new HashMap<Way, List<Link>>();
		HashMap<Link, List<WaySegment>> link2Segment = new HashMap<Link, List<WaySegment>>();
		HashMap<Node, org.openstreetmap.josm.data.osm.Node> node2OsmNode = new HashMap<Node, org.openstreetmap.josm.data.osm.Node>();
		this.progressMonitor.setTicks(4);
		this.progressMonitor.setCustomText("loading nodes..");

		for (Node node : tempNetwork.getNodes().values()) {
			Coord tmpCoor = node.getCoord();
			LatLon coor = new LatLon(tmpCoor.getY(), tmpCoor.getX());
			org.openstreetmap.josm.data.osm.Node nodeOsm = new org.openstreetmap.josm.data.osm.Node(
					coor);
			nodeOsm.put(ImportTask.NODE_TAG_ID, ((NodeImpl)node).getOrigId());
			node2OsmNode.put(node, nodeOsm);
			dataSet.addPrimitive(nodeOsm);
			Node newNode = network.getFactory().createNode(
					new IdImpl(Long.toString(nodeOsm.getUniqueId())),
					node.getCoord());
			((NodeImpl) newNode).setOrigId(((NodeImpl)node).getOrigId());
			network.addNode(newNode);
			
		}

		this.progressMonitor.setTicks(5);
		this.progressMonitor.setCustomText("loading ways..");
		for (Link link : tempNetwork.getLinks().values()) {
			Way way = new Way();
			org.openstreetmap.josm.data.osm.Node fromNode = node2OsmNode
					.get(link.getFromNode());
			way.addNode(fromNode);
			org.openstreetmap.josm.data.osm.Node toNode = node2OsmNode.get(link
					.getToNode());
			way.addNode(toNode);
			way.put(ImportTask.WAY_TAG_ID, ((LinkImpl)link).getOrigId());
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
			((LinkImpl) newLink).setOrigId(((LinkImpl)link).getOrigId());
			network.addLink(newLink);
			way2Links.put(way, Collections.singletonList(newLink));
			link2Segment.put(newLink, Collections.singletonList(new WaySegment(way, 0)));
		}

		this.progressMonitor.setTicks(6);
		this.progressMonitor.setCustomText("creating layer..");

		newLayer = new NetworkLayer(dataSet, null, null, network,
				TransformationFactory.WGS84, way2Links, link2Segment);
	}

	@Override
	protected void finish() {
		if (newLayer != null) {
			Main.main.addLayer(newLayer);
			Main.map.mapView.setActiveLayer(newLayer);
		}
	}

}

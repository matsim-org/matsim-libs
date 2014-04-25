package org.matsim.contrib.josm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

public class Layer2Network {

	private static Map<Layer, Layer2Network> layer2Network = new HashMap<Layer, Layer2Network>();
	private Network network;
	private Map<Way, List<Link>> way2Links;
	private Map<Link, WaySegment> link2Segment;
	
	public Layer2Network(OsmDataLayer layer, Network network, Map<Way, List<Link>> way2Links, Map<Link, WaySegment> link2Segment) {
		this.network = network;
		this.way2Links = way2Links;
		this.link2Segment = link2Segment;
		layer2Network.put(layer, this);
	}
	
	public static Map<Way, List<Link>> getWay2Links(Layer layer) {
		return layer2Network.get(layer).way2Links;
	}
	
	public static Network getNetwork(Layer layer) {
		return layer2Network.get(layer).network;
	}
	
	public static Map<Link, WaySegment> getLink2Segment(Layer layer) {
		return layer2Network.get(layer).link2Segment;
	}
	
	public static boolean containsLayer(Layer layer) {
		return layer2Network.containsKey(layer);
	}
}

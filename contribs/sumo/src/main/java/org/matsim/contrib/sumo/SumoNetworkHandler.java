package org.matsim.contrib.sumo;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Parses and holds specific information from sumo network xml file.
 */
public class SumoNetworkHandler extends DefaultHandler {

	final double[] netOffset = new double[2];

	/**
	 * All junctions.
	 */
	final Map<String, Junction> junctions = new HashMap<>();

	/**
	 * Edges mapped by id.
	 */
	final Map<String, Edge> edges = new HashMap<>();

	/**
	 * Map lane id to their edge.
	 */
	final Map<String, Edge> lanes = new HashMap<>();

	/**
	 * All connections mapped by the origin (from).
	 */
	final Map<String, List<Connection>> connections = new HashMap<>();

	/**
	 * Parsed link types.
	 */
	final Map<String, Type> types = new HashMap<>();

	/**
	 * Attribute names that have been observed during parsing.
	 */
	Set<String> attributes = new LinkedHashSet<>();

	/**
	 * Stores current parsed edge.
	 */
	private Edge tmpEdge = null;

	/**
	 * Current parsed junction.
	 */
	private Junction tmpJunction = null;

	private SumoNetworkHandler() {
	}

	/**
	 * Creates a new sumo handler by reading data from xml file.
	 */
	static SumoNetworkHandler read(File file) throws ParserConfigurationException, SAXException, IOException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		SumoNetworkHandler sumoHandler = new SumoNetworkHandler();
		saxParser.parse(file, sumoHandler);
		return sumoHandler;
	}

	private static BitSet parseBitSet(String value) {
		BitSet bitSet = new BitSet(value.length());
		for (int i = 0; i < value.length(); i++) {
			if (value.charAt(i) == '1')
				bitSet.set(i);
		}
		return bitSet;
	}

	/**
	 * Parse mode list from attribute.
	 */
	private static Set<String> parseModes(String modes) {
		if (modes == null)
			return null;

		return Arrays.stream(modes.split(" "))
			.filter(s -> !s.isEmpty())
			.map(String::trim)
			.map(String::intern)
			.collect(Collectors.toSet());
	}

	public Map<String, Junction> getJunctions() {
		return junctions;
	}

	public Map<String, Edge> getEdges() {
		return edges;
	}

	public Map<String, Edge> getLanes() {
		return lanes;
	}

	public Map<String, Type> getTypes() {
		return types;
	}

	/**
	 * Merges another sumo network into this one.
	 * To work properly, this requires that edge und junction ids are the same in both networks.
	 * This function does not clean left over edges, when using this, a network cleaner should be used afterward.
	 *
	 * @param other other network to merge into this one
	 * @param ct    coordinate transformation to apply
	 */
	void merge(SumoNetworkHandler other, CoordinateTransformation ct) {

		Set<String> notDeadEnd = other.junctions.entrySet().stream()
			.filter((e) -> !"dead_end".equals(e.getValue().type))
			.map(Map.Entry::getKey)
			.collect(Collectors.toSet());

		// lanes length may get incorrect
		// this uses the maximum length for merged edges
		for (Map.Entry<String, Edge> e : other.edges.entrySet()) {
			if (edges.containsKey(e.getKey())) {
				for (int i = 0; i < Math.min(e.getValue().lanes.size(), edges.get(e.getKey()).lanes.size()); i++) {
					Lane l = e.getValue().lanes.remove(i);
					Lane o = edges.get(e.getKey()).lanes.get(i);
					e.getValue().lanes.add(i, l.withLength(Double.max(l.length, o.length)));
				}
			}
		}

		edges.keySet().removeAll(other.edges.keySet());
		lanes.keySet().removeAll(other.lanes.keySet());

		junctions.keySet().removeAll(notDeadEnd);

		// Re-project to new ct
		other.edges.values().forEach(e -> e.proj(other.netOffset, netOffset, ct));
		other.junctions.values().forEach(j -> j.proj(other.netOffset, netOffset, ct));

		edges.putAll(other.edges);
		lanes.putAll(other.lanes);

		other.junctions.forEach((k, v) -> {
			if (notDeadEnd.contains(k))
				junctions.put(k, v);
			else
				junctions.putIfAbsent(k, v);
		});

		// connections are merged individually
		for (Map.Entry<String, List<Connection>> e : other.connections.entrySet()) {

			if (connections.containsKey(e.getKey())) {

				// remove connections that point to edges that are also in the other network
				connections.get(e.getKey()).removeIf(c -> other.edges.containsKey(c.to));

				// add all other connections
				connections.get(e.getKey()).addAll(e.getValue());

			} else
				connections.put(e.getKey(), e.getValue());

		}

		connections.putAll(other.connections);
	}

	Coord createCoord(double[] xy) {
		return new Coord(xy[0] - netOffset[0], xy[1] - netOffset[1]);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {


		switch (qName) {

			case "location":
				String[] netOffsets = attributes.getValue("netOffset").split(",");
				netOffset[0] = Double.parseDouble(netOffsets[0]);
				netOffset[1] = Double.parseDouble(netOffsets[1]);

				break;

			case "type":

				String typeId = attributes.getValue("id");

				types.put(typeId, new Type(typeId, attributes.getValue("allow"), attributes.getValue("disallow"),
					Double.parseDouble(attributes.getValue("speed"))));

				break;

			case "edge":

				// Internal edges are not needed
				if ("internal".equals(attributes.getValue("function")))
					break;

				String shape = attributes.getValue("shape");
				tmpEdge = new Edge(
					attributes.getValue("id"),
					attributes.getValue("from"),
					attributes.getValue("to"),
					attributes.getValue("type"),
					Integer.parseInt(attributes.getValue("priority")),
					attributes.getValue("name"),
					shape == null ? new String[0] : shape.split(" ")
				);

				break;

			case "lane":

				// lane of internal edge
				if (tmpEdge == null)
					break;

				Lane lane = new Lane(
					attributes.getValue("id"),
					Integer.parseInt(attributes.getValue("index")),
					Double.parseDouble(attributes.getValue("length")),
					Double.parseDouble(attributes.getValue("speed")),
					parseModes(attributes.getValue("allow")),
					parseModes(attributes.getValue("disallow"))
				);

				tmpEdge.lanes.add(lane);
				lanes.put(lane.id, tmpEdge);

				break;

			case "param":

				if (tmpEdge == null)
					break;

				String value = attributes.getValue("value");

				switch (attributes.getValue("key")) {
					case "origId":
						tmpEdge.origId = value;
						break;
					case "origFrom":
						tmpEdge.origFrom = value;
						break;
					case "origTo":
						tmpEdge.origTo = value;
						break;
					// Redundant attribute, that does not need to be stored
					case "highway":
						break;
					default:
						String attribute = attributes.getValue("key").intern();
						this.attributes.add(attribute);
						tmpEdge.attributes.put(attribute, value);
						break;
				}

				break;

			case "junction":

				String inc = attributes.getValue("incLanes");

				List<String> lanes = Arrays.asList(inc.split(" "));
				String id = attributes.getValue("id");
				tmpJunction = new Junction(
					id,
					attributes.getValue("type"),
					lanes,
					new double[]{Double.parseDouble(attributes.getValue("x")), Double.parseDouble(attributes.getValue("y"))}
				);

				junctions.put(id, tmpJunction);

				break;

			case "request":

				if (this.tmpJunction != null)
					this.tmpJunction.requests.add(
						new Req(
							parseBitSet(attributes.getValue("response")),
							parseBitSet(attributes.getValue("foes"))
						)
					);

				break;

			case "connection":

				// aggregate edges split by sumo again
				String from = attributes.getValue("from");
				Edge fromEdge = edges.get(from);
				Junction j = fromEdge != null ? junctions.get(fromEdge.to) : null;

				Connection conn = new Connection(from, attributes.getValue("to"),
					Integer.parseInt(attributes.getValue("fromLane")),
					Integer.parseInt(attributes.getValue("toLane")),
					attributes.getValue("dir"), j != null ? j.connIdx++ : -1);

				if (j != null)
					j.connections.add(conn);

				connections.computeIfAbsent(from, k -> new ArrayList<>()).add(conn);

				break;
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if ("edge".equals(qName) && tmpEdge != null) {
			edges.put(tmpEdge.id, tmpEdge);
			tmpEdge = null;
		}
	}

	/**
	 * Edge from the SUMO network.
	 */
	static final class Edge {

		final String id;
		final String from;
		final String to;
		final String type;
		final int priority;

		@Nullable
		final String name;

		final List<double[]> shape = new ArrayList<>();

		final List<Lane> lanes = new ArrayList<>();

		String origId;

		@Nullable
		String origFrom;

		@Nullable
		String origTo;

		final Map<String, String> attributes = new HashMap<>();

		public Edge(String id, String from, String to, String type, int priority, String name, String[] shape) {
			this.id = id;
			this.from = from;
			this.to = to;
			this.type = type;
			this.priority = priority;
			this.name = name;

			for (String coords : shape) {
				String[] split = coords.split(",");
				this.shape.add(new double[]{Double.parseDouble(split[0]), Double.parseDouble(split[1])});
			}
		}

		/**
		 * Calculate edge length as max of lanes.
		 */
		public double getLength() {
			return lanes.stream().mapToDouble(l -> l.length).max().orElseThrow();
		}

		@Override
		public String toString() {
			return "Edge{" +
				"id='" + id + '\'' +
				", from='" + from + '\'' +
				", to='" + to + '\'' +
				", origId='" + origId + '\'' +
				", origFrom='" + origFrom + '\'' +
				", origTo='" + origTo + '\'' +
				'}';
		}


		/**
		 * Project edge geometry to new coordinate system. (in situ)
		 */
		private void proj(double[] fromOffset, double[] toOffset, CoordinateTransformation ct) {
			for (double[] xy : shape) {

				Coord from = new Coord(xy[0] - fromOffset[0], xy[1] - fromOffset[1]);
				Coord to = ct.transform(from);

				xy[0] = to.getX() + toOffset[0];
				xy[1] = to.getY() + toOffset[1];
			}
		}
	}

	static final class Lane {

		final String id;
		final int index;
		final double length;
		final double speed;

		/**
		 * Allowed vehicle types on this lane.
		 */
		@Nullable
		final Set<String> allow;
		@Nullable
		final Set<String> disallow;

		Lane(String id, int index, double length, double speed, @Nullable Set<String> allow, @Nullable Set<String> disallow) {
			this.id = id;
			this.index = index;
			this.length = length;
			this.speed = speed;
			this.allow = allow;
			this.disallow = disallow;
		}

		Lane withLength(double newLength) {
			return new Lane(id, index, newLength, speed, allow, disallow);
		}
	}

	record Req(BitSet response, BitSet foes) {
	}

	static final class Junction {

		final String id;
		final String type;
		final List<String> incLanes;
		final double[] coord;

		final List<Req> requests = new ArrayList<>();
		final List<Connection> connections = new ArrayList<>();

		/**
		 * Mutable connection index.
		 */
		private int connIdx;

		Junction(String id, String type, List<String> incLanes, double[] coord) {
			this.id = id;
			this.type = type;
			this.incLanes = incLanes;
			this.coord = coord;
		}

		private void proj(double[] fromOffset, double[] toOffset, CoordinateTransformation ct) {
			Coord from = new Coord(coord[0] - fromOffset[0], coord[1] - fromOffset[1]);
			Coord to = ct.transform(from);

			coord[0] = to.getX() + toOffset[0];
			coord[1] = to.getY() + toOffset[1];
		}
	}

	static final class Connection {

		final String from;
		final String to;
		final int fromLane;
		final int toLane;

		// could be enum probably
		final String dir;

		/**
		 * Request index on junction.
		 */
		final int reqIdx;

		Connection(String from, String to, int fromLane, int toLane, String dir, int reqIdx) {
			this.from = from;
			this.to = to;
			this.fromLane = fromLane;
			this.toLane = toLane;
			this.dir = dir;
			this.reqIdx = reqIdx;
		}

		@Override
		public String toString() {
			return "Connection{" +
				"from='" + from + '\'' +
				", to='" + to + '\'' +
				", fromLane=" + fromLane +
				", toLane=" + toLane +
				", dir='" + dir + '\'' +
				'}';
		}
	}


	static final class Type {

		final String id;
		final Set<String> allow = new HashSet<>();
		final Set<String> disallow = new HashSet<>();
		final double speed;

		/**
		 * Set if id is highway.[type]
		 */
		final String highway;

		Type(String id, String allow, String disallow, double speed) {
			this.id = id;
			this.speed = speed;
			if (allow != null)
				Collections.addAll(this.allow, allow.split(" "));

			if (disallow != null)
				Collections.addAll(this.disallow, disallow.split(" "));

			if (id.startsWith("highway.")) {
				// split compound types
				if (id.contains("|"))
					id = id.split("\\|")[0];

				highway = id.substring(8);
			} else
				highway = null;

		}
	}
}

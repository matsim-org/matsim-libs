package org.matsim.contrib.sumo;

import org.apache.commons.csv.CSVPrinter;
import org.matsim.contrib.osm.networkReader.LinkProperties;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates edge features from a network. These features might not be the same as in the network because they
 * have been cleaned and preprocessed.
 */
class SumoNetworkFeatureExtractor {
	private final SumoNetworkHandler handler;

	/**
	 * Maps junction id to incoming edges.
	 */
	private final Map<String, List<SumoNetworkHandler.Edge>> incomingEdges;

	private final Map<String, LinkProperties> osm = LinkProperties.createLinkProperties();

	SumoNetworkFeatureExtractor(SumoNetworkHandler handler) {
		this.handler = handler;

		incomingEdges = new HashMap<>();

		for (SumoNetworkHandler.Edge edge : this.handler.edges.values()) {
			incomingEdges.computeIfAbsent(edge.to, (k) -> new ArrayList<>())
				.add(edge);
		}
	}

	private static String getHighwayType(String type) {
		if (type != null)
			type = type.replaceFirst("^highway\\.", "");

		if (type == null || type.isBlank())
			type = "unclassified";

		return type;
	}

	private static SumoNetworkHandler.Req or(SumoNetworkHandler.Req r1, SumoNetworkHandler.Req r2) {
		BitSet response = (BitSet) r1.response().clone();
		BitSet foes = (BitSet) r1.foes().clone();
		response.or(r2.response());
		foes.or(r2.foes());
		return new SumoNetworkHandler.Req(response, foes);
	}

	private static String calcPrio(int priority, List<Integer> prios) {
		double ref = (prios.size() - 1) / 2.0;
		int cmp = prios.indexOf(priority);

		if (cmp > ref)
			return "higher";
		else if (cmp < ref)
			return "lower";
		else
			return "equal";
	}

	private static String bool(boolean b) {
		return b ? "1" : "0";
	}

	private static Set<Character> directionSet(SumoNetworkHandler.Connection c) {
		// turn is ignored
		String dirs = c.dir.toLowerCase().replace("t", "");
		Set<Character> set = new HashSet<>();
		for (int i = 0; i < dirs.length(); i++) {
			set.add(dirs.charAt(i));
		}
		return set;
	}

	/**
	 * Get priority. Higher is more important.
	 */
	private int getPrio(SumoNetworkHandler.Edge edge) {
		return -osm.getOrDefault(getHighwayType(edge.type), new LinkProperties(LinkProperties.LEVEL_UNCLASSIFIED, 1, 1, 1, true)).getHierarchyLevel();
	}

	public List<String> getHeader() {
		return List.of("linkId", "highway_type", "speed", "length", "num_lanes", "change_num_lanes", "change_speed", "num_to_links", "num_conns",
			"num_response", "num_foes", "dir_multiple_s", "dir_l", "dir_r", "dir_s", "dir_exclusive",
			"junction_type", "junction_inc_lanes", "priority_higher", "priority_equal", "priority_lower",
			"is_secondary_or_higher", "is_primary_or_higher", "is_motorway", "is_link");
	}

	public void print(CSVPrinter out) {
		handler.edges.keySet().stream().sorted().forEach(e -> {
			try {
				print(out, e, handler.edges.get(e));
			} catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		});
	}

	public void print(CSVPrinter out, String linkId, SumoNetworkHandler.Edge edge) throws IOException {

		String highwayType = getHighwayType(edge.type);

		SumoNetworkHandler.Junction junction = handler.junctions.get(edge.to);
		List<SumoNetworkHandler.Connection> connections = handler.connections.computeIfAbsent(edge.id, (k) -> new ArrayList<>());

		Set<SumoNetworkHandler.Edge> toEdges = connections.stream()
			.filter(c -> !c.dir.equals("t"))
			.map(c -> handler.edges.get(c.to))
			.collect(Collectors.toSet());

		int maxLanes = toEdges.stream().mapToInt(e -> e.lanes.size()).max().orElse(1);
		double maxSpeed = toEdges.stream().flatMap(e -> e.lanes.stream()).mapToDouble(l -> l.speed).max().orElse(edge.lanes.get(0).speed);

		List<SumoNetworkHandler.Req> req = connections.stream()
			.filter(c -> !c.dir.equals("t"))
			.map(c -> c.reqIdx)
			// Filter connections without junction
			.filter(idx -> idx >= 0 && idx < junction.requests.size())
			.map(junction.requests::get).toList();

		SumoNetworkHandler.Req aggr = req.stream().reduce(SumoNetworkFeatureExtractor::or)
			.orElseGet(() -> new SumoNetworkHandler.Req(new BitSet(0), new BitSet(0)));

		Set<Character> dirs = new HashSet<>();
		boolean multipleDirS = false;
		boolean exclusiveDirs = true;
		for (SumoNetworkHandler.Connection c : connections) {

			Set<Character> d = directionSet(c);
			if (dirs.contains('s') && d.contains('s'))
				multipleDirS = true;

			Set<Character> intersection = new HashSet<>(dirs); // use the copy constructor
			intersection.retainAll(d);
			if (!intersection.isEmpty())
				exclusiveDirs = false;

			dirs.addAll(d);
		}

		List<Integer> prios = incomingEdges.get(junction.id).stream()
			.map(this::getPrio).distinct().sorted()
			.toList();
		String prio = calcPrio(getPrio(edge), prios);

		int incomingLanes = incomingEdges.get(junction.id).stream()
			.mapToInt(e -> e.lanes.size())
			.sum();

		boolean geq_secondary = switch (highwayType) {
			case "secondary", "primary", "trunk", "motorway" -> true;
			default -> false;
		};

		boolean geq_primary = switch (highwayType) {
			case "primary", "trunk", "motorway" -> true;
			default -> false;
		};

		out.print(linkId);
		out.print(highwayType);
		out.print(Math.max(8.33, edge.lanes.get(0).speed));
		out.print(edge.lanes.get(0).length);
		out.print(edge.lanes.size());
		out.print(Math.max(-3, Math.min(3, maxLanes - edge.lanes.size())));
		out.print(maxSpeed - edge.lanes.get(0).speed);
		out.print(toEdges.size());
		out.print(Math.min(6, handler.connections.get(edge.id).size()));
		out.print(Math.min(12, aggr.response().cardinality()));
		out.print(Math.min(12, aggr.foes().cardinality()));
		out.print(bool(multipleDirS));
		out.print(bool(dirs.contains('l')));
		out.print(bool(dirs.contains('r')));
		out.print(bool(dirs.contains('s')));
		out.print(bool(exclusiveDirs));
		out.print(junction.type);
		out.print(Math.min(12, incomingLanes));
		out.print(bool("higher".equals(prio)));
		out.print(bool("equal".equals(prio)));
		out.print(bool("lower".equals(prio)));
		out.print(bool(geq_secondary));
		out.print(bool(geq_primary));
		out.print(bool("motorway".equals(highwayType)));
		out.print(bool(highwayType.contains("link")));

		out.println();
	}

}

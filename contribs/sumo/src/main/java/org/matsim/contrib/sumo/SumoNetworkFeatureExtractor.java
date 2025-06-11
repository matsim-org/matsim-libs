package org.matsim.contrib.sumo;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
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
			incomingEdges.computeIfAbsent(edge.to, k -> new ArrayList<>())
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
	 * Calculate the curvature of an edge. One gon is 1/400 of a full circle.
	 * The formula is: KU = (Sum of the curvature of the subsegments) / (Length of the edge)
	 *
	 * @return curvature in gon/km
	 */
	static double calcCurvature(SumoNetworkHandler.Edge edge) {
		double totalGon = 0;
		List<double[]> coordinates = edge.shape;

		for (int i = 2; i < coordinates.size(); i++) {
			double[] pointA = coordinates.get(i - 2);
			double[] pointB = coordinates.get(i - 1);
			double[] pointC = coordinates.get(i);

			double[] vectorAB = {pointB[0] - pointA[0], pointB[1] - pointA[1]};
			double[] vectorBC = {pointC[0] - pointB[0], pointC[1] - pointB[1]};

			double dotProduct = calcDotProduct(vectorAB, vectorBC);
			double magnitudeAB = calcMagnitude(vectorAB);
			double magnitudeBC = calcMagnitude(vectorBC);

			double cosine = dotProduct / (magnitudeAB * magnitudeBC);
			double angleRadians = Math.acos(cosine);
			double angleDegrees = Math.toDegrees(angleRadians);

			totalGon += Math.abs((angleDegrees / 360) * 400);
		}

		return totalGon / (edge.getLength() / 1000);
	}

	/**
	 * Calculate the dot product of two vectors.
	 *
	 * @param vec1 vector 1
	 * @param vec2 vector 2
	 * @return dot product
	 */
	private static double calcDotProduct(double[] vec1, double[] vec2) {
		return vec1[0] * vec2[0] + vec1[1] * vec2[1];
	}

	/**
	 * Calculate the magnitude of a vector.
	 *
	 * @param vec vector
	 * @return magnitude
	 */
	private static double calcMagnitude(double[] vec) {
		return Math.sqrt(vec[0] * vec[0] + vec[1] * vec[1]);
	}


	/**
	 * Get priority. Higher is more important.
	 */
	private int getPrio(SumoNetworkHandler.Edge edge) {
		return -osm.getOrDefault(getHighwayType(edge.type), new LinkProperties(LinkProperties.LEVEL_UNCLASSIFIED, 1, 1, 1, true)).getHierarchyLevel();
	}

	public List<String> getHeader() {
		return List.of("linkId", "highway_type", "speed", "length", "num_lanes", "change_num_lanes", "change_speed", "num_to_links", "num_conns",
			"num_response", "num_foes", "dir_multiple_s", "dir_l", "dir_r", "dir_s", "dir_exclusive", "curvature",
			"junction_type", "junction_inc_lanes", "priority_higher", "priority_equal", "priority_lower",
			"is_secondary_or_higher", "is_primary_or_higher", "is_motorway",
			"is_link", "has_merging_link", "is_merging_into",
			"num_left", "num_right", "num_straight"
		);
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
		List<SumoNetworkHandler.Connection> connections = handler.connections.computeIfAbsent(edge.id, k -> new ArrayList<>());

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

		// Number of connections per direction
		Object2IntMap<Character> numConnections = new Object2IntOpenHashMap<>();

		for (SumoNetworkHandler.Connection c : connections) {

			Set<Character> d = directionSet(c);
			if (dirs.contains('s') && d.contains('s'))
				multipleDirS = true;

			d.forEach(dir -> numConnections.mergeInt(dir, 1, Integer::sum));

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

		boolean merging = incomingEdges.get(junction.id).stream()
			.anyMatch(e -> e.type.contains("link"));

		OptionalInt highestPrio = incomingEdges.get(junction.id).stream()
			.mapToInt(this::getPrio).max();

		// Find category of the highest merging lane
		String mergingHighest = "";
		if (highestPrio.isPresent() && highestPrio.getAsInt() > getPrio(edge)) {
			Optional<Map.Entry<String, LinkProperties>> m = osm.entrySet().stream().filter(e -> e.getValue().getHierarchyLevel() == -highestPrio.getAsInt())
				.findFirst();
			if (m.isPresent())
				mergingHighest = m.get().getKey();
		}

		boolean geqSecondary = switch (highwayType) {
			case "secondary", "primary", "trunk", "motorway" -> true;
			default -> false;
		};

		boolean geqPrimary = switch (highwayType) {
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
		out.print(Math.min(6, connections.size()));
		out.print(Math.min(12, aggr.response().cardinality()));
		out.print(Math.min(12, aggr.foes().cardinality()));
		out.print(bool(multipleDirS));
		out.print(bool(dirs.contains('l')));
		out.print(bool(dirs.contains('r')));
		out.print(bool(dirs.contains('s')));
		out.print(bool(exclusiveDirs));
		out.print(calcCurvature(edge));
		out.print(junction.type);
		out.print(Math.min(12, incomingLanes));
		out.print(bool("higher".equals(prio)));
		out.print(bool("equal".equals(prio)));
		out.print(bool("lower".equals(prio)));
		out.print(bool(geqSecondary));
		out.print(bool(geqPrimary));
		out.print(bool("motorway".equals(highwayType)));
		out.print(bool(highwayType.contains("link")));
		out.print(bool(merging));
		out.print(mergingHighest);
		out.print(numConnections.getInt('l'));
		out.print(numConnections.getInt('r'));
		out.print(numConnections.getInt('s'));

		for (String attribute : handler.attributes) {
			out.print(edge.attributes.getOrDefault(attribute, ""));
		}

		out.println();
	}

}

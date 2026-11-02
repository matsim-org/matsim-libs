package org.matsim.contrib.msp.traveltimematrix;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which road classes the congestion index is measured separately for, and which class a link belongs to.
 * <p>
 * Level 1 of the matrix needs this twice and has to get the same answer both times: once when the index
 * is measured per class on a run, and once when the free-speed travel time of an origin-destination pair
 * is split by class. The classes therefore live in one place, in a fixed order, and are addressed by
 * <b>index</b> rather than by name - the split is stored as one byte per class and per zone pair, so the
 * order is part of the data.
 * <p>
 * The class of a link is derived from {@link NetworkUtils#getHighwayType(Link)}, i.e. the OSM
 * {@code highway} tag the network generation wrote into the link attributes. A link whose attribute is
 * missing reads {@code unclassified}, which is what that method returns.
 *
 * @see #single() level 0 - one class, the whole network at once
 * @see #byHighwayType(Network) one class per OSM highway type present in the network
 * @see #byGroups(Network, Map) groups of highway types, e.g. everything motorway-like in one class
 */
public final class RoadClassification {

	private static final Logger log = LogManager.getLogger(RoadClassification.class);

	/**
	 * The name of the one class of {@link #single()}: every road at once. Also the name under which the
	 * network-wide index is stored in a {@link org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndexProfile},
	 * whatever the classification - level 1 needs the network-wide value as a fallback for a class that
	 * carried no traffic.
	 */
	public static final String ALL = "all";

	/**
	 * The class a highway type lands in when {@link #byGroups(Network, Map)} has no group for it.
	 */
	public static final String OTHER = "other";

	/**
	 * A grouping that is coarse enough to keep the index statistically meaningful and fine enough to
	 * separate the roads a long trip uses from the ones a short trip uses. The names are the group's
	 * most important member, which is also what a reader expects to see in a table.
	 * <p>
	 * Anything not listed - {@code service}, {@code track}, a type a non-OSM network invented - goes to
	 * {@value #OTHER}. The {@code _link} ramps are deliberately in the group of the road they serve:
	 * a motorway ramp is where a motorway trip loses its time.
	 */
	public static final Map<String, Set<String>> DEFAULT_GROUPS = Map.of(
		"motorway", Set.of("motorway", "motorway_link", "trunk", "trunk_link"),
		"primary", Set.of("primary", "primary_link"),
		"secondary", Set.of("secondary", "secondary_link"),
		"tertiary", Set.of("tertiary", "tertiary_link"),
		"residential", Set.of("residential", "living_street", "unclassified", "road")
	);

	private final List<String> roadClasses;

	/**
	 * Highway type to the index of its class in {@link #roadClasses}. A type that is not in here is
	 * {@link #OTHER}, if the classification has such a class, and {@link #ALL} otherwise.
	 */
	private final Map<String, Integer> indexByHighwayType;

	/**
	 * The index {@link #indexOf(Link)} returns for a highway type the map does not know. Never negative:
	 * every classification has a class that takes the rest.
	 */
	private final int fallbackIndex;

	/**
	 * Highway types that were asked for and are not in {@link #indexByHighwayType}, so that the warning
	 * about them is logged once per type rather than once per link. Concurrent because
	 * {@link #indexOf(Link)} is called from every thread of the road class share computation.
	 */
	private final Set<String> unknownTypesWarnedAbout = ConcurrentHashMap.newKeySet();

	private RoadClassification(List<String> roadClasses, Map<String, Integer> indexByHighwayType, int fallbackIndex) {
		this.roadClasses = List.copyOf(roadClasses);
		this.indexByHighwayType = Map.copyOf(indexByHighwayType);
		this.fallbackIndex = fallbackIndex;
	}

	/**
	 * One class, {@value #ALL}, holding the whole network. This is level 0 of the matrix: the congestion
	 * index does not depend on where the trip went, so no road class split is stored and no per class
	 * index is measured.
	 */
	public static RoadClassification single() {
		return new RoadClassification(List.of(ALL), Map.of(), 0);
	}

	/**
	 * One class per OSM highway type that actually occurs in the network, in alphabetical order so that
	 * the order does not depend on the iteration order of the network.
	 * <p>
	 * This is the finest split the index can be measured at. It is useful for finding out which types
	 * behave alike before deciding on a {@link #byGroups(Network, Map) grouping}, and for a network whose
	 * types are few. For a city-size network it usually has classes so small that their index is noise:
	 * the index is volume-weighted, so a class with little traffic is measured on little traffic.
	 */
	public static RoadClassification byHighwayType(Network network) {
		Set<String> types = new TreeSet<>();
		for (Link link : network.getLinks().values()) {
			types.add(NetworkUtils.getHighwayType(link));
		}
		List<String> classes = new ArrayList<>(types);
		// a type that shows up only at query time (a link outside the network this was built over) has
		// no class of its own, so it goes to the last class rather than being refused
		classes.add(OTHER);

		Map<String, Integer> index = new LinkedHashMap<>();
		for (int i = 0; i < classes.size(); i++) {
			index.put(classes.get(i), i);
		}
		return new RoadClassification(classes, index, classes.size() - 1);
	}

	/**
	 * The groups of highway types given, plus {@value #OTHER} for everything else. The classes come out
	 * in the alphabetical order of the group names, again so that the order is reproducible.
	 *
	 * @param network only used to log which types of it ended up in {@value #OTHER} - a long list there
	 * usually means the grouping does not fit the network
	 * @param groups class name to the highway types it holds, e.g. {@link #DEFAULT_GROUPS}
	 * @throws IllegalArgumentException if a highway type is in two groups, since its class would then
	 * depend on the iteration order
	 */
	public static RoadClassification byGroups(Network network, Map<String, Set<String>> groups) {

		List<String> classes = new ArrayList<>(new TreeSet<>(groups.keySet()));
		classes.add(OTHER);

		Map<String, Integer> index = new HashMap<>();
		for (Map.Entry<String, Set<String>> group : groups.entrySet()) {
			int classIndex = classes.indexOf(group.getKey());
			for (String type : group.getValue()) {
				Integer previous = index.put(type, classIndex);
				if (previous != null && previous != classIndex) {
					throw new IllegalArgumentException("Highway type '" + type + "' is in two road classes, '"
						+ classes.get(previous) + "' and '" + group.getKey() + "', so its class would depend on the "
						+ "order the groups are read in. Put it in one of the two.");
				}
			}
		}

		RoadClassification classification = new RoadClassification(classes, index, classes.size() - 1);
		classification.logOtherTypes(network);
		return classification;
	}

	/**
	 * The default classification over the given network: {@link #DEFAULT_GROUPS}.
	 */
	public static RoadClassification byDefaultGroups(Network network) {
		return byGroups(network, DEFAULT_GROUPS);
	}

	/**
	 * The road classes, in the order their index refers to. Index {@code i} of every array the level 1
	 * machinery passes around is the class at position {@code i} of this list.
	 */
	public List<String> getRoadClasses() {
		return this.roadClasses;
	}

	/**
	 * @return how many classes there are, i.e. the length of a road class share vector
	 */
	public int size() {
		return this.roadClasses.size();
	}

	/**
	 * @return true if this is {@link #single()}, i.e. there is nothing to split by
	 */
	public boolean isSingleClass() {
		return this.roadClasses.size() == 1;
	}

	/**
	 * @return the index of the class the link belongs to, always a valid position in
	 * {@link #getRoadClasses()}
	 */
	public int indexOf(Link link) {
		String type = NetworkUtils.getHighwayType(link);
		Integer index = this.indexByHighwayType.get(type);
		if (index != null) {
			return index;
		}
		if (!this.roadClasses.get(this.fallbackIndex).equals(ALL) && this.unknownTypesWarnedAbout.add(type)) {
			log.warn("Highway type '{}' has no road class of its own, so its links count as '{}'.", type,
				this.roadClasses.get(this.fallbackIndex));
		}
		return this.fallbackIndex;
	}

	/**
	 * @return the name of the class the link belongs to
	 */
	public String classOf(Link link) {
		return this.roadClasses.get(indexOf(link));
	}

	/**
	 * Logs which highway types of the network fell through to {@value #OTHER}, with their share of the
	 * lane kilometres, because a grouping that misses an important type is otherwise hard to notice: the
	 * index is still computed, just over a class that means nothing.
	 */
	private void logOtherTypes(Network network) {

		Map<String, Double> laneKmByType = new LinkedHashMap<>();
		double total = 0;
		for (Link link : network.getLinks().values()) {
			double laneKm = link.getLength() * link.getNumberOfLanes() / 1000;
			total += laneKm;
			if (this.indexByHighwayType.get(NetworkUtils.getHighwayType(link)) == null) {
				laneKmByType.merge(NetworkUtils.getHighwayType(link), laneKm, Double::sum);
			}
		}

		if (laneKmByType.isEmpty()) {
			log.info("Road classes {}: every highway type of the network has a class.", this.roadClasses);
			return;
		}

		double otherLaneKm = laneKmByType.values().stream().mapToDouble(Double::doubleValue).sum();
		log.info("Road classes {}: the types {} have no class of their own and count as '{}' - {} of {} lane km "
				+ "({}%) of the network.", this.roadClasses, new TreeSet<>(laneKmByType.keySet()), OTHER,
			Math.round(otherLaneKm), Math.round(total), Math.round(100 * otherLaneKm / total));
	}

	/**
	 * @return the classes a profile has to hold for this classification: every class plus {@value #ALL},
	 * which is the fallback for a class that carried no traffic in a time bin
	 */
	public Collection<String> getRoadClassesWithAll() {
		Set<String> all = new LinkedHashSet<>();
		all.add(ALL);
		all.addAll(this.roadClasses);
		return all;
	}
}

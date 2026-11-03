package org.matsim.contrib.msp.traveltimematrix.prepare;

import jakarta.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.scenario.ProjectionUtils;

import java.util.Locale;
import java.util.Set;

/**
 * Cuts the part of a network the matrix is built over: the links of one mode inside a study area, as one
 * connected network.
 * <p>
 * A travel time matrix is quadratic in the number of zones and the number of nodes, so a Germany-wide
 * network is not something one builds a matrix over when the question is about one city. Cutting it is
 * therefore not an optimisation but the thing that makes the matrix possible at all.
 * <p>
 * <b>The link ids are kept.</b> That is the whole point of cutting rather than re-generating: the
 * congested travel times and the volumes measured on the <em>full</em> run still address the links of the
 * cut network, so the congestion index of the study area can be measured without re-running anything.
 * {@link NetworkFilterManager} copies the attributes along with them, so the OSM highway type the
 * {@link org.matsim.contrib.msp.traveltimematrix.RoadClassification road classes} are derived from
 * survives as well.
 * <p>
 * <b>Give the area a buffer.</b> Around a city, the quickest way between two points often leaves the
 * city and comes back - the motorway ring is outside the administrative boundary almost everywhere. Cut
 * at the boundary and those paths are gone, so the matrix reports the trip as slower than it is, in a
 * way that looks like congestion but is not. A few kilometres of buffer around the area the results are
 * reported for is the cheap fix, and the log line below is how to check it was enough: a sharp drop in
 * links against a modest drop in the area means the cut went through the ring.
 */
public final class StudyAreaNetwork {

	private static final Logger log = LogManager.getLogger(StudyAreaNetwork.class);

	private StudyAreaNetwork() {
	}

	/**
	 * The car subnetwork of the study area, cleaned.
	 *
	 * @see #cut(Network, ShpOptions.Index, Set)
	 */
	public static Network cutCarNetwork(Network network, @Nullable ShpOptions.Index studyArea) {
		return cut(network, studyArea, Set.of(TransportMode.car));
	}

	/**
	 * @param network the network of the run, untouched - the cut network is a copy
	 * @param studyArea the area to keep, or null to keep the whole network and only filter by mode. A
	 * link is kept if <b>both</b> of its nodes are inside: a link with one end outside leads out of the
	 * area, and keeping it would put travel times into the matrix whose congestion was never measured.
	 * @param modes the modes a link has to allow at least one of
	 * @return a new network of the links that survived, with their ids and attributes, reduced to its
	 * largest strongly connected component for the given modes by
	 * {@link NetworkUtils#cleanNetwork(Network, Set)} - a zonal matrix needs every pair of nodes to be
	 * connected, so a dead end left in by the cut would make the matrix throw rather than be slightly
	 * wrong
	 */
	public static Network cut(Network network, @Nullable ShpOptions.Index studyArea, Set<String> modes) {

		NetworkFilterManager filter = new NetworkFilterManager(network, new NetworkConfigGroup());
		filter.addLinkFilter(link -> link.getAllowedModes().stream().anyMatch(modes::contains));
		if (studyArea != null) {
			filter.addLinkFilter(link -> inside(studyArea, link));
		}

		Network cut = filter.applyFilters();
		int linksBeforeCleaning = cut.getLinks().size();
		NetworkUtils.cleanNetwork(cut, modes);

		log.info("Study area network: {} of {} links ({}%) allow {} and lie inside the area; cleaning for a "
				+ "connected network dropped another {}, leaving {} links and {} nodes.", linksBeforeCleaning,
			network.getLinks().size(),
			String.format(Locale.ROOT, "%.1f", 100.0 * linksBeforeCleaning / network.getLinks().size()), modes,
			linksBeforeCleaning - cut.getLinks().size(), cut.getLinks().size(), cut.getNodes().size());

		if (cut.getLinks().isEmpty()) {
			throw new IllegalArgumentException("No link of the network allows " + modes + " inside the study area, so "
				+ "there is nothing to build a matrix over. Check the modes and whether the shape file and the "
				+ "network use the same coordinate reference system.");
		}

		return cut;
	}

	/**
	 * The study area of a {@code --shp} command line option, in the coordinate reference system of the
	 * <b>network</b> - taken from the network itself when it carries one, so that a shape file in a
	 * different projection is still read correctly and a silently empty cut is avoided.
	 * <p>
	 * Every command of this package resolves its study area this way, and they have to: the congestion
	 * index, the matrix and the evaluation of the matrix all have to be built over the same area, or the
	 * numbers describe different cities.
	 *
	 * @return the index to cut with, or null when no shape file was given - which keeps the whole network
	 * and is only right when the network already is the study area
	 */
	@Nullable
	public static ShpOptions.Index index(ShpOptions shp, Network network) {
		if (!shp.isDefined()) {
			log.warn("No --shp given, so the whole network is used as the study area. That is only what you want if "
				+ "the network is already cut to the area the results are about.");
			return null;
		}
		String crs = ProjectionUtils.getCRS(network);
		return shp.createIndex(crs != null ? crs : shp.getShapeCrs(), "_");
	}

	/**
	 * A link counts as inside when both of its nodes are. Using the nodes rather than the link's own
	 * coordinate matters at the edge of the area: the matrix routes from node to node, so a link whose
	 * far node was cut away is a link into nowhere.
	 */
	private static boolean inside(ShpOptions.Index studyArea, Link link) {
		return studyArea.contains(link.getFromNode().getCoord()) && studyArea.contains(link.getToNode().getCoord());
	}
}

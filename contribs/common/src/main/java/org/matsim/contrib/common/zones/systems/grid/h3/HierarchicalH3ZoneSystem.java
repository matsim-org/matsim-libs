package org.matsim.contrib.common.zones.systems.grid.h3;

import com.uber.h3core.AreaUnit;
import com.uber.h3core.H3Core;
import com.uber.h3core.LengthUnit;
import com.uber.h3core.util.LatLng;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.GridZoneSystem;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.matsim.contrib.common.zones.systems.grid.h3.H3Utils.coordToLatLng;

/**
 * A hierarchical H3 zone system that adaptively subdivides cells based on a
 * configurable {@link SubdivisionCriterion}.
 *
 * <p>Starting at {@code minResolution}, network nodes are grouped into H3 cells.
 * Cells satisfying the subdivision criterion are iteratively split into finer-resolution
 * child cells until either the criterion is no longer met or {@code maxResolution}
 * is reached.</p>
 *
 * @author nkuehnel / MOIA
 */
public class HierarchicalH3ZoneSystem implements GridZoneSystem {

	private static final Logger log = LogManager.getLogger(HierarchicalH3ZoneSystem.class);

	private final IdMap<Zone, Zone> zones = new IdMap<>(Zone.class);
	private final IdMap<Zone, List<Link>> zoneToLinksMap = new IdMap<>(Zone.class);

	private final CoordinateTransformation toLatLong;
	private final CoordinateTransformation fromLatLong;

	private final int minResolution;
	private final int maxResolution;

	private final Network network;
	private final Predicate<Zone> filter;
	private final SubdivisionCriterion subdivisionCriterion;

	/**
	 * @param crs                   coordinate reference system of the network
	 * @param minResolution         starting H3 resolution (coarsest)
	 * @param maxResolution         maximum H3 resolution (finest allowed)
	 * @param network               the MATSim network
	 * @param subdivisionCriterion  strategy to decide whether to subdivide a cell
	 * @param filter                predicate to include/exclude created zones
	 */
	public HierarchicalH3ZoneSystem(String crs, int minResolution, int maxResolution,
									Network network, SubdivisionCriterion subdivisionCriterion,
									Predicate<Zone> filter) {
		this.fromLatLong = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, crs);
		this.toLatLong = TransformationFactory.getCoordinateTransformation(crs, TransformationFactory.WGS84);
		this.minResolution = minResolution;
		this.maxResolution = maxResolution;
		this.network = network;
		this.subdivisionCriterion = subdivisionCriterion;
		this.filter = filter;
		init();
	}

	/**
	 * Convenience constructor with no zone filter (all zones accepted).
	 */
	public HierarchicalH3ZoneSystem(String crs, int minResolution, int maxResolution,
									Network network, SubdivisionCriterion subdivisionCriterion) {
		this(crs, minResolution, maxResolution, network, subdivisionCriterion, z -> true);
	}

	/**
	 * Creates a hierarchical zone system that subdivides cells exceeding a maximum node count.
	 */
	public static HierarchicalH3ZoneSystem withNodeCountCriterion(String crs, int minResolution, int maxResolution,
																  int maxNodesPerZone, Network network,
																  Predicate<Zone> filter) {
		return new HierarchicalH3ZoneSystem(crs, minResolution, maxResolution, network,
				new NodeCountSubdivisionCriterion(maxNodesPerZone), filter);
	}

	/**
	 * Creates a hierarchical zone system that subdivides cells exceeding a maximum node count.
	 */
	public static HierarchicalH3ZoneSystem withNodeCountCriterion(String crs, int minResolution, int maxResolution,
																  int maxNodesPerZone, Network network) {
		return withNodeCountCriterion(crs, minResolution, maxResolution, maxNodesPerZone, network, z -> true);
	}

	@Override
	public Optional<Zone> getZoneForCoord(Coord coord) {
		int resolution = minResolution;
		do {
			long h3Address = getH3Cell(coord, resolution);
			Id<Zone> zoneId = Id.create(h3Address, Zone.class);
			if (this.zones.containsKey(zoneId)) {
				return Optional.of(zones.get(zoneId));
			}

			if (resolution > minResolution) {
				long parent = H3Utils.getInstance().cellToParent(h3Address, resolution - 1);
				zoneId = Id.create(parent, Zone.class);
				if (this.zones.containsKey(zoneId)) {
					return Optional.of(zones.get(zoneId));
				}
			}
			resolution++;
		} while (resolution <= maxResolution);
		return Optional.empty();
	}

	@Override
	public Optional<Zone> getZoneForLinkId(Id<Link> link) {
		return getZoneForCoord(network.getLinks().get(link).getToNode().getCoord());
	}

	@Override
	public Optional<Zone> getZoneForNodeId(Id<Node> nodeId) {
		return getZoneForCoord(network.getNodes().get(nodeId).getCoord());
	}

	@Override
	public List<Link> getLinksForZoneId(Id<Zone> zone) {
		return zoneToLinksMap.getOrDefault(zone, Collections.emptyList());
	}

	@Override
	public Map<Id<Zone>, Zone> getZones() {
		return Collections.unmodifiableMap(zones);
	}

	private void init() {
		H3Core h3 = H3Utils.getInstance();
		log.info("Creating hierarchical H3 zone system with resolution range [{}, {}]", minResolution, maxResolution);
		printResolutionStats(minResolution);
		printResolutionStats(maxResolution);

		Map<Long, List<Node>> cellToNodes = network.getNodes().values().stream()
				.collect(Collectors.groupingBy(node -> {
					LatLng latLng = coordToLatLng(toLatLong.transform(node.getCoord()));
					return h3.latLngToCell(latLng.lat, latLng.lng, minResolution);
				}));

		while (cellToNodes.entrySet().stream().anyMatch(e ->
				subdivisionCriterion.shouldSubdivide(e.getKey(), e.getValue(), h3.getResolution(e.getKey()), maxResolution))) {
			subdivide(cellToNodes);
		}

		for (Map.Entry<Long, List<Node>> entry : cellToNodes.entrySet()) {
			Optional<Zone> zone = H3Utils.createZone(entry.getKey(), this.fromLatLong);
			if (zone.isPresent() && this.filter.test(zone.get())) {
				zones.put(zone.get().getId(), zone.get());
				List<Link> links = entry.getValue()
						.stream()
						.<Link>flatMap(node -> node.getInLinks().values().stream())
						.distinct()
						.toList();
				zoneToLinksMap.put(zone.get().getId(), links);
			}
		}

		log.info("Created {} zones.", zones.size());
	}

	private void subdivide(Map<Long, List<Node>> cellToNodes) {
		H3Core h3 = H3Utils.getInstance();
		Iterator<Map.Entry<Long, List<Node>>> iterator = cellToNodes.entrySet().iterator();
		Map<Long, List<Node>> childCells = new HashMap<>();

		while (iterator.hasNext()) {
			Map.Entry<Long, List<Node>> entry = iterator.next();
			int resolution = h3.getResolution(entry.getKey());
			if (subdivisionCriterion.shouldSubdivide(entry.getKey(), entry.getValue(), resolution, maxResolution)) {
				iterator.remove();
				for (Node node : entry.getValue()) {
					LatLng latLng = coordToLatLng(toLatLong.transform(node.getCoord()));
					long childAddress = h3.latLngToCell(latLng.lat, latLng.lng, resolution + 1);
					childCells.computeIfAbsent(childAddress, a -> new ArrayList<>()).add(node);
				}
			}
		}

		for (Map.Entry<Long, List<Node>> entry : childCells.entrySet()) {
			long parentAddress = h3.cellToParent(entry.getKey(), h3.getResolution(entry.getKey()) - 1);
			if (cellToNodes.containsKey(parentAddress)) {
				cellToNodes.get(parentAddress).addAll(entry.getValue());
			} else {
				cellToNodes.merge(entry.getKey(), entry.getValue(), (existing, additional) -> {
					existing.addAll(additional);
					return existing;
				});
			}
		}
	}

	private long getH3Cell(Coord coord, int resolution) {
		return H3Utils.getH3Cell(toLatLong.transform(coord), resolution);
	}

	private void printResolutionStats(int resolution) {
		H3Core h3 = H3Utils.getInstance();
		double edgeLength = h3.getHexagonEdgeLengthAvg(resolution, LengthUnit.m);
		double area = h3.getHexagonAreaAvg(resolution, AreaUnit.m2);
		log.info("Resolution [{}] edge length: {} m, centroid distance: {} m, area: {} m²",
				resolution, edgeLength, edgeLength * Math.sqrt(3.0), area);
	}
}
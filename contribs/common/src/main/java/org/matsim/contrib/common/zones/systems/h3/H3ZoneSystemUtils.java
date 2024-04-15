package org.matsim.contrib.common.zones.systems.h3;

import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import one.util.streamex.EntryStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.prep.PreparedPolygon;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdCollectors;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.zones.*;
import org.matsim.contrib.common.zones.util.ZoneFinder;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author nkuehnel / MOIA
 */
public class H3ZoneSystemUtils {

	static final Logger log = LogManager.getLogger(H3ZoneSystemUtils.class);


	public static ZoneSystem createFromPreparedGeometries(Network network, Map<String, PreparedPolygon> geometries,
														  String crs, int resolution) {

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(crs, TransformationFactory.WGS84);
		Map<Id<Zone>, Zone> zones = EntryStream.of(geometries)
			.mapKeyValue((id, polygon) -> new ZoneImpl(ZoneSystemUtils.createZoneId(id), polygon, null))
			.collect(IdCollectors.toIdMap(Zone.class, Identifiable::getId, zone -> zone));

		H3ZoneFinder h3ZoneFinder = new H3ZoneFinder(resolution, ct, zones);

		Set<Id<Zone>> requiredZones = network.getLinks().values().stream()
			.map(link -> h3ZoneFinder.findZone(link.getToNode().getCoord()))
			.filter(Optional::isPresent) // Filter out any null zones
			.map(op -> op.get().getId()) // Convert Zone to Id<Zone>, adjust this line if findZone() directly returns Id<Zone>
			.collect(Collectors.toSet()); // Collect unique Ids into a Set

		zones.keySet().retainAll(requiredZones);

		log.info("Network filtered zone system contains " + requiredZones.size() + " zones for "
			+ network.getLinks().size() + " links and " + network.getNodes().size() + " nodes.");
		return new ZoneSystemImpl(zones.values(), h3ZoneFinder, network);
	}

	private static class H3ZoneFinder implements ZoneFinder {

		private final H3Core h3 = H3Utils.getInstance();
		private final int resolution;
		private final CoordinateTransformation ct;

		private final Map<Id<Zone>, Zone> zones;

		H3ZoneFinder(int resolution, CoordinateTransformation ct, Map<Id<Zone>, Zone> zones) {
			this.resolution = resolution;
			this.ct = ct;
			this.zones = zones;
		}

		@Override
		public Optional<Zone> findZone(Coord coord) {
			LatLng latLng = H3GridUtils.coordToLatLng(ct.transform(coord));
            Id<Zone> zoneId = ZoneSystemUtils.createZoneId(h3.latLngToCellAddress(latLng.lat, latLng.lng, resolution));
            return Optional.ofNullable(zones.get(zoneId));
		}
	}
}

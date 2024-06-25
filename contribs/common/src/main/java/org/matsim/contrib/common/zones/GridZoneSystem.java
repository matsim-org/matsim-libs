package org.matsim.contrib.common.zones;

import org.matsim.api.core.v01.Coord;

import java.util.Optional;

public interface GridZoneSystem extends ZoneSystem {

	Optional<Zone> getZoneForCoord(Coord coord);

}

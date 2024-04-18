package org.matsim.contrib.common.zones;

import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;

import javax.annotation.Nullable;
import java.util.List;

public interface Zone extends BasicLocation, Identifiable<Zone> {
	@Nullable
	PreparedGeometry getPreparedGeometry();

	Coord getCentroid();

	List<Link> getLinks();
}

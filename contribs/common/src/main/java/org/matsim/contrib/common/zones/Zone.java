package org.matsim.contrib.common.zones;

import org.locationtech.jts.geom.prep.PreparedPolygon;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.utils.objectattributes.attributable.Attributable;

import javax.annotation.Nullable;

public interface Zone extends BasicLocation, Identifiable<Zone>, Attributable {
	@Nullable
	PreparedPolygon getPreparedGeometry();

	Coord getCentroid();
	String getType();

}

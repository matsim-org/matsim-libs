package org.matsim.contrib.common.zones;

import org.locationtech.jts.geom.prep.PreparedPolygon;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import javax.annotation.Nullable;

public class ZoneImpl implements Zone {

	private final Id<Zone> id;
	@Nullable
	private PreparedPolygon preparedGeometry; //null for virtual/dummy zones
	private final Coord centroid;
	private String type;

	private final Attributes attributes = new AttributesImpl();


	public ZoneImpl(Id<Zone> id, PreparedPolygon preparedGeometry, @Nullable String type) {
		this(id, preparedGeometry, MGC.point2Coord(preparedGeometry.getGeometry().getCentroid()), type);
	}

	public ZoneImpl(Id<Zone> id, @Nullable PreparedPolygon preparedGeometry, Coord centroid, @Nullable String type) {
		this.id = id;
		this.preparedGeometry = preparedGeometry;
		this.centroid = centroid;
        this.type = type;
    }

	@Override
	public Id<Zone> getId() {
		return id;
	}

	@Override
	public Coord getCoord() {
		return centroid;
	}

	@Override
	@Nullable
	public PreparedPolygon getPreparedGeometry() {
		return preparedGeometry;
	}

	@Override
	public Coord getCentroid() {
		return centroid;
	}

	@Override
	public String getType() {
		return type;
	}

	boolean isDummy() {
		return preparedGeometry == null;
	}

	public void setGeometry(PreparedPolygon preparedPolygon) {
		this.preparedGeometry = preparedPolygon;
	}


	public static ZoneImpl createDummyZone(Id<Zone> id, Coord centroid) {
		return new ZoneImpl(id, null, centroid, null);
	}

	@Override
	public Attributes getAttributes() {
		return attributes;
	}
}

package org.matsim.contrib.common.zones;

import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.geotools.MGC;

import javax.annotation.Nullable;
import java.util.List;

public class ZoneImpl implements Zone {

	private final Id<Zone> id;
	@Nullable
	private final PreparedGeometry preparedGeometry; //null for virtual/dummy zones
	private final List<Link> links;
	private final Coord centroid;

	public ZoneImpl(Id<Zone> id, PreparedGeometry preparedGeometry, List<Link> links) {
		this(id, preparedGeometry, links, MGC.point2Coord(preparedGeometry.getGeometry().getCentroid()));
	}

	private ZoneImpl(Id<Zone> id, @Nullable PreparedGeometry preparedGeometry, List<Link> links, Coord centroid) {
		this.id = id;
		this.preparedGeometry = preparedGeometry;
		this.links = links;
		this.centroid = centroid;
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
	public PreparedGeometry getPreparedGeometry() {
		return preparedGeometry;
	}

	@Override
	public Coord getCentroid() {
		return centroid;
	}

	@Override
	public List<Link> getLinks() {
		return links;
	}

	boolean isDummy() {
		return preparedGeometry == null;
	}

	public static ZoneImpl createDummyZone(Id<Zone> id, List<Link> links, Coord centroid) {
		return new ZoneImpl(id, null, links, centroid);
	}

}

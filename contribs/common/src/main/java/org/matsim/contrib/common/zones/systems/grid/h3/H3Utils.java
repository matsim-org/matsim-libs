package org.matsim.contrib.common.zones.systems.grid.h3;

import com.google.common.base.Preconditions;
import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedPolygon;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.GeometryUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author nkuehnel / MOIA
 */
public final class H3Utils {

	private static H3Core h3;

    static {
        try {
            h3 = H3Core.newInstance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public final static int MAX_RES = 16;


	public static H3Core getInstance() {
		if(h3 == null) {
			try {
				h3 = H3Core.newInstance();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return h3;
	}

	private static Polygon getPolygon(String h3Id, CoordinateTransformation fromLatLong) {
		Preconditions.checkArgument(h3.isValidCell(h3Id), "Not a valid H3 address: " + h3Id);
		List<Coord> coordinateList = h3.cellToBoundary(h3Id)
			.stream()
			.map(latLng -> fromLatLong.transform(latLngToCoord(latLng)))
			.collect(Collectors.toList());

		if (!coordinateList.isEmpty()) {
			coordinateList.add(coordinateList.get(0));
		}

        return GeometryUtils.createGeotoolsPolygon(coordinateList);
	}

	private static Polygon getPolygon(long h3Id, CoordinateTransformation fromLatLong) {
		Preconditions.checkArgument(h3.isValidCell(h3Id), "Not a valid H3 address: " + h3Id);
		List<Coord> coordinateList = h3.cellToBoundary(h3Id)
			.stream()
			.map(latLng -> fromLatLong.transform(latLngToCoord(latLng)))
			.collect(Collectors.toList());

		if (!coordinateList.isEmpty()) {
			coordinateList.add(coordinateList.get(0));
		}

        return GeometryUtils.createGeotoolsPolygon(coordinateList);
	}

	public static LatLng coordToLatLng(Coord coord) {
		//invert coordinate order
		return new LatLng(coord.getY(), coord.getX());
	}

	public static Coord latLngToCoord(LatLng latLng) {
		//invert coordinate order
		return new Coord(latLng.lng, latLng.lat);
	}

	public static String getH3Address(Coord latLong, int resolution) {
		LatLng latLng = coordToLatLng(latLong);
		return h3.latLngToCellAddress(latLng.lat, latLng.lng, resolution);

	}

	public static long getH3Cell(Coord latLong, int resolution) {
		LatLng latLng = coordToLatLng(latLong);
		return h3.latLngToCell(latLng.lat, latLng.lng, resolution);

	}

	public static Optional<Zone> createZone(long id, CoordinateTransformation fromLatLong) {
		if(h3.isValidCell(id)) {
			return Optional.of(new ZoneImpl(Id.create(id, Zone.class), new PreparedPolygon(getPolygon(id, fromLatLong)), "h3"));
		} else {
			return Optional.empty();
		}
	}
}

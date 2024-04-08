package org.matsim.contrib.common.zones.h3;

import com.uber.h3core.AreaUnit;
import com.uber.h3core.H3Core;
import com.uber.h3core.LengthUnit;
import com.uber.h3core.util.LatLng;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author nkuehnel / MOIA
 */
public class H3GridUtils {

	static final Logger log = LogManager.getLogger(H3GridUtils.class);

	public static Map<String, PreparedGeometry> createH3GridFromNetwork(Network network, int resolution, String crs) {

		H3Core h3 = H3Utils.getInstance();

		log.info("start creating H3 grid from network at resolution " + resolution);
		double hexagonEdgeLengthAvg = h3.getHexagonEdgeLengthAvg(resolution, LengthUnit.m);
		log.info("Average edge length: " + hexagonEdgeLengthAvg + " meters.");
		log.info("Average centroid distance: " + hexagonEdgeLengthAvg * Math.sqrt(3) + " meters.");
		log.info("Average hexagon area: " + h3.getHexagonAreaAvg(resolution, AreaUnit.m2) + " m^2");

		double[] boundingbox = NetworkUtils.getBoundingBox(network.getNodes().values());
		double minX = boundingbox[0];
		double maxX = boundingbox[2];
		double minY = boundingbox[1];
		double maxY = boundingbox[3];

		GeometryFactory gf = new GeometryFactory();
		PreparedGeometryFactory preparedGeometryFactory = new PreparedGeometryFactory();
		Map<String, PreparedGeometry> grid = new HashMap<>();
		CoordinateTransformation toLatLong = TransformationFactory.getCoordinateTransformation(crs, TransformationFactory.WGS84);
		CoordinateTransformation fromLatLong = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, crs);

		List<LatLng> boundingBoxPoints = new ArrayList<>();

		Coord bottomLeft = toLatLong.transform(new Coord(minX, minY));
		Coord topLeft = toLatLong.transform(new Coord(minX, maxY));
		Coord topRight = toLatLong.transform(new Coord(maxX, maxY));
		Coord bottomRight = toLatLong.transform(new Coord(maxX, minY));

		boundingBoxPoints.add(coordToLatLng(bottomLeft));
		boundingBoxPoints.add(coordToLatLng(topLeft));
		boundingBoxPoints.add(coordToLatLng(topRight));
		boundingBoxPoints.add(coordToLatLng(bottomRight));
		boundingBoxPoints.add(coordToLatLng(bottomLeft));

		long millis = System.currentTimeMillis();

		//get cells in a finer resolution to catch links at the border
		List<String> h3Grid = h3.polygonToCellAddresses(boundingBoxPoints, Collections.emptyList(), Math.min(H3Utils.MAX_RES, resolution));
		h3Grid = h3Grid
			.parallelStream()
			//also include neighbors with distance 1
			.flatMap(h3Id -> h3.gridDisk(h3Id, 1).stream())
			.distinct()
			.toList();

		if(h3Grid.isEmpty()) {
			// bounding box too small to cover even a single H3 cell for a significant part. Use bounding box coords directly.
			h3Grid = boundingBoxPoints.stream().map(corner -> h3.latLngToCellAddress(corner.lat, corner.lng, resolution)).distinct().toList();
		}

		log.info("Obtained " + h3Grid.size() + " H3 cells in " + (System.currentTimeMillis() - millis) + " ms.");


		for (String h3Id : h3Grid) {
			List<Coordinate> coordinateList = h3.cellToBoundary(h3Id)
				.stream()
				.map(latLng -> CoordUtils.createGeotoolsCoordinate(fromLatLong.transform(latLngToCoord(latLng))))
				.collect(Collectors.toList());

			if (!coordinateList.isEmpty()) {
				coordinateList.add(coordinateList.get(0));
			}

			Polygon polygon = new Polygon(gf.createLinearRing(coordinateList.toArray(new Coordinate[0])), null, gf);
			grid.put(h3Id, preparedGeometryFactory.create(polygon));
		}

		log.info("finished creating H3 grid from network.");
		return grid;
	}

	public static LatLng coordToLatLng(Coord coord) {
		//invert coordinate order
		return new LatLng(coord.getY(), coord.getX());
	}

	public static Coord latLngToCoord(LatLng latLng) {
		//invert coordinate order
		return new Coord(latLng.lng, latLng.lat);
	}
}

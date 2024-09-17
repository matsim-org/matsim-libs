package org.matsim.contrib.vsp.pt.fare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;

/**
 * This class calculates the fare for a public transport trip based on the distance between the origin and destination. If a shape file is
 * provided, the
 * fare will be calculated only if the trip is within the shape.
 */
public class DistanceBasedPtFareCalculator implements PtFareCalculator {
	private static final Logger log = LogManager.getLogger(DistanceBasedPtFareCalculator.class);

	private final double minFare;
	private final SortedMap<Double, DistanceBasedPtFareParams.DistanceClassLinearFareFunctionParams> distanceClassFareParams;
	private ShpOptions shp = null;
	private final String transactionPartner;

	private final Map<Coord, Boolean> inShapeCache = new HashMap<>();

	public DistanceBasedPtFareCalculator(DistanceBasedPtFareParams params, URL context) {
		this.minFare = params.getMinFare();
		this.distanceClassFareParams = params.getDistanceClassFareParams();
		this.transactionPartner = params.getTransactionPartner();

		if (params.getFareZoneShp() != null) {
			log.info("For DistanceBasedPtFareCalculator '{}' a fare zone shape file was provided. During the computation, the fare will be " +
				"calculated only if the trip is within the shape.", params.getDescription());
			this.shp = new ShpOptions(IOUtils.extendUrl(context, params.getFareZoneShp()).toString(), null, null);
		} else {
			log.info("For DistanceBasedPtFareCalculator '{}' no fare zone shape file was provided. The fare will be calculated for all trips.",
				params.getDescription());
		}
	}

	@Override
	public Optional<FareResult> calculateFare(Coord from, Coord to) {
		if (!shapeCheck(from, to)) {
			return Optional.empty();
		}

		double distance = CoordUtils.calcEuclideanDistance(from, to);

		double fare = computeFare(distance, minFare, distanceClassFareParams);
		return Optional.of(new FareResult(fare, transactionPartner));
	}

	private boolean shapeCheck(Coord from, Coord to) {
		if (shp == null) {
			return true;
		}
		return inShapeCache.computeIfAbsent(from, this::inShape) && inShapeCache.computeIfAbsent(to, this::inShape);
	}

	private boolean inShape(Coord coord) {
		return shp.readFeatures().stream().anyMatch(f -> MGC.coord2Point(coord).within((Geometry) f.getDefaultGeometry()));
	}

	public static double computeFare(double distance, double minFare,
									 SortedMap<Double, DistanceBasedPtFareParams.DistanceClassLinearFareFunctionParams> distanceClassFareParams) {
		try {
			DistanceBasedPtFareParams.DistanceClassLinearFareFunctionParams distanceClassFareParam = distanceClassFareParams.tailMap(distance).firstEntry().getValue();
			return Math.max(minFare, distance * distanceClassFareParam.getFareSlope() + distanceClassFareParam.getFareIntercept());
		} catch (IllegalArgumentException e) {
			log.error("No fare found for distance of " + distance + " meters.");
			log.error(e.getMessage());
			throw new RuntimeException("No fare found for distance of " + distance + " meters.");
		}
	}
}

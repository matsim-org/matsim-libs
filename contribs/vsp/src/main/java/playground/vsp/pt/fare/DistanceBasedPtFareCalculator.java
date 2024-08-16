package playground.vsp.pt.fare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This class calculates the fare for a public transport trip based on the distance between the origin and destination. If a shape file is
 * provided, the
 * fare will be calculated only if the trip is within the shape.
 */
public class DistanceBasedPtFareCalculator implements PtFareCalculator {
	private static final Logger log = LogManager.getLogger(DistanceBasedPtFareCalculator.class);

	private final double minFare;
	private final double shortTripIntercept;
	private final double shortTripSlope;
	private final double longTripIntercept;
	private final double longTripSlope;
	private final double longTripThreshold;
	private ShpOptions shp = null;
	private final String transactionPartner;

	private final Map<Coord, Boolean> inShapeCache = new HashMap<>();

	public DistanceBasedPtFareCalculator(DistanceBasedPtFareParams params) {
		this.minFare = params.getMinFare();
		this.shortTripIntercept = params.getNormalTripIntercept();
		this.shortTripSlope = params.getNormalTripSlope();
		this.longTripIntercept = params.getLongDistanceTripIntercept();
		this.longTripSlope = params.getLongDistanceTripSlope();
		this.longTripThreshold = params.getLongDistanceTripThreshold();
		this.transactionPartner = params.getTransactionPartner();

		if (params.getFareZoneShp() != null) {
			log.info("For DistanceBasedPtFareCalculator '{}' a fare zone shape file was provided. During the computation, the fare will be " +
				"calculated only if the trip is within the shape.", params.getDescription());
			this.shp = new ShpOptions(params.getFareZoneShp(), null, null);
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

		double fare = computeFare(distance, longTripThreshold, minFare, shortTripIntercept, shortTripSlope, longTripIntercept, longTripSlope);
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

	public static double computeFare(double distance, double longTripThreshold, double minFare, double shortTripIntercept, double shortTripSlope,
									 double longTripIntercept, double longTripSlope) {
		if (distance <= longTripThreshold) {
			return Math.max(minFare, shortTripIntercept + shortTripSlope * distance);
		} else {
			return Math.max(minFare, longTripIntercept + longTripSlope * distance);
		}
	}
}

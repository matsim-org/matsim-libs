package playground.vsp.pt.fare;

import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

import java.util.List;
import java.util.Optional;

public class DistanceBasedPtFareCalculator implements PtFareCalculator {
	private final double minFare;
	private final double shortTripIntercept;
	private final double shortTripSlope;
	private final double longTripIntercept;
	private final double longTripSlope;
	private final double longTripThreshold;
	private ShpOptions shp = null;
	private final String transactionPartner;

	public DistanceBasedPtFareCalculator(DistanceBasedPtFareParams params) {
		this.minFare = params.getMinFare();
		this.shortTripIntercept = params.getNormalTripIntercept();
		this.shortTripSlope = params.getNormalTripSlope();
		this.longTripIntercept = params.getLongDistanceTripIntercept();
		this.longTripSlope = params.getLongDistanceTripSlope();
		this.longTripThreshold = params.getLongDistanceTripThreshold();
		this.transactionPartner = params.getTransactionPartner();

		if (params.getFareZoneShp() != null) {
			this.shp = new ShpOptions(params.getFareZoneShp(), null, null);
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

		return inShape(from, shp.readFeatures()) && inShape(to, shp.readFeatures());
	}

	boolean inShape(Coord coord, List<SimpleFeature> features) {
		return features.stream().anyMatch(f -> MGC.coord2Point(coord).within((Geometry) f.getDefaultGeometry()));
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

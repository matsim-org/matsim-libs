package playground.vsp.pt.fare;

import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.utils.geometry.geotools.MGC;

import java.util.List;
import java.util.Optional;

public class FareZoneBasedPtFareCalculator implements PtFareCalculator {
	private final ShpOptions shp;
	private final String transactionPartner;

	public static final String FARE = "fare";

	public FareZoneBasedPtFareCalculator(FareZoneBasedPtFareParams params) {
		this.shp = new ShpOptions(params.getFareZoneShp(), null, null);
		transactionPartner = params.getTransactionPartner();
	}

	@Override
	public Optional<FareResult> calculateFare(Coord from, Coord to) {
		Optional<SimpleFeature> departureZone = determineFareZone(from, shp.readFeatures());
		Optional<SimpleFeature> arrivalZone = determineFareZone(to, shp.readFeatures());

		//if one of the zones is empty, it is not included in the shape file, so this calculator cannot compute the fare
		if (departureZone.isEmpty() || arrivalZone.isEmpty()) {
			return Optional.empty();
		}

		if (!departureZone.get().getID().equals(arrivalZone.get().getID())) {
			return Optional.empty();
		}

		Double fare = (Double) departureZone.get().getAttribute(FARE);
		return Optional.of(new FareResult(fare, transactionPartner));
	}

	Optional<SimpleFeature> determineFareZone(Coord coord, List<SimpleFeature> features) {
		return features.stream().filter(f -> MGC.coord2Point(coord).within((Geometry) f.getDefaultGeometry())).findFirst();
	}
}

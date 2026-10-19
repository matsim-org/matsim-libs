package org.matsim.contrib.vsp.pt.fare;

import com.google.common.base.Verify;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This class calculates the fare for a public transport trip based on the fare zone the origin and destination are in.
 */
public class FareZoneBasedPtFareCalculator implements PtFareCalculator {
	private final List<SimpleFeature> features;
	private final String transactionPartner;
	private final Map<Coord, Optional<SimpleFeature>> zoneByCoordCache = new HashMap<>();
	private final String shapefile;

	public static final String FARE = "fare";

	public FareZoneBasedPtFareCalculator(FareZoneBasedPtFareParams params, URL context) {
		ShpOptions shpOptions = new ShpOptions(IOUtils.extendUrl(context, params.getFareZoneShp()).toString(), null, null);
		this.features = shpOptions.readFeatures();
		this.shapefile = shpOptions.getShapeFile();
		transactionPartner = params.getTransactionPartner();
	}

	@Override
	public Optional<FareResult> calculateFare(Coord from, Coord to) {
		Optional<SimpleFeature> departureZone = zoneByCoordCache.computeIfAbsent(from, this::determineFareZone);
		Optional<SimpleFeature> arrivalZone = zoneByCoordCache.computeIfAbsent(to, this::determineFareZone);

		//if one of the zones is empty, it is not included in the shape file, so this calculator cannot compute the fare
		if (departureZone.isEmpty() || arrivalZone.isEmpty()) {
			return Optional.empty();
		}

		if (!departureZone.get().getID().equals(arrivalZone.get().getID())) {
			return Optional.empty();
		}

		Double fare = (Double) departureZone.get().getAttribute(FARE);
		Verify.verifyNotNull(fare, "Fare zone without attribute " + FARE + " in " + this.shapefile +
			" found. Terminating.");
		return Optional.of(new FareResult(fare, transactionPartner));
	}

	Optional<SimpleFeature> determineFareZone(Coord coord) {
		return features.stream().filter(f -> MGC.coord2Point(coord).within((Geometry) f.getDefaultGeometry())).findFirst();
	}
}

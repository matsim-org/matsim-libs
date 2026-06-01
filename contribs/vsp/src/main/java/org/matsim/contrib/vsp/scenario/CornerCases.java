package org.matsim.contrib.vsp.scenario;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import playground.vsp.simpleParkingCostHandler.ParkingCostConfigGroup;
import playground.vsp.simpleParkingCostHandler.ParkingCostModule;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.matsim.vehicles.VehicleType;

import java.nio.file.Path;
import java.util.List;


public class CornerCases {

	private static final Logger log = LogManager.getLogger(CornerCases.class);

	/**
	 * Doubles or increases bike speed.
	 */
	public static void increaseBikeSpeed(Scenario scenario, double factor) {

		VehicleType bike = scenario.getVehicles()
			.getVehicleTypes()
			.get(Id.create("bike", VehicleType.class));

		bike.setMaximumVelocity(
			bike.getMaximumVelocity() * factor
		);

		log.info("Bike speed increased by factor {}", factor);
	}

	/**
	 * Reduces car speed inside a shape area.
	 */
	public static void reduceCarSpeed(
		Scenario scenario,
		Path shp,
		double factor
	) {

		List<PreparedGeometry> geometries =
			ShpGeometryUtils.loadPreparedGeometries(
				IOUtils.resolveFileOrResource(shp.toString())
			);

		scenario.getNetwork().getLinks().values().stream()
			.filter(link -> link.getAllowedModes().contains(TransportMode.car))
			.filter(link -> ShpGeometryUtils.isCoordInPreparedGeometries(link.getCoord(), geometries))
			.filter(link -> !((String) link.getAttributes().getAttribute("type")).contains("motorway"))
			.forEach(link ->
				link.setFreespeed(link.getFreespeed() * factor)
			);

		log.info("Reduced speeds by factor {}", factor);
	}

	/**
	 * Adds parking costs inside shape area.
	 */
	public static void addParkingCosts(
		Scenario scenario,
		Config config,
		Controler controler,
		Path shp,
		double firstHourCost
	) {

		ParkingCostConfigGroup group =

			ConfigUtils.addOrGetModule(config, ParkingCostConfigGroup.class);

		group.setFirstHourParkingCostLinkAttributeName(
			"firstHourParkingCost"
		);

		group.setExtraHourParkingCostLinkAttributeName(
			"extraHourParkingCost"
		);

		group.setResidentialParkingFeeAttributeName(
			"residentialParkingFee"
		);

		controler.addOverridingModule(new ParkingCostModule());
		controler.addOverridingModule(new PersonMoneyEventsAnalysisModule());

		List<PreparedGeometry> geometries =
			ShpGeometryUtils.loadPreparedGeometries(
				IOUtils.resolveFileOrResource(shp.toString())
			);

		scenario.getNetwork().getLinks().values().stream()
			.filter(link -> link.getAllowedModes().contains(TransportMode.car))
			.filter(link -> ShpGeometryUtils.isCoordInPreparedGeometries(link.getCoord(), geometries))
			.forEach(link -> {
				link.getAttributes().putAttribute(
					"firstHourParkingCost",
					firstHourCost
				);
				link.getAttributes().putAttribute(
					"extraHourParkingCost",
					firstHourCost / 2
				);
			});

		log.info("Applied parking costs.");
	}


}

package org.matsim.contrib.vsp.scenario;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.matsim.vehicles.VehicleType;
import playground.vsp.simpleParkingCostHandler.ParkingCostConfigGroup;
import playground.vsp.simpleParkingCostHandler.ParkingCostModule;

import java.nio.file.Path;
import java.util.Set;

/**

 * Collection of reusable scenario modifications for MATSim sensitivity analyses.

 * <p>
 * This utility class contains methods that apply common "what-if" interventions
 * to a scenario without requiring changes to the underlying scenario generation.
 * Typical applications include:
 * <ul>
 *   <li>changing bicycle travel speeds,</li>
 *   <li>reducing car speeds or capacities within a geographic area, and</li>
 *   <li>configuring and applying parking costs.</li>
 * </ul>
 * <p>
 * The methods are intended as building blocks when defining
 * policy scenarios or robustness tests.
 */
public class CornerCases {

	private static final Logger log = LogManager.getLogger(CornerCases.class);
	public static final String SPEED_REDUCTION_FACTOR_ATTRIBUTE = "cornerCasesSpeedReductionFactor";
	public static final String CAPACITY_REDUCTION_FACTOR_ATTRIBUTE = "cornerCasesCapacityReductionFactor";

	//teleported non teleported
	/**
	 * Modifies bike speed depending on whether bikes are
	 * teleported or network routed.
	 */
	public static void modifyBikeSpeed(Scenario scenario, double factor) {

		validatePositiveFactor(factor);
		Config config = scenario.getConfig();

		// teleported routing
		var teleportedParams = config.routing()
			.getModeRoutingParams()
			.get(TransportMode.bike);

		if (teleportedParams != null
			&& teleportedParams.getTeleportedModeSpeed() != null) {

			double oldSpeed = teleportedParams.getTeleportedModeSpeed();

			teleportedParams.setTeleportedModeSpeed(
				oldSpeed * factor
			);

			log.info(
				"Modified teleported bike speed by factor {}",
				factor
			);

			return;
		}

		// network routing
		VehicleType bike = scenario.getVehicles()
			.getVehicleTypes()
			.get(Id.create(TransportMode.bike, VehicleType.class));

		if (bike != null) {

			bike.setMaximumVelocity(
				bike.getMaximumVelocity() * factor
			);

			log.info(
				"Modified network bike speed by factor {}",
				factor
			);

			return;
		}

		throw new IllegalArgumentException(
			"Bike mode is configured neither as a teleported mode nor as a network vehicle type."
		);
	}

	/**
	 * Reduces car speed inside a shape area, except on links whose type matches
	 * one of the supplied exclusions.
	 */
	public static void reduceCarSpeed(
		Scenario scenario,
		Path shp,
		double factor,
		Set<String> typesToExclude
	) {

		validateReductionFactor(factor);
		PreparedGeometry area = loadPreparedArea(shp);

		scenario.getNetwork().getLinks().values().stream()
			.filter(link -> link.getAllowedModes().contains(TransportMode.car))
			.filter(link -> isInShape(link, area))
			.filter(link -> !hasExcludedType(link, typesToExclude))
			.forEach(link -> {
				link.setFreespeed(link.getFreespeed() * factor);
				link.getAttributes().putAttribute(SPEED_REDUCTION_FACTOR_ATTRIBUTE, factor);
			});

		log.info("Reduced speeds by factor {}", factor);
	}

	public static void prepareConfigForParkingCost(
		Config config
	) {

		ParkingCostConfigGroup group =
			ConfigUtils.addOrGetModule(
				config,
				ParkingCostConfigGroup.class
			);

		group.setFirstHourParkingCostLinkAttributeName(
			"firstHourParkingCost"
		);

		group.setExtraHourParkingCostLinkAttributeName(
			"extraHourParkingCost"
		);

		group.setResidentialParkingFeeAttributeName(
			"residentialParkingFee"
		);
	}

	public static void prepareControllerForParkingCost(
		Controler controler
	) {

		controler.addOverridingModule(new ParkingCostModule());
		controler.addOverridingModule(
			new PersonMoneyEventsAnalysisModule()
		);
	}

	public static void prepareScenarioForParkingCost(
		Scenario scenario,
		Path shp,
		double firstHourCost,
		double extraHourCost,
		double residentialParkingFee
	) {

		PreparedGeometry area = loadPreparedArea(shp);

		scenario.getNetwork().getLinks().values().stream()
			.filter(link ->
				link.getAllowedModes().contains(TransportMode.car)
			)
			.filter(link -> isInShape(link, area))
			.forEach(link -> {

				link.getAttributes().putAttribute(
					"firstHourParkingCost",
					firstHourCost
				);

				link.getAttributes().putAttribute(
					"extraHourParkingCost",
					extraHourCost
				);

				link.getAttributes().putAttribute(
					"residentialParkingFee",
					residentialParkingFee
				);
			});

		log.info(
			"Applied parking costs: firstHour={}, extraHour={}, residential={}",
			firstHourCost,
			extraHourCost,
			residentialParkingFee
		);
	}

	/**
	 * Reduces capacities of car links in the specified area, except on links whose
	 * type matches one of the supplied exclusions.
	 */
	public static void reduceCarCapacities(
		Scenario scenario,
		Path shp,
		double reductionFactor,
		Set<String> typesToExclude
	) {

		validateReductionFactor(reductionFactor);
		PreparedGeometry area = loadPreparedArea(shp);

		scenario.getNetwork().getLinks().values().stream()

			.filter(link -> link.getAllowedModes().contains(TransportMode.car))
			.filter(link -> isInShape(link, area))
			.filter(link -> !hasExcludedType(link, typesToExclude))

			.forEach(link -> {

				if (link.getCapacity() > 0) {
					link.setCapacity(link.getCapacity() * reductionFactor);
				}

				if (link.getNumberOfLanes() > 2.0) {
					link.setNumberOfLanes(Math.max(1, Math.round(link.getNumberOfLanes() * reductionFactor)));
				}

				link.getAttributes().putAttribute(CAPACITY_REDUCTION_FACTOR_ATTRIBUTE, reductionFactor);
			});

		log.info(
			"Reduced car capacities by factor {} in selected area",
			reductionFactor
		);
	}

	private static PreparedGeometry loadPreparedArea(Path shp) {
		var geometries = ShpGeometryUtils.loadGeometries(
			IOUtils.resolveFileOrResource(shp.toString())
		);
		if (geometries.isEmpty()) {
			throw new IllegalArgumentException("The shape file does not contain any geometries: " + shp);
		}
		return PreparedGeometryFactory.prepare(UnaryUnionOp.union(geometries));
	}

	private static boolean hasExcludedType(Link link, Set<String> typesToExclude) {
		if (typesToExclude == null || typesToExclude.isEmpty()) {
			return false;
		}
		Object type = link.getAttributes().getAttribute("type");
		return type instanceof String typeName
			&& typesToExclude.contains(typeName);
	}

	private static void validateReductionFactor(double factor) {
		if (!Double.isFinite(factor) || factor <= 0 || factor > 1) {
			throw new IllegalArgumentException("Reduction factor must be finite and in the range (0, 1].");
		}
	}

	private static void validatePositiveFactor(double factor) {
		if (!Double.isFinite(factor) || factor <= 0) {
			throw new IllegalArgumentException("Factor must be finite and greater than zero.");
		}
	}

	// Endpoint-based selection is intentional because MATSim links do not contain
	// high-resolution road geometries.
	private static boolean isInShape(Link link, PreparedGeometry area) {
		return area.contains(MGC.coord2Point(link.getFromNode().getCoord()))
			|| area.contains(MGC.coord2Point(link.getToNode().getCoord()));
	}

}

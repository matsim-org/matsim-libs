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
import org.matsim.api.core.v01.network.Link;

import java.nio.file.Path;
import java.util.List;

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

	//teleported non teleported
	/**
	 * Modifies bike speed depending on whether bikes are
	 * teleported or network routed.
	 */
	public static void modifyBikeSpeed(Scenario scenario, double factor) {

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
		}
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
			.filter(link -> isInShape(link, geometries))
			.filter(link -> !((String) link.getAttributes().getAttribute("type")).contains("motorway"))
			.forEach(link ->
				link.setFreespeed(link.getFreespeed() * factor)
			);

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

		List<PreparedGeometry> geometries =
			ShpGeometryUtils.loadPreparedGeometries(
				IOUtils.resolveFileOrResource(shp.toString())
			);

		scenario.getNetwork().getLinks().values().stream()
			.filter(link ->
				link.getAllowedModes().contains(TransportMode.car)
			)
			.filter(link -> isInShape(link, geometries))
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
	 * Reduce the capacities of car links in the specified area,
	 * excluding motorways and trunks.
	 */
	public static void reduceCarCapacities(
		Scenario scenario,
		List<PreparedGeometry> areaFilter,
		double reductionFactor
	) {

		if (areaFilter == null || areaFilter.isEmpty()) {
			throw new IllegalArgumentException("areaFilter must be provided and not empty.");
		}

		scenario.getNetwork().getLinks().values().stream()

			.filter(link -> link.getAllowedModes().contains(TransportMode.car))
			.filter(link -> isInShape(link, areaFilter))

			.filter(link -> {
				String typeObj = (String) link.getAttributes().getAttribute("type");
				return typeObj != null
					&& !typeObj.contains("motorway")
					&& !typeObj.contains("trunk");
			})

			.forEach(link -> {

				if (link.getCapacity() > 0) {
					link.setCapacity(link.getCapacity() * reductionFactor);
				}

				if (link.getNumberOfLanes() > 2.0) {
					link.setNumberOfLanes(link.getNumberOfLanes() * reductionFactor);
				}
			});

		log.info(
			"Reduced car capacities by factor {} in selected area",
			reductionFactor
		);
	}

	// Returns true if either endpoint of the link is in any of the given geometries.
	private static boolean isInShape(Link link, List<PreparedGeometry> geometries) {
	    return ShpGeometryUtils.isCoordInPreparedGeometries(
	        link.getFromNode().getCoord(),
	        geometries
	    ) ||
	    ShpGeometryUtils.isCoordInPreparedGeometries(
	        link.getToNode().getCoord(),
	        geometries
	    );
	}

}

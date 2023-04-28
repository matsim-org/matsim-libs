package ch.sbb.matsim.contrib.railsim.prototype.supply;

import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.MatsimVehicleWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Example run of RailsimSupplyBuilder
 * <p>
 * Create a small example with different lines: slow, express, internation and cargo
 *
 * @author Merlin Unterfinger
 */
public final class RunRailsimSupplyBuilder {

	public static void main(String[] args) {
		// Note! Overwrites test04 example
		final String outputDir = "contribs/railsim/test/input/ch/sbb/matsim/contrib/railsim/prototype/RunRailsimTest/test04/";
		final double waitingTime = 3 * 60.;
		createOutputDirectory(outputDir);
		// configure
		var config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("CH1903plus_LV95");
		var railsimConfigGroup = ConfigUtils.addOrGetModule(config, RailsimSupplyConfigGroup.class);
		railsimConfigGroup.setCircuitPlanningApproach(RailsimSupplyConfigGroup.CircuitPlanningApproach.DEFAULT);
		var scenario = ScenarioUtils.loadScenario(config);
		// setup supply builder
		var supply = new RailsimSupplyBuilder(scenario);

		// first add the stop information
		supply.addStop("lyon_part_dieu", 2399392., 1070947.);
		supply.addStop("geneve_la_praille", 2498624., 1115476.);
		supply.addStop("geneve", 2499965., 1119074.);
		supply.addStop("versoix", 2501905., 1126194.);
		supply.addStop("coppet", 2503642., 1130305.);
		supply.addStop("nyon", 2507480., 1137714.);
		supply.addStop("gland", 2510108., 1141628.);
		supply.addStop("rolle", 2515307., 1146292.);
		supply.addStop("allaman", 2520200., 1147698.);
		supply.addStop("morges", 2527534., 1151505.);
		supply.addStop("renens", 2534852., 1154073.);
		supply.addStop("lausanne", 2538101., 1151907.);
		supply.addStop("puidoux", 2548503., 1148595);
		supply.addStop("vevey", 2554287., 1145945.);
		supply.addStop("fribourg", 2581093., 1182309);
		supply.addStop("bern", 2598563, 1200033.);
		supply.addStop("bern_express", 2599563, 1200333.); // some minor shifts of the coordinates
		supply.addStop("bern_cargo", 2599563, 1199833.); // some minor shifts of the coordinates

		// add transit lines
		// slow line
		var slowLine = supply.addTransitLine("slow", "slow", "geneve", waitingTime);
		slowLine.addStop("versoix", 5 * 60., waitingTime);
		slowLine.addStop("coppet", 4 * 60., waitingTime);
		slowLine.addStop("nyon", 5 * 60., waitingTime);
		slowLine.addStop("gland", 3 * 60., waitingTime);
		slowLine.addStop("rolle", 4 * 60., waitingTime);
		slowLine.addStop("allaman", 3 * 60., waitingTime);
		slowLine.addStop("morges", 5 * 60., waitingTime);
		slowLine.addStop("renens", 6 * 60., waitingTime);
		slowLine.addStop("lausanne", 6 * 60., waitingTime);
		slowLine.addStop("puidoux", 10 * 60., waitingTime);
		slowLine.addStop("vevey", 5 * 60., waitingTime);
		// express line
		var expressLine = supply.addTransitLine("express", "express", "geneve", waitingTime);
		expressLine.addPass("versoix");
		expressLine.addPass("coppet");
		expressLine.addPass("nyon");
		expressLine.addPass("gland");
		expressLine.addPass("rolle");
		expressLine.addPass("allaman");
		expressLine.addPass("morges");
		expressLine.addPass("renens");
		expressLine.addStop("lausanne", 45 * 60., waitingTime);
		expressLine.addStop("fribourg", 41 * 60., waitingTime);
		expressLine.addPass("bern");
		expressLine.addStop("bern_express", 24 * 60., waitingTime);
		// international line
		var internationalLine = supply.addTransitLine("international", "international", "lyon_part_dieu", waitingTime);
		internationalLine.addStop("geneve", 128 * 60., waitingTime);
		internationalLine.addPass("versoix");
		internationalLine.addPass("coppet");
		internationalLine.addPass("nyon");
		internationalLine.addPass("gland");
		internationalLine.addPass("rolle");
		internationalLine.addPass("allaman");
		internationalLine.addPass("morges");
		internationalLine.addPass("renens");
		internationalLine.addStop("lausanne", 45 * 60., waitingTime);
		internationalLine.addStop("fribourg", 41 * 60., waitingTime);
		internationalLine.addPass("bern");
		internationalLine.addStop("bern_express", 24 * 60., waitingTime);
		// cargo line
		var cargoLine = supply.addTransitLine("cargo", "cargo", "geneve_la_praille", waitingTime);
		cargoLine.addPass("geneve");
		cargoLine.addPass("versoix");
		cargoLine.addPass("coppet");
		cargoLine.addPass("nyon");
		cargoLine.addPass("gland");
		cargoLine.addPass("rolle");
		cargoLine.addPass("allaman");
		cargoLine.addPass("morges");
		cargoLine.addPass("renens");
		cargoLine.addPass("lausanne");
		cargoLine.addPass("fribourg");
		cargoLine.addPass("bern");
		cargoLine.addStop("bern_cargo", 193 * 60., waitingTime);

		// add departures: slow line
		addDepartureTimes(slowLine, 0. * 3600., 3600., 900.);
		// add departures: express line
		addDepartureTimes(expressLine, 0.5 * 3600., 7200., 3600.);
		// add departures: international line
		addDepartureTimes(internationalLine, 0.25 * 3600., 3600 * 3., 7200.);
		// add departures: cargo line
		addDepartureTimes(cargoLine, 0.75 * 3600., 3600 * 3., 7200.);

		// build schedule
		supply.build();

		// export
		new NetworkWriter(scenario.getNetwork()).write(outputDir + "trainNetwork.xml.gz");
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(outputDir + "transitSchedule.xml.gz");
		new MatsimVehicleWriter(scenario.getTransitVehicles()).writeFile(outputDir + "transitVehicles.xml.gz");
	}

	private static void addDepartureTimes(TransitLineInfo slowLine, double initialDeparture, double nvzHeadway, double hvzHeadway) {
		for (double timeOfDay = initialDeparture; timeOfDay <= 24 * 3600.; ) {
			slowLine.addDeparture(RouteDirection.FORWARD, timeOfDay);
			slowLine.addDeparture(RouteDirection.REVERSE, timeOfDay);
			if (timeOfDay < 6 * 3600. || timeOfDay > 19 * 3600.) {
				timeOfDay = timeOfDay + nvzHeadway;
			} else {
				timeOfDay = timeOfDay + hvzHeadway;
			}
		}
	}

	private static void createOutputDirectory(String outputDir) {
		try {
			Files.createDirectories(Paths.get(outputDir));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}



package org.matsim.simwrapper.dashboard;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.simwrapper.*;
import org.matsim.testcases.MatsimTestUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class CommercialTrafficDashboardTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testCommercialViewer() {


		Config config = utils.createConfigWithTestInputFilePathAsContext();

		config.global().setCoordinateSystem(null);
		config.network().setInputFile("output_network.xml.gz");
		config.plans().setInputFile("output_plans.xml.gz");
		config.vehicles().setVehiclesFile("output_vehicles.xml.gz");
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);
		config.global().setCoordinateSystem(TransformationFactory.EPSG4326);
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("commercial_start").setTypicalDuration(30 * 60));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("home").setTypicalDuration(30 * 60));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("work").setTypicalDuration(8 * 3600));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("commercial_end").setTypicalDuration(30 * 60));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("service").setTypicalDuration(30 * 60));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("pickup").setTypicalDuration(30 * 60));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("delivery").setTypicalDuration(30 * 60));

		List<String> commercialPersonTraffic = List.of("commercialPersonTraffic", "commercialPersonTraffic_service", "goodsTraffic");
		commercialPersonTraffic.forEach(subpopulation -> {
			config.replanning().addStrategySettings(
				new ReplanningConfigGroup.StrategySettings().setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta).setWeight(
					0.85).setSubpopulation(subpopulation));

			config.replanning().addStrategySettings(
				new ReplanningConfigGroup.StrategySettings().setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute).setWeight(
					0.1).setSubpopulation(subpopulation));
		});
		Set<String> modes = Set.of("ride", "truck8t", "truck18t", "truck26t", "truck40t");

		modes.forEach(mode -> {
			ScoringConfigGroup.ModeParams thisModeParams = new ScoringConfigGroup.ModeParams(mode);
			config.scoring().addModeParams(thisModeParams);
		});

		Set<String> qsimModes = new HashSet<>(config.qsim().getMainModes());
		config.qsim().setMainModes(Sets.union(qsimModes, modes));

		Set<String> networkModes = new HashSet<>(config.routing().getNetworkModes());
		config.routing().setNetworkModes(Sets.union(networkModes, modes));
		config.routing().removeTeleportedModeParams("ride");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getNetwork().getLinks().forEach((linkId, link) -> {
			if (link.getAllowedModes().contains("car") && !link.getAllowedModes().contains("car2")) {
				Set<String> newModes = new HashSet<>(link.getAllowedModes());
				newModes.add("ride");
				link.setAllowedModes(newModes);
			}
		});
		PopulationFactory popFactory = PopulationUtils.getFactory();
		for (int i = 1; i < 8; i++) {
			//Id<Person> personId =
			Person person = popFactory.createPerson(Id.createPersonId("person_" + i));
			PopulationUtils.putSubpopulation(person, "person");
			Plan plan = PopulationUtils.createPlan(person);
			Link homelink = scenario.getNetwork().getLinks().get(Id.createLinkId("i(" + i + ",0)"));
			Link worklink = scenario.getNetwork().getLinks().get(Id.createLinkId("i(" + (9 - i) + ",0)"));
			person.getAttributes().putAttribute("home_x", homelink.getCoord().getX());
			person.getAttributes().putAttribute("home_y", homelink.getCoord().getY());
			String mode = i % 3 == 0 ? "ride" : "car";
			Activity act1 = PopulationUtils.createAndAddActivityFromCoord(plan, "home", homelink.getCoord());
			act1.setEndTime(i * 1.5 * 3600);
			if (i != 1) {
				PopulationUtils.createAndAddLeg(plan, mode);
				PopulationUtils.createAndAddActivityFromCoord(plan, "work", worklink.getCoord()).setMaximumDuration(i * 1.1 * 3600);
				PopulationUtils.createAndAddLeg(plan, mode);
				PopulationUtils.createAndAddActivityFromCoord(plan, "home", homelink.getCoord());
			}
			person.addPlan(plan);
			scenario.getPopulation().addPerson(person);
		}
		final Controler controler = new Controler(scenario);

		ShpOptions shpOptions = new ShpOptions(utils.getInputDirectory() + "shp/testRegions.shp", TransformationFactory.ATLANTIS, null);
		Geometry geometry = shpOptions.getGeometry().getCentroid();
		CoordinateTransformation ts = TransformationFactory.getCoordinateTransformation(TransformationFactory.ATLANTIS, TransformationFactory.WGS84);
		Coord coord = ts.transform(MGC.coordinate2Coord(geometry.getCoordinate()));
		String center = coord.getX() + "," + coord.getY();
		SimWrapper sw = SimWrapper.create(config);
		sw.getConfigGroup().defaultParams().setShp("shp/testRegions.shp");
		sw.getConfigGroup().setSampleSize(0.1);
		sw.getConfigGroup().defaultParams().setMapCenter(center);
		sw.getConfigGroup().defaultParams().setMapZoomLevel(10.);
		sw.getConfigGroup().setDefaultDashboards(SimWrapperConfigGroup.Mode.disabled);
		sw.addDashboard(
			new TripDashboard().setGroupsOfSubpopulationsForPersonAnalysis("personGroup=person").setGroupsOfSubpopulationsForCommercialAnalysis(
				"commercialPersonTrafficGroup=commercialPersonTraffic,commercialPersonTraffic_service;smallScaleGoodsTraffic=goodsTraffic"));
		sw.addDashboard(new CommercialTrafficDashboard(config.global().getCoordinateSystem()).setGroupsOfSubpopulationsForCommercialAnalysis(
			"commercialPersonTrafficGroup=commercialPersonTraffic,commercialPersonTraffic_service;smallScaleGoodsTraffic=goodsTraffic"));

		controler.addOverridingModule(new SimWrapperModule(sw));

		controler.run();
	}
}

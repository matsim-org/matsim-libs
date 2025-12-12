package org.matsim.simwrapper.dashboard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.simwrapper.TestScenario;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URL;
import java.nio.file.Path;
import java.util.*;

public class AccessibilityDashboardTest {


	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void kelheimDrt() {


		//CONFIG
		Config config = DrtTestScenario.loadConfig(utils);
		config.controller().setLastIteration(1);
		config.controller().setWritePlansInterval(1);
		config.controller().setWriteEventsInterval(1);

		config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.setInScenario);

		//simwrapper
		SimWrapperConfigGroup group = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
		group.setSampleSize(0.001);
		group.defaultParams().setMapCenter("11.891000, 48.911000");

		//drt
		//we have 2 operators ('av' + 'drt'), configure one of them to be areaBased (the other remains stopBased)
		MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		DrtConfigGroup drtConfigGroup = multiModeDrtConfigGroup.getModalElements().stream().filter(x -> x.getMode().equals(TransportMode.drt)).findFirst().get();
		config.removeModule(MultiModeDrtConfigGroup.GROUP_NAME);
		MultiModeDrtConfigGroup multiModeDrtConfigGroup2 = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		multiModeDrtConfigGroup2.addParameterSet(drtConfigGroup);
//
//		for (DrtConfigGroup drtCfg : multiModeDrtConfigGroup.getModalElements()) {
//			if (drtCfg.getMode().equals("av")){
//				drtCfg.operationalScheme = DrtConfigGroup.OperationalScheme.serviceAreaBased;
//				drtCfg.drtServiceAreaShapeFile = "drt-zones/drt-zonal-system.shp";
//			}
//		}

		//accessibility

		double mapCenterX = 712144.17;
		double mapCenterY = 5422153.87;

		double tileSize = 100;
		double num_rows = 45;

		final AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		acg.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromBoundingBox);
		acg.setBoundingBoxLeft(mapCenterX - num_rows * tileSize - tileSize / 2);
		acg.setBoundingBoxRight(mapCenterX + num_rows * tileSize + tileSize / 2);
		acg.setBoundingBoxBottom(mapCenterY - num_rows * tileSize - tileSize / 2);
		acg.setBoundingBoxTop(mapCenterY + num_rows * tileSize + tileSize / 2);
		acg.setTileSize_m((int) tileSize);


		List<Modes4Accessibility> accModes = List.of(Modes4Accessibility.freespeed, Modes4Accessibility.car, Modes4Accessibility.estimatedDrt);

		for(Modes4Accessibility mode : accModes) {
			acg.setComputingAccessibilityForMode(mode, true);
		}
		acg.setUseParallelization(false);

		// CONTROLLER
		SimWrapper sw = SimWrapper.create(config).addDashboard(new AccessibilityDashboard(config.global().getCoordinateSystem(), List.of("trainStation", "cityCenter"), accModes));

		Controler controler = MATSimApplication.prepare(new DrtTestScenario(config), config);

		ActivityFacilitiesFactory af = controler.getScenario().getActivityFacilities().getFactory();
		// train station
		double trainStationX = 715041.71;
		double trainStationY = 5420617.28;
		ActivityFacility fac1 = af.createActivityFacility(Id.create("xxx", ActivityFacility.class), new Coord(trainStationX, trainStationY));
		ActivityOption ao = af.createActivityOption("trainStation");
		fac1.addActivityOption(ao);
		controler.getScenario().getActivityFacilities().addActivityFacility(fac1);

		// innenstadt
		double cityCenterX = 711144.17;
		double cityCenterY = 5422153.87;
		ActivityFacility fac2 = af.createActivityFacility(Id.create("yyy", ActivityFacility.class), new Coord(cityCenterX, cityCenterY));
		ActivityOption ao2 = af.createActivityOption("cityCenter");
		fac2.addActivityOption(ao2);
		controler.getScenario().getActivityFacilities().addActivityFacility(fac2);

		controler.addOverridingModule(new SimWrapperModule(sw));
		controler.run();

	}


}

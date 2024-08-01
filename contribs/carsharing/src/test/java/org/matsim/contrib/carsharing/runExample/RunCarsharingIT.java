/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.carsharing.runExample;

import static org.matsim.core.config.groups.RoutingConfigGroup.AccessEgressType;
import static org.matsim.core.config.groups.RoutingConfigGroup.TeleportedModeParams;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.analysis.LegHistogram;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.FreeFloatingConfigGroup;
import org.matsim.contrib.carsharing.config.OneWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.TwoWayCarsharingConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author nagel
 */
public class RunCarsharingIT {

	private final static Logger log = LogManager.getLogger(RunCarsharingIT.class);

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	final void test() {
		Config config = ConfigUtils.loadConfig(utils.getClassInputDirectory() + "/config.xml",
				new FreeFloatingConfigGroup(),
				new OneWayCarsharingConfigGroup(),
				new TwoWayCarsharingConfigGroup(),
				new CarsharingConfigGroup(),
				new DvrpConfigGroup());

		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

		config.network().setInputFile("network.xml");

		config.plans().setInputFile("10persons.xml");
		//		config.plans().setInputPersonAttributeFile("1000desiresAttributes.xml");
		//		config.plans().setInputPersonAttributeFile( null );
		// (Not needed.  I removed this from config.xml, so setting to null is not needed here.. kai, jun'19)

		config.facilities().setInputFile("facilities.xml");
		config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.fromFile);

		config.routing().setAccessEgressType(AccessEgressType.none); // otherwise does not work. kai,feb'16
		config.routing().setRoutingRandomness(0.);
		//		config.plansCalcRoute().setInsertingAccessEgressWalk(AccessEgressType.directWalk);

		CarsharingConfigGroup csConfig = (CarsharingConfigGroup) config.getModule(CarsharingConfigGroup.GROUP_NAME);
		csConfig.setvehiclelocations(utils.getClassInputDirectory() + "/CarsharingStations.xml");
		csConfig.setmembership(utils.getClassInputDirectory() + "/CSMembership.xml");
		//OneWayCarsharingConfigGroup oneWayConfig = (OneWayCarsharingConfigGroup) config.getModule( OneWayCarsharingConfigGroup.GROUP_NAME ) ;
		//oneWayConfig.setvehiclelocations( utils.getClassInputDirectory()+"/Stations.txt");

		//TwoWayCarsharingConfigGroup twoWayConfig = (TwoWayCarsharingConfigGroup) config.getModule( TwoWayCarsharingConfigGroup.GROUP_NAME ) ;
		//twoWayConfig.setvehiclelocations( utils.getClassInputDirectory()+"/CarsharingStations.xml");

		config.subtourModeChoice().setBehavior(SubtourModeChoice.Behavior.fromAllModesToSpecifiedModes);
		config.subtourModeChoice().setProbaForRandomSingleTripMode(0.);

		//		config.global().setNumberOfThreads(1);
		//		config.qsim().setNumberOfThreads(1);

		{
			TeleportedModeParams params = new TeleportedModeParams(TransportMode.non_network_walk);
			params.setTeleportedModeSpeed(0.83333333333);
			//			params.setTeleportedModeSpeed( 2.0 );
			params.setBeelineDistanceFactor(1.3);
			config.routing().addModeRoutingParams(params);
		}
		{
			config.routing().removeModeRoutingParams(TransportMode.walk);
			TeleportedModeParams params = new TeleportedModeParams(TransportMode.walk);
			params.setTeleportedModeSpeed(0.83333333333);
			//			params.setTeleportedModeSpeed( 2.0 );
			params.setBeelineDistanceFactor(1.3);
			config.routing().addModeRoutingParams(params);
		}

		// ---

		Scenario scenario = ScenarioUtils.loadScenario(config);
		config.routing().setAccessEgressType(AccessEgressType.accessEgressModeToLink);

		// ---

		final Controler controler = new Controler(scenario);
		//		controler.setDirtyShutdown(true);

		RunCarsharing.installCarSharing(controler);

		final MyAnalysis myAnalysis = new MyAnalysis();
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.bind(MyAnalysis.class).toInstance(myAnalysis);
				this.addControlerListenerBinding().toInstance(myAnalysis);
			}
		});

		// ---

		controler.run();

		log.info("done");
	}

	static class MyAnalysis implements AfterMobsimListener {

		@Inject private LegHistogram histogram;

		void testOutput(int iteration) {
			int nofLegs = 0;
			for (int nofDepartures : this.histogram.getDepartures()) {
				nofLegs += nofDepartures;
			}
			log.info("number of legs:\t" + nofLegs + "\t100%");

			for (String legMode : this.histogram.getLegModes()) {
				int nOfModeLegs = 0;
				for (int nofDepartures : this.histogram.getDepartures(legMode)) {
					nOfModeLegs += nofDepartures;
				}
				if (iteration == 0) {
					if (TransportMode.walk.equals(legMode)) {
						// walk is used for access+egress to car
						// -> number of walk legs for access+egress equals twice the number of car legs = 44
						Assertions.assertEquals(44, nOfModeLegs);
					} else if ("oneway_vehicle".equals(legMode)) {
						Assertions.assertEquals(0, nOfModeLegs);
					} else if (TransportMode.car.equals(legMode)) {
						Assertions.assertEquals(22, nOfModeLegs);
					} else if ("egress_walk_ow".equals(legMode)) {
						Assertions.assertEquals(0, nOfModeLegs);
					} else if ("access_walk_ow".equals(legMode)) {
						Assertions.assertEquals(0, nOfModeLegs);
					}
				} else if (iteration == 10) {

					if (TransportMode.walk.equals(legMode)) {

						Assertions.assertEquals(2, nOfModeLegs);
					} else if ("bike".equals(legMode)) {
						Assertions.assertEquals(2, nOfModeLegs);
					} else if (TransportMode.car.equals(legMode)) {
						Assertions.assertEquals(0, nOfModeLegs);
					} else if ("twoway_vehicle".equals(legMode)) {

						Assertions.assertEquals(6, nOfModeLegs);

					} else if ("oneway_vehicle".equals(legMode)) {
						Assertions.assertEquals(0, nOfModeLegs);

					} else if ("egress_walk_ow".equals(legMode)) {
						Assertions.assertEquals(0, nOfModeLegs);
					} else if ("access_walk_ow".equals(legMode)) {
						Assertions.assertEquals(0, nOfModeLegs);
					} else if ("egress_walk_tw".equals(legMode)) {
						Assertions.assertEquals(3, nOfModeLegs);
					} else if ("access_walk_tw".equals(legMode)) {
						Assertions.assertEquals(3, nOfModeLegs);
					} else if ("egress_walk_ff".equals(legMode)) {
						Assertions.assertEquals(2, nOfModeLegs);
					} else if ("access_walk_ff".equals(legMode)) {
						Assertions.assertEquals(0, nOfModeLegs);
					}
				} else if (iteration == 20) {
					if (TransportMode.walk.equals(legMode)) {
						Assertions.assertEquals(5, nOfModeLegs);
					} else if ("bike".equals(legMode)) {
						Assertions.assertEquals(0, nOfModeLegs);
					} else if ("twoway_vehicle".equals(legMode)) {
						Assertions.assertEquals(6, nOfModeLegs);
					} else if ("freefloating_vehicle".equals(legMode)) {
						Assertions.assertEquals(0, nOfModeLegs);
					} else if ("egress_walk_tw".equals(legMode)) {
						Assertions.assertEquals(3, nOfModeLegs);
					} else if ("access_walk_tw".equals(legMode)) {
						Assertions.assertEquals(3, nOfModeLegs);
					} else if ("access_walk_ff".equals(legMode)) {
						Assertions.assertEquals(0, nOfModeLegs);

					} else if ("egress_walk_ff".equals(legMode)) {
						Assertions.assertEquals(0, nOfModeLegs);

					}
				}
			}

		}

		@Override
		public void notifyAfterMobsim(AfterMobsimEvent event) {
			testOutput(event.getIteration());
		}

	}
}


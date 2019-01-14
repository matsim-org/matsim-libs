/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 *
 */
package org.matsim.contrib.drt.run.examples;

import java.util.Collections;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author jbischoff
 */
public class RunDrtExampleIT {
	
	private class PersonIdValidator implements PassengerRequestValidator {
		private boolean validateRequestWasCalled = false;
		
		@Override
		public Set<String> validateRequest(PassengerRequest request) {
			validateRequestWasCalled = true;
			return request.getPassenger().getId().toString().equalsIgnoreCase("12052000_12052000_100") ?
					Collections.singleton("REJECT_12052000_12052000_100") :
					Collections.emptySet();
		}

		boolean isValidateRequestWasCalled() {
			return validateRequestWasCalled;
		}

	}

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testRunDrtExample() {
		String configFile = "./src/main/resources/drt_example/drtconfig_door2door.xml";
		Config config = ConfigUtils.loadConfig(configFile, new DrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());
		config.plans().setInputFile("cb-drtplans_test.xml.gz");

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);
	}

	@Test
	public void testRunDrtExampleWithCustomDrtRequestValidator() {
		String configFile = "./src/main/resources/drt_example/drtconfig_door2door.xml";
		Config config = ConfigUtils.loadConfig(configFile, new DrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());
		config.plans().setInputFile("cb-drtplans_test.xml.gz");

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		Controler controler = DrtControlerCreator.createControler(config, false);

		PersonIdValidator personIdValidator = new PersonIdValidator();

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.bind(PassengerRequestValidator.class).annotatedWith(DvrpModes.mode(TransportMode.drt)).toInstance(personIdValidator);
			}
		});
		controler.run();
		
		Assert.assertEquals("passenger request validator was not called", true, personIdValidator.isValidateRequestWasCalled());
	}

	@Test
	public void testRunDrtStopbasedExample() {
		String configFile = "./src/main/resources/drt_example/drtconfig_stopbased.xml";
		Config config = ConfigUtils.loadConfig(configFile, new DrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());
		config.plans().setInputFile("cb-drtplans_test.xml.gz");

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);
	}
}

/* *********************************************************************** *
 * project: org.matsim.*
 * CliquesXmlReaderTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.cliquessim.population;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.cliquessim.config.CliquesConfigGroup;
import playground.thibautd.cliquessim.utils.JointControlerUtils;


/**
 * @author thibautd
 */
public class CliquesXmlReaderTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testPartialCliqueCovering() {
		Id missingAgent = new IdImpl( 7 );
		Config config = utils.loadConfig( null );
		config.addModule( CliquesConfigGroup.GROUP_NAME , new CliquesConfigGroup() );
		ConfigUtils.loadConfig( config , utils.getInputDirectory() + "config-no-agent-7.xml" );
		config.controler().setOutputDirectory( utils.getOutputDirectory() );
		Scenario scen = JointControlerUtils.createScenario( config );

		Assert.assertFalse(
				"agent not in any clique still in the population",
				scen.getPopulation().getPersons().keySet().contains( missingAgent ) );
	}
}


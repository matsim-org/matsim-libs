/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.evacuation.base;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.Module;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.evacuation.config.EvacuationConfigGroup;
import org.matsim.testcases.MatsimTestCase;

public class EvacuationPlansGeneratorAndNetworkTrimmerTest extends MatsimTestCase {

	public void testEvacuationPlansGeneratorAndNetworkTrimmer() {
		String config = getInputDirectory() + "config.xml";
		Config c = super.loadConfig(config);
		Module m = c.getModule("evacuation");
		EvacuationConfigGroup ec = new EvacuationConfigGroup(m);
		c.getModules().put("evacuation", ec);

		String refPlans = getInputDirectory() + "evacuationplans.xml";
		String refNet = getInputDirectory() + "evacuationnetwork.xml";

		ScenarioImpl scenario = new ScenarioImpl(c);
		NetworkImpl net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(c.network().getInputFile());

		Population pop = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(c.plans().getInputFile());

		new EvacuationNetGenerator(net, c).run();

		new EvacuationPlansGenerator(pop, net, net.getLinks().get(new IdImpl("el1"))).run();

		new PopulationWriter(pop, net).write(getOutputDirectory() + "plans.xml");
		assertEquals("different plans-files.", CRCChecksum.getCRCFromFile(refPlans), CRCChecksum.getCRCFromFile(getOutputDirectory() + "plans.xml"));

		new NetworkWriter(net).write(getOutputDirectory() + "network.xml");
		assertEquals("different network-files.", CRCChecksum.getCRCFromFile(refNet), CRCChecksum.getCRCFromFile(getOutputDirectory() + "network.xml"));
	}
}

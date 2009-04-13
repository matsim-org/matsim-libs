/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationReaderMatsimV4Test.java
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

package org.matsim.core.population;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.api.population.Route;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.testcases.MatsimTestCase;
import org.xml.sax.SAXException;

public class PopulationReaderMatsimV4Test extends MatsimTestCase {

	/**
	 * @author mrieser
	 *
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 */
	public void testReadRoute() throws SAXException, ParserConfigurationException, IOException {
		final NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).parse("test/scenarios/equil/network.xml");
		Population population = new PopulationImpl();
		PopulationReaderMatsimV4 reader = new PopulationReaderMatsimV4(population, network);
		reader.parse(getInputDirectory() + "plans2.xml");

		assertEquals("population size.", 2, population.getPersons().size());
		Person person1 = population.getPersons().get(new IdImpl("1"));
		Plan plan1 = person1.getPlans().get(0);
		Leg leg1a = (Leg) plan1.getPlanElements().get(1);
		Route route1a = leg1a.getRoute();
		assertEquals("different startLink for first leg.", network.getLink(new IdImpl("1")), route1a.getStartLink());
		assertEquals("different endLink for first leg.", network.getLink(new IdImpl("20")), route1a.getEndLink());
		Leg leg1b = (Leg) plan1.getPlanElements().get(3);
		Route route1b = leg1b.getRoute();
		assertEquals("different startLink for second leg.", network.getLink(new IdImpl("20")), route1b.getStartLink());
		assertEquals("different endLink for second leg.", network.getLink(new IdImpl("20")), route1b.getEndLink());
		Leg leg1c = (Leg) plan1.getPlanElements().get(5);
		Route route1c = leg1c.getRoute();
		assertEquals("different startLink for third leg.", network.getLink(new IdImpl("20")), route1c.getStartLink());
		assertEquals("different endLink for third leg.", network.getLink(new IdImpl("1")), route1c.getEndLink());

		Person person2 = population.getPersons().get(new IdImpl("2"));
		Plan plan2 = person2.getPlans().get(0);
		Leg leg2a = (Leg) plan2.getPlanElements().get(1);
		Route route2a = leg2a.getRoute();
		assertEquals("different startLink for first leg.", network.getLink(new IdImpl("2")), route2a.getStartLink());
		assertEquals("different endLink for first leg.", network.getLink(new IdImpl("20")), route2a.getEndLink());
		Leg leg2b = (Leg) plan2.getPlanElements().get(3);
		Route route2b = leg2b.getRoute();
		assertEquals("different startLink for third leg.", network.getLink(new IdImpl("20")), route2b.getStartLink());
		assertEquals("different endLink for third leg.", network.getLink(new IdImpl("1")), route2b.getEndLink());
	}

}

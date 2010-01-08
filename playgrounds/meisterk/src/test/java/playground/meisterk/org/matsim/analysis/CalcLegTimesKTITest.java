/* *********************************************************************** *
 * project: org.matsim.*
 * CalcLegTimesKTITest.java
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

package playground.meisterk.org.matsim.analysis;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;

import playground.meisterk.org.matsim.population.algorithms.AbstractClassifiedFrequencyAnalysis.CrosstabFormat;

public class CalcLegTimesKTITest extends MatsimTestCase {

	public static final double[] timeBins = new double[]{
		0.0 * 60.0, 
		5.0 * 60.0, 
		10.0 * 60.0, 
		15.0 * 60.0, 
		20.0 * 60.0, 
		25.0 * 60.0, 
		30.0 * 60.0, 
		60.0 * 60.0, 
		120.0 * 60.0, 
		240.0 * 60.0, 
		480.0 * 60.0, 
		960.0 * 60.0, 
	};

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testGenerateDistribution() {
		
		PopulationImpl pop = new PopulationImpl();
		PersonImpl testPerson = new PersonImpl(new IdImpl("1"));
		pop.addPerson(testPerson);
		Id personId = testPerson.getId();
		
		PrintStream out = null;
		try {
			out = new PrintStream(this.getOutputDirectory() + "actualOutput.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		CalcLegTimesKTI testee = new CalcLegTimesKTI(pop, out);
		
		EventsManagerImpl events = new EventsManagerImpl();
		events.addHandler(testee);

		NetworkLayer testNetwork = new NetworkLayer();
		Node node1 = testNetwork.createAndAddNode(new IdImpl("1"), new CoordImpl(0.0, 0.0));
		Node node2 = testNetwork.createAndAddNode(new IdImpl("2"), new CoordImpl(100.0, 100.0));
		Link link = testNetwork.createAndAddLink(new IdImpl("200"), node1, node2, 0, 0, 0, 0);
		LegImpl leg = new LegImpl(TransportMode.car);
		Id linkId = link.getId();
		
		events.processEvent(new AgentDepartureEventImpl(Time.parseTime("06:00:00"), personId, linkId, leg.getMode()));
		events.processEvent(new AgentArrivalEventImpl(Time.parseTime("06:30:00"), personId, linkId, leg.getMode()));
		
		assertEquals(1, testee.getNumberOfModes());
		assertEquals(1, testee.getNumberOfLegs(TransportMode.car, timeBins[5], timeBins[6]));
		assertEquals(1, testee.getNumberOfLegs(TransportMode.car, timeBins[6], timeBins[5]));

		leg.setMode(TransportMode.pt);
		events.processEvent(new AgentDepartureEventImpl(Time.parseTime("06:00:00"), personId, linkId, leg.getMode()));
		events.processEvent(new AgentArrivalEventImpl(Time.parseTime("06:00:01"), personId, linkId, leg.getMode()));

		assertEquals(2, testee.getNumberOfModes());
		assertEquals(1, testee.getNumberOfLegs(TransportMode.pt, timeBins[0], timeBins[1]));
		assertEquals(1, testee.getNumberOfLegs(TransportMode.pt, timeBins[1], timeBins[0]));

		leg.setMode(TransportMode.car);
		events.processEvent(new AgentDepartureEventImpl(Time.parseTime("06:00:00"), personId, linkId, leg.getMode()));
		events.processEvent(new AgentArrivalEventImpl(Time.parseTime("06:00:00"), personId, linkId, leg.getMode()));

		assertEquals(3, testee.getNumberOfLegs());
		assertEquals(2, testee.getNumberOfLegs(TransportMode.car));
		assertEquals(1, testee.getNumberOfLegs(TransportMode.pt));
		
		assertEquals(1, testee.getNumberOfLegs(-1000.0, timeBins[0]));
		assertEquals(0, testee.getNumberOfLegs(timeBins[5], timeBins[4]));
		assertEquals(1, testee.getNumberOfLegs(timeBins[5], timeBins[6]));
		assertEquals(1, testee.getNumberOfLegs(timeBins[0], timeBins[1]));
		
		for (boolean isCumulative : new boolean[]{false, true}) {
			for (CrosstabFormat crosstabFormat : CrosstabFormat.values()) {
				testee.printClasses(crosstabFormat, isCumulative, timeBins, out);
			}
		}

		testee.printDeciles(true, out);

		testee.printQuantiles(true, 12, out);

		out.close();

		final long expectedChecksum = CRCChecksum.getCRCFromFile(this.getInputDirectory() + "expectedOutput.txt");
		final long actualChecksum = CRCChecksum.getCRCFromFile(this.getOutputDirectory() + "actualOutput.txt");
		assertEquals(expectedChecksum, actualChecksum);


	}
	
}

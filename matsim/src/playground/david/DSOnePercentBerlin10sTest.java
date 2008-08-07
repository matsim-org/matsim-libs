/* *********************************************************************** *
 * project: org.matsim.*
 * DSOnePercentBerlin10sTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.david;

import org.matsim.events.EventAgentStuck;
import org.matsim.events.Events;
import org.matsim.events.algorithms.EventWriterTXT;
import org.matsim.events.algorithms.GenerateRealPlans;
import org.matsim.events.handler.EventHandlerAgentStuckI;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.QueueSimulation;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPlansReader;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.population.algorithms.PlanAverageScore;
import org.matsim.world.World;

class StuckAndAbortCounter implements EventHandlerAgentStuckI
{

	public int stuckvehs = 0;
	public void handleEvent(final EventAgentStuck event) {
		this.stuckvehs++;
	}

	public void reset(final int iteration) {
		this.stuckvehs = 0;
	}

}
public class DSOnePercentBerlin10sTest {

	public String configfile;

	public DSOnePercentBerlin10sTest() {
		this.configfile = "../MatsimJ-Testing/testdata/config.xml";
	}

	public void testOnePercent10s() {
		String netFileName = "../MatsimJ-Testing/testdata/studies/WIP/wip_net.xml";
		String popFileName = "../MatsimJ-Testing/testdata/studies/WIP/kutter001car5.debug.router_wip.plans.xml.gz";

		Gbl.random.setSeed(7411L);

		World world = Gbl.getWorld();
		// this needs to be done before reading the network
		// because QueueLinks timeCap dependents on SIM_TICK_TIME_S
		Gbl.getConfig().simulation().setTimeStepSize(10.0);
		Gbl.getConfig().simulation().setFlowCapFactor(0.01);
		Gbl.getConfig().simulation().setStorageCapFactor(0.04);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFileName);
		world.setNetworkLayer(network);
		QueueNetworkLayer qnet = new QueueNetworkLayer(network);
		double sum = 0.0;

		for (QueueLink link : qnet.getLinks().values()) {
			sum+= link.getSpaceCap()*network.getEffectiveCellSize();
		}
		System.out.println("Overall network length = " + sum);

		Population population = new Population() ;
		// Read plans file with special dtd version
		PopulationReader plansReader = new MatsimPlansReader(population);
		plansReader.readFile(popFileName);

		Events events = new Events();
		EventWriterTXT writer = new EventWriterTXT("../MatsimJ-Testing/testdata/tmp/DSberlin1PercentEvents.txt");
		events.addHandler(writer);

		StuckAndAbortCounter counter = new StuckAndAbortCounter();
		events.addHandler(counter);

		GenerateRealPlans realPlansGenerator = new GenerateRealPlans(population);
		events.addHandler(realPlansGenerator);

		QueueSimulation sim = new QueueSimulation(network, population, events);
		sim.run();

		writer.closefile();

		System.out.println("Aborted Veh # " + counter.stuckvehs);
		//
		// score plans and calc average
		//

		PlanAverageScore average = new PlanAverageScore();
		Population realPop = realPlansGenerator.getPlans();

		// does not compile anymore??
		//realPop.addAlgorithm(new PlanCalcScore());
		//realPop.addAlgorithm(new UpdateScores(population));
		average.run(realPop);

		System.out.println("S C O R I N G" +  "] the average score is: " + average.getAverage());


	}
	public void setUp() {

		Gbl.startMeasurement();

		String [] args = {this.configfile};
		if (this.configfile != null) Gbl.createConfig(args);
		else Gbl.createConfig(new String[0]);
	}

	public static void main(final String[] args) {
		DSOnePercentBerlin10sTest test = new DSOnePercentBerlin10sTest();
		try {
			test.setUp();
		} catch (Exception e) {
			e.printStackTrace();
		}
		test.testOnePercent10s();

	}
}

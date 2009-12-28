/* *********************************************************************** *
 * project: org.matsim.*
 * MyControler1.java
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

package playground.andreas.itsumo;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.population.routes.NodeNetworkRouteImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.run.Events2Snapshot;
import org.matsim.vis.netvis.NetVis;


public class MyControler1 extends Controler {

	private static final Logger log = Logger.getLogger(MyControler1.class);

	public MyControler1(final Config config) {
		super(config);
	}

	protected int[] generateDistribution(final LinkImpl[] netLinks, final int popSize, final LinkImpl[] givenLinks, final double[] probs) {

		int[] quant = new int[netLinks.length];
		boolean[] aux = new boolean[netLinks.length];
		int rest = popSize;

		// put the desired number of agents on the informed links and zero the rest
		for (int i=0; i<netLinks.length; i++) {
			aux[i] = false;
			for (int j=0; j<givenLinks.length; j++) {
				if (netLinks[i].equals(givenLinks[j])) {
					quant[i] = (int) Math.floor(popSize * probs[j]);
					rest -= quant[i];
					aux[i] = true;
					break;
				}
			}
			if (!aux[i])
				quant[i] = 0;
		}

		// equally distribute the rest of agents on the remaining links
		int others = rest / (netLinks.length - givenLinks.length);
		for (int i=0; i<netLinks.length; i++) {
			if (!aux[i]) {
				quant[i] = others;
				rest -= quant[i];
			}
		}

		// handle rouding difference
		while (rest > 0) {
			for (int i=0; i<netLinks.length; i++) {
				if (!aux[i]) {
					quant[i]++;
					rest--;
					aux[i] = true;
					break;
				}
			}
		}

		return quant;
	}

	@Override
	protected void runMobSim() {
		ItsumoSim sim = new ItsumoSim(this.population, this.events);
		sim.setControlerIO(this.getControlerIO());
		sim.setIteration(this.getIteration());
		sim.run();
	}

	@Override
	protected Population loadPopulation() {


		PopulationImpl population = new PopulationImpl();

		log.info("  generating plans... ");


		LinkImpl link9 = this.network.getLink("9");
		LinkImpl link15 = this.network.getLink("15");
		for (int i=0; i<100; i++) {
			PersonImpl p = new PersonImpl(new IdImpl(i+1));

			try {
				PlanImpl plan1 = new org.matsim.core.population.PlanImpl(p);
				ActivityImpl act1a = plan1.createAndAddActivity("h", new CoordImpl(100., 100.));
				act1a.setLink(link9);
				act1a.setEndTime(0*60*60.);
				LegImpl leg = plan1.createAndAddLeg(TransportMode.car);
				NetworkRouteWRefs route = new NodeNetworkRouteImpl(link9, link15);
				route.setNodes(link9, NetworkUtils.getNodes(this.network, "3 4"), link15);
				leg.setRoute(route);
				ActivityImpl act1b = plan1.createAndAddActivity("h", new CoordImpl(200., 200.));
				act1b.setLink(link15);
				act1b.setStartTime(8*60*60);
				p.addPlan(plan1);

				PlanImpl plan2 = new org.matsim.core.population.PlanImpl(p);
				ActivityImpl act2a = plan1.createAndAddActivity("h", new CoordImpl(100., 100.));
				act2a.setLink(link9);
				act2a.setEndTime(0*60*60.);
				LegImpl leg2 = plan2.createAndAddLeg(TransportMode.car);
				NetworkRouteWRefs route2 = new NodeNetworkRouteImpl(link9, link15);
				route2.setNodes(link9, NetworkUtils.getNodes(this.network, "3 6 4"), link15);
				leg2.setRoute(route2);
				ActivityImpl act2b = plan1.createAndAddActivity("h", new CoordImpl(200., 200.));
				act2b.setLink(link15);
				act2b.setStartTime(8*60*60);
				p.addPlan(plan2);

				population.addPerson(p);
			} catch (Exception e1) {
				e1.printStackTrace();
			}

			// generatePerson(i+1, network.getLink("9"), network.getLink("15"), population);
		}



		/*
		Link[] netLinks = new Link[network.getLinks().size()];
		int n=0;
		for (Iterator iter = network.getLinks().iterator(); iter.hasNext();) {
			netLinks[n++] = (Link) iter.next();
		}

		// maybe 400, 500, 600, 700
		int popSize = 500;

		Link[] source = {network.getLink("181"), network.getLink("159"), network.getLink("75")};
		double[] probSource = {0.05, 0.04, 0.03};

		Link[] dest = {network.getLink("207")};
		double[] probDest = {0.6};

		int[] quantSource = generateDistribution(netLinks, popSize, source, probSource);
		int[] quantDest = generateDistribution(netLinks, popSize, dest, probDest);

		Link[] agentsSource = new Link[popSize];
		Link[] agentsDest = new Link[popSize];




		for (int i=0; i<popSize; i++) {
			for (int j=0; j<network.getLinks().size(); j++) {
				if (quantSource[j] > 0) {
					agentsSource[i] = netLinks[j];
					quantSource[j]--;
					break;
				}
			}

			boolean safe = false;
			for (int j=0; j<network.getLinks().size(); j++) {
				if (quantDest[j] > 0 && !netLinks[j].equals(agentsSource[i])) {
					agentsDest[i] = netLinks[j];
					quantDest[j]--;
					safe = true;
					break;
				}
			}

			if (!safe) {
				for (int j=0; j<network.getLinks().size(); j++) {
					if (quantDest[j] > 0) {
						agentsDest[i] = netLinks[j];
						quantDest[j]--;
						break;
					}
				}
			}
		}


		Plans population = new Plans(Plans.NO_STREAMING);

		log.info("  generating plans... ");

		for (int i=0; i<popSize; i++) {
			generatePerson(i+1, agentsSource[i], agentsDest[i], population);
		}
		*/


		/* ANDREAS CODE

		// Plans generation for the sesam scenario

		int ii = 1;
		Link destLink = network.getLink("207");
		Link sourceLink = null;

		// Put 6 Agents at every link except the destLink
		for (Iterator iter = network.getLinks().iterator(); iter.hasNext();) {
			sourceLink = (Link) iter.next();

			if (!sourceLink.equals(destLink)){
				for (int jj = 1; jj <= 6; jj++) {
					generatePerson(ii, sourceLink, destLink, population);
					ii++;
				}
			}

		}

		// Put another 24 agents at link 181 (C2B2) > 30 agents on the link now
		sourceLink = network.getLink("181");
		for (int jj = 1; jj <= 24; jj++) {
			generatePerson(ii, sourceLink, destLink, population);
			ii++;
		}

		// Put another 19 agents at link 159 (E1D1) > 25 agents on the link now
		sourceLink = network.getLink("159");
		for (int jj = 1; jj <= 19; jj++) {
			generatePerson(ii, sourceLink, destLink, population);
			ii++;
		}

		// Put another 14 agents at link 75 (B5B4) > 20 agents on the link now
		sourceLink = network.getLink("75");
		for (int jj = 1; jj <= 14; jj++) {
			generatePerson(ii, sourceLink, destLink, population);
			ii++;
		}

		ANDREAS CODE */

		log.info("  done");

		return population;
	}

	//	/*	Comment it, if you want to read a native MATSim network
	@Override
	protected NetworkLayer loadNetwork() {

		log.info("  creating network layer... ");
		NetworkLayer network = (NetworkLayer) super.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		log.info("  done");

		log.info("  reading network xml file... ");
		ITSUMONetworkReader reader = new ITSUMONetworkReader(network);
		reader.read(Gbl.getConfig().getParam(ItsumoSim.CONFIG_MODULE, "itsumoInputNetworkFile"));
//		NetworkParser network_parser = new NetworkParser(network);
//		network_parser.parse();

		NetworkWriter network_writer = new NetworkWriter(network);
		network_writer.writeFile(config.network().getOutputFile());
		log.info("  done");

		return network;
	}
	//	*/

	/**
	 * Conversion of events -> snapshots
	 *
	 */
	protected void makeVis(){

		File driversLog = new File("./drivers.txt");
		File visDir = new File("./output/vis");
		File eventsFile = new File("./output/vis/events.txt");

		if (driversLog.exists()){
			visDir.mkdir();
			driversLog.renameTo(eventsFile);

			Events2Snapshot events2Snapshot = new org.matsim.run.Events2Snapshot();
			events2Snapshot.run(eventsFile, Gbl.getConfig(), this.network);

			// Run NetVis if possible
			if (Gbl.getConfig().getParam("simulation", "snapshotFormat").equalsIgnoreCase("netvis")){
				String[] visargs = {"./output/vis/Snapshot"};
				NetVis.main(visargs);
			}

		} else {
			System.err.println("Couldn't find " + driversLog);
			System.exit(0);
		}
	}

	private void generatePerson(final int ii, final LinkImpl sourceLink, final LinkImpl destLink, final PopulationImpl population){
		PersonImpl p = new PersonImpl(new IdImpl(ii));
		PlanImpl plan = new org.matsim.core.population.PlanImpl(p);
		try {
			ActivityImpl act1 = plan.createAndAddActivity("h", new CoordImpl(100., 100.));
			act1.setLink(sourceLink);
			act1.setStartTime(0.);
			act1.setEndTime(0 * 60 * 60.);

			plan.createAndAddLeg(TransportMode.car);
			ActivityImpl act2 = plan.createAndAddActivity("h", new CoordImpl(200., 200.));
			act2.setLink(destLink);
			act2.setStartTime(8 * 60 * 60);

			p.addPlan(plan);
			population.addPerson(p);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	public static void main(final String[] args) {

		if ( args.length==0 ) {
			Gbl.createConfig(new String[] {"./examples/itsumo-sesam-scenario/config.xml"});
		} else {
			Gbl.createConfig(args) ;
		}

		final MyControler1 controler = new MyControler1(Gbl.getConfig());
		controler.setOverwriteFiles(true);
		controler.run();

		controler.makeVis();
	}

}

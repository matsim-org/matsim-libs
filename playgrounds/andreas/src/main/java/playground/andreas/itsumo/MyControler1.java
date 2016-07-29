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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.run.Events2Snapshot;

import com.google.inject.Provider;


public class MyControler1 {

	private static final Logger log = Logger.getLogger(MyControler1.class);

	protected int[] generateDistribution(final Link[] netLinks, final int popSize, final Link[] givenLinks, final double[] probs) {

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

	/*package*/ static void loadPopulation(final Scenario scenario) {

		Population population = scenario.getPopulation();
		Network network = scenario.getNetwork();

		log.info("  generating plans... ");

		Link link9 = network.getLinks().get(Id.create("9", Link.class));
		Link link15 = network.getLinks().get(Id.create("15", Link.class));
		for (int i=0; i<100; i++) {
			Person p = PopulationUtils.getFactory().createPerson(Id.create(i + 1, Person.class));

			try {
				Plan plan1 = PopulationUtils.createPlan(p);
				Activity act1a = PopulationUtils.createAndAddActivityFromCoord(plan1, (String) "h", new Coord(100., 100.));
				act1a.setLinkId(link9.getId());
				act1a.setEndTime(0*60*60.);
				Leg leg = PopulationUtils.createAndAddLeg( plan1, (String) TransportMode.car );
				NetworkRoute route = new LinkNetworkRouteImpl(link9.getId(), link15.getId());
				route.setLinkIds(link9.getId(), NetworkUtils.getLinkIds(RouteUtils.getLinksFromNodes(NetworkUtils.getNodes(network, "3 4"))), link15.getId());
				leg.setRoute(route);
				Activity act1b = PopulationUtils.createAndAddActivityFromCoord(plan1, (String) "h", new Coord(200., 200.));
				act1b.setLinkId(link15.getId());
				act1b.setStartTime(8*60*60);
				p.addPlan(plan1);

				Plan plan2 = PopulationUtils.createPlan(p);
				Activity act2a = PopulationUtils.createAndAddActivityFromCoord(plan1, (String) "h", new Coord(100., 100.));
				act2a.setLinkId(link9.getId());
				act2a.setEndTime(0*60*60.);
				Leg leg2 = PopulationUtils.createAndAddLeg( plan2, (String) TransportMode.car );
				NetworkRoute route2 = new LinkNetworkRouteImpl(link9.getId(), link15.getId());
				route2.setLinkIds(link9.getId(), NetworkUtils.getLinkIds(RouteUtils.getLinksFromNodes(NetworkUtils.getNodes(network, "3 6 4"))), link15.getId());
				leg2.setRoute(route2);
				Activity act2b = PopulationUtils.createAndAddActivityFromCoord(plan1, (String) "h", new Coord(200., 200.));
				act2b.setLinkId(link15.getId());
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
	}

	/*package*/ static void loadNetwork(final Scenario scenario) {

		log.info("  creating network layer... ");
		Network network = scenario.getNetwork();
		log.info("  done");

		log.info("  reading network xml file... ");
		ITSUMONetworkReader reader = new ITSUMONetworkReader((Network) network);
		reader.read(scenario.getConfig().getParam(ItsumoSim.CONFIG_MODULE, "itsumoInputNetworkFile"));

		NetworkWriter network_writer = new NetworkWriter(network);
		network_writer.write(scenario.getConfig().controler().getOutputDirectory() + "/network.xml");
		log.info("  done");

	}

	/**
	 * Conversion of events -> snapshots
	 *
	 */
	protected static void makeVis(MatsimServices controler){

		File driversLog = new File("./drivers.txt");
		File visDir = new File("./output/vis");
		File eventsFile = new File("./output/vis/events.txt");

		if (driversLog.exists()){
			visDir.mkdir();
			driversLog.renameTo(eventsFile);

			Events2Snapshot events2Snapshot = new org.matsim.run.Events2Snapshot();
            events2Snapshot.run(eventsFile, controler.getConfig(), controler.getScenario().getNetwork());

			// Run NetVis if possible
			if (controler.getConfig().getParam("simulation", "snapshotFormat").equalsIgnoreCase("netvis")){
				String[] visargs = {"./output/vis/Snapshot"};
				// NetVis.main(visargs);
			}

		} else {
			System.err.println("Couldn't find " + driversLog);
			System.exit(0);
		}
	}

	private void generatePerson(final int ii, final Link sourceLink, final Link destLink, final Population population){
		Person p = PopulationUtils.getFactory().createPerson(Id.create(ii, Person.class));
		Plan plan = PopulationUtils.createPlan(p);
		try {
			Activity act1 = PopulationUtils.createAndAddActivityFromCoord(plan, (String) "h", new Coord(100., 100.));
			act1.setLinkId(sourceLink.getId());
			act1.setStartTime(0.);
			act1.setEndTime(0 * 60 * 60.);

			PopulationUtils.createAndAddLeg( plan, (String) TransportMode.car );
			Activity act2 = PopulationUtils.createAndAddActivityFromCoord(plan, (String) "h", new Coord(200., 200.));
			act2.setLinkId(destLink.getId());
			act2.setStartTime(8 * 60 * 60);

			p.addPlan(plan);
			population.addPerson(p);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
//	@Override
//	protected void runMobSim(int iteration) {
//		ItsumoSim sim = new ItsumoSim(this.scenarioData, this.events);
//		sim.setControlerIO(this.getControlerIO());
//		sim.setIteration(iteration);
//		sim.run();
//	}
	// see below. kai, feb'13

	public static void main(final String[] args) {

//		ScenarioLoaderImpl sl;
//		if ( args.length==0 ) {
//			sl = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed("./examples/itsumo-sesam-scenario/config.xml");
//		} else {
//			sl = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(args[0]);
//		}
//		final Scenario scenario = sl.getScenario();

		// the following should be equivalent, but is not an automatic refactoring. kai, oct'13
		String str = "./examples/itsumo-sesam-scenario/config.xml" ;
		if ( args.length!=0 ) {
			str = args[0] ;
		}
		final Config config = ConfigUtils.loadConfig(str);
		final Scenario scenario = ScenarioUtils.createScenario(config) ;
		MatsimRandom.reset(config.global().getRandomSeed());
		// (probably not needed, but was in original called code. kai, oct'13)


		loadNetwork(scenario);
		loadPopulation(scenario);

		final Controler controler = new Controler(scenario);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		final OutputDirectoryHierarchy controlerIO = controler.getControlerIO() ;

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return new MobsimFactory() {
							@Override
							public Mobsim createMobsim(final Scenario sc, final EventsManager eventsManager) {
								final ItsumoSim sim = new ItsumoSim(sc, eventsManager);
								sim.setControlerIO(controlerIO);
								sim.setIteration(-1); // not directly available; if truly necessary, something else needs to be done (e.g. events handler). kai, feb'13
								throw new RuntimeException("inserted this factory instead of overriding runMobSim above.  not tested, this aborting ...");
//				return sim ;
							}
						}.createMobsim(controler.getScenario(), controler.getEvents());
					}
				});
			}
		});

		controler.run();

		makeVis(controler);
	}

}

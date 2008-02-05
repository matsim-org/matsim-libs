/* *********************************************************************** *
 * project: org.matsim.*
 * QControler.java
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

package playground.andreas.intersection;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.SimulationTimer;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.run.Events2Snapshot;
import org.matsim.utils.misc.Time;
import org.matsim.utils.vis.netvis.NetVis;

import playground.andreas.intersection.sim.QNetworkLayer;
import playground.andreas.intersection.sim.QSim;

public class QControler extends Controler {

	final private static Logger log = Logger.getLogger(QControler.class);

	public QControler(final Config config) {
		super(config);
	}

	@Override
	protected void runMobSim() {

		SimulationTimer.setTime(0);

		// TODO [an] Is needed ? or
		// remove eventswriter, as the external mobsim has to write the events */
//		this.events.removeHandler(this.eventwriter);
//		this.eventwriter = new EventWriterTXT(getIterationFilename(Controler.FILENAME_EVENTS.replaceAll(".gz", "")));
//		this.events.addHandler(this.eventwriter);
		// I don't think this is really needed. -marcel/21jan2008

		QSim sim = new QSim(this.events, this.population, (QNetworkLayer) this.network);
		sim.run();

	}

	/** Needed to specify a QNetworkLayer as target */
	@Override
	protected NetworkLayer loadNetwork() {
		QNetworkLayer network = new QNetworkLayer();
		Gbl.getWorld().setNetworkLayer(network);

		new MatsimNetworkReader(network).readFile(this.config.network().getInputFile());

		return network;
	}

	/** Should be overwritten in case of artificial population */
	@Override
	protected Plans loadPopulation() {

		Plans pop = new Plans(Plans.NO_STREAMING);

		log.info("  generating plans... ");

		for (int jj = 1; jj <= 10; jj++) {

			Link destLink = network.getLink("20");
			Link sourceLink = network.getLink("60");
			generatePerson(jj, sourceLink, destLink, pop);
		}

		return pop;

	}

	/** Generates one Person a time */
	private void generatePerson(final int ii, final Link sourceLink, final Link destLink, final Plans population) {
		Person p = new Person(String.valueOf(ii), "m", "12", "yes", "always", "yes");
		Plan plan = new Plan(p);
		try {
			plan.createAct("h", 100., 100., sourceLink, 0., 3 * 60 * 60., Time.UNDEFINED_TIME, true);
			plan.createLeg("1", "car", null, null, null);
			plan.createAct("h", 200., 200., destLink, 8 * 60 * 60, 0., 0., true);

			p.addPlan(plan);
			population.addPerson(p);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	/** Conversion of events -> snapshots */
	protected void makeVis() {

		File driversLog = new File("./output/ITERS/it.0/0.events.txt");
		File visDir = new File("./output/vis");
		File eventsFile = new File("./output/vis/events.txt");

		if (driversLog.exists()) {
			visDir.mkdir();
			driversLog.renameTo(eventsFile);

			Events2Snapshot events2Snapshot = new org.matsim.run.Events2Snapshot();
			events2Snapshot.run(eventsFile, Gbl.getConfig(), this.network);

			// Run NetVis if possible
			if (Gbl.getConfig().getParam("simulation", "snapshotFormat").equalsIgnoreCase("netvis")) {
				String[] visargs = { "./output/vis/Snapshot" };
				NetVis.main(visargs);
			}

		} else {
			System.err.println("Couldn't find " + driversLog);
			System.exit(0);
		}

		String[] visargs = { "./output/ITERS/it.0/Snapshot" };
		NetVis.main(visargs);
	}

	public static void main(final String[] args) {

		Config config;

		if (args.length == 0) {
			config = Gbl.createConfig(new String[] { "./test/shared/itsumo-sesam-scenario/config.xml" });
		} else {
			config = Gbl.createConfig(args);
		}

		final QControler controler = new QControler(config);
		controler.setOverwriteFiles(true);

		controler.run();

		controler.makeVis();
	}

}

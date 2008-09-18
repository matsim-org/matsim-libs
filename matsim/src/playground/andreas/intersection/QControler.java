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
import org.matsim.run.Events2Snapshot;
import org.matsim.utils.vis.netvis.NetVis;

import playground.andreas.intersection.dijkstra.TravelTimeCalculatorTrafficLight;
import playground.andreas.intersection.sim.QSim;

public class QControler extends Controler {

	@SuppressWarnings("unused")
	final private static Logger log = Logger.getLogger(QControler.class);

	final String newLSADef;
	final String newLSADefCfg;
	
	static boolean useOTF;

	public QControler(final Config config, boolean useOTF, String newLSADef, String newLSADefCfg) {
		super(config);
		this.newLSADef = newLSADef;
		this.newLSADefCfg = newLSADefCfg;
		QControler.useOTF = useOTF;
	}

	@Override
	protected void runMobSim() {
		QSim sim = new QSim(this.events, this.population, this.network, useOTF, this.newLSADef, this.newLSADefCfg);
//		QueueSimulation sim = new QueueSimulation(this.network, this.population, this.events);
		sim.run();
	}

	@Override
	protected void setup() {
		// TODO Auto-generated method stub
		double endTime = this.config.simulation().getEndTime() > 0 ? this.config.simulation().getEndTime() : 30*3600;
		this.travelTimeCalculator = new TravelTimeCalculatorTrafficLight(this.network, getTraveltimeBinSize(), (int) endTime);
		super.setup();
	}

	/** Conversion of events -> snapshots */
	protected void makeVis() {

		File driversLog = new File("./output/ITERS/it.0/0.events.txt.gz");
		File visDir = new File("./output/vis");
		File eventsFile = new File("./output/vis/events.txt.gz");

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

//		if (args.length == 0) {
//			config = Gbl.createConfig(new String[] { "./src/playground/andreas/intersection/config.xml" });
//		} else {
//			config = Gbl.createConfig(args);
//		}

		config = Gbl.createConfig(new String[] {"./src/playground/andreas/intersection/test/data/fourways/config.xml"});
		final String newLSADef = "./src/playground/andreas/intersection/test/data/fourways/lsa.xml";
		final String newLSADefCfg = "./src/playground/andreas/intersection/test/data/fourways/lsa_config.xml";
		config.plans().setInputFile("src/playground/andreas/intersection/test/data/fourways/plans_uturn.xml");

//		config = Gbl.createConfig(new String[] {"./src/playground/andreas/intersection/test/data/twoways/config.xml"});
//		final String newLSADef = "./src/playground/andreas/intersection/test/data/twoways/lsa_2.xml";
//		final String newLSADefCfg = "./src/playground/andreas/intersection/test/data/twoways/lsa_2_config.xml";

//		config = Gbl.createConfig(new String[] {"./src/playground/andreas/intersection/test/data/oneways/config.xml"});
//		final String newLSADef = "./src/playground/andreas/intersection/test/data/oneways/lsa.xml";
//		final String newLSADefCfg = "./src/playground/andreas/intersection/test/data/oneways/lsa_config.xml";
		
//		config = Gbl.createConfig(new String[] {"./src/playground/andreas/intersection/test/data/twoways/config.xml"});
//		config = Gbl.createConfig(new String[] {"./examples/two-routes/config.xml"});
//		final String newLSADef = null;
//		final String newLSADefCfg = null;
		
		final QControler controler = new QControler(config, false, newLSADef, newLSADefCfg);
		controler.setOverwriteFiles(true);
		controler.setWriteEvents(true);
//		controler.setTraveltimeBinSize(30*60);

		controler.run();
//		controler.makeVis();
		
//		if (QControler.useOTF){
////			OTFVis.main(new String [] {"./output/ITERS/it.0/0.otfvis.mvi"});
//		} else {
//			NetVis.main(new String[]{"./output/ITERS/it.0/Snapshot"});
//		}
	}

}

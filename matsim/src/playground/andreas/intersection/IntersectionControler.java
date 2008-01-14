package playground.andreas.intersection;
/*
 * $Id: MyControler1.java,v 1.3 2007/11/20 15:31:20 fboffo Exp $
 */

/* *********************************************************************** *
 *                                                                         *
 *                                                                         *
 *                          ---------------------                          *
 * copyright       : (C) 2007 by Michael Balmer, Marcel Rieser,            *
 *                   David Strippgen, Gunnar Flötteröd, Konrad Meister,    *
 *                   Kai Nagel, Kay W. Axhausen                            *
 *                   Technische Universitaet Berlin (TU-Berlin) and        *
 *                   Swiss Federal Institute of Technology Zurich (ETHZ)   *
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

import java.io.File;

import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.SimulationTimer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.run.Events2Snapshot;
import org.matsim.utils.vis.netvis.NetVis;

import playground.andreas.intersection.sim.QNetworkLayer;
import playground.andreas.intersection.sim.QSim;


public class IntersectionControler extends Controler {
	
	protected void runMobSim() {
		
		SimulationTimer.setTime(0);
				
		/* remove eventswriter, as the external mobsim has to write the events */
		this.events.removeHandler(this.eventwriter);
		QSim sim = new QSim(this.events, this.population, (QNetworkLayer) this.network);
		sim.run();
		
	}
	
	@Override
	protected NetworkLayer loadNetwork() {
		printNote("", "  creating network layer... ");
		QNetworkLayer network = new QNetworkLayer();
		Gbl.getWorld().setNetworkLayer(network);
		printNote("", "  done");

		printNote("", "  reading network xml file... ");
		new MatsimNetworkReader(network).readFile(this.config.network().getInputFile());
		printNote("", "  done");

		return network;
	}

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
			events2Snapshot.run(eventsFile, Gbl.getConfig(), network);			
			
			// Run NetVis if possible
			if (Gbl.getConfig().getParam("simulation", "snapshotFormat").equalsIgnoreCase("netvis")){
				String[] visargs = {"./output/vis/Snapshot"};						
				NetVis.main(visargs);
			}			
			
		} else {
			System.err.println("Couldn't find " + driversLog);
			System.exit(0);
		}		
		
		String[] visargs = {"./output/ITERS/it.0/Snapshot"};						
		NetVis.main(visargs);
	}
	
		
	public static void main(final String[] args) {
		
		if ( args.length==0 ) {
			Gbl.createConfig(new String[] {"./test/shared/itsumo-sesam-scenario/config.xml"});	
		} else {
			Gbl.createConfig(args) ;
		}
				
		final IntersectionControler controler = new IntersectionControler();
		controler.setOverwriteFiles(true) ;
		
		controler.run(null);
		
//		controler.makeVis();
	}	

}

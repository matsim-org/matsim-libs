/* *********************************************************************** *
 * project: org.matsim.*
 * RelaxationControler.java
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

package playground.meisterk.phd.controler;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.NumberFormat;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;

import playground.meisterk.phd.config.PopulationConvergenceConfigGroup;
import playground.meisterk.phd.replanning.PhDStrategyManager;

public class PhDControler extends Controler {

	private static final Logger log;
	private static final NumberFormat timeFormat; 

	private final PopulationConvergenceConfigGroup populationConvergenceConfigGroup = new PopulationConvergenceConfigGroup();
	
	static {
		log = Logger.getLogger(PhDControler.class);
		timeFormat = NumberFormat.getInstance();
		timeFormat.setMaximumFractionDigits(2);
	}
	
	public PhDControler(String[] args) {
		super(args);
		super.config.addModule(PopulationConvergenceConfigGroup.GROUP_NAME, populationConvergenceConfigGroup);
	}

	@Override
	protected void loadControlerListeners() {
		super.loadControlerListeners();
		this.addControlerListener(new LinkTravelTimeWriter());
		this.addControlerListener(new PersonTreatmentRecorder(this.populationConvergenceConfigGroup));
	}

	@Override
	protected StrategyManager loadStrategyManager() {
		PhDStrategyManager manager = new PhDStrategyManager();
		StrategyManagerConfigLoader.load(this, manager);
		return manager;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: PhDControler config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controler = new PhDControler(args);
			controler.run();
		}
		System.exit(0);
	}

	public class LinkTravelTimeWriter implements IterationEndsListener {

		private static final String FILENAME = "linkTravelTimes.txt"; 
		
		public LinkTravelTimeWriter() {
			super();
		}

		public void notifyIterationEnds(IterationEndsEvent event) {

			Controler c = event.getControler();
			NetworkImpl network = c.getNetwork();
			TravelTime travelTime = c.getTravelTimeCalculator();
			double binSize = c.getConfig().travelTimeCalculator().getTraveltimeBinSize();
			
			log.info("Writing results file...");
			PrintStream out = null;
			try {
				out = new PrintStream(event.getControler().getControlerIO().getIterationFilename(event.getIteration(), FILENAME));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			
			out.print("#link");
			for (double time=Time.parseTime("00:00:00"); time<Time.parseTime("24:00:00"); time+=binSize) {
				out.print("\t" + time);
			}
			out.println();
			
			for (LinkImpl link : network.getLinks().values()) {
				out.print(link.getId());
				for (double time=Time.parseTime("00:00:00"); time<Time.parseTime("24:00:00"); time+=binSize) {
					out.print("\t" + timeFormat.format(travelTime.getLinkTravelTime(link, time)));
				}
				out.println();
			}
			
			out.close();
			log.info("Writing results file...done.");

		}
		
	}
	
}

/* *********************************************************************** *
 * project: org.matsim.*
 * CalcLegTimesKTIListener.java
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

package herbie.running.controler.listeners;

import herbie.running.analysis.CalcLegTimesKTI;
import herbie.running.population.algorithms.AbstractClassifiedFrequencyAnalysis.CrosstabFormat;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.misc.Time;


public class CalcLegTimesKTIListener implements StartupListener, AfterMobsimListener, ShutdownListener, IterationEndsListener {

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

	final private String averagesSummaryFilename;
	final private String travelTimeDistributionFilename;

	private PrintStream iterationSummaryOut;
	private CalcLegTimesKTI calcLegTimesKTI;

	private String[] modes = {TransportMode.car, TransportMode.ride, TransportMode.bike, TransportMode.walk, TransportMode.transit_walk, TransportMode.pt};

	private final static Logger log = Logger.getLogger(CalcLegTimesKTIListener.class);

	public CalcLegTimesKTIListener(String averagesSummaryFilename, String travelTimeDistributionFilename) {
		super();
		this.averagesSummaryFilename = averagesSummaryFilename;
		this.travelTimeDistributionFilename = travelTimeDistributionFilename;
	}

	@Override
	public void notifyStartup(StartupEvent event) {

		try {
			this.iterationSummaryOut = new PrintStream(event.getControler().getControlerIO().getOutputFilename(this.averagesSummaryFilename));
			this.iterationSummaryOut.print("#iteration\tall");
			for (String mode : modes) {
				this.iterationSummaryOut.print("\t" + mode);
			}
			this.iterationSummaryOut.println();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		this.calcLegTimesKTI = new CalcLegTimesKTI(event.getControler().getPopulation(), iterationSummaryOut);
		event.getControler().getEvents().addHandler(this.calcLegTimesKTI);

	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {

		TreeMap<String, Double> avgTripDurations = this.calcLegTimesKTI.getAverageTripDurationsByMode();
		String str;

		this.iterationSummaryOut.print(Integer.toString(event.getIteration()));
		this.iterationSummaryOut.print("\t" + Time.writeTime(this.calcLegTimesKTI.getAverageOverallTripDuration()));
		for (String mode : modes) {
			if (avgTripDurations.containsKey(mode)) {
				str = Time.writeTime(avgTripDurations.get(mode));
			} else {
				str = "---";
			}
			this.iterationSummaryOut.print("\t" + str);
		}
		this.iterationSummaryOut.println();
		this.iterationSummaryOut.flush();
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		if (event.getIteration() % 10 == 0) {

			PrintStream out = null;
			try {
				out = new PrintStream(event.getControler().getControlerIO().getIterationFilename(event.getIteration(), travelTimeDistributionFilename));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			log.info("Writing results file...");
			this.calcLegTimesKTI.printClasses(CrosstabFormat.ABSOLUTE, false, timeBins, out);
			this.calcLegTimesKTI.printDeciles(true, out);
			out.close();
			log.info("Writing results file...done.");

		}

	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		this.iterationSummaryOut.close();
	}

}

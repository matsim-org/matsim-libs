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

package playground.meisterk.kti.controler.listeners;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.TreeMap;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.misc.Time;

import playground.meisterk.org.matsim.analysis.CalcLegTimesKTI;

public class CalcLegTimesKTIListener implements StartupListener, AfterMobsimListener, ShutdownListener {

	final private String filename;
	private PrintStream out;
	private CalcLegTimesKTI calcLegTimesKTI;
	
	
	public CalcLegTimesKTIListener(String filename) {
		super();
		this.filename = filename;
	}
	
	public void notifyStartup(StartupEvent event) {

		try {
			this.out = new PrintStream(org.matsim.core.controler.Controler.getOutputFilename(this.filename));
			this.out.print("#iteration\tall");
			for (TransportMode mode : TransportMode.values()) {
				this.out.print("\t" + mode.toString());
			}
			this.out.println();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		this.calcLegTimesKTI = new CalcLegTimesKTI(event.getControler().getPopulation(), out);
		event.getControler().getEvents().addHandler(this.calcLegTimesKTI);

	}
	
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		
		TreeMap<TransportMode, Double> avgTripDurations = this.calcLegTimesKTI.getAverageTripDurationsByMode();
		String str;
		
		this.out.print(Integer.toString(event.getIteration()));
		this.out.print("\t" + Time.writeTime(this.calcLegTimesKTI.getAverageOverallTripDuration()));
		for (TransportMode mode : TransportMode.values()) {
			if (avgTripDurations.containsKey(mode)) {
				str = Time.writeTime(avgTripDurations.get(mode));
			} else {
				str = "---";
			}
			this.out.print("\t" + str);
		}
		this.out.println();
		this.out.flush();
	}

	public void notifyShutdown(ShutdownEvent event) {
		this.out.close();
	}

}

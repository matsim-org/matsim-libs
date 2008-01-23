/* *********************************************************************** *
 * project: org.matsim.*
 * NewPtcheckControler.java
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

package playground.yu.mautZH;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.matsim.analysis.CalcAverageTolledTripLength;
import org.matsim.analysis.CalcAverageTripLength;
import org.matsim.analysis.ScoreStats;
import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;

import playground.yu.analysis.CalcAvgSpeed;
import playground.yu.analysis.CalcTrafficPerformance;
import playground.yu.ivtch.PtRate;

/**
 * test of PtCheck and PtRate, outputs Public-Transit user fraction
 *
 * @author ychen
 *
 */
public class NewPtcheckControler extends Controler {
	/**
	 * internal outputStream
	 */
	private DataOutputStream out;

	/**
	 * adds a ControlerListener to Controler - PtRate
	 */
	@Override
	protected void setup() {
		super.setup();
		try {
			addControlerListener(new PtRate(this.getPopulation(), getOutputFilename("PtRate.txt"), this));
			addControlerListener(new PtCheckListener());
			this.out = new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(new File(
							getOutputFilename("tollPaid.txt")))));
			this.out
					.writeBytes("Iter\tBetaTraveling\tBetaTravelingPt\ttoll_amount[EUR/m]"
							+ "\ttoll_paid[EUR]\tavg. executed score\tNumber of Drawees"
							+ "\tavg. triplength\tavg. tolled triplength"
							+ "\ttraffic persformance\tavg. travel speed\n");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public static class PtCheckListener implements StartupListener, IterationEndsListener {
		private DataOutputStream out;
		private final ScoreStats scoreStats = null;
		private CalcAverageTolledTripLength cattl = null;
		private CalcAverageTripLength catl = null;
		private CalcTrafficPerformance ctpf = null;
		private CalcAvgSpeed cas = null;

		public void notifyStartup(final StartupEvent event) {
			Controler controler = event.getControler();
			Events events = controler.getEvents();
			NetworkLayer network = event.getControler().getNetwork();
			if (event.getControler().getConfig().roadpricing().getTollLinksFile() != null) {
				this.cattl = new CalcAverageTolledTripLength(network, toll);
				events.addHandler(this.cattl);
			}
			this.ctpf = new CalcTrafficPerformance(network);
			this.cas = new CalcAvgSpeed(network);
			events.addHandler(this.ctpf);
			events.addHandler(this.cas);
		}

		public void notifyIterationEnds(final IterationEndsEvent event) {
			int iteration = event.getIteration();
			Config config = event.getControler().getConfig();
			this.catl = new CalcAverageTripLength();
			this.catl.run(event.getControler().getPopulation());
			try {
				this.out.writeBytes(iteration
						+ "\t"
						+ config.getParam("planCalcScore", "traveling")
						+ "\t"
						+ config.getParam("planCalcScore", "travelingPt")
						+ "\t"
						+ ((Gbl.useRoadPricing()) ? toll.getCostArray()[0].amount
								: 0)
						+ "\t"
						+ ((tollCalc != null) ? tollCalc.getAllAgentsToll() : 0.0)
						+ "\t"
						+ this.scoreStats.getHistory()[3][iteration]
						+ "\t"
						+ ((tollCalc != null) ? tollCalc.getDraweesNr() : 0)
						+ "\t"
						+ this.catl.getAverageTripLength()
						+ "\t"
						+ ((((tollCalc != null) && (this.cattl != null))) ? this.cattl
								.getAverageTripLength() : 0.0) + "\t"
						+ this.ctpf.getTrafficPerformance() + "\t" + this.cas.getAvgSpeed()
						+ "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	// -------------------------MAIN FUNCTION--------------------
	/**
	 * @param args -
	 *            the path of config-file
	 */
	public static void main(final String[] args) {
		final NewPtcheckControler controler;
		controler = new NewPtcheckControler();
		controler.run(args);
		try {
			controler.out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}

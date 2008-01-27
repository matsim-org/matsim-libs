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

package playground.yu.ivtch;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.matsim.analysis.CalcAverageTolledTripLength;
import org.matsim.analysis.CalcAverageTripLength;
import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.controler.corelisteners.RoadPricing;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.events.ShutdownEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.controler.listener.ShutdownListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.roadpricing.CalcPaidToll;

import playground.yu.analysis.CalcAvgSpeed;
import playground.yu.analysis.CalcTrafficPerformance;

/**
 * test of PtCheck and PtRate, outputs Public-Transit user fraction
 * 
 * @author ychen
 * 
 */
public class NewPtcheckControler extends Controler {
	public NewPtcheckControler(String[] configFileName) {
		super(configFileName);
	}

	public static class PtCheckListener implements StartupListener,
			IterationEndsListener, IterationStartsListener, ShutdownListener {
		private CalcTrafficPerformance ctpf = null;
		private CalcAvgSpeed cas = null;
		private CalcAverageTolledTripLength cattl = null;
		/**
		 * internal outputStream
		 */
		private DataOutputStream out;

		public void notifyStartup(StartupEvent event) {
			Controler ctl = event.getControler();
			Config cf = ctl.getConfig();
			try {
				ctl.addControlerListener(new PtRate(ctl.getPopulation(),
						getOutputFilename("PtRate.txt"),
						ctl.getLastIteration(), cf.getParam("planCalcScore",
								"traveling"), cf.getParam("planCalcScore",
								"travelingPt")));
				out = new DataOutputStream(new BufferedOutputStream(
						new FileOutputStream(new File(
								getOutputFilename("tollPaid.txt")))));
				out
						.writeBytes("Iter\tBetaTraveling\tBetaTravelingPt\ttoll_amount[€/m]"
								+ "\ttoll_paid[€]\tavg. executed score\tNumber of Drawees"
								+ "\tavg. triplength\tavg. tolled triplength"
								+ "\ttraffic persformance\tavg. travel speed\n");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void notifyIterationEnds(IterationEndsEvent event) {
			int it = event.getIteration();
			Controler ctl = event.getControler();
			Config cf = ctl.getConfig();
			RoadPricing rp = ctl.getRoadPricing();
			CalcPaidToll tollCalc =null;
			if (rp != null) {
				tollCalc = rp.getPaidTolls();
			}
			CalcAverageTripLength catl = new CalcAverageTripLength();
			catl.run(ctl.getPopulation());
			try {
				out.writeBytes(it
						+ "\t"
						+ cf.getParam("planCalcScore", "traveling")
						+ "\t"
						+ cf.getParam("planCalcScore", "travelingPt")
						+ "\t"
						+ ((Gbl.useRoadPricing()) ? rp.getRoadPricingScheme()
								.getCostArray()[0].amount : 0)
						+ "\t"
						+ ((tollCalc != null) ? tollCalc.getAllAgentsToll()
								: 0.0)
						+ "\t"
						+ ctl.getScoreStats().getHistory()[3][it]
						+ "\t"
						+ ((tollCalc != null) ? tollCalc.getDraweesNr() : 0)
						+ "\t"
						+ catl.getAverageTripLength()
						+ "\t"
						+ ((((tollCalc != null) && (cattl != null))) ? cattl
								.getAverageTripLength() : 0.0) + "\t"
						+ ctpf.getTrafficPerformance() + "\t"
						+ cas.getAvgSpeed() + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void notifyIterationStarts(IterationStartsEvent event) {
			int it = event.getIteration();
			Controler ctl = event.getControler();
			Events events = ctl.getEvents();
			NetworkLayer network = ctl.getNetwork();
			cas = new CalcAvgSpeed(network);
			cas.reset(it);
			if (cattl != null) {
				cattl.reset(it);
			}
			ctpf = new CalcTrafficPerformance(network);
			ctpf.reset(it);
			if (Gbl.useRoadPricing()) {
				cattl = new CalcAverageTolledTripLength(network, ctl
						.getRoadPricing().getRoadPricingScheme());
				events.addHandler(cattl);
			}

			events.addHandler(ctpf);
			events.addHandler(cas);
		}

		public void notifyShutdown(ShutdownEvent event) {
			try {
				out.close();
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
		controler = new NewPtcheckControler(args);
		controler.addControlerListener(new PtCheckListener());
		controler.run();
		System.exit(0);
	}
}

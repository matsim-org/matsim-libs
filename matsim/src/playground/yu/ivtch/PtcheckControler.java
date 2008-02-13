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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.analysis.CalcAverageTripLength;
import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.controler.corelisteners.RoadPricing;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.ShutdownEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.ShutdownListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.events.Events;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.io.IOUtils;

import playground.yu.analysis.CalcAvgSpeed;
import playground.yu.analysis.CalcTrafficPerformance;

/**
 * test of PtCheck and PtRate, outputs Public-Transit user fraction
 * 
 * @author ychen
 * 
 */
public class PtcheckControler extends Controler {

	public PtcheckControler(String[] configFileName) {
		super(configFileName);
	}

	public static class PtCheckListener implements StartupListener,
			IterationEndsListener, ShutdownListener {
		/**
		 * internal bufferedWriter
		 */
		private BufferedWriter out;
		private CalcAverageTripLength catl = null;
		private CalcAvgSpeed cas = null;
		private CalcTrafficPerformance ctpf = null;
		private RoadPricing rp = null;

		public void notifyStartup(StartupEvent event) {
			Controler ctl = event.getControler();
			Config cf = ctl.getConfig();
			try {
				ctl.addControlerListener(new PtRate(ctl.getPopulation(),
						getOutputFilename("PtRate.txt"),
						ctl.getLastIteration(), cf.getParam("planCalcScore",
								"traveling"), cf.getParam("planCalcScore",
								"travelingPt")));
				out = IOUtils
						.getBufferedWriter(getOutputFilename("tollPaid.txt"));
				out
						.write("Iter\tBetaTraveling\tBetaTravelingPt\tavg. executed score\tavg. triplength\ttraffic persformance\tavg. travel speed\ttoll_amount[€/m]\ttoll_paid[€]\tNumber of Drawees\tavg. tolled triplength\n");
				out.flush();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			NetworkLayer network = ctl.getNetwork();
			cas = new CalcAvgSpeed(network);
			ctpf = new CalcTrafficPerformance(network);
			Events events = ctl.getEvents();
			events.addHandler(cas);
			events.addHandler(ctpf);
		}

		public void notifyIterationEnds(IterationEndsEvent event) {
			int it = event.getIteration();
			if (it % 10 == 0) {
				Controler ctl = event.getControler();
				Config cf = ctl.getConfig();
				rp = ctl.getRoadPricing();
				catl = new CalcAverageTripLength();
				catl.run(event.getControler().getPopulation());
				try {
					out
							.write(it
									+ "\t"
									+ cf.getParam("planCalcScore", "traveling")
									+ "\t"
									+ cf.getParam("planCalcScore",
											"travelingPt")
									+ "\t"
									+ ctl.getScoreStats().getHistory()[3][it]
									+ "\t"
									+ catl.getAverageTripLength()
									+ "\t"
									+ ctpf.getTrafficPerformance()
									+ "\t"
									+ cas.getAvgSpeed()
									+ "\t"
									+ (((rp != null) && (rp
											.getRoadPricingScheme() != null)) ? rp
											.getRoadPricingScheme()
											.getCostArray()[0].amount
											+ "\t"
											+ rp.getAllAgentsToll()
											+ "\t"
											+ rp.getDraweesNr()
											+ "\t"
											+ rp.getAvgPaidTripLength()
											: "0.0\t0.0\t0\t0.0") + "\n");
					out.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
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
		final PtcheckControler controler;
		controler = new PtcheckControler(args);
		controler.addControlerListener(new PtCheckListener());
		controler.run();
		System.exit(0);
	}
}

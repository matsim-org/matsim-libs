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

package playground.yu.analysis;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

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
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Plans;
import org.matsim.utils.io.IOUtils;

/**
 * test of PtCheck and PtRate, outputs Public-Transit user fraction for
 * Plansfile with Car_License
 * 
 * @author ychen
 * 
 */
public class PtcheckControler2 extends Controler {

	public PtcheckControler2(String[] configFileName) {
		super(configFileName);
	}

	public static class PtCheckListener implements StartupListener,
			IterationEndsListener, ShutdownListener, IterationStartsListener {
		/**
		 * internal bufferedWriter
		 */
		private BufferedWriter ptRateWriter;

		private CalcAverageTripLength catl = null;

		private CalcNetAvgSpeed cas = null;

		private CalcTrafficPerformance ctpf = null;

		private RoadPricing rp = null;

		private OnRouteModalSplit orms = null;

		private TravelTimeModalSplit ttms = null;

		private LegDistance ld = null;

		private CalcLinkAvgSpeed clas = null;

		public void notifyStartup(StartupEvent event) {
			Controler ctl = event.getControler();
			Config cf = ctl.getConfig();
			try {
				ctl.addControlerListener(new PtRate2(ctl.getPopulation(),
						getOutputFilename("PtRate.txt"),
						ctl.getLastIteration(), cf.getParam("planCalcScore",
								"traveling"), cf.getParam("planCalcScore",
								"travelingPt")));
				ptRateWriter = IOUtils
						.getBufferedWriter(getOutputFilename("tollPaid.txt"));
				ptRateWriter
						.write("Iter\tBetaTraveling\tBetaTravelingPt\tavg. executed score\tavg. triplength\ttraffic persformance\tavg. travel speed\ttoll_amount[€/m]\ttoll_paid[€]\tNumber of Drawees\tavg. tolled triplength\n");
				ptRateWriter.flush();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			NetworkLayer network = ctl.getNetwork();
			cas = new CalcNetAvgSpeed(network);
			ctpf = new CalcTrafficPerformance(network);
			Events events = ctl.getEvents();
			events.addHandler(cas);
			events.addHandler(ctpf);
		}

		public void notifyIterationEnds(IterationEndsEvent event) {
			int it = event.getIteration();
			Controler ctl = event.getControler();
			if (it % 10 == 0) {
				Config cf = ctl.getConfig();
				rp = ctl.getRoadPricing();
				catl = new CalcAverageTripLength();
				catl.run(event.getControler().getPopulation());
				try {
					ptRateWriter
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
									+ cas.getNetAvgSpeed()
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
					ptRateWriter.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (it == ctl.getLastIteration()) {
				if (orms != null) {
					orms.write(getOutputFilename("onRoute.txt.gz"));
					orms.writeCharts(getOutputFilename("onRoute.png"));
				}
				if (ttms != null) {
					ttms.write(getOutputFilename("traveltimes.txt.gz"));
					ttms.writeCharts(getOutputFilename("traveltimes.png"));
				}
				if (ld != null) {
					ld.write(getOutputFilename("legDistances.txt.gz"));
					ld.writeCharts(getOutputFilename("legDistances.png"));
				}
				if (clas != null) {
					clas.write(getOutputFilename("avgSpeed.txt.gz"));
				}
			}
		}

		public void notifyShutdown(ShutdownEvent event) {
			try {
				ptRateWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void notifyIterationStarts(IterationStartsEvent event) {
			Controler c = event.getControler();
			Events es = c.getEvents();
			NetworkLayer nl = c.getNetwork();
			Plans ps = c.getPopulation();
			if (event.getIteration() == c.getLastIteration()) {
				orms = new OnRouteModalSplit(300, nl, ps);
				es.addHandler(orms);
				ttms = new TravelTimeModalSplit(300, nl, ps);
				es.addHandler(ttms);
				ld = new LegDistance(300, nl);
				es.addHandler(ld);
				clas = new CalcLinkAvgSpeed(nl, 682845.0, 247388.0, 2000.0);
				es.addHandler(clas);
				c.getConfig().simulation().setSnapshotPeriod(300);
			} else if (event.getIteration() == c.getFirstIteration()) {
				c.getConfig().simulation().setSnapshotPeriod(300);
			} else {
				c.getConfig().simulation().setSnapshotPeriod(0);
			}
		}
	}

	// -------------------------MAIN FUNCTION--------------------
	/**
	 * @param args -
	 *            the path of config-file
	 */
	public static void main(final String[] args) {
		final PtcheckControler2 controler;
		controler = new PtcheckControler2(args);
		controler.addControlerListener(new PtCheckListener());
		controler.run();
		System.exit(0);
	}
}

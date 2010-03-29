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

package playground.yu.test;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.analysis.CalcAverageTripLength;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.corelisteners.RoadPricing;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;

import playground.yu.analysis.CalcLinksAvgSpeed;
import playground.yu.analysis.CalcNetAvgSpeed;
import playground.yu.analysis.CalcTrafficPerformance;
import playground.yu.analysis.EnRouteModalSplit;
import playground.yu.analysis.LegDistance;
import playground.yu.analysis.LegTravelTimeModalSplit;
import playground.yu.analysis.MyCalcAverageTripLength;
import playground.yu.analysis.PtRate;

/**
 * test of PtCheck and PtRate, outputs Public-Transit user fraction
 * 
 * @author ychen
 * 
 */
public class PtcheckControlerTest extends Controler {

	public PtcheckControlerTest(final String[] configFileName) {
		super(configFileName);
	}

	public static class PtCheckListener implements StartupListener,
			IterationEndsListener, ShutdownListener, IterationStartsListener {
		/**
		 * internal bufferedWriter
		 */
		private BufferedWriter ptRateWriter = null;
		private CalcAverageTripLength catl = null;
		private CalcNetAvgSpeed cas = null;
		private CalcTrafficPerformance ctpf = null;
		private RoadPricing rp = null;
		private EnRouteModalSplit orms = null;
		private LegTravelTimeModalSplit ttms = null;
		private LegDistance ld = null;
		private CalcLinksAvgSpeed clas = null;

		public void notifyStartup(final StartupEvent event) {
			Controler ctl = event.getControler();
			Config cf = ctl.getConfig();
			try {
				ctl.addControlerListener(new PtRate(ctl.getPopulation(), event
						.getControler().getControlerIO().getOutputFilename(
								"PtRate.txt"), ctl.getLastIteration(), cf
						.getParam("planCalcScore", "traveling"), cf.getParam(
						"planCalcScore", "travelingPt")));
				this.ptRateWriter = IOUtils.getBufferedWriter(event
						.getControler().getControlerIO().getOutputFilename(
								"tollPaid.txt"));
				this.ptRateWriter
						.write("Iter\tBetaTraveling\tBetaTravelingPt\tavg. executed score\tavg. triplength\ttraffic persformance\tavg. travel speed\ttoll_amount[�/m]\ttoll_paid[�]\tNumber of Drawees\tavg. tolled triplength\n");
				this.ptRateWriter.flush();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Network network = ctl.getNetwork();
			this.cas = new CalcNetAvgSpeed(network);
			this.ctpf = new CalcTrafficPerformance(network);
			EventsManager events = ctl.getEvents();
			events.addHandler(this.cas);
			events.addHandler(this.ctpf);
		}

		public void notifyIterationEnds(final IterationEndsEvent event) {
			int it = event.getIteration();
			Controler ctl = event.getControler();
			Config cf = ctl.getConfig();
			if (it % 10 == 0) {
				this.rp = ctl.getRoadPricing();
				this.catl = new MyCalcAverageTripLength(ctl.getNetwork());
				this.catl.run(event.getControler().getPopulation());
				try {
					this.ptRateWriter
							.write(it
									+ "\t"
									+ cf.getParam("planCalcScore", "traveling")
									+ "\t"
									+ cf.getParam("planCalcScore",
											"travelingPt")
									+ "\t"
									+ ctl.getScoreStats().getHistory()[3][it]
									+ "\t"
									+ this.catl.getAverageTripLength()
									+ "\t"
									+ this.ctpf.getTrafficPerformance()
									+ "\t"
									+ this.cas.getNetAvgSpeed()
									+ "\t"
									+ (((this.rp != null) && (this.rp
											.getRoadPricingScheme() != null)) ? this.rp
											.getRoadPricingScheme()
											.getCostArray()[0].amount
											+ "\t"
											+ this.rp.getAllAgentsToll()
											+ "\t"
											+ this.rp.getDraweesNr()
											+ "\t"
											+ this.rp.getAvgPaidTripLength()
											: "0.0\t0.0\t0\t0.0") + "\n");
					this.ptRateWriter.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (it == ctl.getLastIteration()) {
				if (this.orms != null) {
					this.orms.write(event.getControler().getControlerIO()
							.getOutputFilename("onRoute.txt.gz"));
					this.orms.writeCharts(event.getControler().getControlerIO()
							.getOutputFilename("onRoute.png"));
				}
				if (this.ttms != null) {
					this.ttms.write(event.getControler().getControlerIO()
							.getOutputFilename("traveltimes.txt.gz"));
					this.ttms.writeCharts(event.getControler().getControlerIO()
							.getOutputFilename("traveltimes.png"));
				}
				if (this.ld != null) {
					this.ld.write(event.getControler().getControlerIO()
							.getOutputFilename("legDistances.txt.gz"));
					this.ld.writeCharts(event.getControler().getControlerIO()
							.getOutputFilename("legDistances.png"));
				}
				if (this.clas != null) {
					this.clas.write(event.getControler().getControlerIO()
							.getOutputFilename("avgSpeed.txt.gz"));
					this.clas.writeChart(event.getControler().getControlerIO()
							.getOutputFilename("avgSpeedCityArea.png"));
				}
			}
		}

		public void notifyShutdown(final ShutdownEvent event) {
			try {
				this.ptRateWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void notifyIterationStarts(final IterationStartsEvent event) {
			Controler c = event.getControler();
			EventsManager es = c.getEvents();
			Network nl = c.getNetwork();
			Population ps = c.getPopulation();
			if (event.getIteration() == c.getLastIteration()) {
				this.orms = new EnRouteModalSplit("Zurich", 300,
				// nl,
						ps);
				es.addHandler(this.orms);
				this.ttms = new LegTravelTimeModalSplit(300, ps);
				es.addHandler(this.ttms);
				this.ld = new LegDistance(300, nl);
				es.addHandler(this.ld);
				this.clas = new CalcLinksAvgSpeed(nl, 682845.0, 247388.0,
						2000.0);
				es.addHandler(this.clas);
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
	 * @param args
	 *            - the path of config-file
	 */
	public static void main(final String[] args) {
		final PtcheckControlerTest controler;
		controler = new PtcheckControlerTest(args);
		controler.addControlerListener(new PtCheckListener());
		controler.run();
		System.exit(0);
	}
}

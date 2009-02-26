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
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.io.IOUtils;

import playground.yu.analysis.CalcLinksAvgSpeed;
import playground.yu.analysis.CalcNetAvgSpeed;
import playground.yu.analysis.CalcTrafficPerformance;
import playground.yu.analysis.LegDistance;
import playground.yu.analysis.OnRouteModalSplit;
import playground.yu.analysis.PtRate;
import playground.yu.analysis.TravelTimeModalSplit;

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
		private BufferedWriter ptRateWriter;
		private CalcAverageTripLength catl = null;
		private CalcNetAvgSpeed cas = null;
		private CalcTrafficPerformance ctpf = null;
		private RoadPricing rp = null;
		private OnRouteModalSplit orms = null;
		private TravelTimeModalSplit ttms = null;
		private LegDistance ld = null;
		private CalcLinksAvgSpeed clas = null;

		public void notifyStartup(final StartupEvent event) {
			Controler ctl = event.getControler();
			Config cf = ctl.getConfig();
			try {
				ctl.addControlerListener(new PtRate(ctl.getPopulation(),
						getOutputFilename("PtRate.txt"),
						ctl.getLastIteration(), cf.getParam("planCalcScore",
								"traveling"), cf.getParam("planCalcScore",
								"travelingPt")));
				this.ptRateWriter = IOUtils
						.getBufferedWriter(getOutputFilename("tollPaid.txt"));
				this.ptRateWriter
						.write("Iter\tBetaTraveling\tBetaTravelingPt\tavg. executed score\tavg. triplength\ttraffic persformance\tavg. travel speed\ttoll_amount[�/m]\ttoll_paid[�]\tNumber of Drawees\tavg. tolled triplength\n");
				this.ptRateWriter.flush();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			NetworkLayer network = ctl.getNetwork();
			this.cas = new CalcNetAvgSpeed(network);
			this.ctpf = new CalcTrafficPerformance(network);
			Events events = ctl.getEvents();
			events.addHandler(this.cas);
			events.addHandler(this.ctpf);
		}

		public void notifyIterationEnds(final IterationEndsEvent event) {
			int it = event.getIteration();
			Controler ctl = event.getControler();
			Config cf = ctl.getConfig();
			if (it % 10 == 0) {
				this.rp = ctl.getRoadPricing();
				this.catl = new CalcAverageTripLength();
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
					this.orms.write(getOutputFilename("onRoute.txt.gz"));
					this.orms.writeCharts(getOutputFilename("onRoute.png"));
				}
				if (this.ttms != null) {
					this.ttms.write(getOutputFilename("traveltimes.txt.gz"));
					this.ttms.writeCharts(getOutputFilename("traveltimes.png"));
				}
				if (this.ld != null) {
					this.ld.write(getOutputFilename("legDistances.txt.gz"));
					this.ld.writeCharts(getOutputFilename("legDistances.png"));
				}
				if (this.clas != null) {
					this.clas.write(getOutputFilename("avgSpeed.txt.gz"));
					this.clas
							.writeChart(getOutputFilename("avgSpeedCityArea.png"));
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
			Events es = c.getEvents();
			NetworkLayer nl = c.getNetwork();
			Population ps = c.getPopulation();
			if (event.getIteration() == c.getLastIteration()) {
				this.orms = new OnRouteModalSplit("Zurich", 300,
				// nl,
						ps);
				es.addHandler(this.orms);
				this.ttms = new TravelTimeModalSplit(300, nl, ps);
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

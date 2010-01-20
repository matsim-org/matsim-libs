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

/**
 * test of PtCheck and PtRate, outputs Public-Transit user fraction
 * 
 * @author ychen
 * 
 */
public class PtcheckControler extends Controler {
	private final String scenario;

	public String getScenarioName() {
		return scenario;
	}

	public PtcheckControler(final String[] configFileName, String scenario) {
		super(configFileName);
		this.scenario = scenario;
	}

	public static class PtCheckListener implements StartupListener,
			IterationEndsListener, ShutdownListener, IterationStartsListener {
		/**
		 * internal bufferedWriter
		 */
		private BufferedWriter ptRateWriter = null;
		private final String scenario;

		private CalcAverageTripLength catl = null;
		private CalcNetAvgSpeed cas = null;
		private CalcTrafficPerformance ctpf = null;
		private RoadPricing rp = null;
		private EnRouteModalSplit orms = null;
		private LegTravelTimeModalSplit ttms = null;
		private LegDistance ld = null;
		private CalcLinksAvgSpeed clas = null;
		private static final String PLANCALCSCORE = "planCalcScore";

		public PtCheckListener(String scenario) {
			this.scenario = scenario;
		}

		public void notifyStartup(final StartupEvent event) {
			Controler ctl = event.getControler();
			Config cf = ctl.getConfig();
			try {
				ctl.addControlerListener(new PtRate(ctl.getPopulation(),
						getOutputFilename("PtRate.txt"),
						ctl.getLastIteration(), cf.getParam(PLANCALCSCORE,
								"traveling"), cf.getParam(PLANCALCSCORE,
								"travelingPt")));
				ptRateWriter = IOUtils
						.getBufferedWriter(getOutputFilename("tollPaid.txt"));
				ptRateWriter
						.write("Iter\tBetaTraveling\tBetaTravelingPt\tavg. executed score\tavg. triplength\ttraffic persformance\tavg. travel speed\ttoll_amount[�/m]\ttoll_paid[�]\tNumber of Drawees\tavg. tolled triplength\n");
				ptRateWriter.flush();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Network network = ctl.getNetwork();
			cas = new CalcNetAvgSpeed(network);
			ctpf = new CalcTrafficPerformance(network);
			EventsManager events = ctl.getEvents();
			events.addHandler(cas);
			events.addHandler(ctpf);
		}

		public void notifyIterationEnds(final IterationEndsEvent event) {
			int it = event.getIteration();
			Controler ctl = event.getControler();
			if (it % 10 == 0) {
				Config cf = ctl.getConfig();
				rp = ctl.getRoadPricing();
				catl = new CalcAverageTripLength(ctl.getNetwork());
				catl.run(event.getControler().getPopulation());
				try {
					ptRateWriter
							.write(it
									+ "\t"
									+ cf.getParam(PLANCALCSCORE, "traveling")
									+ "\t"
									+ cf.getParam(PLANCALCSCORE, "travelingPt")
									+ "\t"
									+ ctl.getScoreStats().getHistory()[3][it
											- ctl.getFirstIteration()]
									+ "\t"
									+ catl.getAverageTripLength()
									+ "\t"
									+ ctpf.getTrafficPerformance()
									+ "\t"
									+ cas.getNetAvgSpeed()
									+ "\t"
									+ (rp != null
											&& rp.getRoadPricingScheme() != null ? rp
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
					ttms.writeCharts(getOutputFilename("traveltimes"));
				}
				if (ld != null) {
					ld.write(getOutputFilename("legDistances.txt.gz"));
					ld.writeCharts(getOutputFilename("legDistances"));
				}
				if (clas != null) {
					clas.write(getOutputFilename("avgSpeed.txt.gz"));
					clas.writeChart(getOutputFilename("avgSpeedCityArea.png"));
				}
			}
		}

		public void notifyShutdown(final ShutdownEvent event) {
			try {
				ptRateWriter.close();
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
				orms = new EnRouteModalSplit(scenario,
				// nl,
						ps);
				es.addHandler(orms);
				ttms = new LegTravelTimeModalSplit(ps);
				es.addHandler(ttms);
				ld = new LegDistance(nl);
				es.addHandler(ld);
				clas = new CalcLinksAvgSpeed(nl, 682845.0, 247388.0, 2000.0);
				es.addHandler(clas);
				c.getConfig().simulation().setSnapshotPeriod(300);
			} else if (event.getIteration() == c.getFirstIteration())
				c.getConfig().simulation().setSnapshotPeriod(300);
			else
				c.getConfig().simulation().setSnapshotPeriod(0);
		}
	}

	// -------------------------MAIN FUNCTION--------------------
	/**
	 * @param args
	 *            - the path of config-file
	 */
	public static void main(final String[] args) {
		final PtcheckControler controler;
		controler = new PtcheckControler(args, "Zurich");
		controler.addControlerListener(new PtCheckListener(controler
				.getScenarioName()));
		controler.run();
		System.exit(0);
	}
}

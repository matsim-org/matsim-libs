/* *********************************************************************** *
 * project: org.matsim.*
 * PtRate.java
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

/**
 * 
 */
package playground.yu.analysis;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.charts.XYLineChart;

/**
 * An implementing of ContorlerListener, in order to output some information
 * about use of public transit through .txt-file and .png-picture
 * 
 * @author yu
 * 
 */
public class PtRate2 implements IterationEndsListener, ShutdownListener {
	// -----------------------------MEMBER VARIABLES-----------------------
	private final Population population;
	private final PtCheck2 check;
	private final int maxIters;
	private final String BetaTraveling;
	private final String BetaTravelingPt;
	/**
	 * @param yLicensedPtRate
	 *            - an array, in which the fraction of persons, who use public
	 *            transit, will be saved.
	 */
	private double[] yLicensedPtRate = null;
	private double[] yLicensedCarRate = null;
	/**
	 * @param yLicensedPtUser
	 *            - an array, in which the amount of persons, who use public
	 *            transit, will be saved.
	 */
	private double[] yLicensedPtUser = null;
	private double[] yLicensedCarUser = null;
	/**
	 * @param yPersons
	 *            - an array, in which the amount of all
	 */
	private double[] yPersons = null;
	private double[] yLicensed = null;

	private static final String SIMULATION = "simulation",
			STRATEGY = "strategy";

	// persons in the simulation will be
	// saved.
	// -------------------------------CONSTRUCTOR---------------------------
	/**
	 * @param population
	 *            - the object of Plans in the simulation
	 * @param filename
	 *            - filename of .txt-file
	 * @param maxIters
	 *            - maximum number of iterations
	 * @param BetaTraveling
	 *            - parameter of marginal Utility of Traveling
	 * @param BetaTravelingPt
	 *            - parameter of marginal Utility of Traveling with public
	 *            transit
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public PtRate2(final Population population, final String filename,
			int maxIters, String BetaTraveling, String BetaTravelingPt)
			throws FileNotFoundException, IOException {
		this.population = population;
		this.maxIters = maxIters;
		this.BetaTraveling = BetaTraveling;
		this.BetaTravelingPt = BetaTravelingPt;
		check = new PtCheck2(filename);
		yLicensedPtRate = new double[maxIters / 10 + 1];
		yLicensedCarRate = new double[maxIters / 10 + 1];
		yLicensedPtUser = new double[maxIters / 10 + 1];
		yLicensedCarUser = new double[maxIters / 10 + 1];
		yPersons = new double[maxIters / 10 + 1];
		yLicensed = new double[maxIters / 10 + 1];
	}

	/**
	 * writes .txt-file and paints 2 .png-picture
	 */
	public void notifyIterationEnds(IterationEndsEvent event) {
		int idx = event.getIteration();
		if (idx % 10 == 0) {
			Config cf = event.getControler().getConfig();
			check.resetCnt();
			check.run(population);
			yLicensedPtRate[idx / 10] = check.getLicensedPtRate();
			yLicensedCarRate[idx / 10] = check.getLicensedCarRate();
			yLicensedPtUser[idx / 10] = check.getLicensedPtUserCnt();
			yLicensedCarUser[idx / 10] = check.getLicensedCarUserCnt();
			yPersons[idx / 10] = check.getPersonCnt();
			yLicensed[idx / 10] = check.getLicensedCnt();
			try {
				check.write(idx);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (idx == maxIters) {
				double[] x = new double[maxIters + 1];
				for (int i = 0; i < maxIters / 10 + 1; i++) {
					x[i] = i * 10;
				}
				StringBuffer sb = new StringBuffer();
				for (StrategySettings ss : cf.strategy().getStrategySettings()) {
					sb.append(ss.getModuleName());
					sb.append('-');
					sb.append(ss.getProbability());
					sb.append(',');
					sb.append(' ');
				}
				XYLineChart ptRateChart = new XYLineChart("Schweiz: PtRate, "
						+ maxIters + "ITERs, BetaTraveling=" + BetaTraveling
						+ ", BetaTravelingPt=" + BetaTravelingPt
						+ ", BetaPerforming="
						+ cf.getParam("planCalcScore", "performing")
						+ ", flowCapacityFactor="
						+ cf.getParam(SIMULATION, "flowCapacityFactor")
						+ ", storageCapacityFactor="
						+ cf.getParam(SIMULATION, "storageCapacityFactor")
						+ ", " + sb, "Iterations", "Pt-Rate");
				ptRateChart.addSeries("licensedPtRate", x, yLicensedPtRate);
				ptRateChart.addSeries("licensedCarRate", x, yLicensedCarRate);
				ptRateChart.saveAsPng(
				    event.getControler().getControlerIO().getOutputFilename("PtRate.png"), 1024, 768);
				XYLineChart personsChart = new XYLineChart(
						"Schweiz: PtUser/Persons, "
								+ maxIters
								+ "ITERs, BetaTraveling="
								+ BetaTraveling
								+ ", BetaTravelingPt="
								+ BetaTravelingPt
								+ ", BetaPerforming="
								+ cf.getParam("planCalcScore", "performing")
								+ ", flowCapacityFactor="
								+ cf.getParam(SIMULATION, "flowCapacityFactor")
								+ ", storageCapacityFactor="
								+ cf.getParam(SIMULATION,
										"storageCapacityFactor") + ", "
								+ cf.getParam(STRATEGY, "ModuleProbability_2")
								+ "-" + cf.getParam(STRATEGY, "Module_2")
								+ ", "
								+ cf.getParam(STRATEGY, "ModuleProbability_3")
								+ "-" + cf.getParam(STRATEGY, "Module_3")
								+ ", "
								+ cf.getParam(STRATEGY, "ModuleProbability_1")
								+ "-" + cf.getParam(STRATEGY, "Module_1"),
						"Iterations", "Persons");
				personsChart.addSeries("licensedPtUser", x, yLicensedPtUser);
				personsChart.addSeries("licensedCarUser", x, yLicensedCarUser);
				personsChart.addSeries("licensedPersons", x, yLicensed);
				personsChart.addSeries("Persons", x, yPersons);
				personsChart.saveAsPng(event.getControler().getControlerIO()
						.getOutputFilename("Persons.png"), 1024, 768);

			}
		}
	}

	public void notifyShutdown(ShutdownEvent event) {
		try {
			check.writeEnd();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

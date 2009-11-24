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
import org.matsim.core.controler.Controler;
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
public class PtRate implements IterationEndsListener, ShutdownListener {
	// -----------------------------MEMBER VARIABLES-----------------------
	private final Population population;
	private final PtCheck check;
	private final int maxIters;
	private final String BetaTraveling;
	private final String BetaTravelingPt;
	private double[] yPtRate = null;// an array, in which the fraction of
									// persons, who use public transit, will be
									// saved.
	private double[] yPtUser = null;// an array, in which the amount of persons,
									// who use public transit, will be saved.
	private double[] yPersons = null;// an array, in which the amount of all
										// persons in the simulation will be
										// saved.
	private static final String SIMULATION = "simulation";

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
	public PtRate(final Population population, final String filename,
			final int maxIters, final String BetaTraveling,
			final String BetaTravelingPt) throws FileNotFoundException,
			IOException {
		this.population = population;
		this.maxIters = maxIters;
		this.BetaTraveling = BetaTraveling;
		this.BetaTravelingPt = BetaTravelingPt;
		check = new PtCheck(filename);
		yPtRate = new double[maxIters / 10 + 1];
		yPtUser = new double[maxIters / 10 + 1];
		yPersons = new double[maxIters / 10 + 1];
	}

	/**
	 * writes .txt-file and paints 2 .png-picture
	 */
	public void notifyIterationEnds(final IterationEndsEvent event) {
		int idx = event.getIteration();
		if (idx % 10 == 0) {
			Config cf = event.getControler().getConfig();
			check.resetCnt();
			check.run(population);
			yPtRate[idx / 10] = check.getPtRate();
			yPtUser[idx / 10] = check.getPtUserCnt();
			yPersons[idx / 10] = check.getPersonCnt();
			try {
				check.write(idx);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (idx == maxIters) {
				double[] x = new double[maxIters + 1];
				for (int i = 0; i < maxIters / 10 + 1; i++)
					x[i] = i * 10;
				XYLineChart ptRateChart = new XYLineChart("Schweiz: PtRate, "
						+ maxIters + "ITERs, BetaTraveling=" + BetaTraveling
						+ ", BetaTravelingPt=" + BetaTravelingPt
						+ ", BetaPerforming="
						+ cf.getParam("planCalcScore", "performing")
						+ ", flowCapacityFactor="
						+ cf.getParam(SIMULATION, "flowCapacityFactor")
						+ ", storageCapacityFactor="
						+ cf.getParam(SIMULATION, "storageCapacityFactor"),
						"Iterations", "Pt-Rate");
				ptRateChart.addSeries("PtRate", x, yPtRate);
				ptRateChart.saveAsPng(
						Controler.getOutputFilename("PtRate.png"), 800, 600);
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
										"storageCapacityFactor"), "Iterations",
						"PtUser/Persons");
				personsChart.addSeries("PtUser", x, yPtUser);
				personsChart.addSeries("Persons", x, yPersons);
				personsChart.saveAsPng(Controler
						.getOutputFilename("Persons.png"), 800, 600);

			}
		}
	}

	public void notifyShutdown(final ShutdownEvent event) {
		try {
			check.writeEnd();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

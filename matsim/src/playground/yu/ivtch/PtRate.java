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
package playground.yu.ivtch;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.plans.Plans;
import org.matsim.utils.charts.XYLineChart;

/**
 * An implementing of ContorlerListener, in order to output some information
 * about use of public transit through .txt-file and .png-picture
 *
 * @author yu
 *
 */
public class PtRate implements IterationEndsListener {
	// -----------------------------MEMBER VARIABLES-----------------------
	private final Plans population;
	private final PtCheck check;
	/** an array, in which the fraction of persons, who use public transit, will be saved. */
	private double[] yPtRate = null;
	/** an array, in which the amount of persons, who use public transit, will be saved. */
	private double[] yPtUser = null;
	/** an array, in which the amount of all persons in the simulation, will be saved. */
	private double[] yPersons = null;

	// -------------------------------CONSTRUCTOR---------------------------
	/**
	 * @param population -
	 *            the object of Plans in the simulation
	 * @param filename -
	 *            filename of .txt-file
	 * @param controler -
	 *            the controler to which this instance is attached to
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public PtRate(final Plans population, final String filename, final Controler controler)
			throws IOException {
		this.population = population;
		this.check = new PtCheck(filename);
		int maxIters = controler.getLastIteration();
		this.yPtRate = new double[maxIters + 1];
		this.yPtUser = new double[maxIters + 1];
		this.yPersons = new double[maxIters + 1];
	}

	/**
	 * writes .txt-file and paints 2 .png-picture
	 */
	public void notifyIterationEnds(final IterationEndsEvent event) {
		Config config = event.getControler().getConfig();
		this.check.resetCnt();
		this.check.run(this.population);
		int iteration = event.getIteration();
		this.yPtRate[iteration] = this.check.getPtRate();
		this.yPtUser[iteration] = this.check.getPtUserCnt();
		this.yPersons[iteration] = this.check.getPersonCnt();
		try {
			this.check.write(iteration);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (iteration == event.getControler().getLastIteration()) {
			double[] x = new double[iteration + 1];
			for (int i = 0; i <= iteration; i++) {
				x[i] = i;
			}
			XYLineChart ptRateChart = new XYLineChart(
					"Schweiz: PtRate, "
							+ iteration
							+ "ITERs, BetaTraveling=" + config.charyparNagelScoring().getTraveling()
							+ ", BetaTravelingPt=" + config.charyparNagelScoring().getTravelingPt()
							+ ", BetaPerforming=6, flowCapacityFactor=0.1, storageCapacityFactor=0.3, 10%-ReRoute_Landmarks, 10%-TimeAllocationMutator, 80%-SelectExpBeta",
					"Iterations", "Pt-Rate");
			ptRateChart.addSeries("PtRate", x, this.yPtRate);
			ptRateChart.saveAsPng(Controler.getOutputFilename("PtRate.png"), 800, 600);
			XYLineChart personsChart = new XYLineChart(
					"Schweiz: PtUser/Persons, "
							+ iteration
							+ "ITERs, BetaTraveling=" + config.charyparNagelScoring().getTraveling()
							+ ", BetaTravelingPt=" + config.charyparNagelScoring().getTravelingPt()
							+ ", BetaPerforming=6, flowCapacityFactor=0.1, storageCapacityFactor=0.3, 10%-ReRoute_Landmarks, 10%-TimeAllocationMutator, 80%-SelectExpBeta",
					"Iterations", "PtUser/Persons");
			personsChart.addSeries("PtUser", x, this.yPtUser);
			personsChart.addSeries("Persons", x, this.yPersons);
			personsChart.saveAsPng(Controler.getOutputFilename("Persons.png"), 800, 600);
			try {
				this.check.writeEnd();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

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
package playground.yu;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.controler.events.ControlerFinishIterationEvent;
import org.matsim.controler.listener.ControlerFinishIterationListener;
import org.matsim.plans.Plans;
import org.matsim.utils.charts.XYLineChart;

/**
 * @author yu
 * 
 */
public class PtRate implements ControlerFinishIterationListener {

	private final Plans population;
	private final PtCheck check;
	private final int maxIters;
	private final String BetaTraveling;
	private final String BetaTravelingPt;
	private double[] yPtRate = null;
	private double[] yPtUser = null;
	private double[] yPersons = null;

	/**
	 * @throws IOException
	 * @throws FileNotFoundException
	 * 
	 */
	public PtRate(final Plans population, final String filename, int maxIters,
			String BetaTraveling, String BetaTravelingPt)
			throws FileNotFoundException, IOException {
		this.population = population;
		this.maxIters = maxIters;
		this.BetaTraveling = BetaTraveling;
		this.BetaTravelingPt = BetaTravelingPt;
		check = new PtCheck(filename);
		yPtRate = new double[maxIters + 1];
		yPtUser = new double[maxIters + 1];
		yPersons = new double[maxIters + 1];
	}

	public void notifyIterationFinished(ControlerFinishIterationEvent event) {
		check.resetCnt();
		check.run(population);
		int idx = event.getIteration();
		yPtRate[idx] = check.getPtRate();
		yPtUser[idx] = check.getPtUserCnt();
		yPersons[idx] = check.getPersonCnt();
		try {
			check.write(idx);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (idx == maxIters) {
			double[] x = new double[maxIters + 1];
			for (int i = 0; i < maxIters + 1; i++) {
				x[i] = i;
			}
			XYLineChart ptRateChart = new XYLineChart(
					"Schweiz: PtRate, "
							+ maxIters
							+ "ITERs, BetaTraveling="
							+ BetaTraveling
							+ ", BetaTravelingPt="
							+ BetaTravelingPt
							+ ", BetaPerforming=6, flowCapacityFactor=0.1, storageCapacityFactor=0.5, 5%-ReRoute_Landmarks, 10%-TimeAllocationMutator, 85%-SelectExpBeta",
					"Iterations", "Pt-Rate");
			ptRateChart.addSeries("PtRate", x, yPtRate);
			ptRateChart.saveAsPng(Controler.getOutputFilename("PtRate.png"),
					800, 600);
			XYLineChart personsChart = new XYLineChart(
					"Schweiz: PtUser/Persons, "
							+ maxIters
							+ "ITERs, BetaTravelling="
							+ BetaTraveling
							+ ", BetaTravelingPt="
							+ BetaTravelingPt
							+ ", BetaPerforming=6, flowCapacityFactor=0.1, storageCapacityFactor=0.5, 5%-ReRoute_Landmarks, 10%-TimeAllocationMutator, 85%-SelectExpBeta",
					"Iterations", "PtUser/Persons");
			personsChart.addSeries("PtUser", x, yPtUser);
			personsChart.addSeries("Persons", x, yPersons);
			personsChart.saveAsPng(Controler.getOutputFilename("Persons.png"),
					800, 600);
			try {
				check.writeEnd();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}

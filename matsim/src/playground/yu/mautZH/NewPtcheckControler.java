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

package playground.yu.mautZH;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.matsim.analysis.CalcAverageTolledTripLength;
import org.matsim.analysis.CalcAverageTripLength;
import org.matsim.analysis.ScoreStats;
import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;

import playground.yu.analysis.CalcAvgSpeed;
import playground.yu.analysis.CalcTrafficPerformance;

/**
 * test of PtCheck and PtRate, outputs Public-Transit user fraction
 * 
 * @author ychen
 * 
 */
public class NewPtcheckControler extends Controler {
	/**
	 * internal outputStream
	 */
	private DataOutputStream out;
	private ScoreStats scoreStats = null;
	private CalcAverageTolledTripLength cattl = null;
	private CalcAverageTripLength catl = null;
	private CalcTrafficPerformance ctpf = null;
	private CalcAvgSpeed cas = null;

	/**
	 * adds a ControlerListener to Controler - PtRate
	 */
	@Override
	protected void loadData() {
		loadWorld();
		this.network = loadNetwork();
		loadFacilities();
		this.population = loadPopulation();
		try {
			// TODO [MR] I "abuse" createLegHistogramPNG here for ScoreStats...
			// create an own flag for this one.
			scoreStats = new ScoreStats(this.population,
					getOutputFilename("scorestats.txt"), true);
			addControlerListener(new PtRate(population,
					getOutputFilename("PtRate.txt"), getMaximumIteration(),
					config.getParam("planCalcScore", "traveling"), config
							.getParam("planCalcScore", "travelingPt")));
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
		if (scoreStats != null) {
			this.addControlerListener(scoreStats);
		}
	}

	@Override
	protected void setupIteration(int iteration) {
		super.setupIteration(iteration);
		cas.reset(iteration);
		cattl.reset(iteration);
		ctpf.reset(iteration);
	}

	@Override
	protected void finishIteration(int iteration) {
		super.finishIteration(iteration);
		catl = new CalcAverageTripLength();
		catl.run(population);
		try {
			out.writeBytes(iteration
					+ "\t"
					+ config.getParam("planCalcScore", "traveling")
					+ "\t"
					+ config.getParam("planCalcScore", "travelingPt")
					+ "\t"
					+ ((Gbl.useRoadPricing()) ? toll.getCostArray()[0].amount
							: 0)
					+ "\t"
					+ ((tollCalc != null) ? tollCalc.getAllAgentsToll() : 0.0)
					+ "\t"
					+ scoreStats.getHistory()[3][iteration]
					+ "\t"
					+ ((tollCalc != null) ? tollCalc.getDraweesNr() : 0)
					+ "\t"
					+ catl.getAverageTripLength()
					+ "\t"
					+ ((((tollCalc != null) && (cattl != null))) ? cattl
							.getAverageTripLength() : 0.0) + "\t"
					+ ctpf.getTrafficPerformance() + "\t" + cas.getAvgSpeed()
					+ "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void startup() {
		super.startup();
		if (Gbl.useRoadPricing()) {
			cattl = new CalcAverageTolledTripLength(network, toll);
			events.addHandler(cattl);
		}
		ctpf = new CalcTrafficPerformance(network);
		cas = new CalcAvgSpeed(network);
		events.addHandler(ctpf);
		events.addHandler(cas);
	}

	// -------------------------MAIN FUNCTION--------------------
	/**
	 * @param args -
	 *            the path of config-file
	 */
	public static void main(final String[] args) {
		final NewPtcheckControler controler;
		controler = new NewPtcheckControler();
		controler.run(args);
		try {
			controler.out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}

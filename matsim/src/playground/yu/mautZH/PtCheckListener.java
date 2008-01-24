/* *********************************************************************** *
 * project: org.matsim.*
 * PtCheckListener.java
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
package playground.yu.mautZH;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.matsim.analysis.CalcAverageTolledTripLength;
import org.matsim.analysis.CalcAverageTripLength;
import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.controler.corelisteners.RoadPricing;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Plans;
import org.matsim.utils.charts.XYLineChart;

import playground.yu.analysis.CalcAvgSpeed;
import playground.yu.analysis.CalcTrafficPerformance;

/**
 * @author ychen
 * 
 */
public class PtCheckListener implements IterationEndsListener, StartupListener,
		IterationStartsListener {
	/**
	 * internal outputStream
	 */
	private DataOutputStream out;
	private CalcAverageTolledTripLength cattl;
	private Controler c;
	private RoadPricing rp;
	private CalcTrafficPerformance ctp;
	private NetworkLayer network;
	private Events events;
	private CalcAvgSpeed cas;
	private CalcAverageTripLength catl;
	private PtCheck check;
	private Plans plans;
	private double[] yPtRate = null;// an array, in which the fraction of
	// persons, who use public transit, will be
	// saved.
	private double[] yPtUser = null;// an array, in which the amount of persons,
	// who use public transit, will be saved.
	private double[] yPersons = null;// an array, in which the amount of all
	// persons in the simulation will be
	// saved.
	private int maxIters;
	private String BetaTraveling, BetaTravelingPt;
	private Config cf;

	public PtCheckListener(Controler c) {
		this.c = c;
		maxIters = c.getLastIteration();
		yPtRate = new double[maxIters + 1];
		yPtUser = new double[maxIters + 1];
		yPersons = new double[maxIters + 1];
	}

	public void notifyIterationEnds(IterationEndsEvent event) {
		plans = c.getPopulation();
		check.resetCnt();
		check.run(plans);
		int idx = Controler.getIteration();
		yPtRate[idx] = check.getPtRate();
		yPtUser[idx] = check.getPtUserCnt();
		yPersons[idx] = check.getPersonCnt();
		try {
			check.write(idx);
		} catch (IOException e) {
			e.printStackTrace();
		}
		cf = c.getConfig();
		BetaTraveling = cf.getParam("planCalcScore", "traveling");
		BetaTravelingPt = cf.getParam("planCalcScore", "travelingPt");
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
				e.printStackTrace();
			}
		}
		catl = new CalcAverageTripLength();
		catl.run(plans);
		
		try {
			out.writeBytes(Controler.getIteration() + "\t"
					+ BetaTraveling + "\t"
					+ BetaTravelingPt + "\t"
					/*
					 * + ((Gbl.useRoadPricing()) ? //
					 * toll.getCostArray()[0].amount: 0)+ "\t" // + ((tollCalc !=
					 * null) ? tollCalc.getAllAgentsToll() : // 0.0)+ "\t" // +
					 * scoreStats.getHistory()[3][iteration]+ "\t" // +
					 * ((tollCalc != null) ? tollCalc.getDraweesNr() : 0)+ //
					 * "\t"
					 */
					+ catl.getAverageTripLength() + "\t"
					/*
					 * + ((((tollCalc != null) && (cattl != null))) ? //
					 * cattl.getAverageTripLength() : 0.0) + "\t"
					 */
					+ ctp.getTrafficPerformance() + "\t" + cas.getAvgSpeed()
					+ "\n");
			if(idx==maxIters){
				out.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void notifyStartup(StartupEvent event) {
		try {
			check = new PtCheck(Controler.getOutputFilename("PtRate.txt"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			out = new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(new File(Controler
							.getOutputFilename("tollPaid.txt")))));
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
		network = c.getNetwork();
		events = c.getEvents();
		if (Gbl.useRoadPricing()) {
			/*
			 * TODO cattl=new //
			 * CalcAverageTolledTripLength(network,RoadPricingScheme scheme); //
			 * TODO events.addHandler(cattl);
			 */
		}
		ctp = new CalcTrafficPerformance(network);
		events.addHandler(ctp);
		cas = new CalcAvgSpeed(network);
		events.addHandler(cas);
	}

	public void notifyIterationStarts(IterationStartsEvent event) {
		int it = Controler.getIteration();
		cas.reset(it);
		if (Gbl.useRoadPricing()) {
			cattl.reset(it);
		}
		ctp.reset(it);
	}

}

/*******************************************************************************
 * Copyright (c) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
 ******************************************************************************/
package org.matsim.contrib.freight.vrp.algorithms.rr;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.algorithms.rr.listener.AlgorithmEndsListener;
import org.matsim.contrib.freight.vrp.algorithms.rr.listener.IterationEndsListener;
import org.matsim.core.utils.charts.XYLineChart;

/**
 * 
 * @author stefan schroeder
 * 
 */

public class RuinAndRecreateChartListener implements IterationEndsListener,
		AlgorithmEndsListener {

	private static Logger log = Logger
			.getLogger(RuinAndRecreateChartListener.class);

	private double[] bestResults;

	private double[] tentativeResults;

	private List<Double> bestResultList = new ArrayList<Double>();

	private List<Double> tentativeResultList = new ArrayList<Double>();

	private String filename;

	public RuinAndRecreateChartListener(String filename) {
		super();
		this.filename = filename;
	}

	public RuinAndRecreateChartListener() {
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	@Override
	public void informIterationEnds(int currentIteration,
			RuinAndRecreateSolution awardedSolution,
			RuinAndRecreateSolution rejectedSolution) {
		// System.out.println()
		bestResultList.add(awardedSolution.getResult());
		tentativeResultList.add(rejectedSolution.getResult());
	}

	@Override
	public void informAlgorithmEnds(RuinAndRecreateSolution currentSolution) {
		log.info("create chart " + filename);
		bestResults = new double[bestResultList.size()];
		tentativeResults = new double[tentativeResultList.size()];
		double[] mutation = new double[bestResultList.size()];
		for (int i = 0; i < bestResultList.size(); i++) {
			bestResults[i] = bestResultList.get(i);
			tentativeResults[i] = tentativeResultList.get(i);
			mutation[i] = i + 1;
		}
		XYLineChart chart = new XYLineChart("Results", "mutation", "costs");
		chart.addSeries("bestResults", mutation, bestResults);
		chart.addSeries("tentativeResults", mutation, tentativeResults);
		chart.saveAsPng(filename, 800, 600);
	}

}

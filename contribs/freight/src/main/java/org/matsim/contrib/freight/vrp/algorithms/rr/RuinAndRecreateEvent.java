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





/**
 * Collector of algo-information. Can be listened to.
 * 
 * @author stefan schroeder
 *
 */

public class RuinAndRecreateEvent {
	
	private int currentMutation;
	
	private double tentativeSolution;
	
	private double currentResult;
	
	private double threshold;
	
	private boolean solutionAccepted;
	
	private RRSolution currentSolution;

	public RuinAndRecreateEvent(int currentMutation, double tentativeSolution,
			double currentResult, double currentThreshold, boolean solutionAccepted) {
		super();
		this.currentMutation = currentMutation;
		this.tentativeSolution = tentativeSolution;
		this.threshold = currentThreshold;
		this.solutionAccepted = solutionAccepted;
		this.currentResult = currentResult;
	}

	public RRSolution getCurrentSolution() {
		return currentSolution;
	}

	public void setCurrentSolution(RRSolution rrSolution) {
		this.currentSolution = rrSolution;
	}

	public double getCurrentResult() {
		return currentResult;
	}

	public int getCurrentMutation() {
		return currentMutation;
	}

	public double getTentativeSolution() {
		return tentativeSolution;
	}

	public double getThreshold() {
		return threshold;
	}

	public boolean isSolutionAccepted() {
		return solutionAccepted;
	}
	
	@Override
	public String toString() {
		return "[currentMutation=" + currentMutation + "][tentativeSolution=" + tentativeSolution + "][currentSolution=" + currentResult + "][currentThreshold=" + threshold + "][isAccepted=" + solutionAccepted + "]";
	}

}

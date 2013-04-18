/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioParams.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.spacetimegeo;

/**
 * @author benjamin
 *
 */
public class ScenarioParams {
	
	Double mu;
	
	Double betaCost;
	
	Double betaPerf;
	Double betaLate;
	Double betaTraveling;

	String choiceModule;

	
	protected Double getMu() {
		return mu;
	}

	protected void setMu(Double mu) {
		this.mu = mu;
	}

	protected Double getBetaCost() {
		return betaCost;
	}

	protected void setBetaCost(Double betaCost) {
		this.betaCost = betaCost;
	}

	protected Double getBetaPerf() {
		return betaPerf;
	}

	protected void setBetaPerf(Double betaPerf) {
		this.betaPerf = betaPerf;
	}

	protected Double getBetaLate() {
		return betaLate;
	}

	protected void setBetaLate(Double betaLate) {
		this.betaLate = betaLate;
	}

	protected Double getBetaTraveling() {
		return betaTraveling;
	}

	protected void setBetaTraveling(Double betaTraveling) {
		this.betaTraveling = betaTraveling;
	}

	protected String getChoiceModule() {
		return choiceModule;
	}

	protected void setChoiceModule(String choiceModule) {
		this.choiceModule = choiceModule;
	}

}

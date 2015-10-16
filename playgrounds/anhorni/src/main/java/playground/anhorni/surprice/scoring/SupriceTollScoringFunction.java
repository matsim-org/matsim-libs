/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.surprice.scoring;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.ScoringFunctionAccumulator.BasicScoring;
import org.matsim.core.scoring.ScoringFunctionAccumulator.MoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import playground.anhorni.surprice.analysis.Analyzer;

public class SupriceTollScoringFunction implements MoneyScoring, BasicScoring {

	protected double score;
	private static final double INITIAL_SCORE = 0.0;
	private Person person;
	private String day;
	protected final CharyparNagelScoringParameters params;
	private double dudm;

	public SupriceTollScoringFunction(final CharyparNagelScoringParameters params, Person person, String day, double dudm) {
		this.params = params;
		this.person = person;
		this.day = day;
		this.dudm = dudm;
		this.reset();
	}

	@Override
	public void reset() {
		this.score = INITIAL_SCORE;
		this.person.getCustomAttributes().put(day + ".tollScore", null);
	}

	@Override
	public void addMoney(final double amount) {
		//this.score -= amount * this.dudm;
		this.score +=  amount * this.dudm; // amount comes negative!
		
		double prevVal = 0.0;
		if (this.person.getCustomAttributes().get(day + ".tollScore") != null) {
			prevVal = (Double)this.person.getCustomAttributes().get(day + ".tollScore");
		}		
		this.person.getCustomAttributes().put(day + ".tollScore", prevVal + amount * this.dudm);
	}

	@Override
	public void finish() {
	}

	@Override
	public double getScore() {
		return this.score;
	}
}

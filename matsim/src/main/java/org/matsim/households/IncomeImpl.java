/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.households;



/**
 * @author dgrether
 */
public class IncomeImpl implements Income {

	private String currency;
	private IncomePeriod period;
	private double income;
	
	public IncomeImpl(double income, IncomePeriod period) {
		this.period = period;
		this.income = income;
	}
	
	@Override
	public String getCurrency() {
		return this.currency;
	}

	@Override
	public double getIncome() {
		return this.income;
	}

	@Override
	public IncomePeriod getIncomePeriod() {
		return this.period;
	}

	@Override
	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public void setIncome(double income, IncomePeriod period) {
		this.income = income;
	}
	
}

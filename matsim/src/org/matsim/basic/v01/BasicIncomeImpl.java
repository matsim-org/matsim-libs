/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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
package org.matsim.basic.v01;

import org.matsim.interfaces.basic.v01.BasicIncome;


/**
 * @author dgrether
 *
 */
public class BasicIncomeImpl implements BasicIncome {

	private String currency;
	private IncomePeriod period;
	private double income;
	
	public BasicIncomeImpl(IncomePeriod period) {
		this.period = period;
	}
	
	public String getCurrency() {
		return this.currency;
	}

	public double getIncome() {
		return this.income;
	}

	public IncomePeriod getIncomePeriod() {
		return this.period;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public void setIncome(double income) {
		this.income = income;
	}
	
}

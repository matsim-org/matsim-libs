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

package org.matsim.contrib.parking.parkingchoice.lib.obj;

public class Pair<FirstValue,SecondValue> {

	private FirstValue fistValue;
	private SecondValue secondValue;

	public Pair(FirstValue fistValue,SecondValue secondValue){
		this.setFistValue(fistValue);
		this.setSecondValue(secondValue);
	}

	public FirstValue getFistValue() {
		return fistValue;
	}

	public void setFistValue(FirstValue fistValue) {
		this.fistValue = fistValue;
	}

	public SecondValue getSecondValue() {
		return secondValue;
	}

	public void setSecondValue(SecondValue secondValue) {
		this.secondValue = secondValue;
	}
	
}

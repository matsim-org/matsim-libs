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

package playground.anhorni.csestimation;

public class PersonModes {
	private boolean modesForWorking [] = {false, false, false, false};
	private int modesForShopping [] = new int[4];
	
	public void setModeForWorking(int index, boolean value) {
		this.modesForWorking[index] = value;
	}
	public void setModesForShopping(int index, int value) {
		this.modesForShopping[index] = value;
	}
	public boolean[] getModesForWorking() {
		return modesForWorking;
	}
	public int[] getModesForShopping() {
		return modesForShopping;
	}
}

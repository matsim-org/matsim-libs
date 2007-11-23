/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdI.java
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

package playground.lnicolas.ktiProject;

/**
 * Household interface. Simulates a household.
 * @author lnicolas
 *
 */
public interface HouseholdI {

	public int getPersonCount();
	
	public int getKidCount();
	
	public double getIncome();
	
	public void setIncome(double income);
}

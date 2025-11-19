/* *********************************************************************** *
 * project: org.matsim.*
 * HbefaTrafficSituation.java
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
package org.matsim.contrib.emissions;

/**
 * @author benjamin
 *
 * The <code>STOPANDGO_HEAVY</code> was introduced from HBEFA 4.1 (jwj, Nov'2020).
 */
 enum HbefaTrafficSituation {

	FREEFLOW, HEAVY, SATURATED, STOPANDGO, STOPANDGO_HEAVY;

	HbefaTrafficSituation(){
	}

	// TODO Check if works properly and replace unnecessary methods by this method
	public HbefaTrafficSituation getLower(){
		switch(this){
			case FREEFLOW -> {
				return HEAVY;
			}
			case HEAVY -> {
				return SATURATED;
			}
			case SATURATED -> {
				return STOPANDGO;
			}
			case STOPANDGO -> {
				return STOPANDGO_HEAVY;
			}
			default -> {
				return null;
			}
		}
	}
}

/* *********************************************************************** *
 * project: org.matsim.*
 * DaganzoRunAll
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
package playground.dgrether.daganzosignal;


/**
 * @author dgrether
 *
 */
public class DaganzoRunAll {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			new DaganzoScenarioGenerator().createScenario();
			new DaganzoRunner().runScenario(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}

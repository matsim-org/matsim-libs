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
package playground.droeder.eMobility.IO;

import org.apache.log4j.Logger;

import playground.droeder.eMobility.EmobilityScenario;

/**
 * @author droeder
 *
 */
public class AdditionalPlansReader {
	private static final Logger log = Logger
			.getLogger(AdditionalPlansReader.class);
	
	 private EmobilityScenario eSc;

	public AdditionalPlansReader(EmobilityScenario sc){
		 this.eSc = sc;
		 if(this.eSc.getSc() == null){
			 throw new RuntimeException("need MatsimSceanrio...");
		 }
	 }
	
	public void readAppointments(String matsimPlan, String appointmentsFile){
		
	}

}

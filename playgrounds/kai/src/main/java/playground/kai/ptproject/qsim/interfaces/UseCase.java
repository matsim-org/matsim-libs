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

package playground.kai.ptproject.qsim.interfaces;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.ptproject.qsim.PersonAgent;
import org.matsim.ptproject.qsim.PersonAgentI;
import org.matsim.ptproject.qsim.QVehicle;
import org.matsim.ptproject.qsim.QueueVehicleImpl;

/**
 * @author nagel
 *
 */
public class UseCase {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario sc = new ScenarioImpl() ;
		Person person = sc.getPopulation().getFactory().createPerson(null) ;
		PersonAgentI qperson = new PersonAgent( person, null ) ;
		MobsimActivityFacility actFac = new MobsimActivityFacility() ;
		actFac.addPerson( qperson ) ;
		
		QVehicle veh = new QueueVehicleImpl(null) ;
		Parking parking = new Parking() ;
		parking.addEmptyVehicle( veh ) ;
		
		actFac.selfUpdate() ;
		
		
		
	}

}

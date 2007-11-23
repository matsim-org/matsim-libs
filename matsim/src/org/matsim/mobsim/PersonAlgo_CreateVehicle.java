/* *********************************************************************** *
 * project: org.matsim.*
 * PersonAlgo_CreateVehicle.java
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

package org.matsim.mobsim;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;

public class PersonAlgo_CreateVehicle extends PersonAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// run Method, creates a new Vehicle for every person
	//////////////////////////////////////////////////////////////////////

	/* (non-Javadoc)
	 * @see org.matsim.plans.algorithms.PersonAlgorithm#run(org.matsim.plans.Person)
	 */
	public void run(Person person) {
		Plan plan = person.getSelectedPlan();
		if (plan != null) {
			Vehicle veh = new Vehicle();
			veh.setActLegs(plan.getActsLegs());
			veh.setDriverID(person.getId().toString());
			veh.setDriver(person);
			veh.initVeh();
		}
	}

	// this function is just for testing the serialize code, not of any other use!
	// TODO [DS] well, if it's for testing only, can't we move this somewhere else? [MR, jan07]
	public static Object deepCopy( Object o ) throws Exception
	{
	  ByteArrayOutputStream baos = new ByteArrayOutputStream();
	  new ObjectOutputStream( baos ).writeObject( o );

	  ByteArrayInputStream bais = new ByteArrayInputStream( baos.toByteArray() );

	  return  new ObjectInputStream(bais).readObject();
	}

}

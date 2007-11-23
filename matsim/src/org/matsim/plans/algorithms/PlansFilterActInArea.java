/* *********************************************************************** *
 * project: org.matsim.*
 * PlansFilterActInArea.java
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

package org.matsim.plans.algorithms;

import java.util.Iterator;

import org.matsim.network.NetworkLayer;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;

/**
 * @author laemmel
 *
 */
public class PlansFilterActInArea extends PersonAlgorithm {

	private static NetworkLayer network;
	private static Plans plans;
	private static String actType;
	
	public PlansFilterActInArea(NetworkLayer net, String a){
		network = net;
		actType = a;
		plans =  new Plans();
	}
	
	public void setArea(String a){
		actType = a;
	}
	
	@Override
	public void run(Person person) {
		Iterator it = person.getSelectedPlan().getIteratorAct();
		while (it.hasNext()){
			Act act = (Act) it.next();
			String type = act.getType();
			if (type.contains(actType)) {
				if(network.getLink(act.getLink().getId().toString()) != null) {
					try {
						plans.addPerson(person);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				
				}
				return;
			}
		}
		
	}
	
	public Plans getPlans(){
		return plans;
	}

}

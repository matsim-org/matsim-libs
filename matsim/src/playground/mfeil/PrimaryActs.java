/* *********************************************************************** *
 * project: org.matsim.*
 * PrimaryActs.java
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

package playground.mfeil;

import org.matsim.facilities.Activity;
import java.util.ArrayList;

/**
 * @author Matthias Feil
 * Provides the primary acts of a plan at runtime. As the corresponding 
 * Knowledge class uses a for-loop this class calls the Knowledge class
 * only once and then stores the list rather than calling the Knowledge
 * class again and again.
 */

public class PrimaryActs {
	private final ArrayList<Activity> primActs;
	
	
	public PrimaryActs (PlanomatXPlan plan){
		this.primActs = plan.getPerson().getKnowledge().getActivities(true);
	}
		
	public ArrayList<Activity> getPrimaryActs (){
		return this.primActs;
	}
}

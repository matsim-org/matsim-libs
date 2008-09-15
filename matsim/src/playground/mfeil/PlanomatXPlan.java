/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatXPlan.java
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

import org.matsim.population.*;


/**
 * @author Matthias Feil
 * Extends the standard Plan object to enable sorting of arrays or lists of Plans after their scores.
 * Use 
 * java.util.Arrays.sort (nameOfArray[]) 
 * or
 * java.util.Collections.sort (nameOfList<>).
 * See e.g., PlanomatX class.
 */
public class PlanomatXPlan extends Plan implements Comparable<PlanomatXPlan>{

	public PlanomatXPlan (Person person){
		super(person);
	}
	
	public final int compareTo(PlanomatXPlan p){
		if (this.getScore() == p.getScore()) {
			return 0;
		}
		else if (this.getScore() - p.getScore() > 0.0){
			return 1;
		}
		else return -1;
	}
}

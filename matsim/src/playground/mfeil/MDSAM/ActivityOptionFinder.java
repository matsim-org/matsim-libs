/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityOptionFinder.java
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

package playground.mfeil.MDSAM;


import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;

import org.matsim.core.api.facilities.ActivityFacilities;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.api.facilities.ActivityOption;
import org.matsim.core.facilities.algorithms.AbstractFacilityAlgorithm;




/**
 * Class that searches all facilities of a given facilities layer (e.g., the facilities of a scenario) 
 * for activity options. Returns a list of all found activity options.
 * 
 * @author Matthias Feil
 */
public class ActivityOptionFinder extends AbstractFacilityAlgorithm {
	
	private List<String> actTypes = new ArrayList<String>();
	
	public void run (final ActivityFacilities facilities) {
		for (ActivityFacility f : facilities.getFacilities().values()) {
			run(f);
		}
	}
	public void run(ActivityFacility facility){
		Collection<ActivityOption> facActTypes = facility.getActivityOptions().values();
		for (Iterator<ActivityOption> iterator = facActTypes.iterator();iterator.hasNext();){
			ActivityOption act = iterator.next();
			if (!this.actTypes.contains(act.getType())){
				this.actTypes.add(act.getType());
			}
		}
	}
	public List<String> getActTypes (){
		return this.actTypes;
	}
	
}

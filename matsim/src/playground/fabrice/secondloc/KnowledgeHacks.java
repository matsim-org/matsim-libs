/* *********************************************************************** *
 * project: org.matsim.*
 * KnowledgeHacks.java
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

package playground.fabrice.secondloc;

import java.util.HashMap;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Knowledge;

public class KnowledgeHacks {

	// This class is doomed to disappear
	// when Knowledge will be refactored
	
	Knowledge knowledge;
	
	public KnowledgeHacks( Knowledge knowledge ){ 
		this.knowledge = knowledge;
	}
	
	public void learn( CoolPlace place ){
		// increase the mental map
		knowledge.map.learn( place );
	}
	
	public void init( HashMap<Activity,CoolPlace> facool ){
		TreeMap<String, ActivityFacilities> actfacs = knowledge.getActivityFacilities();
		for( String type : actfacs.keySet() ){
			ActivityFacilities actfac = actfacs.get(type);		
			TreeMap<Id, Facility> facilities = actfac.getFacilities();
			for( Facility facility : facilities.values()){
				Activity activity = facility.getActivity( type );
				CoolPlace coolplace = facool.get(activity );
				if( coolplace == null )
					Gbl.errorMsg(new Exception("Coolplace could not be found"));
				knowledge.map.learn( coolplace );
			}
		}
	}
}

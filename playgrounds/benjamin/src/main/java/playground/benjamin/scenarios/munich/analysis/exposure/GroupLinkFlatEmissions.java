/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.benjamin.scenarios.munich.analysis.exposure;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * 
 * @author julia
 *
 */
public class GroupLinkFlatEmissions {

	private HashMap<UserGroup, HashMap<Id<Link>, Double>> userGroup2link2flatEmissionCosts;
	public GroupLinkFlatEmissions(){
		this.userGroup2link2flatEmissionCosts = new HashMap<UserGroup, HashMap<Id<Link>, Double>>();
		for(UserGroup ug: UserGroup.values()){
			userGroup2link2flatEmissionCosts.put(ug, new HashMap<Id<Link>, Double>());
		}
	}
	
	public void addEmissionCosts(UserGroup userGroup, Id<Link> linkId,
			double emissionCost) {
		if(userGroup2link2flatEmissionCosts.get(userGroup).containsKey(linkId)){
			double prevValue = userGroup2link2flatEmissionCosts.get(userGroup).get(linkId);
			userGroup2link2flatEmissionCosts.get(userGroup).put(linkId, (prevValue+emissionCost));
		}else{
			userGroup2link2flatEmissionCosts.get(userGroup).put(linkId, emissionCost);
		}
		
	}

	public Map<Id<Link>, Double> getLinks2FlatEmissionsFromCausingUserGroup(UserGroup ug){
		return userGroup2link2flatEmissionCosts.get(ug);
	}
	
	public Double getUserGroupCosts(UserGroup userGroup) {
		Double userGroupCosts =0.0;
		for(Id<Link> linkId: userGroup2link2flatEmissionCosts.get(userGroup).keySet()){
			userGroupCosts +=userGroup2link2flatEmissionCosts.get(userGroup).get(linkId);
		}
		return userGroupCosts;
	}

}

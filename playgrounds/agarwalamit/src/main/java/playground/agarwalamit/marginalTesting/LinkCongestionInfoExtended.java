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
package playground.agarwalamit.marginalTesting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.emissions.types.WarmPollutant;

import playground.ikaddoura.internalizationCar.LinkCongestionInfo;

/**
 * @author amit
 */
public class LinkCongestionInfoExtended extends LinkCongestionInfo{

	private List<Id> enteringAgents = new ArrayList<Id>();
	private Map<Id, Double> personId2DelaysToPayFor = new HashMap<Id, Double>();
	private Map<Id,Map<WarmPollutant, Double>> personId2WarmEmissionsToPayFor = new HashMap<Id, Map<WarmPollutant,Double>>();
	private Map<Id, Id> personId2CausingLinkId = new HashMap<Id, Id>();
	
	public List<Id> getEnteringAgents() {
		return enteringAgents;
	}
	
	public Map<Id , Double> getPersonId2DelaysToPayFor(){
		return personId2DelaysToPayFor;
	}
	
	public Map<Id,Map<WarmPollutant, Double>> getPersonId2WarmEmissionsToPayFor(){
		return personId2WarmEmissionsToPayFor;
	}
	
	public Map<Id, Id> getPersonId2CausingLinkId(){
		return personId2CausingLinkId;
	}
}

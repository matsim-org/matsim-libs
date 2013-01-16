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

package playground.andreas.P2.operator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.pbox.PFranchise;

/**
 * 
 * @author aneumann
 *
 */
public class CooperativeFactory {
	
	private final static Logger log = Logger.getLogger(CooperativeFactory.class);
	
	private final PConfigGroup pConfig;
	private final PFranchise franchise;
	
	public CooperativeFactory(PConfigGroup pConfig, PFranchise franchise){
		this.pConfig = pConfig;
		this.franchise = franchise;
	}
	
	public Cooperative createNewCooperative(Id id){
		if(this.pConfig.getCoopType().equalsIgnoreCase(BasicCooperative.COOP_NAME)){
			return new BasicCooperative(id, this.pConfig, this.franchise);
		} else if(this.pConfig.getCoopType().equalsIgnoreCase(InitCooperative.COOP_NAME)){
			return new InitCooperative(id, this.pConfig, this.franchise);
		} else if(this.pConfig.getCoopType().equalsIgnoreCase(ExtendAndReduceCooperative.COOP_NAME)){
			return new ExtendAndReduceCooperative(id, this.pConfig, this.franchise);
		} else if(this.pConfig.getCoopType().equalsIgnoreCase(MultiPlanCooperative.COOP_NAME)){
			return new MultiPlanCooperative(id, this.pConfig, this.franchise);
		} else if(this.pConfig.getCoopType().equalsIgnoreCase(CarefulMultiPlanCooperative.COOP_NAME)){
			return new CarefulMultiPlanCooperative(id, this.pConfig, this.franchise);
		} else {
			log.error("There is no coop type specified. " + this.pConfig.getCoopType() + " unknown");
			return null;
		}
	}
}
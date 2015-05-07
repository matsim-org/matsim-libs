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

package org.matsim.contrib.minibus.operator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.minibus.PConfigGroup;

/**
 * 
 * @author aneumann
 *
 */
final class OperatorFactory {
	
	private final static Logger log = Logger.getLogger(OperatorFactory.class);
	
	private final PConfigGroup pConfig;
	private final PFranchise franchise;
	private final WelfareAnalyzer welfareAnalyzer;
	
	public OperatorFactory(PConfigGroup pConfig, PFranchise franchise){
		this.pConfig = pConfig;
		this.franchise = franchise;
		this.welfareAnalyzer = null;
	}
	
	public OperatorFactory(PConfigGroup pConfig, PFranchise franchise, WelfareAnalyzer welfareAnalyzer){
		this.pConfig = pConfig;
		this.franchise = franchise;
		this.welfareAnalyzer = welfareAnalyzer;
	}
	
	public Operator createNewOperator(Id<Operator> id){
		if(this.pConfig.getOperatorType().equalsIgnoreCase(BasicOperator.OPERATOR_NAME)){
			return new BasicOperator(id, this.pConfig, this.franchise);
		} else if(this.pConfig.getOperatorType().equalsIgnoreCase(MultiPlanOperator.OPERATOR_NAME)){
			return new MultiPlanOperator(id, this.pConfig, this.franchise);
		} else if(this.pConfig.getOperatorType().equalsIgnoreCase(CarefulMultiPlanOperator.OPERATOR_NAME)){
			return new CarefulMultiPlanOperator(id, this.pConfig, this.franchise);
		} else if(this.pConfig.getOperatorType().equalsIgnoreCase(WelfareCarefulMultiPlanOperator.OPERATOR_NAME)){
			
			if (this.welfareAnalyzer == null) {
				throw new RuntimeException("Welfare analyzer is null. Aborting...");
			}
			
			return new WelfareCarefulMultiPlanOperator(id, this.pConfig, this.franchise, this.welfareAnalyzer);
			
		} else {
			log.error("There is no operator type specified. " + this.pConfig.getOperatorType() + " unknown");
			return null;
		}
	}
}
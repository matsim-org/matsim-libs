/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.andreas.P2.helper;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * Hook providing the ability to register a new TransitSchedule
 * 
 * @author aneumann
 *
 */
public class PScenarioImpl extends ScenarioImpl{
	
	private static final Logger log = Logger.getLogger(PScenarioImpl.class);

	private TransitSchedule pTransitSchedule = null;

	public PScenarioImpl(Config config) {
		super(config);
	}
	
	@Override
	public TransitSchedule getTransitSchedule() {
		if(pTransitSchedule == null){
			log.info("returning old transit schedule");
			return super.getTransitSchedule();
		} else {
			log.info("returning new transit schedule");
			return this.pTransitSchedule;
		}
	}
	
	public void setTransitSchedule(TransitSchedule schedule) {
		this.pTransitSchedule  = schedule;
	}

}
/* *********************************************************************** *
 * project: org.matsim.*
 * FundamentalDiagramsNmodes											   *
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

package playgrounds.ssix;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author ssix
 * A class supposed to go attached to the DreieckNmodes class.
 * It aims at analyzing the flow of events in order to detect:
 * The permanent regime of the system and the following searched values:
 * the permanent flow, the permanent density and the permanent average 
 * velocity for each velocity group.
 */

public class FundamentalDiagramsNmodes implements LinkEnterEventHandler {

	private static final Logger log = Logger.getLogger(FunDiagramsWithPassing3modes.class);
	
	private Scenario scenario;
	private Map<Id, ModeData> modesData;
	private ModeData globalData;
	
	private Id studiedMeasuringPointLinkId = new IdImpl(0);
	
	private boolean speedStability;
	private boolean permanentRegime;
	private double permanentDensity;
	private double permanentAverageVelocity;
	private double permanentFlow;
	
	public FundamentalDiagramsNmodes(Scenario sc, Map<Id, ModeData> modesData){
		this.scenario =  sc;
		this.modesData = modesData;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// TODO Auto-generated method stub
		
	}
	
}

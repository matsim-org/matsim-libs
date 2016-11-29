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

/**
 * 
 * @author ikaddoura
 * 
 */
package playground.vsp.analysis.modules.ptDriverPrefix;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.MutableScenario;

import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * 
 * @author ikaddoura
 *
 */
public class PtDriverIdAnalyzer extends AbstractAnalysisModule{
	private final static Logger log = Logger.getLogger(PtDriverIdAnalyzer.class);
	private MutableScenario scenario;
	
	private PtDriverIdHandler ptDriverPrefixHandler;
	
	public PtDriverIdAnalyzer() {
		super(PtDriverIdAnalyzer.class.getSimpleName());
	}
	
	public void init(MutableScenario scenario) {
		this.scenario = scenario;
		this.ptDriverPrefixHandler = new PtDriverIdHandler();
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new LinkedList<EventHandler>();
		handler.add(this.ptDriverPrefixHandler);		
		return handler;
	}

	@Override
	public void preProcessData() {
	}

	@Override
	public void postProcessData() {
		// Analyzing the ptDriverPrefix here would be to late. Getting the ptDriverPrefix while parsing the events.
	}

	@Override
	public void writeResults(String outputFolder) {
		System.out.println(this.ptDriverPrefixHandler.getPtDriverIDs());
	}
	
	public boolean isPtDriver(Id id) {
		if (this.ptDriverPrefixHandler.getPtDriverIDs().contains(id)){
			return true;
		} else {
			return false;
		}
	}
	
}

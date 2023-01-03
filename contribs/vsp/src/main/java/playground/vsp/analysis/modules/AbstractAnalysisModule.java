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

package playground.vsp.analysis.modules;

import java.util.List;

import org.matsim.core.events.handler.EventHandler;

/**
 * 
 * Some common methods for analysis modules.
 * 
 * @author aneumann
 *
 */
public abstract class AbstractAnalysisModule {
	
	private final String name;
	
	/**
	 * 
	 * @param name The name of the module.
	 */
	public AbstractAnalysisModule(String name){
		this.name = name;
	}
	
	/**
	 * 
	 * @return The name of that module
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * 
	 * @return A list of all the event handler of the module, if necessary, otherwise an empty List.
	 */
	public abstract List<EventHandler> getEventHandler();
	
	/**
	 * Hook called before the events stream is processed.
	 */
	public abstract void preProcessData();
	
	/**
	 * Hook after the events stream is processed.
	 */
	public abstract void postProcessData();
	
	/**
	 * 
	 * @return Write the results collected by the module to the given output folder.
	 */
	public abstract void writeResults(String outputFolder);
	
}

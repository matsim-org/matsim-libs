/* *********************************************************************** *
 * project: org.matsim.*
 * ModuleAnalyzerTask.java
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
package playground.johannes.sna.graph.analysis;



/**
 * An AnalyzerTask that uses a specific module (such as {@link Degree} or
 * {@link Transitivity} for analysis.
 * 
 * @author illenberger
 * 
 */
public abstract class ModuleAnalyzerTask<T> extends AnalyzerTask {
	
	protected T module;
	
	protected String key;
	
	/**
	 * Sets the module to be used for analysis.
	 * 
	 * @param module
	 *            a object that features the functionality for analysis.
	 */
	public void setModule(T module) {
		this.module = module;
	}
	
	public void setKey(String key) {
		this.key = key;
	}
}

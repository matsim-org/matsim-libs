/* *********************************************************************** *
 * project: org.matsim.*
 * PlanModThreadFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.coopsim.mental.planmod.concurrent;

import java.util.concurrent.ThreadFactory;

import playground.johannes.coopsim.mental.planmod.Choice2ModAdaptorFactory;

/**
 * @author illenberger
 * 
 */
class PlanModThreadFactory implements ThreadFactory {

	private final Choice2ModAdaptorFactory facotry;
	
	PlanModThreadFactory(Choice2ModAdaptorFactory factory) {
		this.facotry = factory;
	}

	@Override
	public Thread newThread(Runnable r) {
		return new PlanModThread(facotry.create(), r);
	}

}

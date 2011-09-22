/* *********************************************************************** *
 * project: org.matsim.*
 * PlanModThread.java
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

import playground.johannes.coopsim.mental.planmod.Choice2ModAdaptor;

/**
 * @author illenberger
 *
 */
class PlanModThread extends Thread {

	protected final Choice2ModAdaptor adaptor;
	
	PlanModThread(Choice2ModAdaptor adaptor, Runnable r) {
		super(r);
		this.adaptor = adaptor;
	}
}

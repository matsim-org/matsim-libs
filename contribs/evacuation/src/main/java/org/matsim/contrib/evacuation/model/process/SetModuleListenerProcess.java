/* *********************************************************************** *
 * project: org.matsim.*
 * MyMapViewer.java
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

package org.matsim.contrib.evacuation.model.process;

import org.matsim.contrib.evacuation.control.Controller;
import org.matsim.contrib.evacuation.control.eventlistener.AbstractListener;
import org.matsim.contrib.evacuation.model.AbstractModule;

public class SetModuleListenerProcess extends BasicProcess
{
	
	private AbstractListener listener;
	private AbstractModule module;

	public SetModuleListenerProcess(Controller controller, AbstractModule module, AbstractListener listener)
	{
		super(controller);
		this.listener = listener;
		this.module = module;
	}
	
	@Override
	public void start()
	{
		//set module listeners
//		if ((controller.getListener()==null) || (!(controller.getListener().getClass().isInstance(listener))))
			setListeners(listener);
		
		module.setListener(listener);
	}
	

}

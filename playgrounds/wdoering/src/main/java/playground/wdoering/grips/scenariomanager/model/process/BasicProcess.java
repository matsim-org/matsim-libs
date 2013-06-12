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

package playground.wdoering.grips.scenariomanager.model.process;

import javax.swing.JFileChooser;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.control.eventlistener.AbstractListener;
import playground.wdoering.grips.scenariomanager.model.AbstractModule;
import playground.wdoering.grips.scenariomanager.model.AbstractToolBox;
import playground.wdoering.grips.scenariomanager.model.locale.Locale;
import playground.wdoering.grips.scenariomanager.view.DefaultOpenDialog;
import playground.wdoering.grips.scenariomanager.view.renderer.JXMapRenderer;
import playground.wdoering.grips.scenariomanager.view.renderer.GridRenderer;
import playground.wdoering.grips.scenariomanager.view.renderer.ShapeRenderer;

public abstract class BasicProcess implements ProcessInterface
{
	protected Controller controller;
	protected Locale locale;
	protected AbstractModule module;
	protected static BasicProcess process;
	
	public BasicProcess(AbstractModule module, Controller controller)
	{
		this.module = module;
		this.controller = controller;
		this.locale = controller.getLocale();
		
	}
	
	public BasicProcess(Controller controller)
	{
		this.controller = controller;
		this.locale = controller.getLocale();
	}
	
	public void setListeners(AbstractListener listeners)
	{
		//set and attach listeners
		controller.setListener(listeners);
		controller.setMainPanelListeners(true);
	}
	
	
	public void addNetworkRenderer(GridRenderer renderer)
	{
		controller.addRenderLayer(renderer);
	}
	
	public void addToolBox(AbstractToolBox toolBox)
	{
		this.controller.setActiveToolBox(toolBox);
	}

	@Override
	public void start() {}

}

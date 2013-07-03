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

package playground.wdoering.grips.scenariomanager.model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.control.eventlistener.AbstractListener;
import playground.wdoering.grips.scenariomanager.model.Constants.ModuleType;
import playground.wdoering.grips.scenariomanager.model.process.BasicProcess;
import playground.wdoering.grips.scenariomanager.model.process.ProcessInterface;

/**
 * class describing general purpose module functions
 * 
 * @author wdoering
 *
 */
public abstract class AbstractModule
{
	protected static int width = 800;
	protected static int height = 600;
	protected static int border = 30;
	
	
	protected ArrayList<ProcessInterface> processList;
	protected Controller controller;
	protected Point mousePosition;
	protected boolean mainGoalAchieved;
	
	protected String title;
	protected ModuleType moduleType;
	
	protected ArrayList<ModuleType> nextModules;
	protected ArrayList<ModuleType> pastModules;
	private boolean enabled = false;
	
	protected AbstractToolBox toolBox;
	protected AbstractListener listener;
	
	protected int offsetX;
	protected int offsetY;
	
	public AbstractModule(String title, ModuleType moduleType, Controller controller)
	{
		this.title = title;
		this.moduleType = moduleType;
		this.controller = controller;
		this.processList = new ArrayList<ProcessInterface>();
		this.nextModules = controller.getNextModules(moduleType);
		this.pastModules = controller.getPastModules(moduleType);
		
		//set tool box
		if (this.controller.isStandAlone())
		{
			controller.setActiveToolBox(getToolBox());
			this.enabled = true;
		}
		
		this.controller.setActiveModuleType(this.moduleType);
		
		this.offsetX = this.offsetY = this.controller.getImageContainer().getBorderWidth();
		
	}
	
	public String getTitle()
	{
		return title;
	}
	
	public void setTitle(String title)
	{
		this.title = title;
	}
	

	public AbstractToolBox getToolBox()
	{
		return null;
	}

	public ArrayList<ProcessInterface> getProcessList()
	{
		return processList;
	}
	
	public void setProcessList(ArrayList<ProcessInterface> processList)
	{
		this.processList = processList;
		
	}
	
	public void start()
	{
		
		if (this.processList.size() > 0)
		{
			for (ProcessInterface process : this.processList)
				process.start();
		}
		
		if (this.toolBox!=null)
			this.toolBox.init();
		
		if (this.listener!=null)
			this.listener.init();
	}
	
	public void sleep(int millis)
	{
		try { Thread.sleep(millis); } catch (InterruptedException e) { e.printStackTrace(); }
	}
	
	public boolean isMainGoalAchieved()
	{
		return this.mainGoalAchieved;
	}
	
	public void setMainGoalAchieved(boolean mainGoalAchieved)
	{
		this.mainGoalAchieved = mainGoalAchieved;
	}

	protected void exit(String exitString)
	{
		System.out.println(exitString);
		System.exit(0);
	}

	public ModuleType getModuleType()
	{
		return moduleType;
	}
	
	public void setModuleType(ModuleType moduleType)
	{
		this.moduleType = moduleType;
	}

	public void enableNextModules()
	{
		if (nextModules!=null)
		{
			for (ModuleType nextModuleType : nextModules)
				controller.enableModule(nextModuleType);
		}
	
	}
	
	public void disablePastModules()
	{
		if (pastModules!=null)
		{
			for (ModuleType pastModuleType : pastModules)
				controller.disableModule(pastModuleType);
			
		}
		
	}

	public void setEnabled(boolean b)
	{
		this.enabled  = b;
		
	}
	
	public boolean isEnabled()
	{
		return enabled;
	}
	
	public List<BasicProcess> getProcessChain()
	{
		return null;
	}
	
	public AbstractListener getListener() {
		return listener;
	}
	
	public void setListener(AbstractListener listener) {
		this.listener = listener;
	}
	
	
	

}

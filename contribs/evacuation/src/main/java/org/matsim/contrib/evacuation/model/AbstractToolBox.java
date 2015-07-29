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

package org.matsim.contrib.evacuation.model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

import org.matsim.contrib.evacuation.control.Controller;
import org.matsim.contrib.evacuation.model.locale.Locale;

/**
 * class describing general purpose toolbox functions
 * 
 * @author wdoering
 *
 */
public class AbstractToolBox extends JPanel implements ActionListener
{
	private static final long serialVersionUID = 1L;
	protected Controller controller;
	protected Locale locale;
	protected boolean goalAchieved;
	protected AbstractModule module;
	
	public AbstractToolBox(AbstractModule module, Controller controller)
	{
		this.module = module;
		this.controller = controller;
		this.locale = controller.getLocale();
		this.goalAchieved = false;
	}
	
	public void resetMask()
	{
		
	}
	
	public void updateMask()
	{
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		
	}
	
	public boolean isGoalAchieved()
	{
		return goalAchieved;
	}
	
	public void setGoalAchieved(boolean goalAchieved)
	{
		this.goalAchieved = goalAchieved;
	}
	
	
	public void fireSaveEvent()
	{
		this.actionPerformed(new ActionEvent(this, 0, locale.btSave()));
	}
	
	public Controller getController()
	{
		return controller;
	}

	public void init() {
		
	}

	public boolean save() {
		return true;
	}

}

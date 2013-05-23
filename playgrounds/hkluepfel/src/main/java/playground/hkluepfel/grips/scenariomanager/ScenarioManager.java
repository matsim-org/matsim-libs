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


package playground.hkluepfel.grips.scenariomanager;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.model.AbstractModule;
import playground.wdoering.grips.scenariomanager.model.Constants.ModuleType;
import playground.wdoering.grips.scenariomanager.model.imagecontainer.BufferedImageContainer;
import playground.wdoering.grips.scenariomanager.view.TabButton;
import playground.wdoering.grips.v2.analysis.EvacuationAnalysis;
import playground.wdoering.grips.v2.evacareaselector.EvacAreaSelector;
import playground.wdoering.grips.v2.popareaselector.PopAreaSelector;
import playground.wdoering.grips.v2.ptlines.PTLEditor;
import playground.wdoering.grips.v2.roadclosures.RoadClosureEditor;
import playground.wdoering.grips.v2.scenariogenerator.MatsimScenarioGenerator;
import playground.wdoering.grips.v2.scenariogenerator.ScenarioGenerator;

@SuppressWarnings("serial")
public class ScenarioManager extends playground.wdoering.grips.scenariomanager.ScenarioManager
{
	static int width = 1024;
	static int height = 720;
	static int border = 10;
	public ConcurrentHashMap<ModuleType, TabButton> selectionButtons;
	private ConcurrentHashMap<ModuleType, AbstractModule> modules;

	public static void main(String[] args)
	{
		final Controller controller = new Controller();
		BufferedImage image = new BufferedImage(width - border * 2, height - border * 2, BufferedImage.TYPE_INT_ARGB);
		BufferedImageContainer imageContainer = new BufferedImageContainer(image, border);
		controller.setImageContainer(imageContainer);
		controller.setMainFrameUndecorated(false);
		
		controller.setStandAlone(false);
		
		ArrayList<AbstractModule> moduleChain = new ArrayList<AbstractModule>();
		
		//current work flow
		moduleChain.add(new EvacAreaSelector(controller));
		moduleChain.add(new PopAreaSelector(controller));
		// moduleChain.add(new ComplexODSelector(controller));
		moduleChain.add(new ScenarioGenerator(controller));
		moduleChain.add(new RoadClosureEditor(controller));
		moduleChain.add(new PTLEditor(controller));
		moduleChain.add(new MatsimScenarioGenerator(controller));
		moduleChain.add(new EvacuationAnalysis(controller));
		
		controller.addModuleChain(moduleChain);
		
		ScenarioManager manager = new ScenarioManager(controller);
		
		// set parent component to forward the (re)paint event
//		controller.setParentComponent(manager);		
//		controller.setMainPanel(manager.getMainPanel(), true);
		
		manager.setVisible(true);
	}
	
	public ScenarioManager(Controller controller)
	{
		super(controller);
	}
}


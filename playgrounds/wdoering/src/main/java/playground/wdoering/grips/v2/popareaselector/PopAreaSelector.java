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

package playground.wdoering.grips.v2.popareaselector;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.control.ShapeFactory;
import playground.wdoering.grips.scenariomanager.model.AbstractModule;
import playground.wdoering.grips.scenariomanager.model.AbstractToolBox;
import playground.wdoering.grips.scenariomanager.model.Constants;
import playground.wdoering.grips.scenariomanager.model.imagecontainer.BufferedImageContainer;
import playground.wdoering.grips.scenariomanager.model.process.BasicProcess;
import playground.wdoering.grips.scenariomanager.model.process.DisableLayersProcess;
import playground.wdoering.grips.scenariomanager.model.process.EnableLayersProcess;
import playground.wdoering.grips.scenariomanager.model.process.InitEvacShapeProcess;
import playground.wdoering.grips.scenariomanager.model.process.InitGripsConfigProcess;
import playground.wdoering.grips.scenariomanager.model.process.InitMainPanelProcess;
import playground.wdoering.grips.scenariomanager.model.process.InitMapLayerProcess;
import playground.wdoering.grips.scenariomanager.model.process.InitShapeLayerProcess;
import playground.wdoering.grips.scenariomanager.model.process.SetModuleListenerProcess;
import playground.wdoering.grips.scenariomanager.model.process.SetToolBoxProcess;
import playground.wdoering.grips.scenariomanager.view.DefaultWindow;

public class PopAreaSelector extends AbstractModule
{
	
	private PopToolBox toolBox;
	
	public static void main(String[] args)
	{
		// set up controller and image interface
		final Controller controller = new Controller();
		BufferedImage image = new BufferedImage(width - border * 2, height - border * 2, BufferedImage.TYPE_INT_ARGB);
		BufferedImageContainer imageContainer = new BufferedImageContainer(image, border);
		controller.setImageContainer(imageContainer);

		// inform controller that this module is running stand alone
		controller.setStandAlone(true);
		
		// instantiate evacuation area selector
		AbstractModule popAreaSelector = new PopAreaSelector(controller);

		// create default window for running this module standalone
		DefaultWindow frame = new DefaultWindow(controller);

		// set parent component to forward the (re)paint event
		controller.setParentComponent(frame);
		controller.setMainPanel(frame.getMainPanel(), true);

		// start the process chain
		popAreaSelector.start();
		frame.requestFocus();
		
	}	

	public PopAreaSelector(Controller controller)
	{
		super(controller.getLocale().modulePopAreaSelector(), Constants.ModuleType.POPULATION, controller);
		
		//disable all layers
		this.processList.add(new DisableLayersProcess(controller));

		//initialize GRIPS config
		this.processList.add(new InitGripsConfigProcess(controller));
		
		//add toolbox
		this.processList.add(new SetToolBoxProcess(controller, getToolBox()));
		
		//check if the default render panel is set
		this.processList.add(new InitMainPanelProcess(controller));
		
		// check if there is already a map viewer running, or just (re)set center position
		this.processList.add(new InitMapLayerProcess(controller));
		
		//set module listeners		
		this.processList.add(new SetModuleListenerProcess(controller, this, new PopEventListener(controller)));
		
		// check if there is already a primary shape layer
		this.processList.add(new InitShapeLayerProcess(controller));
		
		//load evacuation area shape		
		this.processList.add(new InitEvacShapeProcess(controller));
		
		//add bounding box
		this.processList.add(new BasicProcess(controller)
		{
			@Override
			public void start()
			{
				int shapeRendererId = controller.getVisualizer().getPrimaryShapeRenderLayer().getId();
				Rectangle2D bbRect = controller.getBoundingBox();
				controller.addShape(ShapeFactory.getNetBoxShape(shapeRendererId, bbRect, true));
			}

		});
		
		//add toolbox
		this.processList.add(new SetToolBoxProcess(controller, getToolBox()));
		
		//enable all layers
		this.processList.add(new EnableLayersProcess(controller));
		
	}
	
	@Override
	public AbstractToolBox getToolBox()
	{
		if (toolBox == null)
			toolBox = new PopToolBox(this, this.controller);
		
		return toolBox;
	}
	
	
	

}

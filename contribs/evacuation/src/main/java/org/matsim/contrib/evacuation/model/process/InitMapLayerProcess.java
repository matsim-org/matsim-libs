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
import org.matsim.contrib.evacuation.view.renderer.JXMapRenderer;

public class InitMapLayerProcess extends BasicProcess {

	public InitMapLayerProcess(Controller controller)
	{
		super(controller);
	}
	
	@Override
	public void start()
	{
		// check if there is already a map viewer running, or just (re)set center position
		if (!controller.hasMapRenderer())
		{
			//add new jx map viewer interface
			JXMapRenderer jxMapRenderer = new JXMapRenderer(controller, controller.getWMS(), controller.getWMSLayer());
			controller.addRenderLayer(jxMapRenderer);
			controller.setSlippyMapEventListeners(jxMapRenderer.getInheritedEventListeners());
		}
		else
			controller.getVisualizer().getActiveMapRenderLayer().setPosition(controller.getCenterPosition());
	}
	

	

}

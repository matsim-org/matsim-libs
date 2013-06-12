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

import playground.wdoering.grips.scenariomanager.control.Controller;

public class InitGripsConfigProcess extends BasicProcess
{

	
	public InitGripsConfigProcess(Controller controller)
	{
		super(controller);
	}
	
	@Override
	public void start()
	{
		// check if Grips config (including the OSM network) has been loaded
		if (!controller.isGripsConfigOpenend())
			if (!controller.openGripsConfig())
				controller.exit(locale.msgOpenGripsConfigFailed());
	}
	
	
	

}

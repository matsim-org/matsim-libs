/* *********************************************************************** *
 * project: matsim
 * AdditionalTeleportationDepartureEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.events.handler;

import org.matsim.core.events.AdditionalTeleportationDepartureEvent;

/**
 * @author nagel
 *
 */
@Deprecated // this is a possibly temporary fix to remove the MobsimFeatures.  do not use.  kai, aug'10
public interface AdditionalTeleportationDepartureEventHandler extends EventHandler {
	
	public void handleEvent( AdditionalTeleportationDepartureEvent eve ) ;

}

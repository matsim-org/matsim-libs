/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.av.intermodal.router;

import java.util.List;

import javax.inject.Inject;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.av.intermodal.router.config.VariableAccessConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.pt.PtConstants;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class VariableAccessMainModeIdentifier implements MainModeIdentifier {

	private final MainModeIdentifier delegate = new MainModeIdentifierImpl();
	private final VariableAccessConfigGroup va;
	/**
	 * 
	 */
	@Inject
	public VariableAccessMainModeIdentifier(Config config) {
		 va = (VariableAccessConfigGroup) config.getModules().get(VariableAccessConfigGroup.GROUPNAME);
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.core.router.MainModeIdentifier#identifyMainMode(java.util.List)
	 */
	@Override
	public String identifyMainMode(List<? extends PlanElement> tripElements) {
		String mode = ((Leg) tripElements.get( 0 )).getMode();

		if ( mode.equals( TransportMode.transit_walk ) ) {
			return TransportMode.pt ;

		}
		
		
		for (PlanElement pe : tripElements)
		{
			if (pe instanceof Activity){
				if (((Activity) pe).getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)){
					return (va.getMode());
				}
			}
		}
		return delegate.identifyMainMode(tripElements);
	}

}

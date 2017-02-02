/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.negotiation.locationnegotiation;

import com.google.inject.Key;
import org.matsim.core.controler.AbstractModule;
import playground.thibautd.negotiation.offlinecoalition.CoalitionChoiceIterator;
import playground.thibautd.negotiation.offlinecoalition.SimpleJointPlanCreator;

/**
 * @author thibautd
 */
public class LocationJointPlanCreatorModule extends AbstractModule {
	@Override
	public void install() {
		// did not manage to generify that enough in base module...
		bind( new Key<CoalitionChoiceIterator.JointPlanCreator<LocationProposition>>() {} )
			.to( new Key<SimpleJointPlanCreator<LocationProposition>>() {} );
	}
}

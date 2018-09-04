/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dynagent.run;

import java.util.*;

import com.google.inject.Inject;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.*;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

public class DynRoutingModule implements RoutingModule {
	private final String stageActivityType;
	@Inject Network network ;
	@Inject Population population ;
	@Inject PlansCalcRouteConfigGroup calcRouteConfig ;
	
	private final String mode;
	private StageActivityTypes stageActivityTypes;

	public DynRoutingModule(String mode) {
		this.mode = mode;
		this.stageActivityType = "other interaction";
	}

	@Override
	public List<? extends PlanElement> calcRoute( Facility fromFacility, Facility toFacility, double departureTime,
								    Person person) {
		
		Gbl.assertNotNull(fromFacility);
		Gbl.assertNotNull(toFacility);
		
		Link accessActLink = FacilitiesUtils.decideOnLink(fromFacility, network );
		
		Link egressActLink = FacilitiesUtils.decideOnLink(toFacility, network );
		
		List<PlanElement> result = new ArrayList<>() ;
		
		// access leg:
		if ( calcRouteConfig.isInsertingAccessEgressWalk() ) {
			departureTime += NetworkRoutingInclAccessEgressModule.addBushwhackingLegFromFacilityToLinkIfNecessary(
					fromFacility, person, accessActLink, departureTime, result, population.getFactory(), stageActivityType ) ;
		}
		
		// leg proper:
		{
			Route route = RouteUtils.createGenericRouteImpl( fromFacility.getLinkId(), toFacility.getLinkId() );
			route.setDistance( Double.NaN );
			route.setTravelTime( Double.NaN );
			
			Leg leg = PopulationUtils.createLeg( mode );
			leg.setDepartureTime( departureTime );
			leg.setTravelTime( Double.NaN );
			leg.setRoute( route );
			if ( fromFacility.getLinkId().equals( toFacility.getLinkId() ) ) {
				leg.setMode( TransportMode.walk );
			}
			result.add( leg );
		}

		// egress leg:
		if ( calcRouteConfig.isInsertingAccessEgressWalk(  ) ) {
			NetworkRoutingInclAccessEgressModule.addBushwhackingLegFromLinkToFacilityIfNecessary(
					toFacility, person, egressActLink, departureTime, result, population.getFactory(), stageActivityType );
		}

		return result ;
	}

	/**
	 * @param stageActivityTypes
	 *            the stageActivityTypes to set
	 */
	public void setStageActivityTypes(StageActivityTypes stageActivityTypes) {
		this.stageActivityTypes = stageActivityTypes;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return this.stageActivityTypes != null ? this.stageActivityTypes : EmptyStageActivityTypes.INSTANCE;
	}
}

/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.facilities;

import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.NetworkRoutingInclAccessEgressModule;

/**
 * Contains several helper methods for working with {@link ActivityFacility facilities}.
 *
 * @author cdobler
 */
public class FacilitiesUtils {
	private static final Logger log = Logger.getLogger( FacilitiesUtils.class ) ;
	
	private FacilitiesUtils() {} // container for static methods; do not instantiate
	
	public static ActivityFacilities createActivityFacilities() {
		return createActivityFacilities(null) ;
	}
	
	public static ActivityFacilities createActivityFacilities(String name) {
		return new ActivityFacilitiesImpl( name ) ;
	}

	/**
	 * @return sorted map containing containing the facilities as values and their ids as keys.
	 */
	public static SortedMap<Id<ActivityFacility>, ActivityFacility> getSortedFacilities(final ActivityFacilities facilities) {
		return new TreeMap<>(facilities.getFacilities());
	}
	
	public static void setLinkID( final Facility facility , Id<Link> linkId ) {
		if ( facility instanceof ActivityFacilityImpl ) {
			((ActivityFacilityImpl) facility).setLinkId(linkId);
		} else {
			throw new RuntimeException("cannot set linkID for this facility type; API needs to be cleaned up") ;
		}
	}
	
	public static Link decideOnLink( final Facility fromFacility, final Network network ) {
		Link accessActLink = null ;
		
		Id<Link> accessActLinkId = null ;
		try {
			accessActLinkId = fromFacility.getLinkId() ;
		} catch ( Exception ee ) {
			// there are implementations that throw an exception here although "null" is, in fact, an interpretable value. kai, oct'18
		}
		
		if ( accessActLinkId!=null ) {
			accessActLink = network.getLinks().get( fromFacility.getLinkId() );
			// i.e. if street address is in mode-specific subnetwork, I just use that, and do not search for another (possibly closer)
			// other link.
			
		}
		
		if ( accessActLink==null ) {
			// this is the case where the postal address link is NOT in the subnetwork, i.e. does NOT serve the desired mode,
			// OR the facility does not have a street address link in the first place.

			if( fromFacility.getCoord()==null ) {
				throw new RuntimeException("link for facility cannot be determined when neither facility link id nor facility coordinate given") ;
			}
			
			accessActLink = NetworkUtils.getNearestLink(network, fromFacility.getCoord()) ;
			if ( accessActLink == null ) {
				int ii = 0 ;
				for ( Link link : network.getLinks().values() ) {
					if ( ii==10 ) {
						break ;
					}
					ii++ ;
					log.warn( link );
				}
			}
			Gbl.assertNotNull(accessActLink);
		}
		return accessActLink;
	}

	public static Facility toFacility( final Activity toWrap, ActivityFacilities activityFacilities ){
		if ( activityFacilities!=null && toWrap.getFacilityId()!=null ){
			ActivityFacility fac = activityFacilities.getFacilities().get( toWrap.getFacilityId() );
			if( fac != null ){
				return fac;
			}
		}
		return new ActivityWrapperFacility( toWrap );
	}

	/**
	 * Preferably use {@link FacilitiesUtils#toFacility(Activity, ActivityFacilities)}.  The method here is left in place if one wants to construct a wrapper decidedly without
	 * automagic.  It deliberately returns the interface.
	 */
	public static Facility wrapActivity ( final Activity toWrap ) {
		return new ActivityWrapperFacility( toWrap ) ;
	}

	public static Facility wrapLink( final Link link ) {
		return new LinkWrapperFacility( link ) ;
	}
}

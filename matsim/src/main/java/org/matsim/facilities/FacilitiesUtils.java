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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.LinkWrapperFacilityWithSpecificCoord;
import org.matsim.core.router.MultimodalLinkChooser;
import org.matsim.utils.objectattributes.attributable.Attributable;

/**
 * Contains several helper methods for working with {@link ActivityFacility facilities}.
 *
 * @author cdobler
 */
public class FacilitiesUtils {
	private static final Logger log = LogManager.getLogger( FacilitiesUtils.class ) ;
	
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
	/**
	 * Compare to {@link #decideOnLink(Facility, Network)}.  Sometimes only the linkId is needed, and often it is cheaper to obtain than the full link.
	 * Then call this method.
	 *
	 * @param facility
	 * @param network
	 * @return
	 */
	public static Id<Link> decideOnLinkId( final Facility facility, final Network network ) {
		Link accessActLink = null ;

		Id<Link> accessActLinkId = null ;
		try {
			accessActLinkId = facility.getLinkId() ;
		} catch ( Exception ee ) {
			// there are implementations that throw an exception here although "null" is, in fact, an interpretable value. kai, oct'18
		}
		if ( accessActLinkId!=null ) {
			return accessActLinkId ;
		}
		return decideOnLink( facility, network ).getId() ;
	}

	/**
	 * @deprecated
	 * Please use {@link MultimodalLinkChooser} instead
	 */
	@Deprecated
	public static Link decideOnLink( final Facility facility, final Network network ) {
		Link accessActLink = null ;
		
		Id<Link> accessActLinkId = null ;
		try {
			accessActLinkId = facility.getLinkId() ;
		} catch ( Exception ee ) {
			// there are implementations that throw an exception here although "null" is, in fact, an interpretable value. kai, oct'18
		}
		
		if ( accessActLinkId!=null ) {
			accessActLink = network.getLinks().get( facility.getLinkId() );
			// i.e. if street address is in mode-specific subnetwork, I just use that, and do not search for another (possibly closer)
			// other link.
			
		}
		
		if ( accessActLink==null ) {
			// this is the case where the postal address link is NOT in the subnetwork, i.e. does NOT serve the desired mode,
			// OR the facility does not have a street address link in the first place.

			if( facility.getCoord()==null ) {
				throw new RuntimeException("link for facility cannot be determined when neither facility link id nor facility coordinate given") ;
			}
			
			accessActLink = NetworkUtils.getNearestLink(network, facility.getCoord()) ;
			if ( accessActLink == null ) {
				log.warn("Facility without link for which no nearest link on the respective network could be found. " +
						"About to abort. Writing out the first 10 links to understand which subnetwork was used to help debugging.");
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

		// I just found out that there are facilities that insist on links that may not be postal addresses since they cannot be reached by car.
		// TransitStopFacility is an example.  kai, jun'19

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
	public static Facility wrapLinkAndCoord(final Link link, final Coord coord){
		return new LinkWrapperFacilityWithSpecificCoord(link,coord);
	}

	/**
	 *  We have situations where the coordinate field in facility is not filled out.
	 */
	public static Coord decideOnCoord( final Facility facility, final Network network, final Config config ) {
		return decideOnCoord( facility, network, config.global().getRelativePositionOfEntryExitOnLink() ) ;
	}
	/**
	 *  We have situations where the coordinate field in facility is not filled out.
	 */
	public static Coord decideOnCoord( final Facility facility, final Network network, double relativePositionOfEntryExitOnLink ) {
		if ( facility.getCoord() != null && ! ( facility instanceof LinkWrapperFacility)) {
			return facility.getCoord() ;
		}

		if ( facility.getLinkId()==null ) {
			if ( facility instanceof Identifiable ) {
				throw new RuntimeException( "facility with id=" + ((Identifiable) facility).getId() + " has neither coord nor linkId.  This " +
									    "does not work ..." ) ;
			} else {
				throw new RuntimeException( "facility which does not implement Identifiable has neither coord nor linkId.  This " +
									    "does not work ..." ) ;
			}
		}

		Gbl.assertNotNull( network ) ;
		Link link = network.getLinks().get( facility.getLinkId() ) ;
		Gbl.assertNotNull( link );
		Coord fromCoord = link.getFromNode().getCoord() ;
		Coord toCoord = link.getToNode().getCoord() ;
		return new Coord( fromCoord.getX() + relativePositionOfEntryExitOnLink *( toCoord.getX() - fromCoord.getX()) , fromCoord.getY() + relativePositionOfEntryExitOnLink *( toCoord.getY() - fromCoord.getY() ) );

	}

	// Logic gotten from PopulationUtils, but I am actually a bit unsure about the value of those methods now that
	// attributable is the only way to get attributes... td, aug'19
	// yy I would agree.  They are useful to manage the transition, but can be inlined afterwards.  I would inline for all code we can reach, afterwards
	// resurrect them but mark as deprecated.  kai, nov'19

	public static <F extends Facility & Attributable> Object getFacilityAttribute(F facility, String key) {
		return facility.getAttributes().getAttribute( key );
	}

	public static <F extends Facility & Attributable> void putFacilityAttribute(F facility, String key, Object value ) {
		facility.getAttributes().putAttribute( key, value ) ;
	}

	public static <F extends Facility & Attributable> Object removeFacilityAttribute( F facility, String key ) {
		return facility.getAttributes().removeAttribute( key );
	}
}

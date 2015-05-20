/* *********************************************************************** *
 * project: org.matsim.*
 * BikeSharingFacilities.java
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
package eu.eunoiaproject.bikesharing.framework.scenario;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.internal.MatsimToplevelContainer;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.thibautd.socnetsim.utils.QuadTreeRebuilder;

/**
 * The scenario element containing information about bike sharing facilities.
 * @author thibautd
 */
public class BikeSharingFacilities implements MatsimToplevelContainer {
	private static final Logger log =
		Logger.getLogger(BikeSharingFacilities.class);

	public static final String ELEMENT_NAME = "bikeSharingFacilities";
	private final Map<Id<BikeSharingFacility>, BikeSharingFacility> facilities =
		new LinkedHashMap< >();
	private final Map<Id<BikeSharingFacility>, BikeSharingFacility> unmodifiableFacilities =
		Collections.unmodifiableMap( facilities );
	private final ObjectAttributes facilitiesAttributes = new ObjectAttributes();

	private final QuadTreeRebuilder<BikeSharingFacility> quadTreeBuilder = new QuadTreeRebuilder< >();

	private final Map<String, String> metadata = new LinkedHashMap< >();

	public void addFacility( final BikeSharingFacility facility ) {
		facilities.put( facility.getId() , facility );
		quadTreeBuilder.put( facility.getCoord() , facility );
	}

	public Map<Id<BikeSharingFacility>, BikeSharingFacility> getFacilities() {
		return unmodifiableFacilities;
	}

	/**
	 * may not always return the same instance!
	 */
	public QuadTree<BikeSharingFacility> getCurrentQuadTree() {
		return quadTreeBuilder.getQuadTree();
	}

	@Override
	public BikeSharingFacilitiesFactory getFactory() {
		return new BikeSharingFacilitiesFactory() {
			@Override
			public BikeSharingFacility createBikeSharingFacility(
					final Id<BikeSharingFacility> id,
					final Coord coord,
					final Id<Link> linkId,
					final int capacity,
					final int initialNumberOfBikes) {
				return new BikeSharingFacilityImpl(
							id,
							coord,
							linkId,
							capacity,
							initialNumberOfBikes);
			}
		};
	}

	public ObjectAttributes getFacilitiesAttributes() {
		return facilitiesAttributes;
	}

	/**
	 * retrieve the metadata
	 */
	public Map<String, String> getMetadata() {
		return metadata;
	}

	/**
	 * add metadata. Metadata associates attribute names to values,
	 * and can be used to store any information useful to organize data:
	 * date of generation, source, author, etc.
	 */
	public void addMetadata(final String attribute, final String value) {
		final String old = metadata.put( attribute , value );
		if ( old != null ) log.warn( "replacing metadata \""+attribute+"\" from \""+old+"\" to \""+value+"\"" );
	}
}


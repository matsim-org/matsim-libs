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

package org.matsim.facilities;

import static org.matsim.core.config.groups.FacilitiesConfigGroup.FacilitiesSource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;

/**
 * Generates {@link ActivityFacility}s from the {@link Activity Activities} in a population
 * and assigns the activity facilities as the activity locations in the population while
 * removing the old locations (coord and link) from the population.
 * If an activity already has an ActivityFacility assigned, the ActivityFacility is overwritten.
 * If an activity only has a coordinate, different behavior is possible, see
 * {@link #setAssignLinksToFacilitiesIfMissing(Network)}.
 *
 * @author mrieser
 */
public final class FacilitiesFromPopulation {

	private final static Logger log = LogManager.getLogger(FacilitiesFromPopulation.class);

	private final ActivityFacilities facilities;
	private Scenario scenario;
	private FacilitiesSource facilitiesSource;
	private String idPrefix = "";
	private Network network = null;
	private boolean removeLinksAndCoordinates = true;
	private ScoringConfigGroup scoringConfigGroup = null;
	private boolean addEmptyActivityOptions = false;

	public FacilitiesFromPopulation(final ActivityFacilities facilities) {
		// minimalistic constructor, to configure via external setters
		this.facilities = facilities;
	}

	public FacilitiesFromPopulation( Scenario scenario ) {
		// "fat" constructor, to configure via config etc.
		this(scenario.getActivityFacilities());
		FacilitiesConfigGroup facilityConfigGroup = scenario.getConfig().facilities();
		this.idPrefix = facilityConfigGroup.getIdPrefix();
//		this.removeLinksAndCoordinates = facilityConfigGroup.isRemovingLinksAndCoordinates();
		this.removeLinksAndCoordinates = false ;
//		this.addEmptyActivityOptions = facilityConfigGroup.isAddEmptyActivityOption();
		this.addEmptyActivityOptions = true ;
		this.facilitiesSource = facilityConfigGroup.getFacilitiesSource();
		this.network = scenario.getNetwork() ;
		this.scoringConfigGroup = scenario.getConfig().scoring() ;
		this.scenario = scenario;
	}
	public void setFacilitiesSource( final FacilitiesSource facilitiesSource ) {
		this.facilitiesSource = facilitiesSource;
	}
	/**
	 * Sets whether all activities on a link should be collected within one ActivityFacility.
	 * Default is <code>true</code>. If set to <code>false</code>, for each coordinate
	 * found in the population's activities a separate ActivityFacility will be created.
	 *
	 * @param oneFacilityPerLink
	 *
	 * @deprecated -- better use {@link #setFacilitiesSource(FacilitiesSource)}
	 */
	public void setOneFacilityPerLink(final boolean oneFacilityPerLink) {
		if ( oneFacilityPerLink ) {
			this.facilitiesSource = FacilitiesSource.onePerActivityLinkInPlansFile;
		} else{
			this.facilitiesSource = FacilitiesSource.onePerActivityLocationInPlansFile;
		}
	}

	public void setIdPrefix(final String prefix) {
		this.idPrefix = prefix;
	}

	/**
	 * In the case that a facility has no link assigned, the ActivityFacility can be assigned to the closest link.
	 * If there should be only one ActivityFacility per link (see {@link #setOneFacilityPerLink(boolean)}),
	 * and if no link-assignment should be done, then a new ActivityFacility will be created at that coordinate
	 * and the facility will not be assigned to a link, essentially breaking the contract of
	 * {@link #setOneFacilityPerLink(boolean)}.
	 *
	 * @param network
	 */
	public void setAssignLinksToFacilitiesIfMissing( final Network network ) {
		Gbl.assertNotNull( network );
		this.network = network ;
	}

	/**
	 * If set to <code>true</code> (which is the default), the link and coordinate attributes
	 * are nulled in the activities, as this information is now available via the facility.
	 *
	 * @param doRemoval
	 */
	public void setRemoveLinksAndCoordinates(final boolean doRemoval) {
		this.removeLinksAndCoordinates = doRemoval;
	}

	public void assignOpeningTimes( final ScoringConfigGroup calcScoreConfigGroup ) {
		Gbl.assertNotNull( calcScoreConfigGroup );
		this.scoringConfigGroup = calcScoreConfigGroup ;
	}

	public void run(final Population population) {
		handleActivities(population);
		if (this.scoringConfigGroup != null ) {
			if (this.addEmptyActivityOptions) {
				this.assignOpeningTimes();
			} else{
				log.error("Cannot assign opening times to activity facilities because switch to add empty activity option to activity facilities is set to false.");
			}
		}
	}

	private void handleActivities(final Population population) {
		Gbl.assertNotNull( network ) ;

		int idxCounter = 0;
		ActivityFacilitiesFactory factory = this.facilities.getFactory();
		IdMap<Link, ActivityFacility> facilitiesPerLinkId = new IdMap<>(Link.class);
		Map<Coord, ActivityFacility> facilitiesPerCoordinate = new HashMap<>();

		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Activity) {
						Activity activity = (Activity) pe;

						Coord coord = activity.getCoord();
						// (may be null, and we are not fixing it at this point)

						Id<Link> linkId = activity.getLinkId();
						// (may be null, and we are not fixing it at this point)

						Gbl.assertIf( coord!=null || linkId!=null );
						// (need one of them non-null!)

						ActivityFacility facility ;

						if ( linkId == null ) {
							linkId = NetworkUtils.getNearestLinkExactly(this.network, coord).getId();
							// yyyy we have been using the non-exact version in other parts of the project. kai, mar'19
						}
						if ( coord==null ) {
							coord = PopulationUtils.decideOnCoordForActivity( activity, scenario );
						}

						Gbl.assertNotNull( linkId );

						if ( this.facilitiesSource==FacilitiesSource.onePerActivityLinkInPlansFile
								     || ( this.facilitiesSource==FacilitiesSource.onePerActivityLinkInPlansFileExceptWhenCoordinatesAreGiven && coord==null )
						) {
							facility = facilitiesPerLinkId.get(linkId);
							if (facility == null) {
								final Id<ActivityFacility> facilityId = Id.create( this.idPrefix + linkId.toString() , ActivityFacility.class );
								final ActivityFacility preExistingFacilityIfAny = this.facilities.getFacilities().get( facilityId );
								if ( preExistingFacilityIfAny == null ){
									facility = factory.createActivityFacility( facilityId , coord , linkId );
									facilitiesPerLinkId.put( linkId , facility );
									this.facilities.addActivityFacility( facility );
								} else {
									if ( Objects.equals( preExistingFacilityIfAny.getLinkId() ,
										  linkId ) && Objects.equals( preExistingFacilityIfAny.getCoord() , coord ) ) {
										// do nothing; presumably, same auto-generation has been run before
										facility = preExistingFacilityIfAny;
									} else {
										throw new RuntimeException( "Facility with id=" + facilityId + " but different in coordinates and/or linkId already exists." ) ;
									}
								}
								// above code is a duplicate, but they are difficult to merge because facilitiesPerLinkId is an IdMap while facilitiesPerCoord is a normal Map.  kai, feb'20
							}
						} else if ( this.facilitiesSource==FacilitiesSource.onePerActivityLocationInPlansFile
															      || ( this.facilitiesSource==FacilitiesSource.onePerActivityLinkInPlansFileExceptWhenCoordinatesAreGiven && coord!=null )
						) {
							if (coord == null)  {
								throw new RuntimeException("Coordinate for the activity "+activity+" is null, cannot collect facilities per coordinate. " +
										"Possibly use " + FacilitiesSource.onePerActivityLinkInPlansFile + " " +
													     "instead and collect facilities per link.");
							}

							facility = facilitiesPerCoordinate.get(coord);
							if (facility == null) {
								final Id<ActivityFacility> facilityId = Id.create( this.idPrefix + idxCounter++ , ActivityFacility.class );
								final ActivityFacility preExistingFacilityIfAny = this.facilities.getFacilities().get( facilityId );
								if ( preExistingFacilityIfAny == null ){
									facility = factory.createActivityFacility( facilityId , coord , linkId );
									facilitiesPerCoordinate.put( coord , facility );
									this.facilities.addActivityFacility( facility );
								} else {
									if ( Objects.equals( preExistingFacilityIfAny.getLinkId() , linkId ) && Objects.equals( preExistingFacilityIfAny.getCoord() , coord ) ) {
										// do nothing; presumably, same auto-generation has been run before
										facility = preExistingFacilityIfAny;
									} else {
										throw new RuntimeException( "Facility with id=" + facilityId + " but different in coordinates and/or linkId already exists." ) ;
									}
								}
								// above code is a duplicate, but they are difficult to merge because facilitiesPerLinkId is an IdMap while facilitiesPerCoord is a normal Map.  kai, feb'20
							}
						} else {
							throw new RuntimeException( "should never get to this location; either class/method used with invalid" +
												    " setting of facilitiesSource, or something there is " +
												    "something that was not understood while implementing " +
												    "this." );
						}

						if (this.addEmptyActivityOptions) {
							String actType = activity.getType();
							ActivityOption option = facility.getActivityOptions().get(actType);
							if (option == null) {
								option = factory.createActivityOption(actType);
								facility.addActivityOption(option);
							}
						}

						activity.setFacilityId(facility.getId());
						if (this.removeLinksAndCoordinates) {
							activity.setLinkId(null);
							activity.setCoord(null);
						}
					}
				}
			}
		}
	}

	private void assignOpeningTimes() {
		Set<String> missingActTypes = new HashSet<>();
		for (ActivityFacility af : this.facilities.getFacilities().values()) {
			for (ActivityOption ao : af.getActivityOptions().values()) {
				String actType = ao.getType();
				ActivityParams params = this.scoringConfigGroup.getActivityParams(actType);
				if (params == null) {
					if (missingActTypes.add(actType)) {
						log.error("No information for activity type " + actType + " found in given configuration.");
					}
				} else {
					ao.addOpeningTime(OpeningTimeImpl.createFromOptionalTimes(params.getOpeningTime(),
							params.getClosingTime()));
				}
			}
		}
	}

	public void setAddEmptyActivityOptions(boolean addEmptyActivityOptions) {
		this.addEmptyActivityOptions = addEmptyActivityOptions;
	}
}

package org.matsim.contrib.common.randomizedtransitrouter;/* *********************************************************************** *
 * project: org.matsim.*
 * RandomizedTransitRouterNetworkTravelTimeAndDisutility2
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.pt.router.CustomDataManager;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;


/**
 * When plugged into the transit router, will switch to a different, randomly generated combination of
 * marginal utilities every time it is called for a new agent.
 * <p/>
 * Comments:<ul>
 * <li> In the literature, they seem to use log-normal distributions for the tastes.  (Makes sense
 * since it addresse the symmetry problem which I am addressing ad-hoc.) [M. K. Anderson, hEART'13]
 * kai, sep'13
 * <li> People seem to use "gamma distributions for the error term" ("double stochastic assignment").
 * Not sure what that does, if we need it, etc.  [M. K. Anderson, hEART'13] kai, sep'13
 * </ul>
 * @author kai
 * @author dgrether
 *
 */
public class RandomizedTransitRouterTravelTimeAndDisutility  extends TransitRouterNetworkTravelTimeAndDisutility {

	public enum DataCollection {randomizedParameters, additionalInformation}
	private Id<Person> cachedPersonId = null ;
	private final TransitRouterConfig originalTransitRouterConfig ;

	private double localMarginalUtilityOfTravelTimeWalk_utl_s = Double.NaN ;
	private double localMarginalUtilityOfWaitingPt_utl_s = Double.NaN ;
	private double localUtilityOfLineSwitch_utl = Double.NaN ;
	private double localMarginalUtilityOfTravelTimePt_utl_s = Double.NaN ;
	private double localMarginalUtilityOfTravelDistancePt_utl_m = Double.NaN ;

	private Map<DataCollection,Boolean> dataCollectionConfig = new HashMap<DataCollection,Boolean>() ;
	private Map<DataCollection,StringBuffer> dataCollectionStrings = new HashMap<DataCollection,StringBuffer>() ;
	
	public RandomizedTransitRouterTravelTimeAndDisutility(TransitRouterConfig routerConfig) {
		super(routerConfig);
		
		prepareDataCollection();
		
		// make sure that some parameters are not zero since otherwise the randomization will not work:

		// marg utl time wlk should be around -3/h or -(3/3600)/sec.  Give warning if not at least 1/3600:
		if ( -routerConfig.getMarginalUtilityOfTravelTimeWalk_utl_s() < 1./3600. ) {
			Logger.getLogger(this.getClass()).warn( "marg utl of walk rather close to zero; randomization may not work") ;
		}
		// utl of line switch should be around -300sec or -0.5u.  Give warning if not at least 0.1u:
		if ( -routerConfig.getUtilityOfLineSwitch_utl() < 0.1 ) {
			Logger.getLogger(this.getClass()).warn( "utl of line switch rather close to zero; randomization may not work") ;
		}
			
		this.originalTransitRouterConfig = routerConfig ;

		this.localMarginalUtilityOfTravelDistancePt_utl_m = routerConfig.getMarginalUtilityOfTravelDistancePt_utl_m();
		this.localMarginalUtilityOfTravelTimePt_utl_s = routerConfig.getMarginalUtilityOfTravelTimePt_utl_s() ;
		this.localMarginalUtilityOfTravelTimeWalk_utl_s = routerConfig.getMarginalUtilityOfTravelTimeWalk_utl_s() ;
//		this.localMarginalUtilityOfWaitingPt_utl_s = routerConfig.getMarginalUtilityOfTravelTimePt_utl_s() ;
		this.localMarginalUtilityOfWaitingPt_utl_s = routerConfig.getMarginalUtilityOfWaitingPt_utl_s() ;
		this.localUtilityOfLineSwitch_utl = routerConfig.getUtilityOfLineSwitch_utl() ;
	}
	public final String getDataCollectionString( DataCollection item ) {
		return dataCollectionStrings.get(item).toString() ;
	}
	
	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle, 
			final CustomDataManager dataManager) {
		
		regenerateUtilityParametersIfPersonHasChanged(person);
				
		double disutl;
		if (((TransitRouterNetworkLink) link).getRoute() == null) {
			// (this means that it is a transfer link (walk))

			double transfertime = getLinkTravelTime(link, time, person, vehicle);
			double waittime = this.originalTransitRouterConfig.getAdditionalTransferTime();
			
			// say that the effective walk time is the transfer time minus some "buffer"
			double walktime = transfertime - waittime;
			
			disutl = - walktime * localMarginalUtilityOfTravelTimeWalk_utl_s
			       - waittime * localMarginalUtilityOfWaitingPt_utl_s
			       - localUtilityOfLineSwitch_utl;
			
		} else {
			
			double offVehWaitTime = offVehicleWaitTime(link, time);
			
			double inVehTime = getLinkTravelTime(link,time, person, vehicle) - offVehWaitTime ;

			disutl = -inVehTime * this.localMarginalUtilityOfTravelTimePt_utl_s 
					- offVehWaitTime * this.localMarginalUtilityOfWaitingPt_utl_s
					- link.getLength() * this.localMarginalUtilityOfTravelDistancePt_utl_m;
		}

		if ( this.dataCollectionConfig.get(DataCollection.additionalInformation )) {
			StringBuffer strb = this.dataCollectionStrings.get(DataCollection.additionalInformation ) ;
			strb.append("also collecting additional information") ;
		}

		return disutl;
	}
	
	@Override
	public double getTravelDisutility(Person person, Coord coord, Coord toCoord) {
		regenerateUtilityParametersIfPersonHasChanged(person);
		return - getTravelTime(person, coord, toCoord) * localMarginalUtilityOfTravelTimeWalk_utl_s ;
	}
	
	public final void setDataCollection( DataCollection item, Boolean bbb ) {
		Logger.getLogger(this.getClass()).info( " settin data collection of " + item.toString() + " to " + bbb.toString() ) ;
		dataCollectionConfig.put( item, bbb ) ;
	}

	private void prepareDataCollection() {
		for ( DataCollection dataCollection : DataCollection.values() ) {
			switch ( dataCollection ) {
			case randomizedParameters:
				dataCollectionConfig.put( dataCollection, false ) ;
				dataCollectionStrings.put( dataCollection, new StringBuffer() ) ;
				break;
			case additionalInformation:
				dataCollectionConfig.put( dataCollection, false ) ;
				dataCollectionStrings.put( dataCollection, new StringBuffer() ) ;
				break;
			}
		}
	}

	private void regenerateUtilityParametersIfPersonHasChanged(final Person person) {
		if ( !person.getId().equals(this.cachedPersonId)) {
			// yyyyyy probably not thread safe (?!?!)
			
			// person has changed, so ...
			
			// ... memorize new person id:
			this.cachedPersonId = person.getId() ;
			
			// ... generate new random parameters:
			{
				double tmp = this.originalTransitRouterConfig.getMarginalUtilityOfTravelTimeWalk_utl_s() ;
				tmp *= 5. * MatsimRandom.getRandom().nextDouble() ;
				localMarginalUtilityOfTravelTimeWalk_utl_s = tmp ;
				// yy if this becomes too small, they may walk the whole way (is it really clear why this can happen?)
			}
			{
				double tmp = this.originalTransitRouterConfig.getUtilityOfLineSwitch_utl() ;
				tmp *= 5. * MatsimRandom.getRandom().nextDouble() ;
				localUtilityOfLineSwitch_utl = tmp ;
			}
			{
				double tmp = this.originalTransitRouterConfig.getMarginalUtilityOfWaitingPt_utl_s();
				tmp *= 5. * MatsimRandom.getRandom().nextDouble();
				localMarginalUtilityOfWaitingPt_utl_s = tmp;
			}
			{
				// (Conceptually, the following is not necessary, but empirically, it seems to help. kai, jan'13)
				double tmp = this.originalTransitRouterConfig.getMarginalUtilityOfTravelTimePt_utl_s() ;
				tmp *= 5. * MatsimRandom.getRandom().nextDouble();
				localMarginalUtilityOfTravelTimePt_utl_s = tmp;
			}
			
			if ( this.dataCollectionConfig.get(DataCollection.randomizedParameters) ) {
//				StringBuffer strb = this.dataCollectionStrings.get(DataCollection.randomizedParameters) ;
//				strb.append
				System.out.println("personId: " + person.getId() +
						"; margUtlOfTimeWlk_h: " + this.localMarginalUtilityOfTravelTimeWalk_utl_s*3600. +
						"; utlOfLineSwitch: " + this.localUtilityOfLineSwitch_utl +
						"; margUtlOfWait_h: " + this.localMarginalUtilityOfWaitingPt_utl_s*3600. + 
						"; margUtlOfTimePt_h: " + this.localMarginalUtilityOfTravelTimePt_utl_s*3600. ) ;
			}
		}
	}
	
}

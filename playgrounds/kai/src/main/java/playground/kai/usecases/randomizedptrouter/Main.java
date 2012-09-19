/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package playground.kai.usecases.randomizedptrouter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.pt.router.CustomDataManager;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;

class RandomizedTransitRouterNetworkTravelTimeAndDisutility extends TransitRouterNetworkTravelTimeAndDisutility {
	
	Id cachedPersonId = null ;
	final TransitRouterConfig originalTransitRouterConfig ;
	TransitRouterConfig localConfig ;
	
	public RandomizedTransitRouterNetworkTravelTimeAndDisutility(TransitRouterConfig config) {
		super(config);
		
		// make sure that some parameters are not zero since otherwise the randomization will not work:

		// marg utl time wlk should be around -3/h or -(3/3600)/sec.  Give warning if not at least 1/3600:
		if ( -config.getMarginalUtilityOfTravelTimeWalk_utl_s() < 1./3600. ) {
			Logger.getLogger(this.getClass()).warn( "marg utl of walk rather close to zero; randomization may not work") ;
		}
		// utl of line switch should be around -300sec or -0.5u.  Give warning if not at least 0.1u:
		if ( -config.getUtilityOfLineSwitch_utl() < 0.1 ) {
			Logger.getLogger(this.getClass()).warn( "utl of line switch rather close to zero; randomization may not work") ;
		}

			
			this.originalTransitRouterConfig = config ;
			this.localConfig = config ;
	}
	
	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle, final CustomDataManager dataManager) {
		
		if ( !person.getId().equals(this.cachedPersonId)) {
			// person has changed, so ...
			
			// ... memorize new person id:
			this.cachedPersonId = person.getId() ;
			
			// ... generate new random parameters
			{
				double tmp = this.originalTransitRouterConfig.getMarginalUtilityOfTravelTimeWalk_utl_s() ;
				tmp *= 5. * MatsimRandom.getRandom().nextDouble() ;
				localConfig.setMarginalUtilityOfTravelTimeWalk_utl_s(tmp) ;
			}
			{
				double tmp = this.originalTransitRouterConfig.getUtilityOfLineSwitch_utl() ;
				tmp *= 5. * MatsimRandom.getRandom().nextDouble() ;
				localConfig.setUtilityOfLineSwitch_utl(tmp) ;
			}
			
		}
		
		
		double disutl;
		if (((TransitRouterNetworkLink) link).getRoute() == null) {
			// this means that it is a transfer link (walk)

			double transfertime = getLinkTravelTime(link, time, person, vehicle);
			double waittime = localConfig.additionalTransferTime;
			
			// say that the effective walk time is the transfer time minus some "buffer"
			double walktime = transfertime - waittime;
			
			disutl = -walktime * localConfig.getMarginalUtilityOfTravelTimeWalk_utl_s()
			       -waittime * localConfig.getMarginalUtiltityOfWaiting_utl_s()
			       - localConfig.getUtilityOfLineSwitch_utl();
			
		} else {
			// this means that the time it is a travel link.  With this version, we cannot differentiate between in-vehicle
			// wait and out-of-vehicle wait, but my current intuition is that this will not matter that much (despite what is
			// said in the literature).  kai, sep'12
			
			disutl = - getLinkTravelTime(link, time, person, vehicle) * localConfig.getMarginalUtilityOfTravelTimePt_utl_s() 
			       - link.getLength() * localConfig.getMarginalUtilityOfTravelDistancePt_utl_m();
		}
		return disutl;
	}


}

class Main {

	public static void main(String[] args) {
		final Controler ctrl = new Controler(args) ;
		
		ctrl.setOverwriteFiles(true) ;
		ctrl.getConfig().vspExperimental().setUsingOpportunityCostOfTimeInPtRouting(true) ;
		
		final TransitSchedule schedule = ctrl.getScenario().getTransitSchedule() ;
		
		final TransitRouterConfig trConfig = new TransitRouterConfig( ctrl.getScenario().getConfig() ) ; 
		
		final TransitRouterNetwork routerNetwork = TransitRouterNetwork.createFromSchedule(schedule, trConfig.beelineWalkConnectionDistance);
		
		ctrl.setTransitRouterFactory( new TransitRouterFactory() {

			@Override
			public TransitRouter createTransitRouter() {
				TransitRouterNetworkTravelTimeAndDisutility ttCalculator = new RandomizedTransitRouterNetworkTravelTimeAndDisutility(trConfig);
				return new TransitRouterImpl(trConfig, routerNetwork, ttCalculator, ttCalculator);
			}
			
		}) ;
		
		ctrl.run() ;

	}

}

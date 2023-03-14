package org.matsim.contrib.bicycle;
//import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class PsafeNewStyleScoring implements SumScoringFunction.LegScoring, SumScoringFunction.ArbitraryEventScoring{
	// NTUA TEAM: becomes EscootLegScoring - new function for other modes are developed by NTUA.
	// NTUA TEAM: create a new class per mode
	// private static final Logger log = Logger.getLogger( BicycleLegScoring.class ) ; // 

	private final CharyparNagelLegScoring delegate ; // default scoring function
	// private final double marginalUtilityOfPerceivedSafety_m; // beta parameter of perceived safety - different per mode
	
	private final double marginalUtilityOfPerceivedSafety_car;
	private final double marginalUtilityOfPerceivedSafety_ebike;
	private final double marginalUtilityOfPerceivedSafety_escoot;
	private final double marginalUtilityOfPerceivedSafety_walk;
		
	private final double Dmax_car;
	private final double Dmax_ebike;
	private final double Dmax_escoot;
	private final double Dmax_walk;
	
	private final int inputPsafeThreshold;
	
	private double beta_psafe = 0.;
	private double dmax = 1.;
	private String mode = "car";
	
	private double sumPerceivedSafety; // it gives sum(psafe*distance of link i) from UtilityUtils
	// private double sumDistance;
	// private double minPerceivedSafety;
	// private final String bicycleMode; // NTUA TEAM: change to escootMode when we will update esoot trip execution functions
	
	private final String carMode;
	private final String ebikeMode;
	private final String escootMode;
	private final String walkMode;
	
	private final Network network;
	private double additionalScore = 0.; // this the additional score we get, set to zero per plan (not per leg !!)
	
	PsafeNewStyleScoring( final ScoringParameters params, Network network, Set<String> ptModes, PsafeConfigGroup psafeConfigGroup) {
		// NTUA TEAM: BicycleConfigGroup becomes PsafeConfigGroup
		delegate = new CharyparNagelLegScoring( params, network, ptModes ) ;
		// this.marginalUtilityOfPerceivedSafety_m = bicycleConfigGroup.getMarginalUtilityOfPerceivedSafety_m();
		// this.marginalUtilityOfPerceivedSafety_m = 0;
		
		// set new beta variables related to perceived safety
		this.marginalUtilityOfPerceivedSafety_car = psafeConfigGroup.getMarginalUtilityOfPerceivedSafety_car_m();
		this.marginalUtilityOfPerceivedSafety_ebike = psafeConfigGroup.getMarginalUtilityOfPerceivedSafety_ebike_m();
		this.marginalUtilityOfPerceivedSafety_escoot = psafeConfigGroup.getMarginalUtilityOfPerceivedSafety_escoot_m();
		this.marginalUtilityOfPerceivedSafety_walk = psafeConfigGroup.getMarginalUtilityOfPerceivedSafety_walk_m();
		
		this.Dmax_car = psafeConfigGroup.getDmax_car_m();
		this.Dmax_ebike = psafeConfigGroup.getDmax_ebike_m();
		this.Dmax_escoot = psafeConfigGroup.getDmax_escoot_m();
		this.Dmax_walk = psafeConfigGroup.getDmax_walk_m();
		
		this.inputPsafeThreshold = psafeConfigGroup.getInputPsafeThreshold_m();
		// this.bicycleMode = bicycleConfigGroup.getBicycleMode(); // NTUA TEAM: bicycleMode becomes escootMode
		
		this.carMode = psafeConfigGroup.getCarMode();
		this.ebikeMode = psafeConfigGroup.getEbikeMode();
		this.escootMode = psafeConfigGroup.getEscootMode();
		this.walkMode = psafeConfigGroup.getWalkMode();
		
		this.network = network ;}

	private void calcLegScore( final Leg leg ) {
		if(carMode.equals(leg.getMode())) {
			beta_psafe = marginalUtilityOfPerceivedSafety_car;
			dmax = Dmax_car;
			mode = leg.getMode();}
		if(ebikeMode.equals(leg.getMode())) {
			beta_psafe = marginalUtilityOfPerceivedSafety_ebike;
			dmax = Dmax_ebike;
			mode = leg.getMode();}
		if (escootMode.equals(leg.getMode())) {
			beta_psafe = marginalUtilityOfPerceivedSafety_escoot;
			dmax = Dmax_escoot;
			mode = leg.getMode();}
		if (walkMode.equals(leg.getMode())) {
			beta_psafe = marginalUtilityOfPerceivedSafety_walk;
			dmax = Dmax_walk;
			mode = leg.getMode();}
		
		if (!isSameStartAndEnd(leg)) {
			NetworkRoute networkRoute = (NetworkRoute) leg.getRoute();
			List<Id<Link>> linkIds = new ArrayList<>(networkRoute.getLinkIds());
			linkIds.add(networkRoute.getEndLinkId());
			
			sumPerceivedSafety = 0.; // set of sumPerceivedSafety equal to zero before iterating of links 
			// sumDistance = 0. ; // set of sumDistance equal to zero before iterating links
			// minPerceivedSafety = 7;
			
			for (Id<Link> linkId : linkIds) {
				Link link = network.getLinks().get(linkId);
				double distance = link.getLength(); // this is the length of link i
//				double scoreOnLink = BicycleUtilityUtils.computeLinkBasedScore(network.getLinks().get(linkId),
//						beta_psafe); // NTUA TEAM: definitely BicycleUtilityUtils is not necessary for this case
//			    
				double scoreOnLink = PsafeInput.computePsafeScore(network.getLinks().get(linkId), 
						mode, inputPsafeThreshold);
//				sumDistance += distance; estimate the total distance of the leg FIX FIX FIX FIX FIX
//			    sumPerceivedSafety += scoreOnLink; 
			    sumPerceivedSafety += scoreOnLink * distance/dmax; // estimate based on the new utility function I developec
//			    if (scoreOnLink < minPerceivedSafety) {
//			    	minPerceivedSafety = scoreOnLink;}
			}
			
			
			additionalScore = beta_psafe * sumPerceivedSafety;
		    System.out.println("The additional psafe score is" + additionalScore);
		}
	  }
	    
	private static boolean isSameStartAndEnd(Leg leg) {
		return leg.getRoute().getStartLinkId().toString().equals(leg.getRoute().getEndLinkId().toString());
	}

	@Override public void finish(){
		delegate.finish();
	}

	@Override public double getScore(){
		// System.out.print(this.additionalScore);
		return delegate.getScore() + this.additionalScore ; // add the additional score to the default score of MATSim
	}

	@Override public void handleLeg( Leg leg ){
		delegate.handleLeg( leg );
		calcLegScore( leg );
	}

	@Override public void handleEvent( Event event ){
		delegate.handleEvent( event );
	}
}

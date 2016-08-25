package org.matsim.contrib.carsharing.scoring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.carsharing.events.EndRentalEvent;
import org.matsim.contrib.carsharing.events.StartRentalEvent;
import org.matsim.core.config.Config;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import com.google.common.collect.ImmutableSet;


public class CarsharingLegScoringFunction extends org.matsim.core.scoring.functions.CharyparNagelLegScoring {

	
	private Config config;
	
	
	private double totalffRentalTime = 0.0;
	private double totalowRentalTime = 0.0;
	private double totaltwRentalTime = 0.0;
	
	private Map<String, Stats> vehicleIdStatsMap = new HashMap<String, Stats>();	
	private ArrayList<Stats> ffcsRentals = new ArrayList<Stats>();

	private ArrayList<Stats> owcsRentals = new ArrayList<Stats>();
	
	private ArrayList<Stats> twcsRentals = new ArrayList<Stats>();
	
	private HashMap<Id<Link>, Stats> twMap = new HashMap<Id<Link>, Stats>();
	
	private Stats ffStats;
	private Stats owStats;
	private Stats twStats;
	
	static final  Set<String> walkingLegs = ImmutableSet.of("egress_walk_ow", "access_walk_ow",
			"egress_walk_tw", "access_walk_tw", "egress_walk_ff", "access_walk_ff");
	
	static final  Set<String> carsharingLegs = ImmutableSet.of("oneway", "twoway",
			"freefloating");
	
	public CarsharingLegScoringFunction(CharyparNagelScoringParameters params, Config config,  Network network)
	{
		super(params, network);
		this.config = config;

		owcsRentals = new ArrayList<Stats>();

		twcsRentals = new ArrayList<Stats>();

		twMap = new HashMap<Id<Link>, Stats>();

		totalffRentalTime = 0.0;
		totalowRentalTime = 0.0;
		totaltwRentalTime = 0.0;
		ffStats = new Stats();
		owStats= new Stats();;
		twStats= new Stats();;
	}

	@Override
	public void handleEvent(Event event) {
		if ( event instanceof EndRentalEvent ) {
			
			
			if (((EndRentalEvent) event).getvehicleId().startsWith("FF")) {
				totalffRentalTime += event.getTime();
			}
			else if (((EndRentalEvent) event).getvehicleId().startsWith("OW")) {
				totalowRentalTime += event.getTime();
			}
			else if (((EndRentalEvent) event).getvehicleId().startsWith("TW")) {
				totaltwRentalTime += event.getTime();
			}
		
		}
		else if (event instanceof StartRentalEvent) {
			
			if (((StartRentalEvent) event).getvehicleId().startsWith("FF"))
				totalffRentalTime -= event.getTime();
			else if (((StartRentalEvent) event).getvehicleId().startsWith("OW"))
				totalowRentalTime -= event.getTime();
			else if (((StartRentalEvent) event).getvehicleId().startsWith("TW"))
				totaltwRentalTime -= event.getTime();
			
		}
		super.handleEvent(event);		
	}	
	
	@Override
	public void finish() {		
		super.finish();		
		
	
			
			score += this.ffStats.distance * Double.parseDouble(this.config.getModule("FreeFloating").getParams().get("distanceFeeFreeFloating"));
			score += this.ffStats.drivingTime * Double.parseDouble(this.config.getModule("FreeFloating").getParams().get("timeFeeFreeFloating"));
			score += (this.totalffRentalTime - this.ffStats.drivingTime) * Double.parseDouble(this.config.getModule("FreeFloating").getParams().get("timeParkingFeeFreeFloating"));
			
		
		
		
			
			score += this.owStats.distance * Double.parseDouble(this.config.getModule("OneWayCarsharing").getParams().get("distanceFeeOneWayCarsharing"));
			score += this.owStats.drivingTime * Double.parseDouble(this.config.getModule("OneWayCarsharing").getParams().get("timeFeeOneWayCarsharing"));
			score += (this.totalowRentalTime - this.owStats.drivingTime) * Double.parseDouble(this.config.getModule("OneWayCarsharing").getParams().get("timeParkingFeeOneWayCarsharing"));		

			
		
				
			score += this.twStats.distance * Double.parseDouble(this.config.getModule("TwoWayCarsharing").getParams().get("distanceFeeTwoWayCarsharing"));
			score += this.twStats.drivingTime * Double.parseDouble(this.config.getModule("TwoWayCarsharing").getParams().get("timeFeeTwoWayCarsharing"));
				//score += (s.endTime - s.startTime - s.drivingTime) * Double.parseDouble(this.config.getModule("TwoWayCarsharing").getParams().get("timeParkingFeeFreeFloating"));
			
					
				
	}	
	
	@Override
	protected double calcLegScore(double departureTime, double arrivalTime, Leg leg) {
		
		
		double tmpScore = 0.0D;
		double distance = leg.getRoute().getDistance();
		double travelTime = arrivalTime - departureTime;
		String mode = leg.getMode();
		if (carsharingLegs.contains(mode)) {
			if (mode.equals("freefloating"))
				this.ffStats.distance += distance;
			else if (mode.equals("oneway"))
				this.owStats.distance += distance;
			else if (mode.equals("twoway"))
				this.twStats.distance += distance;
			
			
			if (("oneway").equals(mode)) {				
				tmpScore += Double.parseDouble(this.config.getModule("OneWayCarsharing").getParams().get("constantOneWayCarsharing"));
				tmpScore += travelTime * Double.parseDouble(this.config.getModule("OneWayCarsharing").getParams().get("travelingOneWayCarsharing")) / 3600.0;
			}		
		
			else if (("freefloating").equals(mode)) {				
				
				tmpScore += Double.parseDouble(this.config.getModule("FreeFloating").getParams().get("constantFreeFloating"));
				tmpScore += travelTime * Double.parseDouble(this.config.getModule("FreeFloating").getParams().get("travelingFreeFloating")) / 3600.0;
			}		
			
			else if (("twoway").equals(mode)) {				
				
				tmpScore += Double.parseDouble(this.config.getModule("TwoWayCarsharing").getParams().get("constantTwoWayCarsharing"));
				tmpScore += travelTime * Double.parseDouble(this.config.getModule("TwoWayCarsharing").getParams().get("travelingTwoWayCarsharing")) / 3600.0;
			}
		}
		
		else if (walkingLegs.contains(mode)) {
			
			tmpScore += getWalkScore(leg.getRoute().getDistance(), travelTime);
			
		}			
		
		
		return tmpScore;
	}

	private double getWalkScore(double distance, double travelTime)
	{
		double score = 0.0D;

		score += travelTime * this.params.modeParams.get(TransportMode.walk).marginalUtilityOfTraveling_s + this.params.modeParams.get(TransportMode.walk).marginalUtilityOfDistance_m * distance;

		return score;
	}
		
	private class Stats {
		private double distance;
		private double drivingTime = 0.0;
		
	}
	
}

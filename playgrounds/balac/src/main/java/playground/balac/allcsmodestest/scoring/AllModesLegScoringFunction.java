package playground.balac.allcsmodestest.scoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.Config;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;


public class AllModesLegScoringFunction extends org.matsim.core.scoring.functions.CharyparNagelLegScoring {

	private PlanImpl plan;
	
	private Config config;
	
	private ArrayList<Stats> freefloatingRentals = new ArrayList<Stats>();
	
	private ArrayList<Stats> owcsRentals = new ArrayList<Stats>();
	
	private ArrayList<Stats> twcsRentals = new ArrayList<Stats>();
	
	private HashMap<Id, Stats> twMap = new HashMap<Id, Stats>();
	
	private double distancetw = 0.0;
	
	
	public AllModesLegScoringFunction(PlanImpl plan, CharyparNagelScoringParameters params, Config config,  Network network)
	{
		super(params, network);
		this.plan = plan;		
		this.config = config;
	}
	@Override
	public void reset() {
		super.reset();
		freefloatingRentals = new ArrayList<Stats>();
		
		owcsRentals = new ArrayList<Stats>();
		
		twcsRentals = new ArrayList<Stats>();
		
		twMap = new HashMap<Id, Stats>();
		
		distancetw = 0.0;
		
		
	}
	
	@Override
	public void finish() {		
		super.finish();
		
		double distance = 0.0;
		double time = 0.0;
		double specialTime = 0.0;
		if (!freefloatingRentals.isEmpty()) {
			
			double specialStartTime = Double.parseDouble(this.config.getModule("FreeFloating").getParams().get("specialStartTime"));
			double specialEndTime = Double.parseDouble(this.config.getModule("FreeFloating").getParams().get("specialEndTime"));

			for(Stats s:freefloatingRentals) {
			
				distance += s.distance;
				
				if (s.startTime > specialEndTime || s.endTime < specialStartTime)
				
					time += (s.endTime - s.startTime);
				else {
					
					boolean startBefore = s.startTime < specialStartTime;
					boolean endBefore = s.endTime < specialEndTime;
					
					if (startBefore && endBefore) {
						
						specialTime += s.endTime - specialEndTime;
						time += specialStartTime - s.startTime;
					}
					else if (!startBefore && endBefore) {
						specialTime += s.endTime - s.startTime;
					}
					else if (!startBefore && !endBefore) {
						
						specialTime = specialEndTime - s.startTime;
						time += s.endTime - specialEndTime;
					}
					else {
						
						specialTime += specialEndTime - specialStartTime;
						time += specialStartTime - s.startTime;
						time += s.endTime - specialEndTime;
					}
				}
			}
			
			
			
			score += distance * Double.parseDouble(this.config.getModule("FreeFloating").getParams().get("distanceFeeFreeFloating"));
			score += time * Double.parseDouble(this.config.getModule("FreeFloating").getParams().get("timeFeeFreeFloating"));
			score += specialTime * Double.parseDouble(this.config.getModule("FreeFloating").getParams().get("specialTimeFee"));
			
			
		}
		distance = 0.0;
		time = 0.0;
		if (!owcsRentals.isEmpty()) {
			for(Stats s:owcsRentals) {
			
				distance += s.distance;
				time += (s.endTime - s.startTime);

			}
			
			score += distance * Double.parseDouble(this.config.getModule("OneWayCarsharing").getParams().get("distanceFeeOneWayCarsharing"));
			score += time * Double.parseDouble(this.config.getModule("OneWayCarsharing").getParams().get("timeFeeOneWayCarsharing"));

		}
		distance = 0.0;
		time = 0.0;
		if (!twcsRentals.isEmpty()) {
			for(Stats s:twcsRentals) {
			
				distance += s.distance;
				time = (s.endTime - s.startTime);
				int timeInt = (int) (time / 1800.0)  + 1;
				score += timeInt * 1800 * Double.parseDouble(this.config.getModule("TwoWayCarsharing").getParams().get("timeFeeTwoWayCarsharing"));
				
			}
			
			score += distancetw * Double.parseDouble(this.config.getModule("TwoWayCarsharing").getParams().get("distanceFeeTwoWayCarsharing"));
			
		}
		
		
		
	}	
	
	
	@Override
	protected double calcLegScore(double departureTime, double arrivalTime, Leg leg) {
		
		
		double tmpScore = 0.0D;
		double travelTime = arrivalTime - departureTime;
		
		double dist = 0.0D;
		
		
		if (leg.getMode().equals("freefloating")) {
			
			Stats s = new Stats();
			s.startTime = departureTime;
			s.endTime = arrivalTime;
			s.distance = leg.getRoute().getDistance();
			freefloatingRentals.add(s);
			
		}
		else if (leg.getMode().equals("onewaycarsharing")) {
			Stats s = new Stats();
			s.startTime = departureTime;
			s.endTime = arrivalTime;
			s.distance = leg.getRoute().getDistance();
			owcsRentals.add(s);
			
		}
		else if (leg.getMode().equals("twowaycarsharing")) {
			
			distancetw += leg.getRoute().getDistance();	
			
		}
		
		else if (leg.getMode().equals("walk_rb")) {
			
			Id startLinkId = leg.getRoute().getStartLinkId();
			Id endLinkId = leg.getRoute().getEndLinkId();
			if (twMap.containsKey(endLinkId)) {
				
				twMap.get(endLinkId).endTime = departureTime;
				twcsRentals.add(twMap.remove(endLinkId));
			}
			else {
				
				Stats s = new Stats();
				s.startTime = arrivalTime;
				s.endTime = arrivalTime;
				
				twMap.put(startLinkId, s);
				
			}
		}
		
		if (TransportMode.car.equals(leg.getMode()))
		{
			tmpScore += this.params.modeParams.get(TransportMode.car).constant;
			
			tmpScore += travelTime * this.params.modeParams.get(TransportMode.car).marginalUtilityOfTraveling_s;
		}
		else if (("onewaycarsharing").equals(leg.getMode())) {				
			
			travelTime = arrivalTime - departureTime;
			tmpScore += Double.parseDouble(this.config.getModule("OneWayCarsharing").getParams().get("constantOneWayCarsharing"));
			tmpScore += travelTime * Double.parseDouble(this.config.getModule("OneWayCarsharing").getParams().get("travelingOneWayCarsharing")) / 3600.0;
		}
		
		else if (TransportMode.pt.equals(leg.getMode()))
		{

      			
			tmpScore += getPtScore(dist, travelTime);
			

		}
		else if (("freefloating").equals(leg.getMode())) {				
			
			travelTime = arrivalTime - departureTime;
			tmpScore += Double.parseDouble(this.config.getModule("FreeFloating").getParams().get("constantFreeFloating"));
			tmpScore += travelTime * Double.parseDouble(this.config.getModule("FreeFloating").getParams().get("travelingFreeFloating")) / 3600.0;
		}	
		
		else if (leg.getMode().equals("walk_ff"))
		{
			
			tmpScore += getWalkScore(dist, travelTime);
		}
		else if (TransportMode.walk.equals(leg.getMode()))
		{
			
			tmpScore += getWalkScore(dist, travelTime);
		}
		else if (leg.getMode().equals("walk_ow_sb"))
		{
			
			tmpScore += getWalkScore(dist, travelTime);

		}
		else if (("twowaycarsharing").equals(leg.getMode())) {				
			
			travelTime = arrivalTime - departureTime;
			tmpScore += Double.parseDouble(this.config.getModule("TwoWayCarsharing").getParams().get("constantTwoWayCarsharing"));
			tmpScore += travelTime * Double.parseDouble(this.config.getModule("TwoWayCarsharing").getParams().get("travelingTwoWayCarsharing")) / 3600.0;
		}
		
		else if (leg.getMode().equals("walk_rb"))
		{
			
			tmpScore += getWalkScore(dist, travelTime);

		}
		else if (TransportMode.bike.equals(leg.getMode()))
		{
			tmpScore += this.params.modeParams.get(TransportMode.bike).constant;

			tmpScore += travelTime * this.params.modeParams.get(TransportMode.bike).marginalUtilityOfTraveling_s;
		}
		else if (TransportMode.ride.equals(leg.getMode()))
		{
			
			travelTime *= 3.0D;

			tmpScore += getRideScore(dist, travelTime);
		}
		else
		{
			
			tmpScore += travelTime * this.params.modeParams.get(TransportMode.car).marginalUtilityOfTraveling_s + this.params.modeParams.get(TransportMode.car).marginalUtilityOfDistance_m * dist;
		}

		return tmpScore;
	}

	private double getRideScore(double distance, double travelTime) {

		
		return -100;
	}

	private double getWalkScore(double distance, double travelTime)
	{
		double score = 0.0D;

		score += travelTime * this.params.modeParams.get(TransportMode.walk).marginalUtilityOfTraveling_s + this.params.modeParams.get(TransportMode.walk).marginalUtilityOfDistance_m * distance;

		return score;
	}

	private double getPtScore(double distance, double travelTime)
	{
		double score = 0.0D;

		double distanceCost = 0.0D;
		TreeSet<String> travelCards = PersonUtils.getTravelcards(this.plan.getPerson());
		/*if (travelCards == null || travelCards.contains("ch-HT-mobility"))
			distanceCost = this.params.modeParams.get(TransportMode.pt).marginalUtilityOfDistance_m;
		else if (travelCards.contains("unknown"))
			distanceCost = this.params.modeParams.get(TransportMode.pt).marginalUtilityOfDistance_m;
		else {
			throw new RuntimeException("Person " + this.plan.getPerson().getId() + " has an invalid travelcard. This should never happen.");
		}*/
		score += this.params.modeParams.get(TransportMode.pt).marginalUtilityOfDistance_m * distanceCost / 1000.0D * distance;
		score += travelTime * this.params.modeParams.get(TransportMode.pt).marginalUtilityOfTraveling_s;
		score += this.params.modeParams.get(TransportMode.pt).constant;

		return score;
	}
	
	private class Stats {
		private double startTime;
		private double endTime;
		private double distance;
		
	}
	
}

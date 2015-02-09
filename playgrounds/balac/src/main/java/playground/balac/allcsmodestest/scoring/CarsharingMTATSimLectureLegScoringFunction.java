package playground.balac.allcsmodestest.scoring;

import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.SumScoringFunction.LegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;




public class CarsharingMTATSimLectureLegScoringFunction implements LegScoring, ScoringFunctionAccumulator.LegScoring {

	
private ArrayList<Stats> freefloatingRentals = new ArrayList<Stats>();
	


	private ArrayList<Stats> owcsRentals = new ArrayList<Stats>();
	
	private ArrayList<Stats> twcsRentals = new ArrayList<Stats>();
	
	private HashMap<Id, Stats> twMap = new HashMap<Id, Stats>();
	
	private double distancetw = 0.0;	
	
	private double score = 0;

	private final String mode;
	private final Config config;
	private final Network network;
	private final CarsharingLegScoringParameters params;
	
	public CarsharingMTATSimLectureLegScoringFunction(
			final String mode,
			final Config config,
			final Network network,
			final CarsharingLegScoringParameters params) {
		this.mode = mode;
		this.config = config;
		this.network = network;
		this.params = params;
		this.reset();
	}
	
	
	
	@Override
	public void finish() {
		// TODO Auto-generated method stub
		double distance = 0.0;
		double time = 0.0;
		double specialTime = 0.0;
		if (!freefloatingRentals.isEmpty()) {
			
			double specialStartTime = Double.parseDouble(this.config.getModule("FreeFloating").getParams().get("specialTimeStart"));
			double specialEndTime = Double.parseDouble(this.config.getModule("FreeFloating").getParams().get("specialTimeEnd"));

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
	public double getScore() {
		// TODO Auto-generated method stub
		return this.score;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		freefloatingRentals = new ArrayList<Stats>();
		
		owcsRentals = new ArrayList<Stats>();
		
		twcsRentals = new ArrayList<Stats>();
		
		twMap = new HashMap<Id, Stats>();
		
		distancetw = 0.0;
	}
	
	
	
	

	@Override
	public void startLeg(double time, Leg leg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void endLeg(double time) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleLeg(Leg leg) {
		score += calcLegScore( leg.getDepartureTime(), leg.getDepartureTime() + leg.getTravelTime(), leg );
		
	}
	
    private double calcLegScore(double departureTime, double arrivalTime, Leg leg) {
		
		
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
			
			Id<Link> startLinkId = leg.getRoute().getStartLinkId();
			Id<Link> endLinkId = leg.getRoute().getEndLinkId();
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
		
		if (("onewaycarsharing").equals(leg.getMode())) {				
			
			travelTime = arrivalTime - departureTime;
			tmpScore += Double.parseDouble(this.config.getModule("OneWayCarsharing").getParams().get("constantOneWayCarsharing"));
			tmpScore += travelTime * Double.parseDouble(this.config.getModule("OneWayCarsharing").getParams().get("travelingOneWayCarsharing")) / 3600.0;
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
		
		return tmpScore;
	}
	
	private double getWalkScore(double distance, double travelTime) {
		
	double score = 0.0D;

	score += travelTime * this.params.marginalUtilityOfTraveling_s + this.params.marginalUtilityOfDistance_m * distance;

	return score;
}
	
	public static class CarsharingLegScoringParameters {
		private final double constant;
		private final double marginalUtilityOfTraveling_s;
		private final double marginalUtilityOfDistance_m;

		public CarsharingLegScoringParameters(
				final double constant,
				final double marginalUtilityOfTraveling_s,
				final double marginalUtilityOfDistance_m) {
			this.constant = constant;
			this.marginalUtilityOfTraveling_s = marginalUtilityOfTraveling_s;
			this.marginalUtilityOfDistance_m = marginalUtilityOfDistance_m;
		}
		
		public static CarsharingLegScoringParameters createForWalk(final CharyparNagelScoringParameters params) {
			return new CarsharingLegScoringParameters(
					params.modeParams.get(TransportMode.walk).constant,
					params.modeParams.get(TransportMode.walk).marginalUtilityOfTraveling_s,
					params.modeParams.get(TransportMode.walk).marginalUtilityOfDistance_m);
		}
	}
	
	private class Stats {
		private double startTime;
		private double endTime;
		private double distance;
		
	}

}

package playground.balac.aam.scoring;

import java.util.TreeSet;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.Config;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;


public class AAMLegScoringFunction extends org.matsim.core.scoring.functions.CharyparNagelLegScoring {

	private PlanImpl plan;
	
	private Config config;
	
	public AAMLegScoringFunction(PlanImpl plan, CharyparNagelScoringParameters params, Config config,  Network network)
	{
		super(params, network);
		this.plan = plan;		
		this.config = config;
	}
	@Override
	public void reset() {
		super.reset();
		
		
		
	}
	
	@Override
	public void finish() {		
		super.finish();		
		
	}	
	
	
	@Override
	protected double calcLegScore(double departureTime, double arrivalTime, Leg leg) {
		
		
		double tmpScore = 0.0D;
		double travelTime = arrivalTime - departureTime;
		
		double dist = 0.0D;
		
		if (TransportMode.car.equals(leg.getMode()))
		{
			tmpScore += this.params.modeParams.get(TransportMode.car).constant;
			
			tmpScore += travelTime * this.params.modeParams.get(TransportMode.car).marginalUtilityOfTraveling_s;
		}
		else if (("movingpathways").equals(leg.getMode())) {				
			
			tmpScore += Double.parseDouble(this.config.getModule("MovingPathways").getParams().get("constantMovingPathways"));
			tmpScore += travelTime * Double.parseDouble(this.config.getModule("MovingPathways").getParams().get("travelingMovingPathways")) / 3600.0;
		}
		
		else if (TransportMode.pt.equals(leg.getMode()))
		{

      			
			tmpScore += getPtScore(dist, travelTime);
			

		}
		
				
		else if (TransportMode.walk.equals(leg.getMode()))
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
		TreeSet<String> travelCards = PersonImpl.getTravelcards(this.plan.getPerson());
		if (travelCards == null || travelCards.contains("ch-HT-mobility"))
			distanceCost = this.params.modeParams.get(TransportMode.pt).marginalUtilityOfDistance_m;
		else if (travelCards.contains("unknown"))
			distanceCost = this.params.modeParams.get(TransportMode.pt).marginalUtilityOfDistance_m;
		else {
			throw new RuntimeException("Person " + this.plan.getPerson().getId() + " has an invalid travelcard. This should never happen.");
		}
		score += this.params.modeParams.get(TransportMode.pt).marginalUtilityOfDistance_m * distanceCost / 1000.0D * distance;
		score += travelTime * this.params.modeParams.get(TransportMode.pt).marginalUtilityOfTraveling_s;
		score += this.params.modeParams.get(TransportMode.pt).constant;

		return score;
	}
	
	
	
}

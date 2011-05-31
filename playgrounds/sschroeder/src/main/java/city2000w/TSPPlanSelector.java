package city2000w;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TransportServiceProviderImpl;

public class TSPPlanSelector {
	
	public class RandomScoredPlan {
		private TSPPlan plan;
		private Double randomisedScore;
		
		public RandomScoredPlan(TSPPlan plan, Double randomisedScore) {
			super();
			this.plan = plan;
			this.randomisedScore = randomisedScore;
		}

		TSPPlan getPlan() {
			return plan;
		}

		Double getRandomisedScore() {
			return randomisedScore;
		}
		
	}
	
	private static Logger logger = Logger.getLogger(TSPPlanSelector.class);

	private Random randomiser = new Random();
	private int iteration;
	private TransportServiceProviderImpl tsp;
	private Double MAXDERIVATION = 0.4;

	public TSPPlanSelector(TransportServiceProviderImpl tsp, int iteration) {
		super();
		this.tsp = tsp;
		this.iteration = iteration;
		
	}

	public void run() {
		if(iteration<50){
			TSPPlan plan = selectRandomly();
			tsp.setSelectedPlan(plan);
			return;
		}
		if(unscoredPlan()){
			TSPPlan plan = getUnscoredPlan();
			tsp.setSelectedPlan(plan);
			return;
		}
		List<RandomScoredPlan> randomScoredPlans = new ArrayList<RandomScoredPlan>();
		for(TSPPlan p : tsp.getPlans()){
			Double randomPlanScore = getRandomPlanScore(p.getScore());
			RandomScoredPlan rsPlan = new RandomScoredPlan(p, randomPlanScore);
			logger.info("planScore="+p.getScore()+", randomPlanScore="+randomPlanScore);
			randomScoredPlans.add(rsPlan);
		}
		RandomScoredPlan bestPlan = null;
		for(RandomScoredPlan p : randomScoredPlans){
			if(bestPlan == null){
				bestPlan = p;
				continue;
			}
			else if(p.getRandomisedScore() > bestPlan.getRandomisedScore()){
				bestPlan = p;
			}
		}
		logger.info("bestPlan had a randomPlanScore of " + bestPlan.getRandomisedScore());
		logger.info(bestPlan.getPlan());
		tsp.setSelectedPlan(bestPlan.getPlan());
	}

	private TSPPlan selectRandomly() {
		int nOPlans = tsp.getPlans().size();
		int index = randomiser.nextInt(nOPlans);
		int count=0;
		for(TSPPlan p : tsp.getPlans()){
			if(count == index){
				return p;
			}
			count++;
		}
		throw new IllegalStateException("could not select a plan");
	}

	private Double getRandomPlanScore(Double score) {
		double maxRandomDerivation = score*MAXDERIVATION;
		double randomDerivation = maxRandomDerivation*Math.random();
		if(Math.random()<0.5){
			return score-randomDerivation;
		}
		else{
			return score+randomDerivation;
		}
	}

	private TSPPlan getUnscoredPlan() {
		for(TSPPlan p : tsp.getPlans()){
			if(p.getScore() == null){
				return p;
			}
		}
		throw new IllegalStateException("no unscored plan available");
	}

	private boolean unscoredPlan() {
		for(TSPPlan p : tsp.getPlans()){
			if(p.getScore() == null){
				return true;
			}
		}
		return false;
	}
	
	

}

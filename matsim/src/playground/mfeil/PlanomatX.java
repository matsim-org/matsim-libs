/**
 * 
 */
package playground.mfeil;

import org.matsim.network.NetworkLayer;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.planomat.*;
import org.matsim.router.PlansCalcRouteLandmarks;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;
import org.matsim.population.Act;
import org.matsim.scoring.PlanScorer;
import org.matsim.scoring.ScoringFunctionFactory;
import java.util.ArrayList;


/**
 * @author Matthias Feil
 * PlanomatX will be the class where to implement the Tabu Search. Currently, work focus is on the definition of 
 * the neighbourhood. Changing the order of activities already works. Next is to integrate the TS mechanisms.
 */

public class PlanomatX implements org.matsim.population.algorithms.PlanAlgorithm { 
	
	private final int 						NEIGHBOURHOOD_SIZE, MAX_ITERATIONS;
	private final double 					WEIGHT_CHANGE_ORDER, WEIGHT_CHANGE_NUMBER;// weightChangeType;
	private final PlanAlgorithm 			planomatAlgorithm;
	private final PlansCalcRouteLandmarks 	router;
	private final PlanScorer 				scorer;
	
	
	
	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////
		
	public PlanomatX (LegTravelTimeEstimator legTravelTimeEstimator, NetworkLayer network, TravelCost costCalculator,
			TravelTime timeCalculator, PreProcessLandmarks commonRouterDatafinal, ScoringFunctionFactory factory) {

		planomatAlgorithm = new PlanOptimizeTimes (legTravelTimeEstimator);
		router = new PlansCalcRouteLandmarks (network, commonRouterDatafinal, costCalculator, timeCalculator);
		scorer = new PlanomatXPlanScorer (factory);
		NEIGHBOURHOOD_SIZE = 5;							//TODO @MF: variables to be configured externally, sum must be smaller or equal than 1.0
		WEIGHT_CHANGE_ORDER = 0.8; 
		WEIGHT_CHANGE_NUMBER = 0.2;
			//weightChangeType = 0.0;
		MAX_ITERATIONS = 1;
	}
	
		
	//////////////////////////////////////////////////////////////////////
	// run() method
	//////////////////////////////////////////////////////////////////////
	
	
	
	public void run (Plan plan){
		
		//////////////////////////////////////////////////////////////////////
		// New section TS iterations (under construction)
		//////////////////////////////////////////////////////////////////////
		
		
		int currentIteration;
		
		PlanomatXPlan [] neighbourhood = new PlanomatXPlan [NEIGHBOURHOOD_SIZE+1];
		int neighbourhoodInitialisation;
		for (neighbourhoodInitialisation = 0; neighbourhoodInitialisation < neighbourhood.length; neighbourhoodInitialisation++){
			neighbourhood[neighbourhoodInitialisation] = new PlanomatXPlan (plan.getPerson());
			neighbourhood[neighbourhoodInitialisation].copyPlan(plan);
	
		}
		
		int [] notNewInNeighbourhood = new int [NEIGHBOURHOOD_SIZE];
		int [] tabuInNeighbourhood = new int [NEIGHBOURHOOD_SIZE];
		ArrayList<PlanomatXPlan> nonTabuNeighbourhood = new ArrayList<PlanomatXPlan>();
		
		for (currentIteration = 1; currentIteration<MAX_ITERATIONS+1;currentIteration++){
			
			this.createNeighbourhood(neighbourhood);	
			
			int warningNoNew = this.checkForNoNewSolutions(neighbourhood, notNewInNeighbourhood);
			if (warningNoNew==1) {
				System.out.println("No new solutions availabe for person "+plan.getPerson().getId()+" at iteration "+currentIteration);
				break; 
			}
			
			int warningTabu = this.checkForTabuSolutions(neighbourhood, notNewInNeighbourhood, tabuInNeighbourhood);
			if (warningTabu==1) {
				System.out.println("No non-tabu solutions availabe for person "+plan.getPerson().getId()+" at iteration "+currentIteration);
				break; 
			}
			
			for (int x=0; x<NEIGHBOURHOOD_SIZE;x++){
				if(tabuInNeighbourhood[x]==0){
					
					// Scoring
					System.out.println("run method(), Person: "+neighbourhood[x].getPerson().getId()+", Score vor dem Scorer"+neighbourhood[x]);
					
					neighbourhood[x].setScore(scorer.getScore(neighbourhood[x]));
					nonTabuNeighbourhood.add(0, neighbourhood[x]);
					
					System.out.println("run method(), Person: "+neighbourhood[x].getPerson().getId()+", Score nach dem Scorer"+neighbourhood[x]);
				}
			}
			
			java.util.Collections.sort(nonTabuNeighbourhood);
			
			if (currentIteration==MAX_ITERATIONS) {
				System.out.println("Tabu Search regularly finished for person "+plan.getPerson().getId()+" at iteration "+currentIteration);
			
				//if (nonTabuNeighbourhood.get(nonTabuNeighbourhood.size()-1).getScore()>plan.getScore()){
					ArrayList<Object> al = plan.getActsLegs();
					for (int i = 1; i<al.size()-1;i++){
						al.remove(i);
						al.add(i, nonTabuNeighbourhood.get(nonTabuNeighbourhood.size()-1).getActsLegs().get(i));	
					}
				//}			
			}
		}	
	}
				
	//////////////////////////////////////////////////////////////////////
	// Neighbourhood definition (under construction)
	//////////////////////////////////////////////////////////////////////
	
	public void createNeighbourhood (PlanomatXPlan [] neighbourhood) {
		int neighbourPos;
		int planPos = 2;
		for (neighbourPos = 1; neighbourPos<(int)(NEIGHBOURHOOD_SIZE*WEIGHT_CHANGE_ORDER); neighbourPos++){
			planPos =this.changeOrder(neighbourhood[neighbourPos], planPos);
		}
	
		for (neighbourPos = (int) (NEIGHBOURHOOD_SIZE*WEIGHT_CHANGE_ORDER); neighbourPos<((int)(NEIGHBOURHOOD_SIZE*WEIGHT_CHANGE_ORDER)+(int)(NEIGHBOURHOOD_SIZE*WEIGHT_CHANGE_NUMBER)); neighbourPos++){
			neighbourhood[neighbourPos]=this.changeNumber(neighbourhood[neighbourPos]);
		}
	
		for (neighbourPos = (int) (NEIGHBOURHOOD_SIZE*WEIGHT_CHANGE_ORDER)+(int)(NEIGHBOURHOOD_SIZE*WEIGHT_CHANGE_NUMBER); neighbourPos<NEIGHBOURHOOD_SIZE; neighbourPos++){
			neighbourhood[neighbourPos]=this.changeType(neighbourhood[neighbourPos]);
		}
	}
			
	
	
	public int changeOrder (PlanomatXPlan basePlan, int planBasePos){
		
		ArrayList<Object> actslegs = basePlan.getActsLegs();
		
		if (actslegs.size()<=5){	//If true the plan has not enough activities to change their order. Do nothing.
			
			System.out.println("Person: "+basePlan.getPerson().getId()+", Scoring davor: "+basePlan.getScore());
			System.out.println("Person: "+basePlan.getPerson().getId()+", Scoring davor, nochmal mit Scorer: "+scorer.getScore(basePlan));
			
			return planBasePos;
		}
		else {
			for (int planRunningPos = planBasePos; planRunningPos <= actslegs.size()-4; planRunningPos=planRunningPos+2){ //Go through the "inner" acts only
				planBasePos=planBasePos+2;
				
				//Activity swapping
				
				Act act2 = (Act)(actslegs.get(planRunningPos));
				Act act4 = (Act)(actslegs.get(planRunningPos+4));
				if (act2.getType()!=act4.getType()){
					Act act1 = (Act)(actslegs.get(planRunningPos-2));
					Act act3 = (Act)(actslegs.get(planRunningPos+2));
					if (act1.getType()!=act3.getType()){
						double scoreOne = basePlan.getScore();
						System.out.println("Person: "+basePlan.getPerson().getId()+", Scoring davor: "+scoreOne);
						System.out.println("Person: "+basePlan.getPerson().getId()+", Scoring davor, nochmal mit Scorer: "+scorer.getScore(basePlan));
						
						Act actHelp = new Act ((Act)(actslegs.get(planRunningPos)));
						Act actHelp3 = new Act ((Act) (actslegs.get(planRunningPos+2)));
						actslegs.set(planRunningPos, actslegs.get(planRunningPos+2));
						
						Act act2New = (Act)(actslegs.get(planRunningPos));
						act2New.setStartTime(actHelp.getStartTime());
						act2New.setEndTime(actHelp.getEndTime());

						actslegs.set(planRunningPos+2, actHelp);
						
						Act act3New = (Act)(actslegs.get(planRunningPos+2));
						act3New.setStartTime(actHelp3.getStartTime());
						act3New.setEndTime(actHelp3.getEndTime());
					
						// Routing
					
						this.router.run(basePlan);
						
						//Optimizing the start times
						
						//this.planomatAlgorithm.run (basePlan); //Calling standard Planomat to optimise start times and mode choice
						//System.out.println("Neuer Plan nach Planomat: "+actslegs);
						
						// Scoring
						
						//basePlan.setScore(scorer.getScore(basePlan));
						//double scoreTwo = basePlan.getScore();
						//System.out.println("Person: "+basePlan.getPerson().getId()+", Scoring danach: "+scoreTwo);
						break;
					}
					
				}
			}		
			return planBasePos;
		}
	}
	
	public PlanomatXPlan changeNumber (PlanomatXPlan basePlan){
		//System.out.println("Aufruf Methode changeNumber.");
		return basePlan;
	}
	
	public PlanomatXPlan changeType (PlanomatXPlan basePlan){
		//System.out.println("Aufruf Methode changeType.");
		return basePlan;
	}
	
	
	public int checkForNoNewSolutions (PlanomatXPlan[] neighbourhood, int[] notNewInNeighbourhood){
		int warningInner = 1;
		int warningOuter = 1;
		for (int x=0; x<notNewInNeighbourhood.length;x++){
			if (neighbourhood[x].getActsLegs().toString().equals(neighbourhood[neighbourhood.length-1].getActsLegs().toString())){
				notNewInNeighbourhood[x]=1;
			}
			else {
				notNewInNeighbourhood[x]=0;
				warningInner = 0;
			}
			if (warningInner==0) warningOuter = 0;
		}
		return warningOuter;
	}
	
	
	public int checkForTabuSolutions (PlanomatXPlan[] neighbourhood, int[] notNewInNeighbourhood, int[] tabuInNeighbourhood){
		int warningInner = 1;
		int warningOuter = 1;
		for (int x=0; x<tabuInNeighbourhood.length;x++){
			if (notNewInNeighbourhood[x]==1) tabuInNeighbourhood[x] = 1;
			else {
				tabuInNeighbourhood[x] = 0;
				warningInner = 0;
			}
			if (warningInner==0) warningOuter = 0;
		}
		return warningOuter;
	}
}
	

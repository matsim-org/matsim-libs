/**
 * 
 */
package playground.mfeil;

import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.Plan;
import org.matsim.population.*;
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
import playground.mfeil.*;

/**
 * @author Matthias Feil
 * PlanomatX will be the class where to implement the Tabu Search. Currently, work focus is on the definition of 
 * the neighbourhood. Changing the order of activities already works. Next is to integrate the TS mechanisms.
 */
public class PlanomatX implements org.matsim.population.algorithms.PlanAlgorithm { 
	
	private final int neighbourhoodSize;
	private final double weightChangeOrder, weightChangeNumber;// weightChangeType;
	private final PlanAlgorithm planomatAlgorithm;
	private final PlansCalcRouteLandmarks router;
	private final PlanScorer scorer;
	
	
	
	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////
		
	public PlanomatX (LegTravelTimeEstimator legTravelTimeEstimator, NetworkLayer network, TravelCost costCalculator,
			TravelTime timeCalculator, PreProcessLandmarks commonRouterDatafinal, ScoringFunctionFactory factory) {

		planomatAlgorithm = new PlanOptimizeTimes (legTravelTimeEstimator);
		router = new PlansCalcRouteLandmarks (network, commonRouterDatafinal, costCalculator, timeCalculator);
		scorer = new PlanScorer (factory);
		neighbourhoodSize = 5;//TODO @MF: variables to be configured externally, sum must be smaller or equal than 1.0
		weightChangeOrder = 0.8; 
		weightChangeNumber = 0.2;
		//weightChangeType = 0.0;
	}
	
		
	//////////////////////////////////////////////////////////////////////
	// run() method
	//////////////////////////////////////////////////////////////////////
	
	
	
	public void run (Plan plan){

				
	//////////////////////////////////////////////////////////////////////
	// New section neighbourhood definition (under construction)
	//////////////////////////////////////////////////////////////////////
		
		PlanomatXPlan [] neighbourhood = new PlanomatXPlan [neighbourhoodSize+1];
		int z;
		for (z = 0; z < neighbourhood.length; z++){
			neighbourhood[z] = new PlanomatXPlan (plan.getPerson());
			neighbourhood[z].copyPlan(plan);
		}
		int x = 2;
		for (z = 1; z<(int)(neighbourhoodSize*weightChangeOrder); z++){
			x =this.changeOrder(neighbourhood[z], x);
		}
	
		for (z = (int) (neighbourhoodSize*weightChangeOrder); z<((int)(neighbourhoodSize*weightChangeOrder)+(int)(neighbourhoodSize*weightChangeNumber)); z++){
			neighbourhood[z]=this.changeNumber(neighbourhood[z]);
		}
	
		for (z = (int) (neighbourhoodSize*weightChangeOrder)+(int)(neighbourhoodSize*weightChangeNumber); z<neighbourhoodSize; z++){
			neighbourhood[z]=this.changeType(neighbourhood[z]);
		}
		//for (z = 0; z<neighbourhoodSize; z++) System.out.println(z+". Scoring davor: "+neighbourhood [z].getScore());
		
		java.util.Arrays.sort(neighbourhood,0,5);
		for (z = 0; z<neighbourhoodSize+1; z++) System.out.println(z+". Scoring nach Sortierung: "+neighbourhood [z].getScore());
		
		//System.out.println("Neighbourhood [1]: "+neighbourhood[1].getActsLegs());
		ArrayList<Object> al = plan.getActsLegs();
		int size = al.size();
		for (int i = 1; i<size-1;i=i+2){
			al.remove(i);
			al.add(i, neighbourhood[4].getActsLegs().get(i));
			
		}
		for (int i =2; i<size-1;i=i+2){
			al.remove(i);
			al.add(i, neighbourhood[4].getActsLegs().get(i));
		}
		
		
		//for (z = 0; z<neighbourhoodSize; z++) System.out.println(z+". Scoring danach: "+neighbourhood [z].getScore());
		//int size =0;
		//for (Object o : plan.getActsLegs()){
			//if (o.getClass().equals(Act.class)){
				//plan.removeAct(size);
				//size=size+1;
			//}
			//else {
				//plan.removeLeg(size);
				//size=size+1;
			//}
			
		//}
		
		
		//System.out.println("Plan nach Umschreiben: "+plan.getActsLegs());
		
		//plan.copyPlan(neighbourhood[0]);

			}
	
	
	public int changeOrder (PlanomatXPlan basePlan, int x){
		
		System.out.println("x ist: "+x);
		
		ArrayList<Object> actslegs = basePlan.getActsLegs();
		if (actslegs.size()<=5){	//If true the plan has not enough activities to change their order.
			return x;
		}
		else {
			for (int loopCounter = x; loopCounter <= actslegs.size()-4; loopCounter=loopCounter+2){ //Go through the "inner" acts only
				x=x+2;
				
				//Activity swapping
				
				Act act2 = (Act)(actslegs.get(loopCounter));
				Act act4 = (Act)(actslegs.get(loopCounter+4));
				if (act2.getType()!=act4.getType()){
					//System.out.println("Hab was gefunden!");
					//System.out.println("Plan davor: "+actslegs);
					Act act1 = (Act)(actslegs.get(loopCounter-2));
					Act act3 = (Act)(actslegs.get(loopCounter+2));
					if (act1.getType()!=act3.getType()){
						double scoreOne = basePlan.getScore();
						System.out.println("Scoring davor: "+scoreOne);
						Act actHelp = new Act ((Act)(actslegs.get(loopCounter)));
						actslegs.set(loopCounter, actslegs.get(loopCounter+2));
						actslegs.set(loopCounter+2, actHelp);
						//System.out.println("Plan danach: "+basePlan.getActsLegs());
						
						// Routing
					
						this.router.run(basePlan);
						//System.out.println("Neuer Plan :"+actslegs);
						
						//Optimizing the start times
						
						//this.planomatAlgorithm.run (basePlan); //Calling standard Planomat to optimise start times and mode choice
						//System.out.println("Neuer Plan nach Planomat: "+actslegs);
						
						// Scoring
						
						basePlan.setScore(scorer.getScore(basePlan));
						double scoreTwo = basePlan.getScore();
						System.out.println("Scoring danach: "+scoreTwo);
						break;
					}
					
				}
			}		
			return x;
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
	

}

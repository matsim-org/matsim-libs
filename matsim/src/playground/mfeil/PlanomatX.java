/**
 * 
 */
package playground.mfeil;

import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.Plan;
import org.matsim.population.Leg;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.planomat.*;
import org.matsim.router.PlansCalcRouteLandmarks;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;

import java.util.ArrayList;

/**
 * @author Matthias Feil
 * PlanomatX will be the class where to implement the Tabu Search. Currently, it is a test
 * version for the equil scenario where it turns the order of activities (exchange 2nd and 
 * 3rd activity if there is a 3rd activity). 
 * 2nd section prepares for the neighbourhood definition of the TS algorithm.
 */
public class PlanomatX implements org.matsim.population.algorithms.PlanAlgorithm {
	
	private final int neighbourhoodSize;
	private final double weightChangeOrder, weightChangeNumber;// weightChangeType;
	private final PlanAlgorithm planomatAlgorithm;
	private final PlansCalcRouteLandmarks router;
	
	
	
	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////
		
	public PlanomatX (LegTravelTimeEstimator legTravelTimeEstimator, NetworkLayer network, TravelCost costCalculator,
			TravelTime timeCalculator, PreProcessLandmarks commonRouterDatafinal) {

		planomatAlgorithm = new PlanOptimizeTimes (legTravelTimeEstimator);
		router = new PlansCalcRouteLandmarks (network, commonRouterDatafinal, costCalculator, timeCalculator);
		neighbourhoodSize = 5;//TODO @MF: variables to be configured externally, sum must be smaller or equal than 1.0
		weightChangeOrder = 0.6; 
		weightChangeNumber = 0.2;
		//weightChangeType = 0.2;
	}
	
	//////////////////////////////////////////////////////////////////////
	// run() method
	//////////////////////////////////////////////////////////////////////
	
	
	
	public void run (final Plan plan){

		//ArrayList<Object> al = new ArrayList<Object> (plan.getActsLegs());
		ArrayList<Object> al = plan.getActsLegs();
		int i = (al.size()/2)+1;  //size() returns Acts und Legs, therefore dividing by 2 yields number of Legs, +1 yields number of Acts.
		System.out.println ("Das ist die Länge der Aktivitätenliste "+i);

		Leg leg;
		leg = (Leg)(al.get(1));
		ArrayList<Node> nodes = new ArrayList<Node> ();
		nodes = leg.getRoute().getRoute();
		
		if (i>3){
			//System.out.println("Hier gibt es eine lange Liste: "+al);
			Object oOne = al.get(4);
			Object oTwo = al.get(2);
			//System.out.println("Das ist Objekt o: "+oOne);
			//System.out.println("Das ist Objekt o: "+oTwo);
			al.add(2, oOne);
			al.add(5, oTwo);
			al.remove(3);
			al.remove(5);
			//System.out.println("Nun schaut die Liste so aus: "+al);		
			
		}
		//this.planomatAlgorithm.run (plan); //Calling standard Planomat to optimise start times and mode choice
		System.out.println("Plan davor: "+plan.getPerson().getId()+" mit dem Leg: "+nodes);	
		
		this.router.run(plan);
		
		Leg leg1;
		leg1 = (Leg)(al.get(1));
		ArrayList<Node> nodes1 = new ArrayList<Node> ();
		nodes1 = leg1.getRoute().getRoute();
		System.out.println("Plan danach: "+plan.getPerson().getId()+" mit dem Leg: "+nodes1);
		
		
		
		
	//////////////////////////////////////////////////////////////////////
	// New section neighbourhood definition (under construction)
	//////////////////////////////////////////////////////////////////////
				
		Plan [] neighbourhood = new Plan [neighbourhoodSize];
		//System.out.println("Länge von neighbourhood ist "+neighbourhood.length);
		int z;
		for (z = 0; z<(int)(neighbourhoodSize*weightChangeOrder); z++){
			neighbourhood[z]=changeOrder(plan);
		}
	
		for (z = (int) (neighbourhoodSize*weightChangeOrder); z<((int)(neighbourhoodSize*weightChangeOrder)+(int)(neighbourhoodSize*weightChangeNumber)); z++){
			neighbourhood[z]=changeNumber(plan);
		}
	
		for (z = (int) (neighbourhoodSize*weightChangeOrder)+(int)(neighbourhoodSize*weightChangeNumber); z<neighbourhoodSize; z++){
			neighbourhood[z]=changeType(plan);
		}
		
		//for (z=0;z<neighbourhoodSize; z++){
		//	System.out.println("Das ist das "+z+". Feld von neighbourhood: "+ neighbourhood[z]);
		//}
		
	}
	
	
	public Plan changeOrder (Plan basePlan){
		ArrayList<Object> ActsLegs = new ArrayList<Object>(basePlan.getActsLegs());
		//System.out.println("Aufruf Methode changeOrder.");
		return basePlan;
	}
	
	public Plan changeNumber (Plan basePlan){
		//System.out.println("Aufruf Methode changeNumber.");
		return basePlan;
	}
	
	public Plan changeType (Plan basePlan){
		//System.out.println("Aufruf Methode changeType.");
		return basePlan;
	}
	

}

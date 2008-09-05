/**
 * 
 */
package playground.mfeil;

import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.planomat.*;
import java.util.ArrayList;

/**
 * @author Matthias Feil
 * PlanomatX will be the class where to implement the Tabu Search. Currently, it is a test
 * version for the equil scenario where it turns the order of activities (exchange 2nd and 
 * 3rd activity if there is a 3rd activity). 
 * 2nd section prepares for the neighbourhood definition of the TS algorithm.
 */
public class PlanomatX implements org.matsim.population.algorithms.PlanAlgorithm {
	
	final int neighbourhoodSize;
	final double weightChangeOrder, weightChangeNumber, weightChangeType;
	final PlanAlgorithm planomatAlgorithm;
	
	
	//////////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////////
		
	public PlanomatX (final LegTravelTimeEstimator legTravelTimeEstimator) {

		planomatAlgorithm = new PlanOptimizeTimes (legTravelTimeEstimator);
		neighbourhoodSize = 5;//TODO @MF: variables to be configured externally, sum must equal 1.0
		weightChangeOrder = 0.6; 
		weightChangeNumber = 0.2;
		weightChangeType = 0.2;
	}
	
	//////////////////////////////////////////////////////////////////////
	// run() method
	//////////////////////////////////////////////////////////////////////
	
	
	
	public void run (final Plan plan){
		//System.out.println("Das ist nur ein PlanomatX-Test, Pläne bleiben unverändert.");

		//ArrayList<Object> al = new ArrayList<Object> (plan.getActsLegs());
		ArrayList<Object> al = plan.getActsLegs();
		int i = (al.size()/2)+1;  //size() returns Acts und Legs, therefore dividing by 2 yields number of Legs, +1 yields number of Acts.
		System.out.println ("Das ist die Länge der Aktivitätenliste "+i);

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
		this.planomatAlgorithm.run (plan); //Calling standard Planomat to optimise starttimes and mode choice
		
		
		
		
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
		
		for (z=0;z<neighbourhoodSize; z++){
			System.out.println("Das ist das "+z+". Feld von neighbourhood: "+ neighbourhood[z]);
		}
		
	}
	
	
	public Plan changeOrder (Plan basePlan){
		ArrayList<Object> ActsLegs = new ArrayList<Object>(basePlan.getActsLegs());
		System.out.println("Aufruf Methode changeOrder.");
		return basePlan;
	}
	
	public Plan changeNumber (Plan basePlan){
		System.out.println("Aufruf Methode changeNumber.");
		return basePlan;
	}
	
	public Plan changeType (Plan basePlan){
		System.out.println("Aufruf Methode changeType.");
		return basePlan;
	}
	

}

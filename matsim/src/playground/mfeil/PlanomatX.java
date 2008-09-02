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
 */
public class PlanomatX implements org.matsim.population.algorithms.PlanAlgorithm {
	
	PlanAlgorithm planomatAlgorithm = null;
	public PlanomatX (final LegTravelTimeEstimator legTravelTimeEstimator) {

		planomatAlgorithm = new PlanOptimizeTimes (legTravelTimeEstimator);

		
	}
	public void run (final Plan plan){
		//System.out.println("Das ist nur ein PlanomatX-Test, Pläne bleiben unverändert.");

		//ArrayList<Object> al = new ArrayList<Object> (plan.getActsLegs());
		ArrayList<Object> al = plan.getActsLegs();
		int i = (al.size()/2)+1;  //size() returns Acts und Legs, therefore dividing by 2 yields number of Legs, +1 yields number of Acts.
		System.out.println ("Das ist die Länge der Aktivitätenliste "+i);

		if (i>3){
			System.out.println("Hier gibt es eine lange Liste: "+al);
			Object oOne = al.get(4);
			Object oTwo = al.get(2);
			System.out.println("Das ist Objekt o: "+oOne);
			System.out.println("Das ist Objekt o: "+oTwo);
			al.add(2, oOne);
			al.add(5, oTwo);
			al.remove(3);
			al.remove(5);
			System.out.println("Nun schaut die Liste so aus: "+al);
		}
		//this.planomatAlgorithm.run (plan);
	}

}

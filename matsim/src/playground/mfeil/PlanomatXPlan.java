/**
 * 
 */
package playground.mfeil;

import org.matsim.population.*;


/**
 * @author Matthias Feil
 *
 */
public class PlanomatXPlan extends Plan implements Comparable<PlanomatXPlan>{

	public PlanomatXPlan (Person person){
		super(person);
	}
	
	public final int compareTo(PlanomatXPlan p){
		if (this.getScore() == p.getScore()) {
			return 0;
		}
		else if (this.getScore() - p.getScore() > 0.0){
			return 1;
		}
		else return -1;
	}
}

/**
 * 
 */
package playground.mfeil;

import org.matsim.population.*;


/**
 * @author Matthias Feil
 * Extends the standard Plan object to enable sorting of arrays or lists of Plans after their scores.
 * Use 
 * java.util.Arrays.sort (nameOfArray[]) 
 * or
 * java.util.Collections.sort (nameOfList<>).
 * See e.g., PlanomatX class.
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

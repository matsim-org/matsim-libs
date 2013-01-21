package playground.sergioo.passivePlanning2012.core.population;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PlanImpl;

public class PlanImplV2 extends PlanImpl {

	//Methods
	public int getPlanElementIndex(double time) {
		if(time>=0) {
			for(int i=0; i<this.getPlanElements().size(); i++) {
				PlanElement planElement = this.getPlanElements().get(i);
				if(planElement instanceof Activity)
					if(time<((Activity)planElement).getEndTime())
						return i;
				if(planElement instanceof Leg)
					if(time<((Leg)planElement).getDepartureTime()+((Leg)planElement).getTravelTime())
						return i;
			}
		}
		return -1;
	}

}

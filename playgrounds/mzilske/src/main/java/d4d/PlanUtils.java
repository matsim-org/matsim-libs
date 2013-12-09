package d4d;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.NetworkRoute;

public class PlanUtils {

	static void insertLinkIdsIntoGenericRoutes(Plan plan) {
		forward(plan);
		backward(plan);
	}

	private static void forward(Plan plan) {
		Id lastLinkId = null;
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Activity) {
				lastLinkId = ((Activity) planElement).getLinkId();
			} else if (planElement instanceof Leg) {
				Route route = ((Leg) planElement).getRoute();
				if (route instanceof NetworkRoute) {
					lastLinkId = route.getEndLinkId();
				} else if (route instanceof GenericRoute) {
					((GenericRoute) route).setStartLinkId(lastLinkId);
					lastLinkId = null;
				}
			}
		}
	}

	private static void backward(Plan plan) {
		Id lastLinkId = null;
		for(int j = plan.getPlanElements().size() - 1; j >= 0; j--) {
			PlanElement planElement = plan.getPlanElements().get(j);
			if (planElement instanceof Activity) {
				lastLinkId = ((Activity) planElement).getLinkId();
			} else if (planElement instanceof Leg) {
				Route route = ((Leg) planElement).getRoute();
				if (route instanceof NetworkRoute) {
					lastLinkId = route.getStartLinkId();
				} else if (route instanceof GenericRoute) {
					route.setEndLinkId(lastLinkId);
					lastLinkId = null;
				}
			}
		}
	}

}

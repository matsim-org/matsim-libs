package playground.mzilske.d4d;

import org.matsim.api.core.v01.population.Plan;

class PlanUtils {

	static void insertLinkIdsIntoGenericRoutes(Plan plan) {
		forward(plan);
		backward(plan);
	}

	private static void forward(Plan plan) {
		// make this a NOOP after discussing with michaz at DevMtg2015 // mrieser,8sep2015
//		Id lastLinkId = null;
//		for (PlanElement planElement : plan.getPlanElements()) {
//			if (planElement instanceof Activity) {
//				lastLinkId = ((Activity) planElement).getLinkId();
//			} else if (planElement instanceof Leg) {
//				Route route = ((Leg) planElement).getRoute();
//				if (route instanceof NetworkRoute) {
//					lastLinkId = route.getEndLinkId();
//				} else if (route instanceof GenericRoute) {
//					((GenericRoute) route).setStartLinkId(lastLinkId);
//					lastLinkId = null;
//				}
//			}
//		}
	}

	private static void backward(Plan plan) {
		// make this a NOOP after discussing with michaz at DevMtg2015 // mrieser,8sep2015
//		Id lastLinkId = null;
//		for(int j = plan.getPlanElements().size() - 1; j >= 0; j--) {
//			PlanElement planElement = plan.getPlanElements().get(j);
//			if (planElement instanceof Activity) {
//				lastLinkId = ((Activity) planElement).getLinkId();
//			} else if (planElement instanceof Leg) {
//				Route route = ((Leg) planElement).getRoute();
//				if (route instanceof NetworkRoute) {
//					lastLinkId = route.getStartLinkId();
//				} else if (route instanceof GenericRoute) {
//					route.setEndLinkId(lastLinkId);
//					lastLinkId = null;
//				}
//			}
//		}
	}

}

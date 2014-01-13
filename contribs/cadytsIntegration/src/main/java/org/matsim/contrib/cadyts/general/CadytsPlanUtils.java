package org.matsim.contrib.cadyts.general;

import org.matsim.api.core.v01.network.Link;

public class CadytsPlanUtils {
	
	public static void printCadytsPlan(final cadyts.demand.Plan<Link> cadytsPlan) {
		// prints Cadyts plan
		String sepCadStr = "==printing Cadyts Plan==";
		System.out.println(sepCadStr);
		if (cadytsPlan != null) {
			for (int ii = 0; ii < cadytsPlan.size(); ii++) {
				cadyts.demand.PlanStep<Link> cadytsPlanStep = cadytsPlan.getStep(ii);
				System.out.println("linkId" + cadytsPlanStep.getLink().getId() + " time: " + cadytsPlanStep.getEntryTime_s());
			}
		} else {
			System.out.println(" cadyts plan is null ");
		}
	}
	
}

package org.matsim.core.population.routes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.*;

import java.util.Iterator;
import java.util.List;

public class PopulationComparison{
	public enum Result { equal, notEqual }

	private static final Logger log = LogManager.getLogger( PopulationComparison.class );


	public Result compare( Population population1, Population population2 ){
		Result result=Result.equal;

		Iterator<? extends Person> it1 = population1.getPersons().values().iterator();
		Iterator<? extends Person> it2 = population2.getPersons().values().iterator();

		while( it1.hasNext() || it2.hasNext() ) {
			if ( ! it1.hasNext() ) {
				result = Result.notEqual ;
				return result ;
			}
			if ( ! it2.hasNext() ) {
				result = Result.notEqual ;
				return result ;
			}
			Person person1 = it1.next();
			Person person2 = it2.next();
			if ( !person1.getId().equals( person2.getId() ) ) {
				log.warn( "persons out of sequence" );
				result = Result.notEqual ;
				continue;
			}
			Plan plan1 = person1.getSelectedPlan();
			Plan plan2 = person2.getSelectedPlan();
			if ( Math.abs( plan1.getScore() - plan2.getScore() ) > 100.*Double.MIN_VALUE ||
			 !equals(plan1.getPlanElements(), plan2.getPlanElements())) {

				double maxScore = Double.NEGATIVE_INFINITY;
				for( Plan plan : person2.getPlans() ){
					if ( plan.getScore() > maxScore ) {
						maxScore = plan.getScore() ;
					}
				}

				log.warn( "" );
				log.warn("personId=" + person1.getId() + "; score1=" + plan1.getScore() + "; score2=" + plan2.getScore() + "; maxScore2=" + maxScore ) ;
				log.warn( "" );
				for( PlanElement planElement : plan1.getPlanElements() ){
					log.warn( planElement );
				}
				log.warn( "" );
				for( PlanElement planElement : plan2.getPlanElements() ){
					log.warn( planElement );
				}
				log.warn( "" );

			}
		}
		return result ;
	}


	public static boolean equals(List<PlanElement> planElements,
								 List<PlanElement> planElements2) {
		int nElements = planElements.size();
		if (nElements != planElements2.size()) {
			return false;
		} else {
			for (int i = 0; i < nElements; i++) {
				if (!equals(planElements.get(i), planElements2.get(i))) {
					return false;
				}
			}
		}
		return true;
	}

	/* Warning: This is NOT claimed to be correct. (It isn't.)
	 *
	 */
	private static boolean equals(PlanElement o1, PlanElement o2) {
		if (o1 instanceof Leg) {
			if (o2 instanceof Leg) {
				Leg leg1 = (Leg) o1;
				Leg leg2 = (Leg) o2;
				if (!leg1.getDepartureTime().equals(leg2.getDepartureTime())) {
					return false;
				}
				if (!leg1.getMode().equals(leg2.getMode())) {
					return false;
				}
				if (!leg1.getTravelTime().equals(leg2.getTravelTime())) {
					return false;
				}
			} else {
				return false;
			}
		} else if (o1 instanceof Activity) {
			if (o2 instanceof Activity) {
				Activity activity1 = (Activity) o1;
				Activity activity2 = (Activity) o2;
				if (activity1.getEndTime().isUndefined() ^ activity2.getEndTime().isUndefined()) {
					return false;
				}
				if (activity1.getEndTime().isDefined() && activity1.getEndTime().seconds()
					!= activity2.getEndTime().seconds()) {
					return false;
				}
				if (activity1.getStartTime().isUndefined() ^ activity2.getStartTime().isUndefined()) {
					return false;
				}
				if (activity1.getStartTime().isDefined() && activity1.getStartTime().seconds()
					!= activity2.getStartTime().seconds()) {
					return false;
				}
			} else {
				return false;
			}
		} else {
			throw new RuntimeException ("Unexpected PlanElement");
		}
		return true;
	}
}

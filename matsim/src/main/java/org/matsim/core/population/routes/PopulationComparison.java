package org.matsim.core.population.routes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

import java.util.Iterator;

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
			if ( Math.abs( plan1.getScore() - plan2.getScore() ) > 100.*Double.MIN_VALUE  ) {

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
}

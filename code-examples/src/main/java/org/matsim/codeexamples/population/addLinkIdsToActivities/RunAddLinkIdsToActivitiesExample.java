package org.matsim.codeexamples.population.addLinkIdsToActivities;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

class RunAddLinkIdsToActivitiesExample{

	public static void main( String[] args ){

		Config config = ConfigUtils.loadConfig( "scenarios/equil/config.xml") ;

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		for( Person person : scenario.getPopulation().getPersons().values() ){
			for( Plan plan : person.getPlans() ){
				for( PlanElement planElement : plan.getPlanElements() ){
					if ( planElement instanceof Activity ) {
						Id<Link> linkId = NetworkUtils.getNearestRightEntryLink( scenario.getNetwork(), ((Activity) planElement).getCoord() ).getId() ;
						((Activity) planElement).setLinkId( linkId );
					}
				}
			}
		}

	}
}

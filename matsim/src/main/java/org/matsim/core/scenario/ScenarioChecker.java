package org.matsim.core.scenario;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

import java.util.ArrayList;
import java.util.List;

import static org.matsim.core.router.TripStructureUtils.StageActivityHandling.*;

public final class ScenarioChecker{
	private final Scenario scenario;
	private final List<ScenarioCheckerModule> checkers = new ArrayList<>();

	public interface ScenarioCheckerModule {
		void checkScenario( Scenario scenario );
	}

	// we cannot use injection since after loading a scenario we do not have that.

	public static class ActivityChecker implements ScenarioCheckerModule {
		private static final Logger log = LogManager.getLogger(ScenarioChecker.ActivityChecker.class);


		@Override public void checkScenario( Scenario scenario ){
			log.info( "start checking if activities are roughly within opening times ...");
			Counter counter = new Counter( "# person " );
			final TimeTracker timeTracker = new TimeTracker( TimeInterpretation.create( scenario.getConfig() ) );
			double violationCnt = 0.;
			for( Person person : scenario.getPopulation().getPersons().values() ){
				counter.incCounter();
				timeTracker.setTime( 0. );
				for( Activity activity : TripStructureUtils.getActivities( person.getSelectedPlan(), ExcludeStageActivities ) ){
					ScoringConfigGroup.ActivityParams actParams = scenario.getConfig().scoring().getActivityParams( activity.getType() );

					if ( actParams.getClosingTime().isDefined() ){
						if( actParams.getClosingTime().seconds() < timeTracker.getTime().seconds() ){
//							log.warn( "activity type={}; closing time was at time={}; at time={}, we are already beyond that.",
//								activity.getType(), actParams.getClosingTime().seconds() / 3600, timeTracker.getTime().seconds()/3600. );
							violationCnt++;
						}
					}
					timeTracker.addActivity( activity );
//					if ( timeTracker.getTime().isUndefined() ) {
//						log.warn( "current time={} is undefined after activity={}", timeTracker.getTime(), activity );
//					}
					if ( timeTracker.getTime().isDefined() // otherwise presumably last activity of day
							 && actParams.getOpeningTime().isDefined() ){
						if( timeTracker.getTime().seconds() < actParams.getOpeningTime().seconds() ){
//							log.warn( "activity of type={} ends at time={}, this is before time={} when the activity type opens",
//								activity.getType(), timeTracker.getTime().seconds()/3600., actParams.getOpeningTime().seconds()/3600. );
							violationCnt++ ;
						}
					}
				}
			}
			log.warn("violationCnt={}", violationCnt);
		}
	}
	public ScenarioChecker( Scenario scenario ) {
		this.scenario = scenario;
	}
	public void addScenarioChecker( ScenarioCheckerModule scenarioCheckerModule ) {
		this.checkers.add( scenarioCheckerModule );
	}
	public void run() {
		for( ScenarioCheckerModule checker : checkers ){
			checker.checkScenario( scenario );
		}
	}
}

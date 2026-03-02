package org.matsim.application.analysis.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.GeometryUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.matsim.core.router.TripStructureUtils.StageActivityHandling.StagesAsNormalActivities;

class AgentWiseQuickKN {
	private static final Logger log = LogManager.getLogger( AgentWiseQuickKN.class );

	static void main() throws MalformedURLException{

//		Population pop1 = PopulationUtils.readPopulation( "/Users/kainagel/shared-svn/projects/zez/b_wo_zez/output_plans.xml.gz");
//		Population pop2 = PopulationUtils.readPopulation( "/Users/kainagel/shared-svn/projects/zez/c_w_zez/output_plans.xml.gz" );
		Population pop1 = PopulationUtils.readPopulation( "/Users/kainagel/shared-svn/projects/zez/b_wo_zez/output_experienced_plans.xml.gz");
		Population pop2 = PopulationUtils.readPopulation( "/Users/kainagel/shared-svn/projects/zez/c_w_zez/output_experienced_plans.xml.gz" );

		log.warn("### removing stuck agents ...");
		removeStuckAgents( pop1, "/Users/kainagel/shared-svn/projects/zez/b_wo_zez/onlyMoneyAndStuck.output_events_filtered.xml.gz" );
		removeStuckAgents( pop2, "/Users/kainagel/shared-svn/projects/zez/c_w_zez/onlyMoneyAndStuck.output_events_filtered.xml.gz" );

		log.warn("### removing non-person agents ...");
		removeNonPersonAgents( pop1 );

		log.warn("### removing persons outside shapefile ...");
		final String shapeFileName = "/Users/kainagel/shared-svn/projects/zez/berlin.sph";
		final String networkFileName = "/Users/kainagel/shared-svn/projects/zez/b_wo_zez/output_network.xml.gz";
		removePersonsOutsideShape( pop1, shapeFileName, networkFileName );

		log.warn("### summing up ...");
		double sumDeltaScore = 0.;
		for( Person person1 : pop1.getPersons().values() ){
			double score1 = person1.getSelectedPlan().getScore();

			Person person2 = pop2.getPersons().get( person1.getId() );
			if ( person2 != null ){
				// (may happen if it was stuck)

				double score2 = person2.getSelectedPlan().getScore();

				sumDeltaScore += (score2 - score1);
			}
		}

		log.warn( "delta={} (scaled up)", sumDeltaScore * 33.33 );
	}
	private static void removePersonsOutsideShape( Population pop1, String shapeFileName, String networkFileName ) throws MalformedURLException{
		log.info("about to remove persons outside shapefile; popSize before={}", pop1.getPersons().size() );
		MutableScenario scenario = ScenarioUtils.createMutableScenario( ConfigUtils.createConfig() );
		scenario.setPopulation( pop1 );
		NetworkUtils.readNetwork( scenario.getNetwork(), networkFileName );

		List<PreparedGeometry> geometries = ShpGeometryUtils.loadPreparedGeometries( Paths.get( shapeFileName ).toUri().toURL() );
		List<Id<Person>> toRemove = new ArrayList<>();
		for( Person person : pop1.getPersons().values() ){
			boolean toAnalyse = false;
			for( Activity act : TripStructureUtils.getActivities( person.getSelectedPlan(), StagesAsNormalActivities ) ){
				Coord coord = PopulationUtils.decideOnCoordForActivity( act, scenario );
				Point point = GeometryUtils.createGeotoolsPoint( coord );
				for( PreparedGeometry geometry : geometries ){
					if( geometry.contains( point ) ){
						toAnalyse = true;
					}
				}
			}
			if ( !toAnalyse ) {
				toRemove.add( person.getId() );
			}
		}
		for( Id<Person> personId : toRemove ){
			pop1.removePerson( personId );
		}
		log.info("just removedpersons outside shapefile; popSize after={}", pop1.getPersons().size() );
	}

	static void removeNonPersonAgents( Population population ) {
		double popSizeBefore = population.getPersons().size();
		List<Id<Person>> toRemove = new ArrayList<>();
		for( Person person : population.getPersons().values() ){
			if ( !"person".equals( PopulationUtils.getSubpopulation( person ) ) ){
				toRemove.add( person.getId() );
			}
		}
		for( Id<Person> personId : toRemove ){
			population.removePerson( personId );
		}
		log.info("popSize before={}; popSize after={}; ", popSizeBefore, population.getPersons().size() );
	}

	static void removeStuckAgents( Population population, String eventsFile ) {
		double popSizeBefore = population.getPersons().size();

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler( new AgentWiseComparisonKNUtils.MyMoneyEventsHandler( population ) );
		events.addHandler( new AgentWiseComparisonKNUtils.MyStuckEventsHandler( population ) );
		events.initProcessing();

		MatsimEventsReader baseReader = new MatsimEventsReader( events );
		baseReader.readFile( eventsFile );
		events.finishProcessing();

		log.info("popSize before={}; popSize after={}; ", popSizeBefore, population.getPersons().size() );
	}

}

package org.matsim.codeexamples.population.analyzePlans;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import java.net.URL;

class AnalysePlans{

    public static void main ( String [] args ) {

        URL configUrl = IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" );;
        Config config = ConfigUtils.loadConfig( configUrl ) ;


        Scenario scenario = ScenarioUtils.loadScenario( config ) ;
        final Population pop = scenario.getPopulation();

        long nCarLegs = 0 ;
        long nCarUsingPersons = 0 ;
        for ( Person person : pop.getPersons().values() ) {
            boolean carUser = false ;
            Plan plan = person.getSelectedPlan() ;
            for ( Leg leg : TripStructureUtils.getLegs( plan ) ) {
                if ( TransportMode.car.equals( leg.getMode() ) ) {
                    nCarLegs++ ;
                    carUser = true ;
                }
            }
            if ( carUser ) nCarUsingPersons++ ;
        }

        System.out.println( "Number of persons =" + pop.getPersons().size() ) ;
        System.out.println( "Number of car legs = " + nCarLegs ) ;
        System.out.println( "Number of car legs per person = " + 1.*nCarLegs/pop.getPersons().size() ) ;
        System.out.println( "Number of car using persons = " + nCarUsingPersons ) ;

    }

}

package org.matsim.codeexamples;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import java.util.ArrayList;
import java.util.List;

import static org.matsim.core.network.NetworkChangeEvent.ChangeType.*;

public final class RunAbcSimpleExample{

	public static void main ( String [] args ) {

		Config config = ConfigUtils.loadConfig( args );

		config.network().setTimeVariantNetwork( true );

		Scenario scenario = ScenarioUtils.loadScenario( config );

		for( Person person : scenario.getPopulation().getPersons().values() ){
			person.getAttributes().putAttribute( "income", 20. );

		}



		NetworkFactory nf = scenario.getNetwork().getFactory();


		List<NetworkChangeEvent> changeEvents = new ArrayList<>();

		for( Link link : scenario.getNetwork().getLinks().values() ){

//			NetworkChangeEvent changeEvent = new NetworkChangeEvent( 8*3600. );
//			changeEvent.addLink( link );
//			NetworkChangeEvent.ChangeValue changeValue = new NetworkChangeEvent.ChangeValue( OFFSET_IN_SI_UNITS, -10.*3.6 );
//			changeEvent.setFreespeedChange( changeValue );
//
//			changeEvents.add( changeEvent );

			link.getAttributes().putAttribute( "cobblestone", 20 );

		}

		NetworkUtils.writeNetwork( scenario.getNetwork(), "abc.xml" );

		System.exit(-1);

		NetworkUtils.setNetworkChangeEvents( scenario.getNetwork(), changeEvents );

		Controler controler = new Controler( scenario );

		controler.run() ;

	}

}

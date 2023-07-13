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

		Controler controler = new Controler( scenario );

		controler.run() ;

	}

}

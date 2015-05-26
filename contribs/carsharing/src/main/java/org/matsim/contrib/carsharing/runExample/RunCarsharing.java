package org.matsim.contrib.carsharing.runExample;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.carsharing.control.listeners.CarsharingListener;
import org.matsim.contrib.carsharing.qsim.CarsharingQsimFactory;
import org.matsim.contrib.carsharing.scoring.CarsharingScoringFunctionFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunCarsharing {

	public static void main(String[] args) {
		Logger.getLogger( "org.matsim.core.controler.Injector" ).setLevel(Level.OFF);

		final Config config = ConfigUtils.loadConfig(args[0]);
		CarsharingUtils.addConfigModules(config);

		final Scenario sc = ScenarioUtils.loadScenario(config);

		final Controler controler = new Controler( sc );
		
		installCarSharing(controler);
		
		controler.run();


	}

	public static void installCarSharing(final Controler controler) {
		Scenario sc = controler.getScenario() ;
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider( CarsharingQsimFactory.class );
			}
		});

		controler.setTripRouterFactory(
				CarsharingUtils.createTripRouterFactory(sc));


		//setting up the scoring function factory, inside different scoring functions are set-up
		CarsharingScoringFunctionFactory carsharingScoringFunctionFactory = new CarsharingScoringFunctionFactory(
				sc.getConfig(),
				sc.getNetwork());
		controler.setScoringFunctionFactory(carsharingScoringFunctionFactory);

		controler.addControlerListener(new CarsharingListener(controler,
				Integer.parseInt(controler.getConfig().getModule("Carsharing").getValue("statsWriterFrequency"))));
	}

}

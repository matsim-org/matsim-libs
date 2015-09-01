package playground.tschlenther.CarSharing.Cottbus;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
import org.matsim.contrib.carsharing.control.listeners.CarsharingListener;
import org.matsim.contrib.carsharing.qsim.CarsharingQsimFactory;
import org.matsim.contrib.carsharing.replanning.CarsharingSubtourModeChoiceStrategy;
import org.matsim.contrib.carsharing.replanning.RandomTripToCarsharingStrategy;
import org.matsim.contrib.carsharing.runExample.CarsharingUtils;
import org.matsim.contrib.carsharing.scoring.CarsharingScoringFunctionFactory;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import playground.tschlenther.CarSharing.RunExample.CarSharingConfigCreator;

public class RunCarSharingCottbus {

	public static void main(String[] args){
		Config config = new CarSharingConfigCreator().createConfig();
//		new CreateDemand(config);
		Logger.getLogger( "org.matsim.core.controler.Injector" ).setLevel(Level.OFF);
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler(scenario);
		installCarSharing(controler);
		controler.run();
	}

	public static void installCarSharing(final Controler controler) {
		Scenario sc = controler.getScenario() ;
		
		controler.addOverridingModule( new AbstractModule() {
			@Override
			public void install() {
				this.addPlanStrategyBinding("RandomTripToCarsharingStrategy").to( RandomTripToCarsharingStrategy.class ) ;
				this.addPlanStrategyBinding("CarsharingSubtourModeChoiceStrategy").to( CarsharingSubtourModeChoiceStrategy.class ) ;
			}
		});
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider( CarsharingQsimFactory.class );
			}
		});

		controler.setTripRouterFactory(CarsharingUtils.createTripRouterFactory(sc));

		//setting up the scoring function factory, inside different scoring functions are set-up
		controler.setScoringFunctionFactory(new CarsharingScoringFunctionFactory( sc.getConfig(), sc.getNetwork()));

		final CarsharingConfigGroup csConfig = (CarsharingConfigGroup) controler.getConfig().getModule(CarsharingConfigGroup.GROUP_NAME);
		csConfig.setStatsWriterFrequency("1");
		controler.addControlerListener(new CarsharingListener(controler,
				csConfig.getStatsWriterFrequency() ) ) ;
	}

	
}

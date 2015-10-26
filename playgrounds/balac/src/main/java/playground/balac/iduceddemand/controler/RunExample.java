package playground.balac.iduceddemand.controler;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import playground.balac.iduceddemand.strategies.RandomActivitiesSwaperStrategy;

public class RunExample {

	public static void main(String[] args) {

		final Config config = ConfigUtils.loadConfig(args[0]);

		final Scenario sc = ScenarioUtils.loadScenario(config);

		final Controler controler = new Controler( sc );
	
		controler.addOverridingModule( new AbstractModule() {
			@Override
			public void install() {
				this.addPlanStrategyBinding("RandomActivitiesSwaperStrategy").to( RandomActivitiesSwaperStrategy.class ) ;
			}
		});		
		controler.run();
		
		
	}

}

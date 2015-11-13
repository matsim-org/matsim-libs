package playground.agarwalamit.run;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.SeepageMobsimfactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.SeepageMobsimfactory.QueueWithBufferType;
import org.matsim.core.scenario.ScenarioUtils;

public class AARunOwnQueueWithBuffer {

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig( args[0] ) ;
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		final Controler controler = new Controler( scenario ) ;
		
		final MobsimFactory mobsimFactory = new SeepageMobsimfactory(QueueWithBufferType.amit) ;

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return mobsimFactory.createMobsim(controler.getScenario(), controler.getEvents());
					}
				});
			}
		});

		controler.run();
	}

}

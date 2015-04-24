package playground.agarwalamit.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.SeepageMobsimfactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.SeepageMobsimfactory.QueueWithBufferType;
import org.matsim.core.scenario.ScenarioUtils;

public class AARunOwnQueueWithBuffer {

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig( args[0] ) ;
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		Controler controler = new Controler( scenario ) ;
		
		MobsimFactory mobsimFactory = new SeepageMobsimfactory(QueueWithBufferType.amit) ; 

		controler.setMobsimFactory(mobsimFactory);
		
		controler.run();
	}

}

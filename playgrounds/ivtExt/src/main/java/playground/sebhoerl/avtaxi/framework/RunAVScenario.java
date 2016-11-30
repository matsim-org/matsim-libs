package playground.sebhoerl.avtaxi.framework;

import java.net.MalformedURLException;

import com.google.inject.multibindings.MapBinder;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.single_fifo.SingleFIFODispatcher;

public class RunAVScenario {
	public static void main(String[] args) throws MalformedURLException {
		String configFile = args[0];
		
		Config config = ConfigUtils.loadConfig(configFile, new AVConfigGroup());
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule(0.05));
		controler.addOverridingModule(new DynQSimModule<>(AVQSimProvider.class));
		controler.addOverridingModule(new AVModule());

		/*controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				AVUtils.bindDispatcherFactory(binder(), "MyStrategy").to(SingleFIFODispatcher.Factory.class);
			}
		});*/

		controler.run();
	}
}

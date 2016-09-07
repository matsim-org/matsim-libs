package playground.mrieser.devmtg1;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

public class MyNewProjectScript {

	public static void main(String[] args) {
		
		Config config = ConfigUtils.createConfig();
		config.controler().setLastIteration(1);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
//		ConfigUtils.loadConfig(config, "additionalConfig.xml"); // overwrite stuff
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		// modify scenario
		
		Controler controler = new Controler(scenario);
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
//				this.bindMobsim().toProvider(MyMobsimProvider.class);
//				this.bindMobsim().toProvider(new Provider<Mobsim>() {
//					@Override
//					public Mobsim get() {
//						MyMobsim mobsim = new MyMobsim();
//						mobsim.setSomeOption(true);
//						return mobsim;
//						// the provider could also re-use objects
//					}
//				});
				this.bindMobsim().to(MyMobsim.class);
//				this.addEventHandlerBinding().to(MyEventHandler.class);
				this.bind(MyDummyClass.class).toInstance(new MyDummyClass());
//				this.bind(MyDummyClass.class).to(MyDummyClassImpl.class); // alternative; a new instance will be created every time, like a factory
//				this.bind(MyDummyClass.class); // alternative to the above, but without interface/implementation
//				this.bindMobsim().to(JDEQSimulation.class);
				
//				this.install(otherModule); // include setup from other "modules", might overwrite previous bindings
				
				// the config is already available, so we can access it:
				if (getConfig().controler().getLastIteration() > 100) {
//					this.bind(MyLongRunningAnalysis.class);
				}
			}
		});
		
		controler.run();
		
		// the controler by default loads "ControlerDefaultsModule"

		
		
		// more examples: tutorial.programming.*
		
	}
}

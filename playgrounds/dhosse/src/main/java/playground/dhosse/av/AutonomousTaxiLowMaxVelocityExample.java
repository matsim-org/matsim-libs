package playground.dhosse.av;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.MatsimVrpContextImpl;
import org.matsim.contrib.dvrp.data.VrpDataImpl;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import playground.jbischoff.taxi.usability.TaxiConfigGroup;
import playground.jbischoff.taxi.usability.TaxiserviceRoutingModule;

public class AutonomousTaxiLowMaxVelocityExample {
	
	static MatsimVrpContextImpl context;

	public static void main(String args[]){
		
		Config config = ConfigUtils.loadConfig("/home/dhosse/at/config.xml",
				new TaxiConfigGroup());
		
		config.controler().setLastIteration(0);
		config.controler().setOutputDirectory("/home/dhosse/at/output/");
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		final Controler controler = new Controler(scenario);
		
		initiate(controler);
		
		controler.run();
		
	}
	
	private static void initiate(final Controler controler){
		
		context = new MatsimVrpContextImpl();
		context.setScenario(controler.getScenario());
		VrpDataImpl data = new VrpDataImpl();
		new VehicleReader(controler.getScenario(), data).parse("/home/dhosse/at/vehicles.xml");
		context.setVrpData(data);

		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				
				addRoutingModuleBinding("taxi").toInstance(new TaxiserviceRoutingModule(controler));
				
			}
			
		});
		
		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				bindMobsim().toProvider(AutonomousTaxiQSimProvider.class);
				bind(MatsimVrpContext.class).toInstance(context);
			}
			
		});
		
	}
	
}

package playground.singapore.springcalibration.run;

import org.apache.log4j.Logger;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.SelectBestPlanStrategyProvider;


public class RunSingapore {	
	private final static Logger log = Logger.getLogger(RunSingapore.class);
	
	

	public static void main(String[] args) {
		log.info("Running SingaporeControlerRunner");
				
		Controler controler = new Controler(args[0]);
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding("taxi").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("taxi").to(carTravelDisutilityFactoryKey());
			}
		});
		
			
		controler.addControlerListener(new SingaporeControlerListener());
		
		controler.run();
		log.info("finished SingaporeControlerRunner");
	}
}

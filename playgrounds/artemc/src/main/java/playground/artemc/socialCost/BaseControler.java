package playground.artemc.socialCost;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioImpl;


public class BaseControler {

private static final Logger log = Logger.getLogger(BaseControler.class);
	
	static String configFile;
	
	public static void main(String[] args) {
		
		configFile = args[0];
		BaseControler runner = new BaseControler();
		runner.runBaseScanerio(configFile);
	}
	
	private void runBaseScanerio(String configFile) {
		Controler controler = new Controler(configFile);
		Initializer initializer = new Initializer();
		controler.addControlerListener(initializer);
		
		// Additional analysis
		controler.addControlerListener(new WelfareAnalysisControlerListener((ScenarioImpl) controler.getScenario()));
		controler.setOverwriteFiles(true);
		controler.run();
	}
	
	private static class Initializer implements StartupListener {

		@Override
		public void notifyStartup(StartupEvent event) {
			Controler controler = event.getControler();
			// create a plot containing the mean travel times
			Set<String> transportModes = new HashSet<String>();
			transportModes.add(TransportMode.car);
			transportModes.add(TransportMode.pt);
			transportModes.add(TransportMode.walk);
			MeanTravelTimeCalculator mttc = new MeanTravelTimeCalculator(controler.getScenario(), transportModes);
			controler.addControlerListener(mttc);
			controler.getEvents().addHandler(mttc);
		}
	}

}

package playground.ciarif.retailers;

import org.matsim.config.Config;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.StartupListener;


public class RetailerControlerListener implements StartupListener,IterationEndsListener {

		private final Config config;

		public RetailerControlerListener(final Config config) {
			this.config = config;
		}

		public void notifyStartup(final StartupEvent controlerStartupEvent) {
			//MatsimRetailersReader retailers_parser = new MatsimRetailersReader(Retailers.getSingleton());
			//retailers_parser.readFile("input/retailers.txt"); //TODO Now is hard coded but should use a config file
		}

		public void notifyIterationEnds(IterationEndsEvent event) {
			// TODO Auto-generated method stub
			
		}

	
}

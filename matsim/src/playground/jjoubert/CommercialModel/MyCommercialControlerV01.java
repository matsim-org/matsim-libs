package playground.jjoubert.CommercialModel;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.Events;

import playground.jjoubert.CommercialModel.Listeners.MyAllEventCounter;
import playground.jjoubert.CommercialModel.Listeners.MyIterationEndsListener;
import playground.jjoubert.CommercialModel.Listeners.MyIterationStartsListener;
import playground.jjoubert.CommercialModel.Listeners.MyPostMobsimListener;
import playground.jjoubert.CommercialModel.Listeners.MyPreMobsimListener;
import playground.jjoubert.CommercialModel.Listeners.MyReplanningListener;
import playground.jjoubert.CommercialModel.Listeners.MyScoringListener;
import playground.jjoubert.CommercialModel.Listeners.MyShutdownListener;
import playground.jjoubert.CommercialModel.Listeners.MySimulationStartListener;

public class MyCommercialControlerV01 {
	
	private final static Logger log = Logger.getLogger(MyCommercialControlerV01.class);
	
	public static void main(String[] args){

		Controler c = new Controler("src/playground/jjoubert/CommercialDemand/configLocal.xml");
		long t = System.currentTimeMillis();
		
		// Set some Controler characteristics
		c.setCreateGraphs(false);
		c.setWriteEventsInterval(1);
		
		// Add all the listeners
		c.addControlerListener(new MySimulationStartListener());	// 1.
		c.addControlerListener(new MyIterationStartsListener());	// 2.
		c.addControlerListener(new MyPreMobsimListener());			// 3.
		c.addControlerListener(new MyPostMobsimListener());			// 4.
		c.addControlerListener(new MyScoringListener());			// 5.
		c.addControlerListener(new MyIterationEndsListener());		// 6.
		c.addControlerListener(new MyReplanningListener());			// 7.
		c.addControlerListener(new MyShutdownListener());			// 8.
		
		MyAllEventCounter eventCounter = new MyAllEventCounter();
		Events events = new Events();
		// Add all the events handler(s)
		events.addHandler(eventCounter);
//		c.getEvents().addHandler(eventCounter);
		
		c.run();
		long time = (System.currentTimeMillis() - t) / 1000;
		log.info(" --->  Time taken (in seconds): " + time);
		log.info("  --> Total events: " + eventCounter.totalEvents);
	}

}

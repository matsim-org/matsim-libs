package playground.jjoubert.CommercialModel;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;

import playground.jjoubert.CommercialModel.Listeners.MyCommercialListener;
import playground.jjoubert.CommercialModel.Listeners.MyIterationEndsListener;
import playground.jjoubert.CommercialModel.Listeners.MyIterationStartsListener;
import playground.jjoubert.CommercialModel.Listeners.MyPostMobsimListener;
import playground.jjoubert.CommercialModel.Listeners.MyPreMobsimListener;
import playground.jjoubert.CommercialModel.Listeners.MyReplanningListener;
import playground.jjoubert.CommercialModel.Listeners.MyScoringListener;
import playground.jjoubert.CommercialModel.Listeners.MyShutdownListener;
import playground.jjoubert.CommercialModel.Listeners.MySimulationStartListener;

public class MyCommercialControlerV01 {
	
	private final static Logger LOG = Logger.getLogger(MyCommercialControlerV01.class);
	private final static int COMMERCIAL_THRESHOLD = 1000000;
	
	public static void main(String[] args){

		Controler c = new Controler("src/playground/jjoubert/CommercialDemand/configLocal.xml");
		long t = System.currentTimeMillis();
		
		// Set some Controler characteristics
		c.setCreateGraphs(true);
		c.setWriteEventsInterval(1);
		
		/*
		 * Add all the listeners
		 * NOTE: The event handlers are added in the StartupListener
		 * TODO: Check if this is true.
		 */
		MyCommercialListener cl = new MyCommercialListener(COMMERCIAL_THRESHOLD);
		c.addControlerListener(cl);
				
		c.run();
		long time = (System.currentTimeMillis() - t) / 1000;
		LOG.info(" --->  Time taken (in seconds): " + time);
	}

}

package playground.jjoubert.CommercialModel;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;

import playground.jjoubert.CommercialModel.Listeners.MyCommercialActivityDensityListener;

public class MyCommercialControlerV01 {
	
	private final static Logger LOG = Logger.getLogger(MyCommercialControlerV01.class);
	
	public static void main(String[] args){

//		Controler c = new Controler("src/playground/jjoubert/CommercialModel/configLocal.xml");
		Controler c = new Controler("src/playground/jjoubert/CommercialModel/configIVTSim0.xml");
		long t = System.currentTimeMillis();
		
		// Set some Controler characteristics
		c.setCreateGraphs(true);
		c.setWriteEventsInterval(20);
		
		/*
		 * Add all the listeners
		 * NOTE: The event handlers are added in the StartupListener
		 * TODO: Check if this is true.
		 */
		MyCommercialActivityDensityListener cadl = new MyCommercialActivityDensityListener();
		c.addControlerListener(cadl);
		
//		MyCommercialLegHistogramListener clhl = new MyCommercialLegHistogramListener();
//		c.addControlerListener(clhl);
				
		c.run();
		long time = (System.currentTimeMillis() - t) / 1000;
		LOG.info(" --->  Time taken (in seconds): " + time);
	}

}

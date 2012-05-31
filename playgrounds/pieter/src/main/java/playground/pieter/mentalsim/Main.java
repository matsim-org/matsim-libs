package playground.pieter.mentalsim;

import org.matsim.core.controler.Controler;

import playground.pieter.mentalsim.controler.listeners.MobSimSwitcher;
import playground.pieter.mentalsim.controler.listeners.SimpleAnnealer;
import playground.pieter.mentalsim.trafficinfo.MyTTCalcFactory;


public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controler c = new Controler(args);
		c.setOverwriteFiles(true);
		c.setTravelTimeCalculatorFactory(new MyTTCalcFactory());
		c.addControlerListener(new MobSimSwitcher());
		c.addControlerListener(new SimpleAnnealer());
//		c.addControlerListener(new MyIterationEndsListener());
		c.run();
			
		
	}

}

package playground.pieter.mentalsim;

import org.matsim.core.controler.Controler;

import playground.pieter.mentalsim.controler.MentalSimControler;
import playground.pieter.mentalsim.controler.listeners.MentalSimInit;
import playground.pieter.mentalsim.controler.listeners.MobSimSwitcher;
import playground.pieter.mentalsim.controler.listeners.ScoreResetStrategyModuleAppender;
import playground.pieter.mentalsim.controler.listeners.SimpleAnnealer;
import playground.pieter.mentalsim.trafficinfo.MyTTCalcFactory;


public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MentalSimControler c = new MentalSimControler(args);
//		Controler c = new Controler(args);
		c.setOverwriteFiles(true);
		c.setTravelTimeCalculatorFactory(new MyTTCalcFactory());
//		execution order of these iteration start listeners is in reverse order of adding them to the controler
		c.addControlerListener(new SimpleAnnealer());
//		c.addControlerListener(new MentalSimInit(c));
		c.addControlerListener(new MobSimSwitcher(c));
		c.addControlerListener(new ScoreResetStrategyModuleAppender(c));
		
		c.run();
		System.exit(0);
		
	}

}

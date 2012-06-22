package playground.pieter.mentalsim;

import org.matsim.core.controler.Controler;

import playground.pieter.mentalsim.controler.MentalSimControler;
import playground.pieter.mentalsim.controler.listeners.ExpensiveSimScoreWriter;
import playground.pieter.mentalsim.controler.listeners.MentalSimSubSetSimulationListener;
import playground.pieter.mentalsim.controler.listeners.MobSimSwitcher;
import playground.pieter.mentalsim.controler.listeners.ScoreResetStrategyModuleAppender;
import playground.pieter.mentalsim.controler.listeners.SimpleAnnealer;
import playground.pieter.mentalsim.trafficinfo.MyTTCalcFactory;
import playground.pieter.router.util.StochasticRouterFactory;


public class NoMentalSim {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controler c = new Controler(args);
		c.setOverwriteFiles(true);
//		c.addControlerListener(new SimpleAnnealer());
		StochasticRouterFactory str = new StochasticRouterFactory();
		str.setBeta(0.0000001);
		c.setLeastCostPathCalculatorFactory(str);
		c.run();
		System.exit(0);
		
	}

}

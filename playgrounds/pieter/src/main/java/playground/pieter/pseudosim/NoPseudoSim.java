package playground.pieter.pseudosim;

import org.matsim.core.controler.Controler;

import playground.pieter.pseudosim.controler.PseudoSimControler;
import playground.pieter.pseudosim.controler.listeners.ExpensiveSimScoreWriter;
import playground.pieter.pseudosim.controler.listeners.PseudoSimPlanMarkerModuleAppender;
import playground.pieter.pseudosim.controler.listeners.PseudoSimSubSetSimulationListener;
import playground.pieter.pseudosim.controler.listeners.MobSimSwitcher;
import playground.pieter.pseudosim.controler.listeners.SimpleAnnealer;
import playground.pieter.router.costcalculators.CapacityFavoringStochasticCostCalculator;
import playground.pieter.router.costcalculators.CapacityFavoringStochasticCostCalculatorFactory;
import playground.pieter.router.util.StochasticRouterFactory;


public class NoPseudoSim {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controler c = new Controler(args);
		c.setOverwriteFiles(true);
//		c.addControlerListener(new SimpleAnnealer());
//		StochasticRouterFactory str = new StochasticRouterFactory();
//		str.setBeta(4);
//		c.setLeastCostPathCalculatorFactory(str);
//		c.setTravelDisutilityFactory(new CapacityFavoringStochasticCostCalculatorFactory());
//		c.addControlerListener(new SimpleAnnealer());
		c.run();
		System.exit(0);
		
	}

}

package playground.pieter.pseudosim;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.PlanSelector;

import playground.pieter.pseudosim.controler.PseudoSimControler;
import playground.pieter.pseudosim.controler.listeners.BeforePseudoSimSelectedPlanNullifier;
import playground.pieter.pseudosim.controler.listeners.BeforeMentalSimSelectedPlanScoreRecorder;
import playground.pieter.pseudosim.controler.listeners.ExpensiveSimScoreWriter;
import playground.pieter.pseudosim.controler.listeners.IterationEndsSelectedPlanScoreRestoreListener;
import playground.pieter.pseudosim.controler.listeners.PseudoSimPlanMarkerModuleAppender;
import playground.pieter.pseudosim.controler.listeners.PseudoSimSubSetSimulationListener;
import playground.pieter.pseudosim.controler.listeners.MobSimSwitcher;
import playground.pieter.pseudosim.controler.listeners.SimpleAnnealer;
import playground.pieter.pseudosim.trafficinfo.MyTTCalcFactory;


public class Main {

	/**
	 * @param args - The name of the config file for the mentalsim run.
	 */
	public static void main(String[] args) {
		PseudoSimControler c = new PseudoSimControler(args);
		c.setOverwriteFiles(true);
		c.setSimulateSubsetPersonsOnly(false);
		c.setTravelTimeCalculatorFactory(new MyTTCalcFactory());
//		execution order of these iteration start listeners is in reverse order of adding them to the controler
//		PlanSelector mentalPlanSelector = new ExpBetaPlanSelector(new PlanCalcScoreConfigGroup());
		PlanSelector mentalPlanSelector = new BestPlanSelector();
		c.addControlerListener(new PseudoSimSubSetSimulationListener(c,mentalPlanSelector));
		c.addControlerListener(new SimpleAnnealer());
		c.addControlerListener(new MobSimSwitcher(c));
		c.addControlerListener(new PseudoSimPlanMarkerModuleAppender(c));
		c.addControlerListener(new ExpensiveSimScoreWriter(c));
//		c.addControlerListener(new BeforeMentalSimSelectedPlanNullifier(c));
//		c.addControlerListener(new BeforeMentalSimSelectedPlanScoreRecorder(c));
//		c.addControlerListener(new IterationEndsSelectedPlanScoreRestoreListener(c));
		c.run();
		System.exit(0);
		
	}

}

package playground.pieter.mentalsim;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.PlanSelector;

import playground.pieter.mentalsim.controler.MentalSimControler;
import playground.pieter.mentalsim.controler.listeners.BeforeMentalSimSelectedPlanNullifier;
import playground.pieter.mentalsim.controler.listeners.BeforeMentalSimSelectedPlanScoreRecorder;
import playground.pieter.mentalsim.controler.listeners.ExpensiveSimScoreWriter;
import playground.pieter.mentalsim.controler.listeners.IterationEndsSelectedPlanScoreRestoreListener;
import playground.pieter.mentalsim.controler.listeners.MentalSimSubSetSimulationListener;
import playground.pieter.mentalsim.controler.listeners.MobSimSwitcher;
import playground.pieter.mentalsim.controler.listeners.MentalSimPlanMarkerModuleAppender;
import playground.pieter.mentalsim.controler.listeners.SimpleAnnealer;
import playground.pieter.mentalsim.trafficinfo.MyTTCalcFactory;


public class Main {

	/**
	 * @param args - The name of the config file for the mentalsim run.
	 */
	public static void main(String[] args) {
		MentalSimControler c = new MentalSimControler(args);
		c.setOverwriteFiles(true);
		c.setSimulateSubsetPersonsOnly(true);
		c.setTravelTimeCalculatorFactory(new MyTTCalcFactory());
//		execution order of these iteration start listeners is in reverse order of adding them to the controler
//		PlanSelector mentalPlanSelector = new ExpBetaPlanSelector(new PlanCalcScoreConfigGroup());
		PlanSelector mentalPlanSelector = new BestPlanSelector();
		c.addControlerListener(new MentalSimSubSetSimulationListener(c,mentalPlanSelector));
		c.addControlerListener(new SimpleAnnealer());
		c.addControlerListener(new MobSimSwitcher(c));
		c.addControlerListener(new MentalSimPlanMarkerModuleAppender(c));
		c.addControlerListener(new ExpensiveSimScoreWriter(c));
//		c.addControlerListener(new BeforeMentalSimSelectedPlanNullifier(c));
//		c.addControlerListener(new BeforeMentalSimSelectedPlanScoreRecorder(c));
//		c.addControlerListener(new IterationEndsSelectedPlanScoreRestoreListener(c));
		c.run();
		System.exit(0);
		
	}

}

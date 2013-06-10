package playground.pieter.pseudosim;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.PlanSelector;

import playground.pieter.pseudosim.controler.PseudoSimControler;
import playground.pieter.pseudosim.controler.listeners.BeforePseudoSimSelectedPlanNullifier;
import playground.pieter.pseudosim.controler.listeners.BeforePSimSelectedPlanScoreRecorder;
import playground.pieter.pseudosim.controler.listeners.ExpensiveSimScoreWriter;
import playground.pieter.pseudosim.controler.listeners.IterationEndsSelectedPlanScoreRestoreListener;
import playground.pieter.pseudosim.controler.listeners.PseudoSimPlanMarkerModuleAppender;
import playground.pieter.pseudosim.controler.listeners.MobSimSwitcher;
import playground.pieter.pseudosim.controler.listeners.SimpleAnnealer;


public class Main {

	/**
	 * @param args - The name of the config file for the mentalsim run.
	 */
	public static void main(String[] args) {
		PseudoSimControler c = new PseudoSimControler(args);
		c.setOverwriteFiles(true);
		c.run();
		System.exit(0);
		
	}

}

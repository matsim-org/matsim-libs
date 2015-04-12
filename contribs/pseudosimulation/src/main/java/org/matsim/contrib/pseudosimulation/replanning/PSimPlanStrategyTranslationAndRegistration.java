package org.matsim.contrib.pseudosimulation.replanning;

import org.matsim.contrib.pseudosimulation.replanning.factories.*;
import org.matsim.contrib.pseudosimulation.replanning.modules.PSimPlanMarkerModule;
import org.matsim.core.replanning.DefaultPlanStrategiesModule;
import org.matsim.contrib.pseudosimulation.controler.PSimControler;

import java.util.ArrayList;

/**
 * @author fouriep
 *         <P>
 *         Essentially a lookup used in config translation. Also registers
 *         extended strategies with the controler.
 *         <P>
 *         If a mutating strategy is sent for pseudo-simulation, it needs to be
 *         marked as such, and registered with the {@link PSimControler}.
 *         Non-mutating strategies, e.g. selector strategies, should be disabled
 *         during PSim iterations, and only run during QSim iterations.
 * 
 *         <P>
 *         This class records strategies that should work with PSim. It extends
 *         their factories by appending a {@link PSimPlanMarkerModule} at the
 *         end of each strategy. Each factory is registered during controler
 *         construction, and the config entries are changed to refer to their
 *         PSim equivalents in the controler's substituteStrategies() method.
 * 
 *         <P>
 *
 *         <P>
 *         <B>NOTE:</B> to save processing overhead, selector strategies are set
 *         up to always return the person's current selected plan during
 *         non-QSim iterations.
 * 
 * 
 */
public class PSimPlanStrategyTranslationAndRegistration {

	private final ArrayList<String> compatibleStrategies = new ArrayList<>();

	public PSimPlanStrategyTranslationAndRegistration(PSimControler controler) {
		String strategyName = DefaultPlanStrategiesModule.DefaultSelector.BestScore
				.toString();
		compatibleStrategies.add(strategyName);
		controler.getMATSimControler().addPlanStrategyFactory(strategyName + "PSim",
				new PSimBestScorePlanStrategyFactory());
		strategyName = DefaultPlanStrategiesModule.DefaultSelector.KeepLastSelected
				.toString();
		compatibleStrategies.add(strategyName);
		controler.getMATSimControler().addPlanStrategyFactory(strategyName + "PSim",
				new PSimKeepLastSelectedPlanStrategyFactory());
		strategyName = DefaultPlanStrategiesModule.DefaultSelector.SelectExpBeta.toString();
		compatibleStrategies.add(strategyName);
		controler.getMATSimControler().addPlanStrategyFactory(strategyName + "PSim",
				PSimSelectExpBetaPlanStrategyFactory.class);
		strategyName = DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString();
		compatibleStrategies.add(strategyName);
		controler.getMATSimControler().addPlanStrategyFactory(strategyName + "PSim",
				new PSimChangeExpBetaPlanStrategyFactory(controler.getMATSimControler().getScenario()));
		strategyName = DefaultPlanStrategiesModule.DefaultSelector.SelectRandom.toString();
		compatibleStrategies.add(strategyName);
		controler.getMATSimControler().addPlanStrategyFactory(strategyName + "PSim",
				new PSimSelectRandomStrategyFactory());




		compatibleStrategies.add("TimeAllocationMutator");
		controler.getMATSimControler().addPlanStrategyFactory("TimeAllocationMutatorPSim",
				new PSimTimeAllocationMutatorPlanStrategyFactory(controler));
		compatibleStrategies.add("SubtourModeChoice");
		controler.getMATSimControler().addPlanStrategyFactory("SubtourModeChoicePSim",
				new PSimSubtourModeChoiceStrategyFactory(controler));
		compatibleStrategies.add("TripSubtourModeChoice");
		controler.getMATSimControler().addPlanStrategyFactory("TripSubtourModeChoicePSim",
				new PSimTripSubtourModeChoiceStrategyFactory(controler));
		compatibleStrategies.add("DoNothing");
		controler.getMATSimControler().addPlanStrategyFactory("DoNothingPSim",
				new PSimDoNothingPlanStrategyFactory(controler));
		compatibleStrategies.add("ChangeLegMode");
		controler.getMATSimControler().addPlanStrategyFactory("ChangeLegModePSim",
				new PSimChangeLegModeStrategyFactory(controler));
		compatibleStrategies.add("ChangeSingleLegMode");
		controler.getMATSimControler().addPlanStrategyFactory("ChangeSingleLegModePSim",
				new PSimChangeSingleLegModeStrategyFactory(controler));

	}

	public ArrayList<String> getCompatibleStrategies() {
		return compatibleStrategies;
	}

}

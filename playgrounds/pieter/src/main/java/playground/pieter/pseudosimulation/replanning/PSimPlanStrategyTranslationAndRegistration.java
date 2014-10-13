package playground.pieter.pseudosimulation.replanning;

import java.util.ArrayList;

import playground.pieter.pseudosimulation.controler.PSimControler;
import playground.pieter.pseudosimulation.replanning.factories.PSimBestScorePlanStrategyFactory;
import playground.pieter.pseudosimulation.replanning.factories.PSimChangeExpBetaPlanStrategyFactory;
import playground.pieter.pseudosimulation.replanning.factories.PSimChangeLegModeStrategyFactory;
import playground.pieter.pseudosimulation.replanning.factories.PSimChangeSingleLegModeStrategyFactory;
import playground.pieter.pseudosimulation.replanning.factories.PSimDoNothingPlanStrategyFactory;
import playground.pieter.pseudosimulation.replanning.factories.PSimKeepLastSelectedPlanStrategyFactory;
import playground.pieter.pseudosimulation.replanning.factories.PSimLocationChoicePlanStrategyFactory;
import playground.pieter.pseudosimulation.replanning.factories.PSimReRoutePlanStrategyFactory;
import playground.pieter.pseudosimulation.replanning.factories.PSimSelectExpBetaPlanStrategyFactory;
import playground.pieter.pseudosimulation.replanning.factories.PSimSelectPathSizeLogitStrategyFactory;
import playground.pieter.pseudosimulation.replanning.factories.PSimSelectRandomStrategyFactory;
import playground.pieter.pseudosimulation.replanning.factories.PSimSubtourModeChoiceStrategyFactory;
import playground.pieter.pseudosimulation.replanning.factories.PSimTimeAllocationMutatorPlanStrategyFactory;
import playground.pieter.pseudosimulation.replanning.factories.PSimTripSubtourModeChoiceStrategyFactory;
import playground.pieter.pseudosimulation.replanning.modules.PSimPlanMarkerModule;

import org.matsim.core.controler.PlanStrategyRegistrar;

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
 *         Each strategy name is taken from the enum in the
 *         {@link PlanStrategyRegistrar} to ensure future consistency
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
		String strategyName = PlanStrategyRegistrar.Selector.BestScore
				.toString();
		compatibleStrategies.add(strategyName);
		controler.getMATSimControler().addPlanStrategyFactory(strategyName + "PSim",
				new PSimBestScorePlanStrategyFactory());
		strategyName = PlanStrategyRegistrar.Selector.KeepLastSelected
				.toString();
		compatibleStrategies.add(strategyName);
		controler.getMATSimControler().addPlanStrategyFactory(strategyName + "PSim",
				new PSimKeepLastSelectedPlanStrategyFactory());
		strategyName = PlanStrategyRegistrar.Selector.SelectExpBeta.toString();
		compatibleStrategies.add(strategyName);
		controler.getMATSimControler().addPlanStrategyFactory(strategyName + "PSim",
				new PSimSelectExpBetaPlanStrategyFactory());
		strategyName = PlanStrategyRegistrar.Selector.ChangeExpBeta.toString();
		compatibleStrategies.add(strategyName);
		controler.getMATSimControler().addPlanStrategyFactory(strategyName + "PSim",
				new PSimChangeExpBetaPlanStrategyFactory());
		strategyName = PlanStrategyRegistrar.Selector.SelectRandom.toString();
		compatibleStrategies.add(strategyName);
		controler.getMATSimControler().addPlanStrategyFactory(strategyName + "PSim",
				new PSimSelectRandomStrategyFactory());
		strategyName = PlanStrategyRegistrar.Selector.SelectPathSizeLogit
				.toString();
		compatibleStrategies.add(strategyName);
		controler.getMATSimControler().addPlanStrategyFactory(strategyName + "PSim",
				new PSimSelectPathSizeLogitStrategyFactory());

		strategyName = PlanStrategyRegistrar.Names.ReRoute.toString();
		compatibleStrategies.add(strategyName);
		controler.getMATSimControler().addPlanStrategyFactory(strategyName + "PSim",
				new PSimReRoutePlanStrategyFactory(controler));
		compatibleStrategies.add("LocationChoice");
		controler.getMATSimControler().addPlanStrategyFactory("LocationChoicePSim",
				new PSimLocationChoicePlanStrategyFactory(controler));
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

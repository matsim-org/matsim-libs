package playground.pieter.pseudosim.replanning;

import java.util.ArrayList;

import org.matsim.core.controler.PlanStrategyFactoryRegister;
import org.matsim.core.replanning.modules.ReRoutePlanStrategyFactory;

import playground.pieter.pseudosim.controler.PseudoSimControler;
import playground.pieter.pseudosim.replanning.factories.PSimChangeTripModeStrategyFactory;
import playground.pieter.pseudosim.replanning.factories.PSimDoNothingPlanStrategyFactory;
import playground.pieter.pseudosim.replanning.factories.PSimLocationChoicePlanStrategyFactory;
import playground.pieter.pseudosim.replanning.factories.PSimReRoutePlanStrategyFactory;
import playground.pieter.pseudosim.replanning.factories.PSimSubtourModeChoiceStrategyFactory;
import playground.pieter.pseudosim.replanning.factories.PSimTimeAllocationMutatorPlanStrategyFactory;
import playground.pieter.pseudosim.replanning.factories.PSimTripSubtourModeChoiceStrategyFactory;
import playground.pieter.pseudosim.replanning.factories.PSimTripTimeAllocationMutatorStrategyFactory;

public class PSimPlanStrategyRegistrar {



//		public static enum Names { ChangeLegMode } ;
		// (1) I think there should be constants rather than Strings, because these Strings are used elsewhere in the code. kai, may'13
		// (2) I think enums are better than Strings, since it allows to iterate through the registry.  kai, may'13
		// (3) "Names" could be refactored into something else if appropriate. kai, may'13
		
		private ArrayList<String> compatibleStrategies = new ArrayList<String>();

		public PSimPlanStrategyRegistrar(PseudoSimControler controler){
			
			// strategy packages that select, copy, and modify.  (The copying is done implicitly as soon as "addStrategyModule" is called
			// at least once).
			compatibleStrategies.add("ReRoute");
			controler.addPlanStrategyFactory("ReRoutePSim", new PSimReRoutePlanStrategyFactory(controler));	
			compatibleStrategies.add("LocationChoice");
			controler.addPlanStrategyFactory("LocationChoicePSim", new PSimLocationChoicePlanStrategyFactory(controler));	
			compatibleStrategies.add("TimeAllocationMutator");
			controler.addPlanStrategyFactory("TimeAllocationMutatorPSim", new PSimTimeAllocationMutatorPlanStrategyFactory(controler));
			compatibleStrategies.add("SubtourModeChoice");
			controler.addPlanStrategyFactory("SubtourModeChoicePSim", new PSimSubtourModeChoiceStrategyFactory(controler));
			compatibleStrategies.add("DoNothing");
			controler.addPlanStrategyFactory("DoNothingPSim", new PSimDoNothingPlanStrategyFactory(controler));
			compatibleStrategies.add("TransitTimeAllocationMutator");
			controler.addPlanStrategyFactory("TransitTimeAllocationMutatorPSim", new PSimTripTimeAllocationMutatorStrategyFactory(controler));
			compatibleStrategies.add("TransitChangeLegMode");
			controler.addPlanStrategyFactory("TransitChangeLegModePSim", new PSimChangeTripModeStrategyFactory(controler));
			compatibleStrategies.add("TransitSubtourModeChoice");
			controler.addPlanStrategyFactory("TransitSubtourModeChoicePSim", new PSimTripSubtourModeChoiceStrategyFactory(controler));
//			register.register("SubtourModeChoice", new SubtourModeChoiceStrategyFactory());
//			register.register(Names.ChangeLegMode.toString(), new ChangeLegModeStrategyFactory());
//			register.register("ChangeSingleLegMode", new ChangeSingleLegModeStrategyFactory());
//			register.register("ChangeSingleTripMode", new ChangeSingleTripModeStrategyFactory());
//			register.register("ChangeTripMode", new ChangeTripModeStrategyFactory());
//			register.register("TransitTimeAllocationMutator", new TripTimeAllocationMutatorStrategyFactory());
//			register.register("TripTimeAllocationMutator_ReRoute", new TripTimeAllocationMutatorRerouteStrategyFactory());
//			register.register("TripSubtourModeChoice", new TripSubtourModeChoiceStrategyFactory());
//			
//			// for backwards compatibility:
//			register.register("TransitChangeLegMode", new ChangeTripModeStrategyFactory()); // for backwards compatibility
//			register.register("TransitTimeAllocationMutator_ReRoute", new TripTimeAllocationMutatorRerouteStrategyFactory()); // for backwards compatibility
//			register.register("TransitSubtourModeChoice", new TripSubtourModeChoiceStrategyFactory()); // for backwards compatibility
//			register.register("TransitChangeSingleLegMode", new ChangeSingleTripModeStrategyFactory()); // for backwards compatibility
		}

		public ArrayList<String> getCompatibleStrategies() {
			return compatibleStrategies;
		}

		


	

}

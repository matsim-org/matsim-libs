package org.matsim.contrib.bicycle;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
// import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
// import org.matsim.contrib.bicycle.BicycleConfigGroup.BicycleScoringType;
// import org.matsim.core.api.experimental.events.EventsManager;
// import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;

final class PsafeScoringFunctionFactory implements ScoringFunctionFactory {
	// ok to have this public final when the constructor is package-private/injected: can only used through injection

	@Inject
	private ScoringParametersForPerson parameters;

	@Inject
	private Scenario scenario;

	//@Inject
	//private EventsManager eventsManager;

	@Inject
	private PsafeConfigGroup psafeConfigGroup;

	@Inject
	private PsafeScoringFunctionFactory() {
	}
	
	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		SumScoringFunction sumScoringFunction = new SumScoringFunction();

		final ScoringParameters params = parameters.getScoringParameters(person);
		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring( params ));

		// BicycleScoringType bicycleScoringType = bicycleConfigGroup.getBicycleScoringType();
		
		sumScoringFunction.addScoringFunction(new PsafeNewStyleScoring(params, scenario.getNetwork(), scenario.getConfig().transit().getTransitModes(), psafeConfigGroup));
		

//		if (bicycleScoringType == BicycleScoringType.legBased) {
//			throw new RuntimeException( "this execution path should no longer be used.");
//			sumScoringFunction.addScoringFunction(new BicycleLegScoring(params, scenario.getNetwork(), scenario.getConfig().transit().getTransitModes(), bicycleConfigGroup));
//		} else if (bicycleScoringType == BicycleScoringType.linkBased) {
//			BicycleLinkScoring bicycleLinkScoring = new BicycleLinkScoring(params, scenario, bicycleConfigGroup);
//			sumScoringFunction.addScoringFunction(bicycleLinkScoring);
//			
//			CarCounter carCounter = new CarCounter( bicycleLinkScoring );
//			eventsManager.addHandler(carCounter);
//		} else {
//			throw new IllegalArgumentException("Bicycle scoring type " + bicycleScoringType + " not known.");
//		}
//
		return sumScoringFunction;
	}

	
//	private static class CarCounter implements BasicEventHandler{
//		private final BicycleLinkScoring bicycleLinkScoring;
//
//		private CarCounter( BicycleLinkScoring bicycleLinkScoring ) {
//			this.bicycleLinkScoring = bicycleLinkScoring;
//		}
//
//		@Override
//		public void handleEvent( Event event ) {
//			if ( event instanceof MotorizedInteractionEvent ){
//				bicycleLinkScoring.handleEvent(event);
//			}
//		}
//	}
}

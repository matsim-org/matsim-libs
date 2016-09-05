package playground.kai.usecases.opdytsintegration.modechoice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.Injector;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scoring.ExperiencedPlansService;

import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;

/**
 * 
 * @author Kai Nagel based on Gunnar Flötteröd
 * 
 */
public class ModeChoiceObjectiveFunction implements ObjectiveFunction {
	
	private final List<String> modeHierarchy = new ArrayList<>() ;
	private final Map< String, Double > shares = new HashMap<>() ;

	ModeChoiceObjectiveFunction() {
		modeHierarchy.add( TransportMode.walk ) ;
		modeHierarchy.add( TransportMode.bike ) ;
		modeHierarchy.add( TransportMode.pt ) ;
		modeHierarchy.add( TransportMode.car ) ;
		
		shares.put( TransportMode.walk, 0.2 ) ;
		shares.put( TransportMode.bike, 0.1 ) ;
		shares.put( TransportMode.pt, 0.4 ) ;
		shares.put( TransportMode.car, 0.3 ) ;
	}

	@Override public double value(SimulatorState state) {
		ModeChoiceState mcState = (ModeChoiceState) state ;
		
		long[] cnt = new long[ modeHierarchy.size() ] ; 
		double sum = 0 ;

		for ( Plan plan : mcState.getExperiencedPlans().values() ) {
			List<Trip> trips = TripStructureUtils.getTrips(plan, mcState.getStageActivities() ) ;
			for ( Trip trip : trips ) {
				String mainMode = mcState.getMainModeIdentifier().identifyMainMode(trip.getTripElements()) ;
				cnt[ modeHierarchy.indexOf( mainMode ) ]++ ;
				sum++ ;
			}
		}
		
		double obj = 0 ; 
		for ( String mode : modeHierarchy ) {
			obj += ( cnt[ modeHierarchy.indexOf(mode) ]/sum - shares.get(mode) ) ; 
		}
		return obj ;
		
	}

}

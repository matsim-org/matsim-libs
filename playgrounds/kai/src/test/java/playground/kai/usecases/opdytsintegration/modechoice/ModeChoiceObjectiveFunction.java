package playground.kai.usecases.opdytsintegration.modechoice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
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
	
	private final ExperiencedPlansService experiencedPlansService;
	private final StageActivityTypes stageActivities;
	private final List<String> modeHierarchy = new ArrayList<>() ;
	private final Map< String, Double > shares = new HashMap<>() ;

	ModeChoiceObjectiveFunction(ExperiencedPlansService experiencedPlansService, StageActivityTypes stageActivities ) {
		this.experiencedPlansService = experiencedPlansService;
		this.stageActivities = stageActivities;
		
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
		long[] cnt = new long[ modeHierarchy.size() ] ; 
		double sum = 0 ;

		for ( Plan plan : experiencedPlansService.getExperiencedPlans().values() ) {
			List<Trip> trips = TripStructureUtils.getTrips(plan, stageActivities) ;
			for ( Trip trip : trips ) {
				String mainMode = TransportMode.walk ;
				for ( Leg leg : trip.getLegsOnly() ) {
					if (  modeHierarchy.indexOf( leg.getMode() ) > modeHierarchy.indexOf( mainMode ) ) {
						mainMode = leg.getMode();
					}
				}
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

package playground.kai.usecases.opdytsintegration.modechoice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripRouter;
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
class ModeChoiceObjectiveFunction implements ObjectiveFunction {
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger( ModeChoiceObjectiveFunction.class );
	
	private static ExperiencedPlansService prevExperiencedPlansService = null ;

	private final Map< String, Double > shares = new HashMap<>() ;
	private MainModeIdentifier mainModeIdentifier = new MyMainModeIdentifier() ;

	@Inject ExperiencedPlansService service ;
	@Inject TripRouter tripRouter ;
	// Documentation: "Guice injects ... fields of all values that are bound using toInstance(). These are injected at injector-creation time."
	// https://github.com/google/guice/wiki/InjectionPoints
	// I read that as "the fields are injected (every time again) when the instance is injected".
	// This is the behavior that we want here.  kai, sep'16

	ModeChoiceObjectiveFunction() {
		shares.put( TransportMode.walk, 0.0 ) ;
		shares.put( TransportMode.bike, 0.0 ) ;
		shares.put( TransportMode.pt, 0.95 ) ;
		shares.put( TransportMode.car, 0.05 ) ;
	}

	@Override public double value(SimulatorState state) {
		log.warn( "prevExpPlnsService=" + prevExperiencedPlansService + "; currExpPlnsService=" + service ) ;
		prevExperiencedPlansService = service ;
		
		double sum = 0 ;
		Map<String,Double> cnt = new HashMap<>() ;

		for ( Plan plan : service.getExperiencedPlans().values() ) {
			List<Trip> trips = TripStructureUtils.getTrips(plan, tripRouter.getStageActivityTypes() ) ;
			for ( Trip trip : trips ) {
				String mode = mainModeIdentifier.identifyMainMode(trip.getLegsOnly());
				Double theCnt = cnt.get(mode) ;
				if ( theCnt==null ) {
					cnt.put( mode, 1. ) ;
				} else {
					cnt.put( mode, ++theCnt ) ;
				}
				sum++ ;
//				log.warn( "mode=" + mode + "; cnt=" + cnt.get(mode) + "; sum=" + sum ) ;
			}
		}

		double objective = 0 ; 
		for ( Entry<String, Double> entry : cnt.entrySet() ) {
			String mode = entry.getKey();
			final double diff = entry.getValue()/sum - shares.get(mode);
			objective += diff * diff ; 
//			log.warn( "mode=" + mode + "; cnt= " + entry.getValue() + "; diff= " + diff ) ;
		}
//		System.exit(-1);
		return objective ;

	}
	private static class MyMainModeIdentifier implements MainModeIdentifier {

		private final List<String> modeHierarchy = new ArrayList<>() ;

		MyMainModeIdentifier() {
			modeHierarchy.add( TransportMode.walk ) ;
			modeHierarchy.add( TransportMode.bike ) ;
			modeHierarchy.add( TransportMode.pt ) ;
			modeHierarchy.add( TransportMode.car ) ;
		}

		@Override public String identifyMainMode( List<? extends PlanElement> planElements ) {
			int mainModeIndex = -1 ;
			for ( PlanElement pe : planElements ) {
				if ( pe instanceof Leg ) {
					Leg leg = (Leg) pe ;
					int index = modeHierarchy.indexOf( leg.getMode() ) ;
					Gbl.assertIf( index >= 0 );
					if ( index > mainModeIndex ) {
						mainModeIndex = index ;
					}
				}
			}
			return modeHierarchy.get( mainModeIndex ) ;
		}
	}

}

package org.matsim.analysis;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.MainModeIdentifier;

/**
 * Attempt to provide a {@link MainModeIdentifier} that is justified from the transport engineering perspective, not from the
 * software perspective.
 * 
 * @author nagel
 *
 */
public final class TransportPlanningMainModeIdentifier implements MainModeIdentifier {
	// I am against other people changing this lightheartedly since some of my analysis depends on it.  So either please discuss,
	// or use your own variant.  kai, sep'16

	private final List<String> modeHierarchy = new ArrayList<>() ;

	public TransportPlanningMainModeIdentifier() {
		modeHierarchy.add( TransportMode.access_walk ) ;
		modeHierarchy.add( TransportMode.egress_walk ) ;
		modeHierarchy.add( TransportMode.transit_walk ) ;
		modeHierarchy.add( "undefined" ) ;
		modeHierarchy.add( TransportMode.other ) ;
		modeHierarchy.add( TransportMode.transit_walk ) ;
		modeHierarchy.add( TransportMode.walk ) ;
		modeHierarchy.add( TransportMode.bike ) ;
		modeHierarchy.add( TransportMode.pt ) ;
		modeHierarchy.add( TransportMode.ride ) ;
		modeHierarchy.add( TransportMode.car ) ;
		
		// NOTE: This hierarchical stuff is not so great: is park-n-ride a car trip or a pt trip?  Could weigh it by distance, or by time spent
		// in respective mode.  Or have combined modes as separate modes.  In any case, can't do it at the leg level, since it does not
		// make sense to have the system calibrate towards something where we have counted the car and the pt part of a multimodal
		// trip as two separate trips. kai, sep'16
	}

	@Override public String identifyMainMode( List<? extends PlanElement> planElements ) {
		int mainModeIndex = -1 ;
		for ( PlanElement pe : planElements ) {
			if ( pe instanceof Leg ) {
				Leg leg = (Leg) pe ;
				int index = modeHierarchy.indexOf( leg.getMode() ) ;
				if ( index < 0 ) {
					throw new RuntimeException("unknown mode=" + leg.getMode() ) ;
				}
				if ( index > mainModeIndex ) {
					mainModeIndex = index ;
				}
			}
		}
		if ( mainModeIndex <= modeHierarchy.indexOf( TransportMode.transit_walk ) ) {
			mainModeIndex = modeHierarchy.indexOf( TransportMode.walk ) ;
		}
		return modeHierarchy.get( mainModeIndex ) ;
	}
}
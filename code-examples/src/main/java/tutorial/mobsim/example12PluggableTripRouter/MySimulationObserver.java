package tutorial.mobsim.example12PluggableTripRouter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.vehicles.Vehicle;

public class MySimulationObserver implements BasicEventHandler {

	// Actually, no need to be concurrent here.
	// Event handlers are always considered to be "one event at a time" actors.
	// If you ever get Events concurrently, something is wrong.
	private Map<Id<Vehicle>, Double> enterEvents = new ConcurrentHashMap<>() ;

	private List<Double> sum = new ArrayList<Double>() ;
	private List<Double> cnt = new ArrayList<Double>() ;

	private int lastBin;
	
	MySimulationObserver() {
		lastBin = 36*4 ;
		for ( int ii=0 ; ii<=lastBin; ii++ ) {
			sum.add(0.) ;
			cnt.add(0.) ;
		}
	}
	
	@Override
	public void reset(int iteration) {
		
	}

	@Override
	public void handleEvent(Event event) {
		if ( event instanceof LinkEnterEvent ) {
			LinkEnterEvent ev = (LinkEnterEvent) event ;
			enterEvents.put( ev.getVehicleId() , ev.getTime() ) ;
		} else if (event instanceof LinkLeaveEvent ) {
			LinkLeaveEvent ev = (LinkLeaveEvent) event ;
			Double linkEnterTime = enterEvents.get( ev.getVehicleId() ) ; 
			if ( linkEnterTime != null ) {

				double ttime = ev.getTime() - linkEnterTime ;

				int bin = time2bin( linkEnterTime ) ;
				
				if ( bin > lastBin ) {
					for (int ii = lastBin+1 ; ii<=bin; ii++ ) {
						sum.add(0. ) ;
						cnt.add(0. ) ;
					}
					lastBin = bin ;
				}
				
				double oldTtime = sum.get( bin ) ;
				sum.set(bin, oldTtime+ttime ) ;

				double oldCnt = cnt.get( bin ) ;
				cnt.set( bin, oldCnt+1 ) ;

				enterEvents.remove( ev.getVehicleId() ) ;
			}
		} else if ( event instanceof VehicleArrivesAtFacilityEvent ) { // is this also thrown when entering parking???  kai, may'13
			VehicleArrivesAtFacilityEvent ev = (VehicleArrivesAtFacilityEvent) event ;
			Double linkEnterTime = enterEvents.get( ev.getVehicleId() ) ; 
			if ( linkEnterTime != null ) {
				enterEvents.remove( ev.getVehicleId() ) ; // i.e. do not use
			}
		} else if ( event instanceof VehicleDepartsAtFacilityEvent ) {
			// ...
		}
		
	}

	private int time2bin(Double linkEnterTime) {
		return (int) (linkEnterTime/900.) ;
	}

	public Object getIterationData() {
		// TODO: Return whatever this observer has observed in the previous iteration.
		return null;
	}


}

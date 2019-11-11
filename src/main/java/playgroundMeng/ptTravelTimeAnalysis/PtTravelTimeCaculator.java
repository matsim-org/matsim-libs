package playgroundMeng.ptTravelTimeAnalysis;

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Inject;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import playgroundMeng.ptAccessabilityAnalysis.activitiesAnalysis.Trip;

public class PtTravelTimeCaculator {
	@Inject
	Network network;
	@Inject
	SwissRailRaptor swissRailRaptor;
	
	Trip trip;
	
	public PtTravelTimeCaculator(Trip trip, Network network, SwissRailRaptor swissRailRaptor) {
		this.trip = trip;
		this.network = network;
		this.swissRailRaptor = swissRailRaptor;
	}
	
	public void caculate() {
		 Coord fromCoord = trip.getActivityEndImp().getCoord();
	     Coord toCoord = trip.getActivityStartImp().getCoord();
	     List<Leg> legs = swissRailRaptor.calcRoute(new FakeFacility(fromCoord), new FakeFacility(toCoord), trip.getActivityEndImp().getStartTime(), null);
	     this.trip.setPtTraveInfo(new PtTraveInfo(legs));
	}

}

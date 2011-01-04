package playground.mmoyo.zz_archive.precalculation;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**reports the departures of transitStopFacilities without logic layer*/
public class PlainTimeTable {
	TransitSchedule transitSchedule;
	Map <Id, double[]> transitRouteDepartureMap = new TreeMap <Id, double[]>();
	
	public PlainTimeTable (final TransitSchedule transitSchedule){
		this.transitSchedule = transitSchedule;
		setDepartures();
	}
	
	/**creates array of departures for every transitRoute*/
	private void setDepartures(){
		for (TransitLine transitLine : 	transitSchedule.getTransitLines().values()){
			for (TransitRoute transitRoute: transitLine.getRoutes().values()){
				double[] departureArray = new double[transitRoute.getDepartures().size()];
				int i=0;
				for (Departure departure: transitRoute.getDepartures().values()){
					departureArray[i++]= departure.getDepartureTime();
				}
				Arrays.sort(departureArray);
				transitRouteDepartureMap.put(transitRoute.getId(), departureArray);
			}
		}
	}

	/**returns next departure at a stop facility after a given time*/
	public double getNextDeparture(final TransitRoute transitRoute, final TransitStopFacility transitStopFacility, double time){
		double[] departureArray = transitRouteDepartureMap.get(transitRoute.getId());
		TransitRouteStop transitRouteStop =	transitRoute.getStop(transitStopFacility);
		double ArrivalOffset = transitRouteStop.getArrivalOffset();
		
		time = time - ArrivalOffset;  

		int length = departureArray.length;
		int index =  Arrays.binarySearch(departureArray, time);
		if (index<0){
			index = -index;
			if (index <= length)index--; else index=0;	
		}else{
			if (index < (length-1))index++; else index=0;	
		}
		return departureArray[index]+ ArrivalOffset;
	}

}

package playground.sergioo.eventAnalysisTools2013.excessWaitingTime;

/*import java.io.FileNotFoundException;
import java.io.PrintWriter;*/
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

public class ExcessWaitingTimeCalculator implements Serializable, PersonDepartureEventHandler, PersonEntersVehicleEventHandler, TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8622249257876448432L;
	
	public enum Mode {
		TIME_WEIGHT,
		NUM_PEOPLE_WEIGHT,
		FULL_SAMPLE;
	}
	private final Map<Id<Person>, Double> agentsWaitingData = new HashMap<Id<Person>, Double>();
	private final Map<Tuple<Id<TransitStopFacility>, Tuple<Id<TransitLine>, Id<TransitRoute>>>, List<Double>> headways = new HashMap<Tuple<Id<TransitStopFacility>, Tuple<Id<TransitLine>, Id<TransitRoute>>>, List<Double>>();
	private final Map<Tuple<Id<TransitStopFacility>, Tuple<Id<TransitLine>, Id<TransitRoute>>>, List<Collection<Double>>> waitTimes = new HashMap<Tuple<Id<TransitStopFacility>, Tuple<Id<TransitLine>, Id<TransitRoute>>>, List<Collection<Double>>>();
	private final Map<Id<Vehicle>, Tuple<Id<TransitLine>, Id<TransitRoute>>> linesRoutesNumVehicle = new HashMap<Id<Vehicle>, Tuple<Id<TransitLine>, Id<TransitRoute>>>();
	private Map<Tuple<Id<TransitStopFacility>, Tuple<Id<TransitLine>, Id<TransitRoute>>>, Double> timeOfStopLineRoute = new HashMap<Tuple<Id<TransitStopFacility>, Tuple<Id<TransitLine>, Id<TransitRoute>>>, Double>();
	private Map<Id<Vehicle>, Id<TransitStopFacility>> stopOfVehicle = new HashMap<Id<Vehicle>, Id<TransitStopFacility>>();

	@Override
	public void reset(int iteration) {
		agentsWaitingData.clear();
		headways.clear();
		waitTimes.clear();
		linesRoutesNumVehicle.clear();
		timeOfStopLineRoute.clear();
		stopOfVehicle.clear();
	}
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if(event.getLegMode().equals("pt") && agentsWaitingData.get(event.getPersonId())==null)
			agentsWaitingData.put(event.getPersonId(), event.getTime());
		else if(agentsWaitingData.get(event.getPersonId())!=null)
			new RuntimeException("Departing with old data");
	}
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(event.getTime()<26*3600) {
			Double startWaitingTime = agentsWaitingData.get(event.getPersonId());
			if(startWaitingTime!=null) {
				double waitTime = event.getTime()-startWaitingTime;
				Tuple<Id<TransitStopFacility>, Tuple<Id<TransitLine>, Id<TransitRoute>>> keyStopLineRoute = new Tuple<Id<TransitStopFacility>, Tuple<Id<TransitLine>, Id<TransitRoute>>>(stopOfVehicle.get(event.getVehicleId()), linesRoutesNumVehicle.get(event.getVehicleId()));
				List<Collection<Double>> waitTimesCollections = waitTimes.get(keyStopLineRoute);
				waitTimesCollections.get(waitTimesCollections.size()-1).add(waitTime);
				agentsWaitingData.remove(event.getPersonId());
			}
		}
	}
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		Tuple<Id<TransitLine>, Id<TransitRoute>> lineRoute = new Tuple<Id<TransitLine>, Id<TransitRoute>>(event.getTransitLineId(), event.getTransitRouteId());
		linesRoutesNumVehicle.put(event.getVehicleId(), lineRoute);
	}
	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		if(event.getTime()<26*3600) {
			Tuple<Id<TransitLine>, Id<TransitRoute>> linesRoute = linesRoutesNumVehicle.get(event.getVehicleId());
			if(linesRoute!=null) {
				stopOfVehicle.put(event.getVehicleId(), event.getFacilityId());
				Tuple<Id<TransitStopFacility>, Tuple<Id<TransitLine>, Id<TransitRoute>>> keyStopLineRoute = new Tuple<Id<TransitStopFacility>, Tuple<Id<TransitLine>, Id<TransitRoute>>>(event.getFacilityId(), linesRoute);
				Double previousTime = timeOfStopLineRoute.get(keyStopLineRoute);
				if(previousTime!=null) {
					List<Double> headwaysList = headways.get(keyStopLineRoute);
					if(headwaysList==null) {
						headwaysList = new ArrayList<Double>();
						headways.put(keyStopLineRoute, headwaysList);
					}
					headwaysList.add(event.getTime()-previousTime);
				}
				timeOfStopLineRoute.put(keyStopLineRoute, event.getTime());
				List<Collection<Double>> waitTimesCollections = waitTimes.get(keyStopLineRoute);
				if(waitTimesCollections==null) {
					waitTimesCollections = new ArrayList<Collection<Double>>();
					waitTimes.put(keyStopLineRoute, waitTimesCollections);
				}
				waitTimesCollections.add(new ArrayList<Double>());
			}
		}
	}
	public double getAverageWaitTime(Id<TransitLine> lineId, Id<TransitRoute> routeId, Id<TransitStopFacility> stopId, Mode mode) {
		double average=0, sum=0;
		Tuple<Id<TransitStopFacility>, Tuple<Id<TransitLine>, Id<TransitRoute>>> key = new Tuple<Id<TransitStopFacility>, Tuple<Id<TransitLine>, Id<TransitRoute>>>(stopId, new Tuple<Id<TransitLine>, Id<TransitRoute>>(lineId, routeId));
		switch(mode) {
		case TIME_WEIGHT:
			for(double headway:headways.get(key)) {
				average += headway*headway/2;
				sum += headway;
			}
			return average/sum;
		case NUM_PEOPLE_WEIGHT:
			List<Collection<Double>> waitTimeCollections = waitTimes.get(key);
			List<Double> headwaysLineRouteStop = headways.get(key);
			for(int i=0; i<headwaysLineRouteStop.size(); i++) {
				average += waitTimeCollections.get(i+1).size()*headwaysLineRouteStop.get(i)/2;
				sum += waitTimeCollections.get(i+1).size();
			}
			return average/sum;
		case FULL_SAMPLE:
			for(Collection<Double> waitTimeCollection:waitTimes.get(key))
				for(Double waitTime:waitTimeCollection) {
					average += waitTime;
					sum++;
				}
			return average/sum;
		}
		return 0;
	}
	public double getExcessWaitTime(Id<TransitLine> lineId, TransitRoute route, Id<TransitStopFacility> stopId, Mode mode) {
		double average=0, sum=0;
		Tuple<Id<TransitStopFacility>, Tuple<Id<TransitLine>, Id<TransitRoute>>> key = new Tuple<Id<TransitStopFacility>, Tuple<Id<TransitLine>, Id<TransitRoute>>>(stopId, new Tuple<Id<TransitLine>, Id<TransitRoute>>(lineId, route.getId()));
		TransitRouteStop stop = null;
		STOPS:
		for(TransitRouteStop s:route.getStops())
			if(s.getStopFacility().getId().equals(stopId)) {
				stop = s;
				break STOPS;
			}
		if(stop==null)
			throw new RuntimeException("Stop doesn't belong to the given line-route");
		SortedMap<Double, Departure> sortedDepartures = new TreeMap<Double, Departure>();
		for(Departure departure:route.getDepartures().values())
			if(departure.getDepartureTime()<26*3600)
				sortedDepartures.put(departure.getDepartureTime(), departure);
		Iterator<Departure> it;
		double pDeparture, cDeparture;
		switch(mode) {
		case TIME_WEIGHT:
			it = sortedDepartures.values().iterator();
			pDeparture = it.next().getDepartureTime();
			for(double headway:headways.get(key)) {
				cDeparture = it.next().getDepartureTime();
				average += headway*(headway/2-(cDeparture-pDeparture)/2);
				pDeparture = cDeparture;
				sum += headway;
			}
			/*if(Integer.parseInt(stopId.toString())==1229) {
				try {
					average=0; sum=0;
					PrintWriter writer = new PrintWriter("./data/testWhy1.csv");
					it = sortedDepartures.values().iterator();
					pDeparture = it.next().getDepartureTime();
					for(double headway:headways.get(key)) {
						cDeparture = it.next().getDepartureTime();
						writer.println(pDeparture+","+cDeparture+","+headway+","+(cDeparture-pDeparture)+","+(headway*(headway/2-(cDeparture-pDeparture)/2))+","+average+","+sum);
						average += headway*(headway/2-(cDeparture-pDeparture)/2);
						pDeparture = cDeparture;
						sum += headway;
					}
					writer.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if(Integer.parseInt(stopId.toString())==1329) {
				try {
					PrintWriter writer = new PrintWriter("./data/testWhy2.csv");
					it = sortedDepartures.values().iterator();
					pDeparture = it.next().getDepartureTime();
					for(double headway:headways.get(key)) {
						cDeparture = it.next().getDepartureTime();
						writer.println(pDeparture+","+cDeparture+","+headway+","+(cDeparture-pDeparture)+","+(headway*(headway/2-(cDeparture-pDeparture)/2))+","+average+","+sum);
						average += headway*(headway/2-(cDeparture-pDeparture)/2);
						pDeparture = cDeparture;
						sum += headway;
					}
					writer.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}*/
			return average/sum;
		case NUM_PEOPLE_WEIGHT:
			List<Collection<Double>> waitTimeCollections = waitTimes.get(key);
			List<Double> headwaysLineRouteStop = headways.get(key);
			it = sortedDepartures.values().iterator();
			pDeparture = it.next().getDepartureTime();
			for(int i=0; i<headwaysLineRouteStop.size(); i++) {
				cDeparture = it.next().getDepartureTime();
				average += waitTimeCollections.get(i+1).size()*(headwaysLineRouteStop.get(i)/2-(cDeparture-pDeparture)/2);
				pDeparture = cDeparture;
				sum += waitTimeCollections.get(i+1).size();
			}
			return average==0 && sum==0?0:average/sum;
		case FULL_SAMPLE:
			it = sortedDepartures.values().iterator();
			pDeparture = it.next().getDepartureTime();
			boolean first = true;
			for(Collection<Double> waitTimeCollection:waitTimes.get(key)) {
				if(!first) {
					cDeparture = it.next().getDepartureTime();
					for(Double waitTime:waitTimeCollection) {
						average += waitTime-(cDeparture-pDeparture)/2;
						sum++;
					}
					pDeparture = cDeparture;
				}
				else
					first = false;
			}
			return average==0 && sum==0?0:average/sum;
		}
		return 0;
	}
	
}

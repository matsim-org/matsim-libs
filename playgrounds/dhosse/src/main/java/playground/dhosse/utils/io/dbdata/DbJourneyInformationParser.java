package playground.dhosse.utils.io.dbdata;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.xml.sax.Attributes;

import playground.dhosse.scenarios.generic.utils.Modes;

public class DbJourneyInformationParser extends MatsimXmlParser {

	private static final String TAG_NAMES = "Names";
	private static final String TAG_NAME = "Name";
	private static final String TAG_NOTES = "Notes";
	private static final String TAG_NOTE = "Note";
	private static final String TAG_OPERATORS = "Operators";
	private static final String TAG_OPERATOR = "Operator";
	private static final String TAG_STOPS = "Stops";
	private static final String TAG_STOP = "Stop";
	private static final String TAG_TYPES = "Types";
	private static final String TAG_TYPE = "Type";
	
	private static final String ATT_ARRIVAL_DATE = "arrDate";
	private static final String ATT_ARRIVAL_TIME = "arrTime";
	private static final String ATT_DEPARTURE_DATE = "depDate";
	private static final String ATT_DEPARTURE_TIME = "depTime";
	private static final String ATT_ID = "id";
	private static final String ATT_KEY = "key";
	private static final String ATT_LAT = "lat";
	private static final String ATT_LON = "lon";
	private static final String ATT_NAME = "name";
	private static final String ATT_PRIORITY = "priority";
	private static final String ATT_ROUTE_INDEX = "routeIdx";
	private static final String ATT_ROUTE_IDX_FROM = "routeIdxFrom";
	private static final String ATT_ROUTE_IDX_TO = "routeIdxTo";
	private static final String ATT_TRACK = "track";
	
	private TransitSchedule schedule;
	private TransitRoute currentRoute;
	
	private CoordinateTransformation transform;
	
	private LinkedList<StopEntry> stops;
	
	public DbJourneyInformationParser(final TransitSchedule schedule, String toCRS){
		
		this.schedule = schedule;
		this.transform = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
				toCRS);
		this.setValidating(false);
		
	}
	
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		
		if(TAG_STOPS.equals(name)){
			
			this.stops = new LinkedList<>();
			
		} else if(TAG_STOP.equals(name)){
			
			startStop(atts, context);
			
		} else if("Note".equals(name)){
			for(int i = 0; i < atts.getLength(); i++){
				System.out.println(atts.getValue(i));
			}
		}
		else {
			
		}
		
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		
		if(TAG_STOPS.equals(name)){
			
			finalizeStops();
			
		}
		
	}
	
	private void startStop(Attributes atts, Stack<String> context){
		
		String name = atts.getValue(ATT_NAME);
		String id = atts.getValue(ATT_ID);
		String lon = atts.getValue(ATT_LON);
		String lat = atts.getValue(ATT_LAT);
		String routeIdx = atts.getValue(ATT_ROUTE_INDEX);
		String arrivalTime = atts.getValue(ATT_ARRIVAL_TIME);
		String arrivalDate = atts.getValue(ATT_ARRIVAL_DATE);
		String departureTime = atts.getValue(ATT_DEPARTURE_TIME);
		String departureDate = atts.getValue(ATT_DEPARTURE_DATE);
		String track = atts.getValue(ATT_TRACK);
		
		Id<TransitStopFacility> facilityId = Id.create(id, TransitStopFacility.class);
		
		if(!this.schedule.getFacilities().containsKey(facilityId)){
			
			Coord coord = this.transform.transform(new Coord(Double.parseDouble(lon), Double.parseDouble(lat)));
			
			TransitStopFacility stop = this.schedule.getFactory()
					.createTransitStopFacility(facilityId, coord, false);
			stop.setName(name);
			this.schedule.addStopFacility(stop);
			
		}
		
		double dep = Time.parseTime(departureTime);
		double arr = dep;
		
		if(arrivalTime != null){
			
			arr = Time.parseTime(arrivalTime);
			
		}
		if(departureTime == null){
			
			dep = arr;
			
		}
		
		this.stops.addLast(new StopEntry(id, arr, dep));
		
	}
	
	private void finalizeStops(){
		
		int idx = 0;
		double firstArrival = 0;
		double firstDeparture = 0;
		
		List<TransitRouteStop> stopsList = new ArrayList<>();
		
		for(StopEntry entry : this.stops){
			
			if(idx == 0){
				
				idx++;
				firstArrival = entry.arrivalTime;
				firstDeparture = entry.departureTime;
				
			}
			
			Id<TransitStopFacility> facilityId = Id.create(entry.id, TransitStopFacility.class);
			
			TransitRouteStop stop = this.schedule.getFactory().createTransitRouteStop(
					this.schedule.getFacilities().get(facilityId), entry.arrivalTime - firstArrival,
					entry.departureTime - firstDeparture);
			stop.setAwaitDepartureTime(true);
			
			stopsList.add(stop);
			
		}
		
		this.currentRoute = this.schedule.getFactory().createTransitRoute(Id.create("0", TransitRoute.class),
				null, stopsList, Modes.TRAIN);
		this.currentRoute.addDeparture(this.schedule.getFactory().createDeparture(Id.create("0", Departure.class), firstDeparture));
		TransitLine line = this.schedule.getFactory().createTransitLine(Id.create("0", TransitLine.class));
		line.addRoute(currentRoute);
		this.schedule.addTransitLine(line);
		
	}
	
	class StopEntry{
		
		String id;
		double arrivalTime;
		double departureTime;
		
		StopEntry(String id, double arrivalTime, double departureTime){
			this.id = id;
			this.arrivalTime = arrivalTime;
			this.departureTime = departureTime;
		}
		
	}

}

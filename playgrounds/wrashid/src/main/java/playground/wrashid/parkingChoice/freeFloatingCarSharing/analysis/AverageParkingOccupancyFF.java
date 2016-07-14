package playground.wrashid.parkingChoice.freeFloatingCarSharing.analysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.PC2.simulation.ParkingArrivalEvent;
import org.matsim.contrib.parking.PC2.simulation.ParkingDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.wrashid.parkingChoice.infrastructure.api.PParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ParkingLoader;

public class AverageParkingOccupancyFF implements BasicEventHandler {
	private HashMap<Id<PParking>, PParking> parking;

	Map<String, Data> mapa = new HashMap<String, Data>();
	public Map<String, DataPerson> mapa_person_rentals = new HashMap<String, DataPerson>();

	private int count = 0;
	Set<Data> rentals = new TreeSet<Data>(); 
	private Scenario scenario;
	public AverageParkingOccupancyFF(Scenario scenario) {
		this.scenario = scenario;
		LinkedList<PParking> pp = getParking();
		parking = new HashMap<Id<PParking>, PParking>();
		
		for (PParking p : pp) {
			
			parking.put(p.getId(), p);
		}
		
	}
	public static String getGroup(Id parkingId){
		if (parkingId.toString().contains("stp")) {
			return "streetParking";
		} else if (parkingId.toString().contains("gp")) {
			return "garageParking";
		} else if (parkingId.toString().contains("publicPOutsideCity")) {
			return "publicPOutsideCity";
		} else {
			return "privateParking";
		}
	}
	
	private static LinkedList<PParking> getParking() {
		ParkingLoader.garageParkingCalibrationFactor=0.13;
		ParkingLoader.parkingsOutsideZHCityScaling =0.1;
		ParkingLoader.populationScalingFactor =0.1;
		ParkingLoader.privateParkingCalibrationFactorZHCity  =0.28;
		ParkingLoader.streetParkingCalibrationFactor  =0.18;
		
		LinkedList<PParking> parkings = ParkingLoader.getParkingsForScenario("C:/Users/balacm/Desktop/ktiParking/parkingFlat/");
		return parkings;
	}
	public static void main(String[] args) throws IOException {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(args[1]);
		
		EventsManager events = EventsUtils.createEventsManager();
		AverageParkingOccupancyFF occ = new AverageParkingOccupancyFF(scenario);
		events.addHandler(occ); 

		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		occ.readRentals();

		reader.parse(args[0]);
		
		
        CoordinateReferenceSystem crs = MGC.getCRS("EPSG:21781");    // EPSG Code for Swiss CH1903_LV03 coordinate system


		 Collection<SimpleFeature> featuresMovedIncrease = new ArrayList<SimpleFeature>();
	        featuresMovedIncrease = new ArrayList<SimpleFeature>();
	        PointFeatureFactory nodeFactory = new PointFeatureFactory.Builder().
	                setCrs(crs).
	                setName("nodes").
	                addAttribute("ID", String.class).
	               addAttribute("Time", Double.class).
	               addAttribute("Vehicles", Integer.class).
	                //addAttribute("Be Af Mo", String.class).
	                
	                create();
		
		int i = 0;
		
		double bla = 0.0;
		int cars = 0;
		
		for (Data d : occ.mapa.values()) {
			double start = 0.0;
			double end = 0.0;
			for (double n : d.startTime) {
				start += n;
			}
			for (double n : d.endTime) {
				end += n;
			}
				
			if (d.startTime.size() < d.endTime.size())
				System.out.println();
			else {
				end += 86400.0* (d.startTime.size() - d.endTime.size());
			if (end < start)
				System.out.println();
			else {
			Coord coord = d.coord;
		      
		      SimpleFeature ft = nodeFactory.createPoint(coord, new Object[] {Integer.toString(i), (end - start)/(3600.0*(d.startTime.size())),
		    		  d.startTime.size()}, null);
		      cars += d.startTime.size();
		      bla += (end - start)/3600.0;
			//if (!scenario1.getActivityFacilities().getFacilities().containsKey(f1.getId()))
			featuresMovedIncrease.add(ft);
			i++;
			}
			}
			
		}
		System.out.println(bla);
		System.out.println(cars);

        ShapeFileWriter.writeGeometries(featuresMovedIncrease, "C:/Users/balacm/Desktop/SHP_files/average_time_medium_4x.shp");
		
		
		

	}

	@Override
	public void handleEvent(Event event) {
		if (event.getEventType().equalsIgnoreCase(ParkingArrivalEvent.EVENT_TYPE)
				) {
			String s = ParkingArrivalEvent.getParkingId(event.getAttributes()).toString();
			Id<Person> personId=ParkingArrivalEvent.getPersonId(event.getAttributes());
				if (personId.toString().equals("null")) {
					
					
					Coord coord = this.parking.get(ParkingArrivalEvent.getParkingId(event.getAttributes())).getCoord();
					
					if (!mapa.containsKey(s))
						mapa.put(s, new Data());
					mapa.get(s).startTime.add(0.0);
					mapa.get(s).parkingId = s;
					mapa.get(s).coord = coord;

				}
				
				else {
					Coord coord = this.parking.get(ParkingArrivalEvent.getParkingId(event.getAttributes())).getCoord();

					if (carsharingRentalEnd(personId.toString(), event.getTime())) {
						
						if (!mapa.containsKey(s))
							mapa.put(s, new Data());
						mapa.get(s).startTime.add(event.getTime());
						mapa.get(s).parkingId = s;
						mapa.get(s).coord = coord;

					}
					
					
					
				}
			}
		else if (event.getEventType().equalsIgnoreCase(ParkingDepartureEvent.EVENT_TYPE)
				) {
			Coord coord = this.parking.get(ParkingDepartureEvent.getParkingId(event.getAttributes())).getCoord();

			String s = ParkingDepartureEvent.getParkingId(event.getAttributes()).toString();
			Id<Person> personId=ParkingDepartureEvent.getPersonId(event.getAttributes());
			if (carsharingRentalStart(personId.toString(), event.getTime())) {
				
				if (!mapa.containsKey(s))
					mapa.put(s, new Data());
				mapa.get(s).endTime.add(event.getTime());
				mapa.get(s).parkingId = s;
				mapa.get(s).coord = coord;

			}
			
		}
		
	}
	
	private boolean carsharingRentalStart(String personId, double time) {

		if (!this.mapa_person_rentals.containsKey(personId))
			return false;
		else {
			
			for (double d : this.mapa_person_rentals.get(personId).startTime) {
				if (d == time ) {
					count++;
					return true;
					
				}
			}
			
		}		
		
		return false;
	}
	
	private boolean carsharingRentalEnd(String personId, double time) {

		if (!this.mapa_person_rentals.containsKey(personId))
			return false;
		else {
			
			for (double d : this.mapa_person_rentals.get(personId).endTime) {
				if (d == time || (d < time + 400 && d > time))
					return true;
			}
			
		}		
		
		return false;
	}
	public void readRentals() throws IOException {
		
		final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/1.300.FF_CS");

		readLink.readLine();
		String s = readLink.readLine();
		
		while (s!=null) {
			
			String[] arr = s.split("\\s");
			if (Double.parseDouble(arr[2]) < 86400 ) {
				if (!this.mapa_person_rentals.containsKey(arr[0])) {
					
					this.mapa_person_rentals.put(arr[0], new DataPerson());
					
				}
				
				this.mapa_person_rentals.get(arr[0]).startTime.add(Double.parseDouble(arr[1]) - Double.parseDouble(arr[6]));
				this.mapa_person_rentals.get(arr[0]).endTime.add(Double.parseDouble(arr[2]));				
			
			}
			s = readLink.readLine();
		}
		
			
		
		//create shapefile based on rentals
		
	}
	
	

	@Override
	public void reset(int iteration) {

		
	}

	private class Data{
		
		public String parkingId = "";
		public Coord coord;
		public LinkedList<Double> startTime = new LinkedList<Double>();
		public LinkedList<Double> endTime = new LinkedList<Double>();
	}
	private class DataPerson{
		
		public String personId = "";
		public LinkedList<Double> startTime = new LinkedList<Double>();
		public LinkedList<Double> endTime = new LinkedList<Double>();
	}

}

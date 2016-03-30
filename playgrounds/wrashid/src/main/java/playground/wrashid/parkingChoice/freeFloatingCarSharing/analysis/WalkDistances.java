package playground.wrashid.parkingChoice.freeFloatingCarSharing.analysis;


import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.PC2.infrastructure.PC2Parking;
import org.matsim.contrib.parking.PC2.simulation.ParkingArrivalEvent;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.wrashid.parkingChoice.infrastructure.api.PParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ParkingLoader;


public class WalkDistances implements BasicEventHandler {

	//Map<String, Integer> count = new HashMap<String, Integer>();
	private HashMap<Id<PParking>, PParking> parking;

	private HashMap<String, LinkedList<Double>> walkDistancesPrivate;
	private HashMap<String, LinkedList<Double>> walkDistancesCarsharing;

	private Map<String, LinkedList<String>> mapa;
	
	double walkCS = 0.0;
	int count = 0;
	int counx = 0;
	
	@Override
	public void reset(int iteration) {
	}

	public WalkDistances(Map<String, LinkedList<String>> mapa2) {
		
		LinkedList<PParking> pp = getParking();
		parking = new HashMap<Id<PParking>, PParking>();
		walkDistancesPrivate = new HashMap<String, LinkedList<Double>>();
		walkDistancesCarsharing = new HashMap<String, LinkedList<Double>>();

		this.mapa = mapa2;
		for (PParking p : pp) {
			
			parking.put(p.getId(), p);
		}
		
	}

	@Override
	public void handleEvent(Event event) {
		if (event.getEventType().equalsIgnoreCase(ParkingArrivalEvent.EVENT_TYPE)
				) {
			Id<Person> personId=ParkingArrivalEvent.getPersonId(event.getAttributes());
			if (!personId.toString().equals("null")) {
				Id<PC2Parking> parkingId = ParkingArrivalEvent.getParkingId(event.getAttributes());
				
				Coord destCoord = ParkingArrivalEvent.getDestCoord(event.getAttributes());
				
				double walkDistance = GeneralLib.getDistance(parking.get(parkingId).getCoord(),
						destCoord);
				if (this.mapa.containsKey(personId.toString())) {
					
					if (isFFParking(event.getTime(), personId.toString())) {
						
						if (!walkDistancesCarsharing.containsKey(getGroup(parkingId))){
							walkDistancesCarsharing.put(getGroup(parkingId), new LinkedList<Double>());
						}
						this.walkCS += walkDistance;
						count++;
						walkDistancesCarsharing.get(getGroup(parkingId)).add(walkDistance);	
					}
				}
						
						
				
				
				else {
				
					if (!walkDistancesPrivate.containsKey(getGroup(parkingId))){
						walkDistancesPrivate.put(getGroup(parkingId), new LinkedList<Double>());
					}
					
					walkDistancesPrivate.get(getGroup(parkingId)).add(walkDistance);
				}
				
			}
		}		
	}
	
	public boolean isFFParking(double t, String personId) {
		
		for (String s: this.mapa.get(personId)) {
			
			if (t <= Double.parseDouble(s)  && t > Double.parseDouble(s) - 420 )	
				return true;
			
		}
		
		return false;
	}
	
	
	public void outp() {
		int[] walkGroups = new int[150];

		for (String group : walkDistancesPrivate.keySet()) {
			
			if (group.equals("streetParking")) {
				for (Double d: walkDistancesPrivate.get(group)) {
					
					walkGroups[(int) (d/50)]++;
				}
			}
		}
		
		for(int i : walkGroups) {
			
			System.out.println(i);
		}
	}
	
	public void outcs() {
		int[] walkGroups = new int[150];

		for (String group : walkDistancesCarsharing.keySet()) {
			
			if (group.equals("streetParking")) {
				for (Double d: walkDistancesCarsharing.get(group)) {
					
					walkGroups[(int) (d/50)]++;
				}
			}
		}
		
		for(int i : walkGroups) {
			
			System.out.println(i);
		}
		
		System.out.println(count);
		System.out.println(1.3*this.walkCS/this.count);
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

		final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/1.300.FF_CS");
		
		readLink.readLine();
		String s = readLink.readLine();
		Map<String, LinkedList<String>> mapa = new HashMap<String, LinkedList<String>>();
		while(s != null) {
			String[] arr = s.split("\\s");
			
			if (mapa.containsKey(arr[0])) {
				
				mapa.get(arr[0]).add(arr[2]);
			}
			
			else {
				
				mapa.put(arr[0], new LinkedList<String>());
				mapa.get(arr[0]).add(arr[2]);
			}
						
			s = readLink.readLine();
		}
		
		
		
		
		EventsManager events = EventsUtils.createEventsManager();	
		
		WalkDistances occ = new WalkDistances(mapa);
		events.addHandler(occ); 

		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(args[0]);	
		occ.outp();
		System.out.println();
		occ.outcs();

	}

}


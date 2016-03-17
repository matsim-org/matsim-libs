package playground.dhosse.scenarios.generic.population.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class WriteEventsIntoDbScheme implements PersonDepartureEventHandler, PersonArrivalEventHandler,
	ActivityStartEventHandler, ActivityEndEventHandler, LinkEnterEventHandler {

	private static final Logger log = Logger.getLogger(WriteEventsIntoDbScheme.class);
	
	static Scenario scenario;
	
	static Map<String, List<TrackInfo>> personId2TrackInfos = new HashMap<String, List<TrackInfo>>();
	static Set<String> teleportedModes = CollectionUtils.stringToSet("bike,ride,walk");
	
	static final String SEPARATOR = ";";
	
	/**
	 * 
	 * @param args</br>
	 * 	0: runId
	 * 	1: plans file</br>
	 * 	2: network file</br>
	 * 	3: events file</br>
	 * 	4: output csv file
	 */
	public static void main(String[] args) {
		
		if(args.length == 0){
			
			throw new RuntimeException("No runtime arguments given! Aborting...");
			
		}
		
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(args[2]);
		
		new MatsimPopulationReader(scenario).readFile(args[1]);

		log.info("Storing persons for leg analysis...");
		
		for(Person person : scenario.getPopulation().getPersons().values()){
			
			personId2TrackInfos.put(person.getId().toString(), new ArrayList<TrackInfo>());
			
		}
		
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(new WriteEventsIntoDbScheme());
		
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(args[3]);
		
		writeOutput(args);
		
		log.info("Done.");

	}

	private static void writeOutput(String[] args) {
		
		log.info("Writing legs to " + args[4] + "...");
		
		BufferedWriter writer = IOUtils.getBufferedWriter(args[4]);
		
		try {
			
			writer.write("id" + SEPARATOR + "user_id" + SEPARATOR + "started_at" + SEPARATOR + "finished_at" +
						SEPARATOR + "created_with" + SEPARATOR + "activity_from" + SEPARATOR + "activity_to"
						+ SEPARATOR + "length" + SEPARATOR + "geometry" + SEPARATOR + "mode");
			
			GeometryFactory factory = new GeometryFactory();
			
			
			for(String personId : personId2TrackInfos.keySet()){
				
				for(TrackInfo info : personId2TrackInfos.get(personId)){
					
					Geometry geometry = null;
					
					if(info.coords.size() > 1){
						
						Coordinate[] coordinates = new Coordinate[info.coords.size()];
						
						for(int i = 0; i < info.coords.size(); i++){
							
							coordinates[i] = MGC.coord2Coordinate(info.coords.get(i));
							
						}
						
						geometry = factory.createLineString(coordinates);
						
						String geoString = geometry != null ? geometry.toString() : "";
						
						writer.newLine();
						writer.write(personId + "_" + info.id + SEPARATOR + personId + SEPARATOR +
								Time.writeTime(info.startedAt) + SEPARATOR + Time.writeTime(info.finishedAt) +
								SEPARATOR + info.createdWith + SEPARATOR +  info.fromAct + SEPARATOR + info.toAct +
								SEPARATOR + info.length + SEPARATOR + geoString + SEPARATOR + info.mode);
						
					}
					
				}
				
			}
			
			writer.flush();
			writer.close();
			
		} catch (IOException e) {

			e.printStackTrace();
			
		}
		
	}

	@Override
	public void reset(int iteration) {
		
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		
		String personId = event.getPersonId().toString();
		TrackInfo info = personId2TrackInfos.get(personId).get(personId2TrackInfos.get(personId).size() - 1);
		info.mode = event.getLegMode();
		info.startedAt = event.getTime();
		
		info.coords.add(scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord());
		
	}
	
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		
		String personId = event.getPersonId().toString();
		TrackInfo info = personId2TrackInfos.get(personId).get(personId2TrackInfos.get(personId).size() - 1);
		info.finishedAt = event.getTime();
		info.coords.add(scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord());
	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
		
		//create a new TrackInfo for the next leg
		String personId = event.getPersonId().toString();
		TrackInfo info = new TrackInfo();
		
		//set some attributes
		info.id = personId2TrackInfos.get(personId).size();
		info.fromAct = event.getActType();
		
		//add the new info to the list
		personId2TrackInfos.get(personId).add(info);
			
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		
		String personId = event.getPersonId().toString();
		TrackInfo info = personId2TrackInfos.get(personId).get(personId2TrackInfos.get(personId).size() - 1);
		info.toAct = event.getActType();
		
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
	
		String id = event.getVehicleId().toString();
		
		if(personId2TrackInfos.containsKey(id)){
			
			TrackInfo info = personId2TrackInfos.get(id).get(personId2TrackInfos.get(id).size() - 1);
			info.length += scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
			info.coords.add(scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord());
			
		}
		
	}
	
	class TrackInfo{
		
		int id;
		double startedAt;
		double finishedAt;
		String createdWith = "modeled";
		String fromAct;
		String toAct;
		int length;
		List<Coord> coords = new LinkedList<Coord>();
		String mode;
		
		TrackInfo(){};
		
	}

}

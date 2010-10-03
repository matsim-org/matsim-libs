package playground.mmoyo.Validators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.xml.sax.SAXException;

import playground.mmoyo.utils.DataLoader;

/**validates that pt veh in a events file followed their predefined transit route**/
public class TransitRoutePathValidator implements BasicEventHandler { 
	private static final Logger log = Logger.getLogger(TransitRoutePathValidator.class);
	private ScenarioImpl scenario;
	private Map<String, Tuple<Double, List<Id>>> tempLinkMap = new TreeMap <String, Tuple<Double, List<Id>>>();
	private Map<Id, Tuple<Double, List<Id>>> simPathMap = new TreeMap <Id, Tuple<Double, List<Id>>>();
	private Map<String, Boolean> boolMap = new TreeMap <String, Boolean>();
	private Map<Id, List<Double>> depsMap = new TreeMap <Id, List<Double>>();
	
	//String constants
	final String SP = " "; 
	final String TAB = "\t";
	final String COL = ":";
	final String N = "\n";
	final String PERSON = "person";
	final String LINK = "link";
	final String TIME = "time";
	final String LOWLINE = "_";
	final String PT = "pt";
	final String POINT ="\\.";
	final String NO_TRROUTE = "\nThis veh has not a valid Transit route!:"; 
	
	public TransitRoutePathValidator(ScenarioImpl scenario){
		this.scenario = scenario;
	}

	public void run(String eventFileName){
		this.readEvents(eventFileName);
		
		System.out.println("=====================================");
		System.out.println("Validate routes");
		for(Map.Entry <Id, Tuple<Double, List<Id>>> entry: simPathMap.entrySet() ){
			Id id = entry.getKey(); 
			double depTime= entry.getValue().getFirst();
			List<Id> list = entry.getValue().getSecond();
		
			//System.out.println("===new route: " + id.toString());
			for (Id id2: list){
				//System.out.print(id2.toString() + SP);
			}
			
			TransitRoute trRoute = getTransitRoute(id.toString(), list); 
			if (trRoute != null){
				System.out.println("depTime: " + depTime + " " + Time.writeTime(depTime) + " corresponding Transit route: " + trRoute.getId());
			}else{
				log.warn(NO_TRROUTE + id);
			}
			//System.out.println();
	
			
			//add departures to depsMap
			if (!depsMap.containsKey(trRoute.getId())){
				depsMap.put(trRoute.getId(), new ArrayList<Double>());	
			}
			depsMap.get(trRoute.getId()).add(depTime);
		}
		
		for(Map.Entry <Id, List<Double>> entry: depsMap.entrySet() ){
			Id id = entry.getKey(); 
			List<Double> value = entry.getValue();
		
			//print simulated departures
			System.out.print("\n\n" + id + " ");
			Collections.sort(value);
			for (Double dep : value){
				System.out.print(Time.writeTime(dep) + " ");
			}

			//print transitSchedule departures
			System.out.print("\n" + id + " ");
			TransitRoute trRoute = this.scenario.getTransitSchedule().getTransitLines().get(new IdImpl(id.toString().split(this.POINT)[0])).getRoutes().get(id);
			for (Departure departure : trRoute.getDepartures().values()){
				System.out.print(Time.writeTime(departure.getDepartureTime()) + " ");
			}
			
		}
		
		/*
		System.out.println("=====================================");
		System.out.println("departures");
		
			for (Departure departure : trRoute.getDepartures().values()){
				departure.getDepartureTime();
			}
		
		for(Map.Entry <Id, List<Double>> entry: depsMap.entrySet() ){
			Id key = entry.getKey(); 
			List<Double> simDepartures = entry.getValue();
			this.scenario.getTransitSchedule().
		}
		*/
		
		System.out.println("done.");
	}
	
	@Override
	public void reset(int iteration) {
	}

	private void readEvents(String eventFileName){
		EventsManagerImpl events = new EventsManagerImpl();
		events.addHandler(this);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		try {reader.parse(eventFileName);} 
		catch (SAXException e) {e.printStackTrace();}
		catch (ParserConfigurationException e) {e.printStackTrace();} 
		catch (IOException e) {e.printStackTrace();}
	}
	
	@Override
	public void handleEvent(Event event) {
		String personAtr = event.getAttributes().get(PERSON);
		double timeAtr = Double.parseDouble(event.getAttributes().get(TIME));
		
		//return if it is not an pt veh event
		String[] pers_elements = personAtr !=null? personAtr.split(LOWLINE): null; 
		boolean isPtVeh = pers_elements !=null && pers_elements.length>0 && pers_elements[0].equals(PT);
		if (!isPtVeh)return;
		
		//Departure
		if(event.getClass() == AgentDepartureEventImpl.class){
			//printAttributes (event);
			//initialize maps for personAtr 
			if (!this.tempLinkMap.containsKey(personAtr)){
				this.boolMap.put(personAtr, true);
				this.tempLinkMap.put(personAtr, null);
			}

			boolean accept =boolMap.get(personAtr);
			if (accept){
				Tuple<Double, List<Id>> tmpDepLinksTuple = new Tuple<Double, List<Id>>(event.getTime(), new ArrayList<Id>());
				this.tempLinkMap.put(personAtr, tmpDepLinksTuple );
				//System.out.println("====agentDeparts===========================================");	
			}else{
				//System.out.println("====ignore departure==========================================");
			}

		//Link Leave
		}else if (event.getClass() == LinkLeaveEventImpl.class){
			if ( boolMap.get(personAtr)){
				Id linkId = new IdImpl((event.getAttributes().get(LINK))); 
				this.tempLinkMap.get(personAtr).getSecond().add(linkId);
				//printAttributes (event);
			}

		//Arrival	
		}else if (event.getClass() == AgentArrivalEventImpl.class){
			boolean accept = boolMap.get(personAtr); 
			if (accept){
				Id simPathId= new IdImpl(personAtr + LOWLINE + Double.toString(timeAtr));
				
				//remove first link and save in definitive route links map
				Tuple<Double, List<Id>> tuple = this.tempLinkMap.get(personAtr);
				tuple.getSecond().remove(0);
				this.simPathMap.put(simPathId, tuple);
				
				//System.out.println(personAtr);
				//System.out.println("====AgentArrivalEventImpl===========================================");	
			}else{
				//System.out.println("====ignore arrival==========================================");
			}
			boolMap.put( personAtr, !accept);
		}
	}

	private void printAttributes (Event event){
		System.out.print(event.getClass().getSimpleName() + TAB );
		for(Map.Entry <String,String> entry: event.getAttributes().entrySet() ){
			System.out.print(entry.getKey() + COL + entry.getValue() + TAB);
		}
		System.out.println();
	}
		
	/**returns the transit route Id or null if not a transit route*/
	private TransitRoute getTransitRoute(String personAtr, List<Id> linkIdlist ){
		String[] pers_elements = personAtr.split(LOWLINE);
		TransitLine line = this.scenario.getTransitSchedule().getTransitLines().get(new IdImpl(pers_elements[4]));
		Iterator <TransitRoute> iter = line.getRoutes().values().iterator();
		Id transitRouteId= null;
		
		TransitRoute trRoute = null;
		while (transitRouteId== null && iter.hasNext()) {
			trRoute = iter.next();
			if ( trRoute.getRoute().getLinkIds().equals(linkIdlist)){
				transitRouteId = trRoute.getId();
			}
		}
		return trRoute;
	}
	
	private TransitRoute getTransitRoute(Id id){
		TransitRoute trRoute = this.scenario.getTransitSchedule().getTransitLines().get(new IdImpl(id.toString().split(this.POINT)[0])).getRoutes().get(id);
		return trRoute;
	}
	
	public static void main(String[] args) {
		String configFile = null;
		String eventFile = null;
		if (args.length==2){
			configFile = args[0];
		}else{
			configFile= "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
			
			eventFile = "../playgrounds/mmoyo/output/input/500.events.xml.gz";
			//eventFile="../playgrounds/mmoyo/output/Cadyts/output/ITERS/it.20/20.events.xml.gz";
			//eventFile = "../playgrounds/mmoyo/output/Cadyts/output/ITERS/it.10/10.events.xml.gz";
			//eventFile = "../playgrounds/mmoyo/output/input/10.events.xml.gz";
		}
		
		ScenarioImpl scenario = new DataLoader().loadScenarioWithTrSchedule(configFile);
		new TransitRoutePathValidator(scenario).run(eventFile);
	}

}
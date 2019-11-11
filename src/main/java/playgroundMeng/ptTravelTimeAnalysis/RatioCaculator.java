package playgroundMeng.ptTravelTimeAnalysis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import com.google.inject.Inject;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import playgroundMeng.ptAccessabilityAnalysis.activitiesAnalysis.ActivitiesEventHandler;
import playgroundMeng.ptAccessabilityAnalysis.activitiesAnalysis.Trip;


public class RatioCaculator {
	
	Network network;
	TravelTimeConfig travelTimeConfig;
	SwissRailRaptor swissRailRaptor;
	
	
	List<Trip> trips = new ArrayList<Trip>();
	List<Trip> tripsWithRatio = new ArrayList<Trip>();
	
	@Inject 
	public RatioCaculator(Network network, TravelTimeConfig timeConfig, SwissRailRaptor swissRailRaptor) {
		this.network = network;
		this.travelTimeConfig = timeConfig;
		this.swissRailRaptor = swissRailRaptor;
		this.readTrips();
		this.dealTripListWithMutiThread();
		this.trips.clear();
		this.trips = tripsWithRatio;	
		
	}
	
	public List<Trip> getTrips() {
		return trips;
	}
	
	public void readTrips(){
		EventsManager eventsManager = EventsUtils.createEventsManager();
		ActivitiesEventHandler activitiesEventHandler = new ActivitiesEventHandler(network);
		eventsManager.addHandler(activitiesEventHandler);
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile("W:/08_Temporaere_Mitarbeiter/082_K-GGSN/0822_Praktikanten/Meng/vw243_cadON_ptSpeedAdj.0.1/vw243_cadON_ptSpeedAdj.0.1.output_events.xml.gz");
		for(Id<Person> personId : activitiesEventHandler.getPersonId2Trips().keySet()) {
			this.trips.addAll(activitiesEventHandler.getPersonId2Trips().get(personId));
		}
	}
	public void dealTripListWithMutiThread(){
        int index = 0;
        ExecutorService ex = Executors.newFixedThreadPool(this.travelTimeConfig.getNumOfThread());
        int dealSize = (int)(this.trips.size()/this.travelTimeConfig.getNumOfThread());
        List<Future<List<Trip>>> futures = new ArrayList<>(this.travelTimeConfig.getNumOfThread());
        
        for(int i=0;i<this.travelTimeConfig.getNumOfThread();i++,index+=dealSize){
            int start = index;
            if(start>=this.trips.size()) break;
            int end = start + dealSize;
            if(i == this.travelTimeConfig.getNumOfThread() -1) {
            	end = this.trips.size();
            }
            futures.add(ex.submit(new Task(this.trips,start,end)));
        }
        try {
            List<Trip>  result = new ArrayList<>();
            for(Future<List<Trip>> future : futures){
                result.add((Trip) future.get());
            }
            this.tripsWithRatio = result;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	  private class Task implements Callable<List<Trip>>{
	        private List<Trip> list;
	        private int start;
	        private int end;
	        public Task(List<Trip> list,int start,int end){
	            this.list = list;
	            this.start = start;
	            this.end = end;
	        }
	        @Override
	        public List<Trip> call() throws Exception {
	            Trip trip = null;
	            List<Trip> retList = new ArrayList<Trip>();
	            for(int i=start;i<end;i++){
	                trip = list.get(i);
	                new CarTravelTimeCaculator(trip,network,travelTimeConfig).caculate();
	                new PtTravelTimeCaculator(trip, network, swissRailRaptor).caculate();
	                retList.add(trip);
	            }
	            return retList;
	        }
	    }
}

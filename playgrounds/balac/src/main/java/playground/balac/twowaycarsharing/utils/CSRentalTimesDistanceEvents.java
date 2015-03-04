package playground.balac.twowaycarsharing.utils;

import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;


public class CSRentalTimesDistanceEvents {

	EventsManager events = (EventsManager) EventsUtils.createEventsManager();
    EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
	
	
	public void run(String[] args){
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[0]);
		
		RentalTimes rentalTimes = new RentalTimes(scenario);		
		events.addHandler(rentalTimes);
    	reader.parse(args[1]);
		
    	System.out.println(rentalTimes.number() * 0.00029 + " ff");
		
    	System.out.println(rentalTimes.number1() * 0.00018 + " rb");
    	
    	System.out.println(rentalTimes.number()/ (double)rentalTimes.countff());
    	
    	System.out.println(rentalTimes.number1()/ (double)rentalTimes.countrb());
		
	}
	private static class RentalTimes implements LinkEnterEventHandler {

		int[] rentalTimes = new int[24];
	
		ScenarioImpl scenario;
		double distanceff = 0.0;
		double distancerb = 0.0;
		int countff = 0;
		int countrb = 0;
		RentalTimes(ScenarioImpl scenario) {
			this.scenario = scenario;
		}
		
		
		public double number() {
			return distanceff;
		}
		public double number1() {
			return distancerb;
		}


		public int countrb() {
			return countrb;
		}
		public int countff() {
			return countff;
		}
		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
		}


		@Override
		public void handleEvent(LinkEnterEvent event) {
			// TODO Auto-generated method stub
			if (event.getVehicleId().toString().contains("c")) {
				
				distancerb += scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
				countrb++;
			}
			else if (event.getVehicleId().toString().contains("W")) {
				
				distanceff += scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
				countff++;
			}
			
		}
		
		
		
		
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CSRentalTimesDistanceEvents f = new CSRentalTimesDistanceEvents();
		f.run(args);		

	}

}

package playground.balac.induceddemand.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.balac.induceddemand.analysis.PlansSimplified.Activities.ActivityInfo;


public class PlansSimplified {
	
	EventsManager events = (EventsManager) EventsUtils.createEventsManager();
    EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);


	public void run(String s) throws IOException{
		final BufferedWriter outLink = IOUtils.getBufferedWriter("C:\\Users\\balacm\\Desktop\\bj14.txt");

		Activities purpose = new Activities();
    	
    
    	events.addHandler(purpose);
    	reader.readFile(s);
    	Set<ActivityInfo> infos =purpose.getInfos();
    	
    	for (ActivityInfo info : infos) {
    		outLink.write(info.getPersonId() + ";" + info.getActivityType() + ";" + info.getStartTime() + ";" + 
    	info.getEndTime() + ";" + info.getMode());
    		outLink.newLine();
    		
    	}
    	outLink.flush();
    	outLink.close();
    }
	public static class Activities implements ActivityStartEventHandler, PersonArrivalEventHandler, ActivityEndEventHandler {

		Map<String,ActivityInfo> activityInformation = new HashMap<>();

		Set<ActivityInfo> infos = new HashSet<>();
		
		public Set<ActivityInfo> getInfos() {
			return infos;
		}

		Map<String,Double> homeEnd = new HashMap<>();		
		
		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void handleEvent(ActivityEndEvent event) {

			if (!event.getActType().equals("home_1") && !event.getActType().equals("home_2")) {
				
				this.activityInformation.get(event.getPersonId().toString()).setEndTime(event.getTime());
				if (!event.getActType().equals("pt interaction"))
					infos.add(this.activityInformation.get(event.getPersonId().toString()));
			}
			else {				
				homeEnd.put(event.getPersonId().toString(), event.getTime());				
			}			
		}

		@Override
		public void handleEvent(PersonArrivalEvent event) {

			ActivityInfo newInfo = new ActivityInfo();
			newInfo.setMode(event.getLegMode());
			newInfo.setPersonId(event.getPersonId().toString());
			this.activityInformation.put(event.getPersonId().toString(), newInfo);			
			
		}

		@Override
		public void handleEvent(ActivityStartEvent event) {
			if (!event.getActType().equals("home_1") && !event.getActType().equals("home_2")) {
				
				this.activityInformation.get(event.getPersonId().toString()).setActivityType(event.getActType());
				this.activityInformation.get(event.getPersonId().toString()).setStartTime(event.getTime());

			}
			else {
				this.activityInformation.get(event.getPersonId().toString()).setActivityType(event.getActType());
				this.activityInformation.get(event.getPersonId().toString()).setStartTime(event.getTime());
				this.activityInformation.get(event.getPersonId().toString()).setEndTime(homeEnd.get(event.getPersonId().toString()) + 24 * 3600.0);
				infos.add(this.activityInformation.get(event.getPersonId().toString()));

			}
						
		}
		
		public class ActivityInfo {
			private String personId;
			private String activityType;
			public String getActivityType() {
				return activityType;
			}
			public void setActivityType(String activityType) {
				this.activityType = activityType;
			}
			public double getStartTime() {
				return startTime;
			}
			public void setStartTime(double startTime) {
				this.startTime = startTime;
			}
			public double getEndTime() {
				return endTime;
			}
			public void setEndTime(double endTime) {
				this.endTime = endTime;
			}
			public String getMode() {
				return mode;
			}
			public void setMode(String mode) {
				this.mode = mode;
			}
			public String getPersonId() {
				return personId;
			}
			public void setPersonId(String personId) {
				this.personId = personId;
			}
			private double startTime;
			private double endTime;
			private String mode = "";
			
		}		
	}	
	
	public static void main(String[] args) throws IOException {

		PlansSimplified cp = new PlansSimplified();
		
		String eventsFilePath = args[0];		
		
		cp.run(eventsFilePath);
		
		
	}

}

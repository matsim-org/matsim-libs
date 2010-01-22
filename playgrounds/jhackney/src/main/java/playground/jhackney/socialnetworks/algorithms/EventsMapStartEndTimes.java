package playground.jhackney.socialnetworks.algorithms;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;

public class EventsMapStartEndTimes implements ActivityStartEventHandler, ActivityEndEventHandler {

	public LinkedHashMap<Id, ArrayList<ActivityStartEvent>> startMap = new LinkedHashMap<Id,ArrayList<ActivityStartEvent>>();
	public LinkedHashMap<Id, ArrayList<ActivityEndEvent>> endMap = new LinkedHashMap<Id,ArrayList<ActivityEndEvent>>();
	public double maxtime=0;
	static final private Logger log = Logger.getLogger(EventsMapStartEndTimes.class);

	public EventsMapStartEndTimes() {
		super();
//		makeTimeWindows();
		log.info(" Looking through plans and mapping social interactions for scoring");
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		ArrayList<ActivityStartEvent> startList;
		if((startMap.get(event.getPersonId())==null)){
			startList=new ArrayList<ActivityStartEvent>();
		}else{
			startList=startMap.get(event.getPersonId());
		}
		startList.add(event);
		startMap.remove(event.getPersonId());
		startMap.put(event.getPersonId(),startList);
		if(event.getTime()>=maxtime) maxtime=event.getTime();
	}

	public void reset(int iteration) {
		startMap.clear();
		endMap.clear();

	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		ArrayList<ActivityEndEvent> endList;
		if((endMap.get(event.getPersonId())== null)){
			endList=new ArrayList<ActivityEndEvent>();
		}else{
			endList=endMap.get(event.getPersonId());
		}
		endList.add(event);
		endMap.remove(event.getPersonId());
		endMap.put(event.getPersonId(),endList);
		if(event.getTime()>=maxtime) maxtime=event.getTime();
	}
	public double getMaxTime(){
		return maxtime;
	}
}

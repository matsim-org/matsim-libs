package playground.sergioo.eventAnalysisTools2012;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;

public class TypeActivityAnalyzer implements ActivityStartEventHandler, ActivityEndEventHandler {

	//Attributes
	private SortedMap<String, SortedMap<Double, Integer>> peoplePerforming = new TreeMap<String, SortedMap<Double,Integer>>();
	private SortedMap<String, SortedMap<Double, Integer>> durations = new TreeMap<String, SortedMap<Double,Integer>>();
	private Map<Id<Person>, Tuple<String, Double>> startTimes = new HashMap<Id<Person>, Tuple<String, Double>>();
	private double timeBin;
	private double totalTime;
	
	//Methods
	public TypeActivityAnalyzer(double timeBin, double totalTime) {
		this.timeBin = timeBin;
		this.totalTime = totalTime;
	}
	public Map<String, SortedMap<Double, Integer>> getPeoplePerforming() {
		return peoplePerforming;
	}
	public Map<String, SortedMap<Double, Integer>> getDurations() {
		return durations;
	}
	@Override
	public void reset(int iteration) {
		
	}
	@Override
	public void handleEvent(ActivityStartEvent event) {
		startTimes.put(event.getPersonId(), new Tuple<String, Double>(event.getActType(), event.getTime()));
	}
	@Override
	public void handleEvent(ActivityEndEvent event) {
		Tuple<String, Double> startTime = startTimes.get(event.getPersonId());
		startTimes.put(event.getPersonId(), null);
		if(startTime==null)
			startTime = new Tuple<String, Double>(event.getActType(), 0.0);
		double duration = event.getTime()-startTime.getSecond();
		SortedMap<Double, Integer> typeMap = durations.get(event.getActType());
		if(typeMap==null) {
			typeMap = new TreeMap<Double, Integer>();
			for(double t=timeBin; t<totalTime+timeBin-1;t+=timeBin)
				typeMap.put(t, 0);
			durations.put(event.getActType(), typeMap);
		}
		typeMap.put(getTimeBin(duration), typeMap.get(getTimeBin(duration))+1);
		typeMap = peoplePerforming.get(event.getActType());
		if(typeMap==null) {
			typeMap = new TreeMap<Double, Integer>();
			for(double t=timeBin; t<totalTime+timeBin-1;t+=timeBin)
				typeMap.put(t, 0);
			peoplePerforming.put(event.getActType(), typeMap);
		}
		Double lastTimeBin = getTimeBin(event.getTime());
		for(double t=getTimeBin(startTime.getSecond()); t<=lastTimeBin;t+=timeBin)
			typeMap.put(t, typeMap.get(t)+1);
	}
	private Double getTimeBin(double time) {
		for(double t=timeBin; t<totalTime+timeBin-1;t+=timeBin)
			if(time<t)
				return t;
		return null;
	}
	public void finishActivities() {
		for(Tuple<String, Double> startTime:startTimes.values())
			if(startTime!=null) {
				double duration = totalTime-startTime.getSecond();
				SortedMap<Double, Integer> typeMap = durations.get(startTime.getFirst());
				if(typeMap==null) {
					typeMap = new TreeMap<Double, Integer>();
					for(double t=timeBin; t<totalTime+timeBin-1;t+=timeBin)
						typeMap.put(t, 0);
					durations.put(startTime.getFirst(), typeMap);
				}
				typeMap.put(getTimeBin(duration), typeMap.get(getTimeBin(duration))+1);
				typeMap = peoplePerforming.get(startTime.getFirst());
				if(typeMap==null) {
					typeMap = new TreeMap<Double, Integer>();
					for(double t=timeBin; t<totalTime+timeBin-1;t+=timeBin)
						typeMap.put(t, 0);
					peoplePerforming.put(startTime.getFirst(), typeMap);
				}
				Double lastTimeBin = getTimeBin(totalTime-1);
				for(double t=getTimeBin(startTime.getSecond()); t<=lastTimeBin;t+=timeBin)
					typeMap.put(t, typeMap.get(t)+1);
			}
				
	}
	//Main
	public static void main(String[] args) throws IOException {
		final String CSV_SEPARATOR = "\t";
		EventsManager events = EventsUtils.createEventsManager();
		TypeActivityAnalyzer typeActivityAnalyzer = new TypeActivityAnalyzer(300,30*3600);
		events.addHandler(typeActivityAnalyzer);
		new EventsReaderXMLv1(events).parse(args[0]);
		typeActivityAnalyzer.finishActivities();
		PrintWriter writer = new PrintWriter(new File("./data/durationsByType.txt"));
		for(String type:typeActivityAnalyzer.getDurations().keySet()) {
			for(Entry<Double,Integer> count:typeActivityAnalyzer.getDurations().get(type).entrySet())
				writer.println(Time.writeTime(count.getKey())+CSV_SEPARATOR+type+CSV_SEPARATOR+count.getValue());
		}
		writer.close();
		writer = new PrintWriter(new File("./data/performingByType.txt"));
		for(String type:typeActivityAnalyzer.getPeoplePerforming().keySet()) {
			for(Entry<Double,Integer> count:typeActivityAnalyzer.getPeoplePerforming().get(type).entrySet())
				writer.println(Time.writeTime(count.getKey())+CSV_SEPARATOR+type+CSV_SEPARATOR+count.getValue());
		}
		writer.close();
	}

}

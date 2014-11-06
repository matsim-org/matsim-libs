package playground.ssix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public class DataAnalyzer implements LinkLeaveEventHandler, LinkEnterEventHandler {
	
	private Scenario scenario;
	
	private Map<Id<Person>, Double> linkEnterTimes;
	private Map<Integer, Integer> intervalNVeh;
	private Map<Integer, List<Double>> intervalSpeeds;
	private Map<Integer, Double> intervalMeanSpeed;
	private Map<Integer, Double> intervalFlow;
	
	public final static Id<Link> studiedMeasuringPointLinkId = Id.create("4to5", Link.class);
	public final static double AGGREGATION_TIME = 600.;
	public final static double DT2ONEHOUR = 3600/DataAnalyzer.AGGREGATION_TIME;
	
	public DataAnalyzer(Scenario sc){
		this.scenario =  sc;
		this.linkEnterTimes = new HashMap<>();
		this.intervalNVeh = new HashMap<Integer, Integer>();
		this.intervalSpeeds = new HashMap<Integer, List<Double>>();
		this.intervalFlow = new HashMap<Integer, Double>();
		this.intervalMeanSpeed = new HashMap<Integer, Double>();
		
		int numberOfIntervals = (int) (LangeStreckeSzenario.END_TIME / DataAnalyzer.AGGREGATION_TIME) +1;
		for (int i=0; i<  numberOfIntervals; i++){
			Integer key = new Integer((int) (i * DataAnalyzer.AGGREGATION_TIME));
			this.intervalNVeh.put(key, 0);
			this.intervalSpeeds.put(key, new ArrayList<Double>());
			this.intervalFlow.put(key, new Double(0.));
		}
	}

	@Override
	public void reset(int iteration) {	
		//TODO
	}


	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (event.getLinkId().equals(Id.create("9to1", Link.class))){
			this.linkEnterTimes.put(event.getPersonId(), event.getTime());
		}
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (event.getLinkId().equals(Id.create("9to1", Link.class))){
			double nowTime = event.getTime();
			
			Integer key =  new Integer ( (int) (nowTime / DataAnalyzer.AGGREGATION_TIME) ) * 600;
			double speed = scenario.getNetwork().getLinks().get(event.getLinkId()).getLength() / ( nowTime - this.linkEnterTimes.get(event.getPersonId()) );
			intervalNVeh.put(key, new Integer(intervalNVeh.get(key) + 1) );
			intervalSpeeds.get(key).add(new Double(speed));
			intervalFlow.put(key, new Double(intervalFlow.get(key) + 1));
		}
	}
	
	public void analyze(){
		for (Integer interval : intervalSpeeds.keySet()){
			//Calculating mean Speed for all intervals
			List<Double> allSpeeds = intervalSpeeds.get(interval);
			double meanSpeed = 0.;
			int nVeh = allSpeeds.size();
			for (int i=0; i<nVeh; i++){
				meanSpeed += allSpeeds.get(i);
			}
			meanSpeed /= nVeh;
			this.intervalMeanSpeed.put(interval, meanSpeed);
			
			//Normalizing flow for all intervals
			if (intervalFlow.get(interval) != (double) intervalNVeh.get(interval)){
				throw new RuntimeException("Counting error somewhere!");
			}
			double flow = intervalFlow.get(interval) * DataAnalyzer.DT2ONEHOUR;
			intervalFlow.put(interval, flow);
		}
		
	}

	public Map<Integer, Integer> getIntervalNVeh() {
		return intervalNVeh;
	}

	public Map<Integer, Double> getIntervalMeanSpeed() {
		return intervalMeanSpeed;
	}

	public Map<Integer, Double> getIntervalFlow() {
		return intervalFlow;
	}
}
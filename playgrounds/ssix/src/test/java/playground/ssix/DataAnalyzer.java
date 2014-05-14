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
import org.matsim.core.basic.v01.IdImpl;

import playgrounds.ssix.DreieckNmodes;
import playgrounds.ssix.ModeData;

public class DataAnalyzer implements LinkLeaveEventHandler, LinkEnterEventHandler {
	
	private Scenario scenario;
	private Map<Id, ModeData> modesData;
	private ModeData globalData;
	
	private Map<Id, Double> linkEnterTimes;
	private Map<Integer, Integer> intervalNVeh;
	private Map<Integer, List<Double>> intervalSpeeds;
	private Map<Integer, Double> intervalFlow;
	
	public final static Id studiedMeasuringPointLinkId = new IdImpl("4to5");
	public final static double AGGREGATION_TIME = 600.;
	public final static double DT2ONEHOUR = 3600/DataAnalyzer.AGGREGATION_TIME;
	
	public DataAnalyzer(Scenario sc, Map<Id, ModeData> modesData){
		this.scenario =  sc;
		this.modesData = modesData;
		for (int i=0; i<DreieckNmodes.NUMBER_OF_MODES; i++){
			this.modesData.get(new IdImpl(DreieckNmodes.NAMES[i])).initDynamicVariables();
		}
		this.globalData = new ModeData();
		this.globalData.setnumberOfAgents(sc.getPopulation().getPersons().size());
		this.globalData.initDynamicVariables();
		this.linkEnterTimes = new HashMap<Id, Double>();
		this.intervalNVeh = new HashMap<Integer, Integer>();
		this.intervalSpeeds = new HashMap<Integer, List<Double>>();
		this.intervalFlow = new HashMap<Integer, Double>();
		
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
		for (int i=0; i<DreieckNmodes.NUMBER_OF_MODES; i++){
			this.modesData.get(new IdImpl(DreieckNmodes.NAMES[i])).reset();
		}
		this.globalData.reset();
	}


	public void handleEvent(LinkEnterEvent event) {
		if (event.getLinkId().equals(new IdImpl("4to5"))){
			this.linkEnterTimes.put(event.getPersonId(), event.getTime());
		}
	}
	
	public void handleEvent(LinkLeaveEvent event) {
		double nowTime = event.getTime();
		
		Integer key =  new Integer ( (int) (nowTime / DataAnalyzer.AGGREGATION_TIME) );
		double speed = scenario.getNetwork().getLinks().get(event.getLinkId()).getLength() / ( nowTime - this.linkEnterTimes.get(event.getPersonId()) );
		intervalNVeh.put(key, new Integer(intervalNVeh.get(key) + 1) );
		intervalSpeeds.get(key).add(new Double(speed));
		intervalFlow.put(key, new Double(intervalFlow.get(key) + 1));
	}
	
	public ModeData getGlobalData(){
		return this.globalData;
	}

	public Map<Id, ModeData> getModesData() {
		return modesData;
	}
	
}
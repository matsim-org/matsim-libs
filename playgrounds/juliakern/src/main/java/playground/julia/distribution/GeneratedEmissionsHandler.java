package playground.julia.distribution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import playground.vsp.emissions.events.ColdEmissionEvent;
import playground.vsp.emissions.events.ColdEmissionEventHandler;
import playground.vsp.emissions.events.WarmEmissionEvent;
import playground.vsp.emissions.events.WarmEmissionEventHandler;
import playground.vsp.emissions.types.ColdPollutant;
import playground.vsp.emissions.types.WarmPollutant;

public class GeneratedEmissionsHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler {

	Double simulationStartTime;
	Double timeBinSize;
	Map<Double, ArrayList<EmPerBin>> emissionPerBin;
	Map<Double, ArrayList<EmPerLink>> emissionPerLink;
	Map<Id,Integer> link2xbins; 
	Map<Id,Integer> link2ybins;
	WarmPollutant warmPollutant2analyze;
	ColdPollutant coldPollutant2analyze;
	
	
	public GeneratedEmissionsHandler(Double simulationStartTime, Double timeBinSize, Map<Id, Integer>link2xbins, Map<Id, Integer>link2ybins,
			WarmPollutant warmPollutant2analyze, ColdPollutant coldPollutant2analyze){
		System.out.println("new handler");
		this.simulationStartTime = simulationStartTime;
		this.timeBinSize= timeBinSize;
		this.link2xbins = link2xbins;
		this.link2ybins = link2ybins;
		this.warmPollutant2analyze = warmPollutant2analyze;
		this.coldPollutant2analyze = coldPollutant2analyze;
		this.emissionPerBin = new HashMap<Double, ArrayList<EmPerBin>>();
		this.emissionPerLink = new HashMap<Double, ArrayList<EmPerLink>>();
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		System.out.println("shtn");
		Id linkId = event.getLinkId();
		Integer xBin = link2xbins.get(linkId);
		Integer yBin = link2ybins.get(linkId);
		
		if (xBin != null && yBin != null) {
		//TODO person id statt vehicleid??? woher?
		Id personId = event.getVehicleId();
		Double value = event.getColdEmissions().get(coldPollutant2analyze); //TODO funktioniert das so? enum casten?
		EmPerBin epb = new EmPerBin(xBin, yBin, personId, value);
		Double endOfTimeIntervall = getEndOfTimeInterval(event.getTime());
		emissionPerBin.get(endOfTimeIntervall).add(epb);
		EmPerLink epl = new EmPerLink(linkId, personId, value);
		emissionPerLink.get(endOfTimeIntervall).add(epl);
		}
	}

	//TODO funktioniert nur so, wenn die simulation start time =0 ist!!!!
	private Double getEndOfTimeInterval(double time) {
		Double end = Math.ceil(time/timeBinSize)*timeBinSize;
		if(end>0.0) return end;
		return timeBinSize;
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		System.out.println("warm event");
		
		Id linkId = event.getLinkId();

		Integer xBin = link2xbins.get(linkId);
		Integer yBin = link2ybins.get(linkId);
		
		if (xBin != null && yBin != null) {
			//TODO person id statt vehicleid??? woher?
			Id personId = event.getVehicleId();
			System.out.println("warm 5");
			Double value = event.getWarmEmissions().get(warmPollutant2analyze); //TODO funktioniert das so? enum casten?
			EmPerBin epb = new EmPerBin(xBin, yBin, personId, value);
			Double endOfTimeIntervall = getEndOfTimeInterval(event.getTime());
			if (!emissionPerBin.containsKey(endOfTimeIntervall)) {
				emissionPerBin.put(endOfTimeIntervall,
						new ArrayList<EmPerBin>());
			}
			emissionPerBin.get(endOfTimeIntervall).add(epb);
			EmPerLink epl = new EmPerLink(linkId, personId, value);
			if (!emissionPerLink.containsKey(endOfTimeIntervall)) {
				emissionPerLink.put(endOfTimeIntervall,
						new ArrayList<EmPerLink>());
			}
			emissionPerLink.get(endOfTimeIntervall).add(epl);
			System.out.println("handling warm with value" + value);
		}
	}

	public Map<Double, ArrayList<EmPerLink>> getEmissionsPerLink() {
		return emissionPerLink;
	}

	public Map<Double, ArrayList<EmPerBin>> getEmissionsPerCell() {
		return emissionPerBin;
	}



}

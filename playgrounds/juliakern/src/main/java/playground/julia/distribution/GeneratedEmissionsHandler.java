package playground.julia.distribution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math.random.EmpiricalDistribution;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.handler.EventHandler;

import playground.vsp.emissions.events.ColdEmissionEvent;
import playground.vsp.emissions.events.ColdEmissionEventHandler;
import playground.vsp.emissions.events.WarmEmissionEvent;
import playground.vsp.emissions.events.WarmEmissionEventHandler;
import playground.vsp.emissions.types.ColdPollutant;
import playground.vsp.emissions.types.WarmPollutant;

public class GeneratedEmissionsHandler implements EventHandler, WarmEmissionEventHandler, ColdEmissionEventHandler {

	Double simulationStartTime;
	Double timeBinSize;
	Map<Double, ArrayList<EmPerBin>> emissionPerBin;
	Map<Double, ArrayList<EmPerLink>> emissionPerLink;
	Map<Id,Integer> link2xbins; 
	Map<Id,Integer> link2ybins;
	String pollutant2analyze;
	
	
	public GeneratedEmissionsHandler(Double simulationStartTime, Double timeBinSize, Map<Id, Integer>link2xbins, Map<Id, Integer>link2ybins, String pollutant2analyze){
		this.simulationStartTime = simulationStartTime;
		this.timeBinSize= timeBinSize;
		this.link2xbins = link2xbins;
		this.link2ybins = link2ybins;
		this.pollutant2analyze = pollutant2analyze; 
		this.emissionPerBin = new HashMap<Double, ArrayList<EmPerBin>>();
		this.emissionPerLink = new HashMap<Double, ArrayList<EmPerLink>>();
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		Id linkId = event.getLinkId();
		int xBin = link2xbins.get(linkId);
		int yBin = link2ybins.get(linkId);
		//TODO person id statt vehicleid??? woher?
		Id personId = event.getVehicleId();
		Double value = event.getColdEmissions().get(pollutant2analyze); //TODO funktioniert das so? enum casten?
		EmPerBin epb = new EmPerBin(xBin, yBin, personId, value);
		Double endOfTimeIntervall = getEndOfTimeInterval(event.getTime());
		emissionPerBin.get(endOfTimeIntervall).add(epb);
		EmPerLink epl = new EmPerLink(linkId, personId, value);
		emissionPerLink.get(endOfTimeIntervall).add(epl);
	}

	//TODO funktioniert nur so, wenn die simulation start time =0 ist!!!!
	private Double getEndOfTimeInterval(double time) {
		Double end = Math.ceil(time/timeBinSize)*timeBinSize;
		if(end>0.0) return end;
		return timeBinSize;
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		Id linkId = event.getLinkId();
		int xBin = link2xbins.get(linkId);
		int yBin = link2ybins.get(linkId);
		//TODO person id statt vehicleid??? woher?
		Id personId = event.getVehicleId();
		Double value = event.getWarmEmissions().get(pollutant2analyze); //TODO funktioniert das so? enum casten?
		EmPerBin epb = new EmPerBin(xBin, yBin, personId, value);
		Double endOfTimeIntervall = getEndOfTimeInterval(event.getTime());
		emissionPerBin.get(endOfTimeIntervall).add(epb);
		EmPerLink epl = new EmPerLink(linkId, personId, value);
		emissionPerLink.get(endOfTimeIntervall).add(epl);
		
	}

	public Map<Double, ArrayList<EmPerLink>> getEmissionsPerLink() {
		return emissionPerLink;
	}

	public Map<Double, ArrayList<EmPerBin>> getEmissionsPerCell() {
		return emissionPerBin;
	}



}

package playground.fhuelsmann.emission.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;

import playground.benjamin.events.emissions.WarmEmissionEvent;
import playground.benjamin.events.emissions.WarmEmissionEventHandler;
import playground.benjamin.events.emissions.WarmPollutant;

public class EmissionsPerLinkWarmEventHandler implements WarmEmissionEventHandler {

	Map<Id, Map<WarmPollutant, Double>> warmEmissionsTotal = new HashMap<Id, Map<WarmPollutant, Double>>();

	public EmissionsPerLinkWarmEventHandler() {
	}

	public void handleEvent(WarmEmissionEvent event) {
		Id linkId= event.getLinkId();
		Map<WarmPollutant, Double> warmEmissionsOfEvent = event.getWarmEmissions();

		if(!warmEmissionsTotal.containsKey(linkId)){
			warmEmissionsTotal.put(linkId, warmEmissionsOfEvent);
			}
			
		else{		
			Map<WarmPollutant, Double> warmEmissionsSoFar = warmEmissionsTotal.get(linkId);
			for(Entry<WarmPollutant, Double> entry : warmEmissionsOfEvent.entrySet()){
				WarmPollutant pollutant = entry.getKey();
				Double eventValue = entry.getValue();
				
				if(!warmEmissionsSoFar.containsKey(pollutant)){
					warmEmissionsSoFar.put(pollutant, eventValue);
					warmEmissionsTotal.put(linkId, warmEmissionsSoFar);
				//	if(linkId.toString().equals("10038"))
				//	System.out.println("linkId "+linkId+ " pollutant "+ pollutant+" eventValue "+eventValue);
				}
				else{
			
				Double previousValue = warmEmissionsSoFar.get(pollutant);
				Double newValue = previousValue + eventValue;
				warmEmissionsSoFar.put(pollutant, newValue);
				warmEmissionsTotal.put(linkId, warmEmissionsSoFar);
			//		if(linkId.toString().equals("10038"))
			//		System.out.println("linkId "+linkId+ "pollutant "+ pollutant+ " previousValue "+previousValue+" eventValue "+eventValue
			//				+" newValue "+newValue);
				}
			}
		}
	}


	public Map<Id, Map<String, Double>> getWarmEmissionsPerLink() {
		Map<Id, Map<String, Double>> linkId2warmEmissionsAsString = new HashMap<Id, Map<String, Double>>();

		for (Entry<Id, Map<WarmPollutant, Double>> entry1: this.warmEmissionsTotal.entrySet()){
			Id linkId = entry1.getKey();
			Map<WarmPollutant, Double> pollutant2Values = entry1.getValue();
			Map<String, Double> pollutantString2Values = new HashMap<String, Double>();
			for (Entry<WarmPollutant, Double> entry2: pollutant2Values.entrySet()){
				String pollutant = entry2.getKey().toString();
				Double value = entry2.getValue();
				pollutantString2Values.put(pollutant, value);
			}
			linkId2warmEmissionsAsString.put(linkId, pollutantString2Values);
		}
		return linkId2warmEmissionsAsString;
	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub
	}
}

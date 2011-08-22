package playground.fhuelsmann.emission.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.benjamin.events.emissions.WarmPollutant;


public class CongestionEmissionCorrelate {
	
	private static final Logger logger = Logger.getLogger(CongestionEmissionCorrelate.class);

	private SortedSet<String> listOfPollutants;

	CongestionEmissionCorrelate() {
	}


	SortedSet<String> getListOfPollutants() {
		return listOfPollutants;
	}
	
	public Map<Double,Map<Id, Map<String, Double>>> compareEmissioneAndCongestion(Map<Double,Map<Id, Map<String, Double>>> warmEmissions, Map<Double,Map<Id, Double>>congestion ) {
		Map<Double,Map<Id, Map<String, Double>>>emissionsAndCongestion = new HashMap<Double,Map<Id, Map<String, Double>>>();
		
		for (Entry<Double,Map<Id,Map<String,Double>>> entry0: warmEmissions.entrySet()){
			Double time = entry0.getKey();
			Map<Id, Map<String, Double>> linkId2warmEmissions = entry0.getValue();
		
			for(Entry<Id, Map<String, Double>> entry1: linkId2warmEmissions.entrySet()){
				Id linkId = entry1.getKey();
				Map<String, Double> linkWarmEmissions = entry1.getValue();
				
				if(congestion.get(time).containsKey(linkId)){
					Map<String, Double> emissionsCongestionCompare = new HashMap<String, Double>();
					
					Double ratio;

					for(String pollutant : listOfPollutants){
						if(linkWarmEmissions.containsKey(pollutant)){
							if (congestion.get(time).get(linkId)!=0.0){
								ratio = linkWarmEmissions.get(pollutant) / congestion.get(time).get(linkId);
							}
							else {ratio = 0.0;}
						} 
						else{
							ratio = 0.0;
						}
						emissionsCongestionCompare.put(pollutant, ratio);
					}
					linkId2warmEmissions.put(linkId, emissionsCongestionCompare);
				} 
				else {
					linkId2warmEmissions.put(linkId, null);
				}
			}
			emissionsAndCongestion.put(time, linkId2warmEmissions);
			logger.info("The Emission and congestion comparison is done for time period "+time/3600);
		}
		return emissionsAndCongestion;
	}



	public void defineListOfPollutants() {
		listOfPollutants = new TreeSet<String>();
		for(WarmPollutant wp : WarmPollutant.values()){
			listOfPollutants.add(wp.toString());
		}
		logger.info("The following pollutants are considered: " + listOfPollutants);
	}
}

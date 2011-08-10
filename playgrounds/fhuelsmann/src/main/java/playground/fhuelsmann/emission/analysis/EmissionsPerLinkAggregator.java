package playground.fhuelsmann.emission.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

import playground.benjamin.events.emissions.EmissionEventsReader;

public class EmissionsPerLinkAggregator {
	private static final Logger logger = Logger.getLogger(EmissionsPerLinkAggregator.class);

	private Network network;
	private final String emissionFile;
	
	private EmissionsPerLinkWarmEventHandler warmHandler;
	private EmissionsPerLinkColdEventHandler coldHandler;
	private Map<Id, Map<String, Double>> warmEmissions;
	private Map<Id, Map<String, Double>> coldEmissions;
	private Map<Id, Map<String, Double>> totalEmissions;
	private SortedSet<String> listOfPollutants;

	EmissionsPerLinkAggregator(Network network, String emissionFile) {
		this.network = network;
		this.emissionFile = emissionFile;
	}

	void run() {
		processEmissions();
		warmEmissions = warmHandler.getWarmEmissionsPerLink();
		coldEmissions = coldHandler.getColdEmissionsPerLink();
		fillListOfPollutants(warmEmissions, coldEmissions);
		setNonCalculatedEmissions(this.network, warmEmissions);
		setNonCalculatedEmissions(this.network, coldEmissions);
		totalEmissions = sumUpEmissions(warmEmissions, coldEmissions);
	}

	Map<Id, Map<String, Double>> getTotalEmissions() {
		return totalEmissions;
	}
	
	Map<Id, Map<String, Double>> getColdEmissions() {
		return coldEmissions;
	}

	Map<Id, Map<String, Double>> getWarmEmissions() {
		return warmEmissions;
	}
	
	SortedSet<String> getListOfPollutants() {
		return listOfPollutants;
	}
	
	private Map<Id, Map<String, Double>> sumUpEmissions(Map<Id, Map<String, Double>> warmEmissions, Map<Id, Map<String, Double>> coldEmissions) {
		Map<Id, Map<String, Double>> totalEmissions = new HashMap<Id, Map<String, Double>>();
		for(Entry<Id, Map<String, Double>> entry : warmEmissions.entrySet()){
			Id linkId = entry.getKey();
			Map<String, Double> individualWarmEmissions = entry.getValue();

			if(coldEmissions.containsKey(linkId)){
				Map<String, Double> individualSumOfEmissions = new HashMap<String, Double>();
				Map<String, Double> individualColdEmissions = coldEmissions.get(linkId);
				Double individualValue;

				for(String pollutant : listOfPollutants){
					if(individualWarmEmissions.containsKey(pollutant)){
						if(individualColdEmissions.containsKey(pollutant)){
							individualValue = individualWarmEmissions.get(pollutant) + individualColdEmissions.get(pollutant);
						} else{
							individualValue = individualWarmEmissions.get(pollutant);
						}
					} else{
						individualValue = individualColdEmissions.get(pollutant);
					}
					individualSumOfEmissions.put(pollutant, individualValue);
				}
				totalEmissions.put(linkId, individualSumOfEmissions);
			} else{
				totalEmissions.put(linkId, individualWarmEmissions);
			}
		}
		return totalEmissions;
	}

	private void setNonCalculatedEmissions(Network network, Map<Id, Map<String, Double>> emissionsPerLink) {
		for(Link link : network.getLinks().values()){
			Id linkId= link.getId();
			if(!emissionsPerLink.containsKey(linkId)){
				Map<String, Double> emissionType2Value = new HashMap<String, Double>();
				for(String pollutant : listOfPollutants){
					// setting emissions that are were not calculated to 0.0 
					emissionType2Value.put(pollutant, 0.0);
				}
				emissionsPerLink.put(linkId, emissionType2Value);
			} else{
				// do nothing
			}
		}
	}

	private void fillListOfPollutants(Map<Id, Map<String, Double>> warmEmissions, Map<Id, Map<String, Double>> coldEmissions) {
		listOfPollutants = new TreeSet<String>();
		for(Map<String, Double> emissionType2Value : warmEmissions.values()){
			for(String pollutant : emissionType2Value.keySet()){
				if(!listOfPollutants.contains(pollutant)){
					listOfPollutants.add(pollutant);
				}
			}
		}
		for(Map<String, Double> emissionType2Value : coldEmissions.values()){
			for(String pollutant : emissionType2Value.keySet()){
				if(!listOfPollutants.contains(pollutant)){
					listOfPollutants.add(pollutant);
				}
			}
		}
		logger.info("The following pollutants are considered: " + listOfPollutants);
	}

	private void processEmissions() {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
		warmHandler = new EmissionsPerLinkWarmEventHandler();
		coldHandler = new EmissionsPerLinkColdEventHandler();
		eventsManager.addHandler(warmHandler);
		eventsManager.addHandler(coldHandler);
		emissionReader.parse(this.emissionFile);
	}
}

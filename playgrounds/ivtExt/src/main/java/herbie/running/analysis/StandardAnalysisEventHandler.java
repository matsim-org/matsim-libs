package herbie.running.analysis;

import java.util.TreeMap;

import org.apache.commons.math.stat.Frequency;
import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.util.ResizableDoubleArray;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import herbie.running.population.algorithms.AbstractClassifiedFrequencyAnalysis;
import utils.Bins;

public class StandardAnalysisEventHandler extends AbstractClassifiedFrequencyAnalysis  
	implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonArrivalEventHandler, 
	PersonDepartureEventHandler, IterationEndsListener{

	private final TreeMap<Id, Double> agentDepartures = new TreeMap<Id, Double>();
	
	// #### was macht genau dieser Constructor? ###
//	public StandardAnalysisEventHandler(Population pop, PrintStream out) {
//		super(out);
//		this.population = pop;
//	}
	public StandardAnalysisEventHandler() {
	}
	
	@Override
	public void reset(int iteration) {
		
		// for average trip duration by mode
		this.rawData.clear();
		this.frequencies.clear();
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
	}
	
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		
		
		// trip durations
		Double depTime = this.agentDepartures.remove(event.getPersonId());
		
		// System.out.println("########  Unclear at this point !!! #########");
		
//		Person agent = this.population.getPersons().get(event.getDriverId());
//		if (depTime != null && agent != null) {
		if (depTime != null) {
			
			
			double travelTime = event.getTime() - depTime;
			String mode = event.getLegMode();

			Frequency frequency = null;
			ResizableDoubleArray rawData = null;
			if (!this.frequencies.containsKey(mode)) {
				frequency = new Frequency();
				this.frequencies.put(mode, frequency);
				rawData = new ResizableDoubleArray();
				this.rawData.put(mode, rawData);
			} else {
				frequency = this.frequencies.get(mode);
				rawData = this.rawData.get(mode);
			}

			frequency.addValue(travelTime);
			rawData.addElement(travelTime);
		}
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		// trip durations
		this.agentDepartures.put(event.getPersonId(), event.getTime());
	}
	/**
	 * 
	 * @param intervalLength in min
	 * @return
	 */
	public TreeMap<String, Bins> getTravelTimeDistributionByMode(double intervalLength){
		
		TreeMap<String, Bins> allTravelTimeDistributions = new TreeMap<String, Bins>();
		
		for (String mode : this.rawData.keySet()) {
			
			double[]weights = new double [this.rawData.get(mode).getElements().length];
			for (int i = 0; i < weights.length; i++) weights[i] = 1.0;
			
			Bins travelTimeDistributionByMode = new Bins(intervalLength*60.0, 24.0*3600.0, "Travel Time Distribution "+mode);
			travelTimeDistributionByMode.addValues(this.rawData.get(mode).getElements(), weights);
			
			allTravelTimeDistributions.put(mode, travelTimeDistributionByMode);
		}
		
		System.out.println("Finished with Travel Time Distribution by Mode.");
		return allTravelTimeDistributions;
	}
	
	public TreeMap<String, Double> getAverageTripDurationsByMode() {
		TreeMap<String, Double> averageTripDurations = new TreeMap<String, Double>();
		for (String mode : this.rawData.keySet()) {
			averageTripDurations.put(mode, StatUtils.mean(this.rawData.get(mode).getElements()));
		}
		return averageTripDurations;
	}

	public double getAverageOverallTripDuration() {

		double overallTripDuration = 0.0;
		int overallNumTrips = 0;

		for (String mode : this.rawData.keySet()) {
			overallTripDuration += StatUtils.sum(this.rawData.get(mode).getElements());
			overallNumTrips += this.rawData.get(mode).getNumElements();
		}

		return (overallTripDuration / overallNumTrips);
	}
	
	@Override
	public void run(Person person) {
	}
	

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
	}
}

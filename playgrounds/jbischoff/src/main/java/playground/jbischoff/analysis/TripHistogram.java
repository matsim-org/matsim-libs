/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package playground.jbischoff.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.PersonExperiencedActivity;
import org.matsim.core.scoring.PersonExperiencedLeg;
import org.matsim.core.utils.misc.Time;

import com.google.inject.Inject;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class TripHistogram implements  EventsToLegs.LegHandler, EventsToActivities.ActivityHandler {

	private final static Logger log = Logger.getLogger(TripHistogram.class);

	private int iteration = 0;
	private final int binSize ;
	private final int nofBins;
	private final Map<String, DataFrame> data = new TreeMap<>();
	
	
	@Inject private Population population;
	@Inject private TripRouter triprouter;
	
	private final Map<Id<Person>, List<PlanElement>> agentLastTripRecord = new HashMap<>();

	@Inject
	TripHistogram(ControlerListenerManager controlerListenerManager, EventsToActivities eventsToActivities, EventsToLegs eventsToLegs) {
		controlerListenerManager.addControlerListener(new IterationStartsListener() {
			@Override
			public void notifyIterationStarts(IterationStartsEvent event) {
				iteration = event.getIteration();
				data.clear();
				agentLastTripRecord.clear();
				for (Person person : population.getPersons().values()) {
					agentLastTripRecord.put(person.getId(), new ArrayList<PlanElement>());
					
				}
			}
		});
		
		eventsToActivities.addActivityHandler(this);
		eventsToLegs.addLegHandler(this);
		binSize = 300;
		nofBins = 30*3600/binSize + 1;
	}

	@Override
	synchronized public void handleLeg(PersonExperiencedLeg o) {
		// Has to be synchronized because the thing which sends Legs and the thing which sends Activities can run
		// on different threads. Will go away when/if we get a more Actor or Reactive Streams like event infrastructure.
		Id<Person> agentId = o.getAgentId();
		Leg leg = o.getLeg();
		List<PlanElement> planElementRecord = agentLastTripRecord.get(agentId);
		if (planElementRecord != null) {
			planElementRecord.add(leg);
		}
	}

	@Override
	synchronized public void handleActivity(PersonExperiencedActivity o) {
		// Has to be synchronized because the thing which sends Legs and the thing which sends Activities can run
		// on different threads. Will go away when/if we get a more Actor or Reactive Streams like event infrastructure.
		Id<Person> agentId = o.getAgentId();
		Activity activity = o.getActivity();
		List<PlanElement> planElementRecord = agentLastTripRecord.get(agentId);
		if (triprouter.getStageActivityTypes().isStageActivity(activity.getType())){
			if (planElementRecord != null) {
				planElementRecord.add(activity);
			}
		} else {
			if ((planElementRecord != null)&&(!planElementRecord.isEmpty())) {
				String mainMode = triprouter.getMainModeIdentifier().identifyMainMode(planElementRecord);
				double tripDepartureTime = Double.MAX_VALUE;
				double tripArrivalTime = Double.MAX_VALUE;
				for (PlanElement pe : planElementRecord){
					if (pe instanceof Leg){
						Leg leg = (Leg) pe;
						if (leg.getDepartureTime()<tripDepartureTime){
							tripDepartureTime = leg.getDepartureTime();
						} 
						tripArrivalTime = leg.getDepartureTime()+leg.getTravelTime();
					}
				}
				int departureBin = this.getBinIndex(tripDepartureTime);
				DataFrame modeDataFrame = this.getDataForMode(mainMode);
				modeDataFrame.countsDep[departureBin]++;
				int arrivalBin = this.getBinIndex(tripArrivalTime);
				modeDataFrame.countsArr[arrivalBin]++;
				planElementRecord.clear();
			}

			
		}
	}
	
	/**
	 * Writes the gathered data tab-separated into a text file.
	 *
	 * @param filename The name of a file where to write the gathered data.
	 */
	public void write(final String filename) {
		try (PrintStream stream = new PrintStream(new File(filename))) {
			write(stream);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Writes the gathered data tab-separated into a text stream.
	 *
	 * @param stream The data stream where to write the gathered data.
	 */
	public void write(final PrintStream stream) {
		stream.print("time\ttime\tdepartures_all\tarrivals_all\tstuck_all\ten-route_all");
		for (String legMode : this.data.keySet()) {
			stream.print("\tdepartures_" + legMode + "\tarrivals_" + legMode + "\ten-route_" + legMode);
		}
		stream.print("\n");
		int allEnRoute = 0;
		int[] modeEnRoute = new int[this.data.size()];
        DataFrame allModesData = getAllModesData();
        for (int i = 0; i < allModesData.countsDep.length; i++) {
			// data about all modes
			allEnRoute = allEnRoute + allModesData.countsDep[i] - allModesData.countsArr[i];
			stream.print(Time.writeTime(i*this.binSize) + "\t" + i*this.binSize);
			stream.print("\t" + allModesData.countsDep[i] + "\t" + allModesData.countsArr[i] + "\t"  + "\t" + allEnRoute);

			// data about single modes
			int mode = 0;
			for (DataFrame dataFrame : this.data.values()) {
				modeEnRoute[mode] = modeEnRoute[mode] + dataFrame.countsDep[i] - dataFrame.countsArr[i];
				stream.print("\t" + dataFrame.countsDep[i] + "\t" + dataFrame.countsArr[i] + "\t" + "\t" + modeEnRoute[mode]);
				mode++;
			}

			// new line
			stream.print("\n");
		}
	}

    /**
	 * @return number of departures per time-bin, for all legs
	 */
	public int[] getDepartures() {
		return this.getAllModesData().countsDep;
	}

	/**
	 * @return number of all arrivals per time-bin, for all legs
	 */
	public int[] getArrivals() {
		return this.getAllModesData().countsArr;
	}

	

	/**
	 * @return Set of all transportation modes data is available for
	 */
	public Set<String> getLegModes() {
		return this.data.keySet();
	}

	/**
	 * @param legMode transport mode
	 * @return number of departures per time-bin, for all legs with the specified mode
	 */
	public int[] getDepartures(final String legMode) {
		DataFrame dataFrame = this.data.get(legMode);
		if (dataFrame == null) {
			return new int[0];
		}
		return dataFrame.countsDep.clone();
	}

	/**
	 * @param legMode transport mode
	 * @return number of all arrivals per time-bin, for all legs with the specified mode
	 */
	public int[] getArrivals(final String legMode) {
		DataFrame dataFrame = this.data.get(legMode);
		if (dataFrame == null) {
			return new int[0];
		}
		return dataFrame.countsArr.clone();
	}

	

    int getIteration() {
        return iteration;
    }

    DataFrame getAllModesData() {
        DataFrame result = new DataFrame(this.binSize, this.nofBins + 1);
        for (DataFrame byMode : data.values()) {
            for (int i=0;i<result.countsDep.length;++i) {
                result.countsDep[i] += byMode.countsDep[i];
            }
            for (int i=0;i<result.countsArr.length;++i) {
                result.countsArr[i] += byMode.countsArr[i];
            }
           
        }
        return result;
    }

	private int getBinIndex(final double time) {
		int bin = (int)(time / this.binSize);
		if (bin >= this.nofBins) {
			return this.nofBins;
		}
		return bin;
	}

	DataFrame getDataForMode(final String legMode) {
		DataFrame dataFrame = this.data.get(legMode);
		if (dataFrame == null) {
			dataFrame = new DataFrame(this.binSize, this.nofBins + 1); // +1 for all times out of our range
			this.data.put(legMode, dataFrame);
		}
		return dataFrame;
	}

	static class DataFrame {
		final int[] countsDep;
		final int[] countsArr;
        final int binSize;

        public DataFrame(final int binSize, final int nofBins) {
			this.countsDep = new int[nofBins];
			this.countsArr = new int[nofBins];
            this.binSize = binSize;
		}
	}

}

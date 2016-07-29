/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.dgrether.analysis.activity;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.BarChart;
import org.matsim.core.utils.misc.Time;

import playground.dgrether.utils.IntegerCountMap;


/**
 * @author dgrether
 *
 */
public class EventModeActivityDurationAnalyser {

	private static final String EXAMPLEBASE = "examples/";

	private static final String EQUILBASE = EXAMPLEBASE + "equil/";

	private static final String NETWORK = EQUILBASE + "network.xml";

	private static final String EVENTSFILEBASE = "/Volumes/data/work/cvsRep/vsp-cvs/runs/run";

	private static final String EVENTSFILE = EVENTSFILEBASE + "591/it.1100/1100.events.txt.gz";

//	private static final String EVENTSFILE = EVENTSFILEBASE + "588/it.1100/1100.events.txt.gz";

	private static final String PLANSFILEBASE = "/Volumes/data/work/cvsRep/vsp-cvs/runs/run";

	private static final String PLANSFILE = PLANSFILEBASE + "591/it.1100/1100.plans.xml.gz";

//	private static final String PLANSFILE = PLANSFILEBASE + "588/it.1000/1000.plans.xml.gz";

	private static final String CONFIGFILE = EQUILBASE + "config.xml";

	private final double t0Home = 12.0*Math.exp(-10.0/12.0);
	private final double t0Work = 8.0*Math.exp(-10.0/8.0);

	private final Config config;

	public EventModeActivityDurationAnalyser() {

		this.config = ConfigUtils.loadConfig(CONFIGFILE);

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);

		Network net = scenario.getNetwork();
		MatsimNetworkReader reader = new MatsimNetworkReader(scenario.getNetwork());
		reader.readFile(NETWORK);

		Population plans = scenario.getPopulation();
		PopulationReader plansParser = new PopulationReader(scenario);
		plansParser.readFile(PLANSFILE);

		EventsManagerImpl events = (EventsManagerImpl) EventsUtils.createEventsManager();
		events.addHandler(new ActivityDurationHandler(plans));
		MatsimEventsReader eventsReader = new MatsimEventsReader(events);
		eventsReader.readFile(EVENTSFILE);
		events.resetHandlers(0);
	}

	private static class ActivityDurationHandler implements ActivityEndEventHandler, ActivityStartEventHandler{

		Map<Id<Person>, ActivityStartEvent> eventMap = new HashMap<>();

		IntegerCountMap<Double> ptStartTimeMap = new IntegerCountMap<Double>();
		IntegerCountMap<Double> carStartTimeMap = new IntegerCountMap<Double>();

		double homeActivityDurationsCar = 0.0;
		double homeActivityDurationsNonCar = 0.0;
		double workActivityDurationsCar = 0.0;
		double workActivityDurationsNonCar = 0.0;
		int homeActivityCarCount = 0;
		int homeActivityNonCarCount = 0;
		int workActivityCarCount = 0;
		int workActivityNonCarCount = 0;
		double durTemp;

		private final Population plans;

		public ActivityDurationHandler(final Population plans) {
			this.plans = plans;
		}

		@Override
		public void handleEvent(final ActivityEndEvent event) {
			ActivityStartEvent startEvent = this.eventMap.get(event.getPersonId());
			Plan p = this.plans.getPersons().get(event.getPersonId()).getSelectedPlan();
			if (startEvent == null) { // must be the end of home_0
				this.durTemp = event.getTime();
			}
			else {
				this.durTemp = event.getTime() - startEvent.getTime();
			}
			if (event.getActType().equalsIgnoreCase("h")) {
				if (((Plan) p).getType().equals(TransportMode.car)) {
					this.homeActivityDurationsCar += this.durTemp;
					this.homeActivityCarCount++;
				}
				else if (((Plan) p).getType().equals(TransportMode.pt)){
					this.homeActivityDurationsNonCar += this.durTemp;
					this.homeActivityNonCarCount++;
				}
			}
			else if (event.getActType().equalsIgnoreCase("w")) {
				if (((Plan) p).getType().equals(TransportMode.car)) {
					this.workActivityDurationsCar += this.durTemp;
					this.workActivityCarCount++;
				}
				else if (((Plan) p).getType().equals(TransportMode.pt)){
					this.workActivityDurationsNonCar += this.durTemp;
					this.workActivityNonCarCount++;
				}
			}
		}

		@Override
		public void handleEvent(final ActivityStartEvent event) {
			this.eventMap.put(event.getPersonId(), event);
			Plan p = this.plans.getPersons().get(event.getPersonId()).getSelectedPlan();
			if (event.getActType().equalsIgnoreCase("w")) {
				if (((Plan) p).getType().equals(TransportMode.pt)) {
					this.ptStartTimeMap.incrementValue(event.getTime());
				}
				else if (((Plan) p).getType().equals(TransportMode.car)) {
					this.carStartTimeMap.incrementValue(event.getTime());
			  }
			}
		}

		@Override
		public void reset(final int iteration) {
			this.homeActivityCarCount /= 2;
			this.homeActivityNonCarCount /= 2;
			System.out.println("Total home activity duration for mode car:     " + this.homeActivityDurationsCar);
			System.out.println("Total home activity duration for mode non-car: " + this.homeActivityDurationsNonCar);
			System.out.println("Total work activity duration for mode car:     " + this.workActivityDurationsCar);
			System.out.println("Total work activity duration for mode non-car: " + this.workActivityDurationsNonCar);
			System.out.println();
			System.out.println("Average home activity duration for mode non-car: " + Time.writeTime(this.homeActivityDurationsNonCar / this.homeActivityNonCarCount));
			System.out.println("Average home activity duration for mode car:     " + Time.writeTime(this.homeActivityDurationsCar / this.homeActivityCarCount));
			System.out.println("Average work activity duration for mode car:     " + Time.writeTime(this.workActivityDurationsCar / this.workActivityCarCount));
			System.out.println("Average work activity duration for mode non-car: " + Time.writeTime(this.workActivityDurationsNonCar / this.workActivityNonCarCount));
			System.out.println();
			System.out.println("Marginal utility of home activity total: " + (6.0 * 12.0 ) / (((this.homeActivityDurationsNonCar + this.homeActivityDurationsCar)  / 3600.0) / (this.homeActivityNonCarCount + this.homeActivityCarCount)));
			System.out.println("Marginal utility of work activity total:" + (6.0 * 8.0 ) / (((this.workActivityDurationsCar + this.workActivityDurationsNonCar)  / 3600.0)  / (this.workActivityCarCount + this.workActivityNonCarCount)));

			System.out.println("Marginal utility of home activity car: " + (6.0 * 12.0 ) / ((this.homeActivityDurationsCar   / 3600.0) / this.homeActivityCarCount));
			System.out.println("Marginal utility of home activity non-car: " + (6.0 * 12.0 ) / ((this.homeActivityDurationsNonCar  / 3600.0)  / this.homeActivityNonCarCount));
			System.out.println("Marginal utility of work activity car: " + (6.0 * 8.0) / ((this.workActivityDurationsCar   / 3600.0) / this.workActivityCarCount));
			System.out.println("Marginal utiltiy of work activity non-car: " + (6.0 * 8.0)  / ((this.workActivityDurationsNonCar   / 3600.0) / this.workActivityNonCarCount));

//			writeChart(this.ptStartTimeMap, "ptArrivals.png");
			writeChart(this.carStartTimeMap, "carArrivals.png");

			System.out.println("finished");
		}

		private void writeChart(final IntegerCountMap<Double> map, final String name) {
			Set<Double> keys = map.keySet();
			SortedSet<Double> sortedKeys = new TreeSet<Double>(keys);
			double first = sortedKeys.first();
			double last = sortedKeys.last();
			int range = (int) (last - first);

			double [] timeSteps = new double[range];
			double [] arrivals = new double[range];
			String[] niceTimeSteps = new String[range];
			for (int i = 0; i < range; i++) {
				timeSteps[i] = first + i;
				niceTimeSteps[i] = Time.writeTime(first + i);
				if (map.containsKey(Double.valueOf(first + i)))
					arrivals[i] =  map.get(Double.valueOf(first + i));
				else {
					arrivals[i] = 0.0;
				}
			}

//			XYLineChart chart = new XYLineChart(name, "time", "arrivals");
//			chart.addSeries("pt arrivals", timeSteps, arrivals);
//			chart.saveAsPng(name, 1600, 1200);

			BarChart c2 = new BarChart(name, "time", "arrivals", niceTimeSteps);
			c2.addSeries(name, arrivals);
			c2.saveAsPng("bar" + name, 1200, 800);
			System.out.println("Written chart...");
		}

	}


	public static void main(final String[] args) {
		new EventModeActivityDurationAnalyser();

	}

}

/* *********************************************************************** *
 * project: org.matsim.*
 * LangeStreckeSzenario													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playgrounds.ssix;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.collections.Tuple;

/*
 * A class supposed to be used in conjonction with LangeStreckeSzenario.
 * See LangeStreckeSzenario for example.
 * 
 */

public class FundamentalDiagrams implements LinkLeaveEventHandler, LinkEnterEventHandler{
	
	private static final Logger log = Logger.getLogger(FundamentalDiagrams.class);
	
	private Scenario scenario;
	
	private Map<Double, Double> timeDens;
	private Map<Double, Double> timeVelo;
	private Map<Double, Double> timeFlow;
	private Map<Id, Double> personSpeed;
	private Map<Id, Double> personEnteringTime;
	private Map<Id, Double> personLeavingTime;
	
	private Id linkId;
	
	private int enteringCount;
	private int leavingCount;
	
	public FundamentalDiagrams(Scenario sc, Id linkId){
		this.scenario = sc;
		this.linkId = linkId;
		this.timeDens = new TreeMap<Double, Double>();
		this.timeVelo = new TreeMap<Double, Double>();
		this.timeFlow = new TreeMap<Double, Double>();
		this.personEnteringTime = new TreeMap<Id, Double>();
		this.personLeavingTime = new TreeMap<Id, Double>();
		this.personSpeed = new TreeMap<Id, Double>();
		this.enteringCount = 0;
		this.leavingCount = 0;
	}
	
	
	
	@Override
	public void reset(int iteration) {
		this.enteringCount = 0;
		this.leavingCount = 0;
		this.timeDens.clear();
		this.timeVelo.clear();
		this.timeFlow.clear();
		this.personSpeed.clear();
		this.personEnteringTime.clear();
		this.personLeavingTime.clear();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (event.getLinkId().equals(linkId)){
			enteringCount++;
			double linkLength = scenario.getNetwork().getLinks().get(linkId).getLength();
			double density = (enteringCount-leavingCount)/linkLength;//there are always more people that entered the link than left he link, so no need for Math.abs()
			timeDens.put(event.getTime(), density);
			personEnteringTime.put(event.getPersonId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (event.getLinkId().equals(linkId)){
			leavingCount++;
			double linkLength = scenario.getNetwork().getLinks().get(linkId).getLength();
			double enteringTime = personEnteringTime.get(event.getPersonId());
			double speed = linkLength/(event.getTime()-enteringTime);
			personSpeed.put(event.getPersonId(), speed);
			personLeavingTime.put(event.getPersonId(), event.getTime());
		}
	}
	
	public void saveAsPng(String dir){
		
		log.info("Saving charts for fundamental diagrams...");
		
		Map<Double, Tuple<Double, Double>> m = collectData();
		XYLineChart chart_v_k = new XYLineChart("Speed-Density Fundamental diagram", "density p/m", "velocity m/s");
		double [] xs = new double[m.size()];
		double [] ys = new double[m.size()];
		int pos = 0;
		Iterator<Double> it = m.keySet().iterator();
		while (it.hasNext()) {
			double speedKey = it.next();
			xs[pos] = m.get(speedKey).getFirst();
			ys[pos++] = speedKey;
		}
		chart_v_k.addSeries("model", xs, ys);
		chart_v_k.saveAsPng(dir + "/fnd_v_k.png", 800, 400);
		
		XYLineChart chart_q_v = new XYLineChart("Flow-Speed Fundamental diagram", "velocity m/s", "flow p/s");
		pos = 0;
		it = m.keySet().iterator();
		while (it.hasNext()) {
			double speedKey = it.next();
			xs[pos] = speedKey;
			ys[pos++] = m.get(speedKey).getSecond();
		}
		chart_q_v.addSeries("model", xs, ys);
		chart_q_v.saveAsPng(dir + "/fnd_q_v.png", 800, 400);
		
		XYLineChart chart_k_q = new XYLineChart("Flow-Density Fundamental diagram", "density p/m", "flow p/s");
		pos = 0;
		it = m.keySet().iterator();
		while (it.hasNext()) {
			double speedKey = it.next();
			xs[pos] = m.get(speedKey).getFirst();
			ys[pos++] = m.get(speedKey).getSecond();
		}
		chart_k_q.addSeries("model", xs, ys);
		chart_k_q.saveAsPng(dir + "/fnd_k_q.png", 800, 400);
		
		log.info("Done!");
	}
	
	private Map<Double, Tuple<Double, Double>> collectData() {
		
		Map<Double, Tuple<Double,Double>> ret = new TreeMap<Double, Tuple<Double,Double>>();
		
		//fill timeVelo: associating one unique velocity to each time with a density
		//Method 1 (used): for a density entry, taking the average speed of all people present on the link at that time
		//Method 2 (commented/imports necessary): taking the median of all speeds of all people present on the link at that time
		for (double time : timeDens.keySet()){
			///*
			int denom = 0;
			double sum = 0.;
			//*/
			/*
			List<Double> velo = new ArrayList<Double>();
			*/
			for (Id personId : personSpeed.keySet()){
				if ((personEnteringTime.get(personId)<=time) && (personLeavingTime.get(personId)>time)){
					//that means he was on the link at that time
					///*
					denom += 1;
					sum += personSpeed.get(personId);
					//*/
					/*
					velo.add(personSpeed.get(personId));
					*/
				}
			}
			///*
			timeVelo.put(time, sum/denom);
			timeFlow.put(time, sum/denom*timeDens.get(time));
			//*/
			//TODO: Is there a more intelligent and more direct way to compute this?
			//The problem is: how to compute space-related figures (speed, flow) with time-related figures (density)?
			/*
			Collections.sort(velo);
			timeVelo.put(time, velo.get(velo.size()/2));
			timeFlow.put(time, timeVelo.get(time)*timeDens.get(time));
			*/
		}
		
		//now for the data collecting part:
		for (Entry<Double, Double> flowEntry : this.timeFlow.entrySet()) {
			double flow = flowEntry.getValue();
			double speed = this.timeVelo.get(flowEntry.getKey());
			double dens = flow/speed;//NB: exactly the same as timeDens.get(flowEntry.getKey())
			Tuple<Double, Double> t = new Tuple<Double, Double>(dens, flow);
			ret.put(speed, t);
		}
		return ret;
	}

}


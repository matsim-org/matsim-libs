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

package playground.ssix;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.collections.Tuple;

public class FundamentalDiagrams implements LinkLeaveEventHandler, LinkEnterEventHandler{
	
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
		List<Tuple<Double, Double>> l = collectData();
		XYLineChart chart1 = new XYLineChart("Flow Fundamental diagram", "velocity m/s", "flow p/s");
		double [] xs = new double[l.size()];
		double [] ys = new double[l.size()];
		int pos = 0;
		for (Tuple<Double, Double> t : l) {

			xs[pos] = t.getFirst();
			ys[pos++] = t.getSecond();
		}

		chart1.addSeries("model", xs, ys);
		chart1.saveAsPng(dir + "/fndflow.png", 800, 400);
		
		XYLineChart chart2 = new XYLineChart("Density Fundamental diagram", "density p/m", "velocity m/s");
		//TODO:complete that function once the triplet structure has been found.
	}
	
	private List<Tuple<Double, Double>> collectData() {
		
		List<Tuple<Double,Double>> ret = new ArrayList<Tuple<Double,Double>>();//TODO:find a structure allowing use of triplets
		
		//fill timeVelo: associating one unique velocity to each time with a density
		for (double time : timeDens.keySet()){
			int denom = 0;
			double sum = 0.;//calculating the mean speed on the link at that time!
			for (Id personId : personSpeed.keySet()){
				if ((personEnteringTime.get(personId)<time) && (personLeavingTime.get(personId)>time)){
					//that means he was on the link at that time
					denom += 1;
					sum += personSpeed.get(personId);
				}
			}
			timeVelo.put(time, sum/denom);
			timeFlow.put(time, sum/denom*timeDens.get(time));
		}
		
		//now for the data collecting part:
		for (Entry<Double, Double> flowEntry : this.timeFlow.entrySet()) {
			double flow = flowEntry.getValue();
			double speed = this.timeVelo.get(flowEntry.getKey());
			Tuple<Double, Double> t = new Tuple<Double, Double>(speed, flow);
			ret.add(t);
		}
		return ret;
	}

}


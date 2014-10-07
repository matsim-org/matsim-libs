/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mmoyo.utils.calibration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.io.PopSecReader;
import playground.mmoyo.utils.DataLoader;
import playground.mmoyo.utils.ExpTransRouteUtils;
import playground.mmoyo.utils.Generic2ExpRouteConverter;

/**
 * This analysis counts the number of stops that the passenger traveled
 * along a given transit line
 */
public class StopNumberPerPassenger implements PersonAlgorithm{
	private Generic2ExpRouteConverter generic2ExpRouteConverter;
	private TransitSchedule schedule;
	private TransitLine line;
	private Network net;
	private Map <Id, List<StopNumRecord>> persId_stopNumRecList_Map = new TreeMap <Id, List<StopNumRecord>>();
	
	public StopNumberPerPassenger(final Network net, final TransitSchedule schedule, final TransitLine line){
		this.schedule = schedule;
		this.generic2ExpRouteConverter = new Generic2ExpRouteConverter(this.schedule);
		this.line = line;
		this.net = net;
	}
	
	@Override
	public void run(Person person) {
		persId_stopNumRecList_Map.put(person.getId(), new ArrayList<StopNumRecord>());
		int x= 0;
		
		for(Plan plan : person.getPlans()){
			int stopNumber=0;
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Leg){
					Leg leg = (Leg)pe;
					if (leg.getMode().equals(TransportMode.pt) && leg.getRoute()!=null){
						ExperimentalTransitRoute expRoute = this.generic2ExpRouteConverter.convert((GenericRouteImpl) leg.getRoute());
						if (expRoute.getLineId().equals(line.getId())){
							ExpTransRouteUtils ptRouteUtill = new ExpTransRouteUtils(net, schedule, expRoute);
							int stopsN = ptRouteUtill.getEgressStopIndex() - ptRouteUtill.getAccessStopIndex();
							stopNumber += stopsN;
						}
					}
				}
			}
			x++;
			StopNumRecord stopNumRecord = new StopNumRecord( x, stopNumber, plan.isSelected());
			persId_stopNumRecList_Map.get(person.getId()).add(stopNumRecord);
		}
	}
	
	public Map <Id, List<StopNumRecord>> get_persId_stopNumRecList_Map(){
		return this.persId_stopNumRecList_Map;
	}
	
	public class StopNumRecord{
		private int planIndex;
		private int stopsNum;
		private boolean isSelected;

		public StopNumRecord(int planIndex, int stopsNum, boolean isSelected) {
			this.planIndex = planIndex;
			this.stopsNum = stopsNum;
			this.isSelected = isSelected;
		}
		
		public int getPlanIndex() {
			return planIndex;
		}
		public int getStopsNum() {
			return stopsNum;
		}
		public boolean isSelected() {
			return isSelected;
		}
	}

	private void printOutput(){
		String sp = " ";
		String nl = "\n";
		String s = "s";
		String empty = "";
		StringBuffer sBuff = new StringBuffer();
		
		for(Map.Entry <Id, List<StopNumRecord> > entry: persId_stopNumRecList_Map.entrySet() ){
			Id personId = entry.getKey(); 
			List<StopNumRecord> stopNumRecordList = entry.getValue();

			sBuff.append(nl);
			
			for(StopNumRecord snRec : stopNumRecordList ){
				snRec.getPlanIndex();
				snRec.getStopsNum();
				snRec.isSelected();
				String sel = snRec.isSelected()? s : empty ;
				sBuff.append(personId +  sp + snRec.getPlanIndex() + sp + snRec.getStopsNum() + sp + sel + nl);
			}
		}
		System.out.println(	sBuff.toString());
	}
	
	public static void main(String[] args) {
		String popFile = "../../runs_manuel/CalibLineM44/automCalib10xTimeMutated/10xrun/it.500/500.plans.xml.gz";
		String scheduleFile = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
		String netFile = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		String strLineId = "B-M44";
		
		DataLoader dataLoader = new DataLoader ();
		ScenarioImpl scn = (ScenarioImpl) dataLoader.createScenario(); 
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scn);
		matsimNetReader.readFile(netFile);
		TransitSchedule schedule = dataLoader.readTransitSchedule(scheduleFile);
		TransitLine line = schedule.getTransitLines().get(Id.create(strLineId, TransitLine.class));
		StopNumberPerPassenger stopNumberPerPassenger = new StopNumberPerPassenger(scn.getNetwork(), schedule, line);
		
		new PopSecReader(scn, stopNumberPerPassenger).readFile(popFile);
		stopNumberPerPassenger.printOutput();
		
	}
	
}
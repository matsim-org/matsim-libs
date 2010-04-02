/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.Analysis.SignalSystems;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.jfree.chart.JFreeChart;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.SignalGroupStateChangedEvent;
import org.matsim.core.events.handler.SignalGroupStateChangedEventHandler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.signalsystems.control.SignalGroupState;

import playground.droeder.DaPaths;
import playground.droeder.charts.DaChartWriter;
import playground.droeder.charts.DaSignalPlanChart;


/**
 * @author droeder
 *
 */
public class SignalGroupStateTimeHandler implements
		SignalGroupStateChangedEventHandler {
	
	
	private static final Logger log = Logger
			.getLogger(SignalGroupStateTimeHandler.class);
	Map<Id, TreeMap<Id, TreeMap<Double, SignalGroupState>>> systemGroupTimeState = new TreeMap<Id, TreeMap<Id,TreeMap<Double,SignalGroupState>>>();
	
	TreeMap<Double, SignalGroupState> temp;
	

	@Override
	public void handleEvent(SignalGroupStateChangedEvent e) {
		if(systemGroupTimeState.containsKey(e.getSignalSystemId())){
			if (systemGroupTimeState.get(e.getSignalSystemId()).containsKey(e.getSignalGroupId())){
				temp = systemGroupTimeState.get(e.getSignalSystemId()).get(e.getSignalGroupId());
				temp.put(e.getTime(), e.getNewState());
			}else{
				systemGroupTimeState.get(e.getSignalSystemId()).put(e.getSignalGroupId(), new TreeMap<Double, SignalGroupState>());
				temp = systemGroupTimeState.get(e.getSignalSystemId()).get(e.getSignalGroupId());
				temp.put(e.getTime(), e.getNewState());
			}
		}else{
			systemGroupTimeState.put(e.getSignalSystemId(), new TreeMap<Id, TreeMap<Double,SignalGroupState>>());
			systemGroupTimeState.get(e.getSignalSystemId()).put(e.getSignalGroupId(), new TreeMap<Double, SignalGroupState>());
			temp = systemGroupTimeState.get(e.getSignalSystemId()).get(e.getSignalGroupId());
			temp.put(e.getTime(), e.getNewState());
		}
		
//		if (idTimeState.containsKey(e.getSignalGroupId())){
//			temp = idTimeState.get(e.getSignalGroupId());
//			temp.put(e.getTime(), e.getNewState());
//		}else{
//			idTimeState.put(e.getSignalGroupId(), new TreeMap<Double, SignalGroupState>());
//			temp = idTimeState.get(e.getSignalGroupId());
//			temp.put(e.getTime(), e.getNewState());
//		}
	}
	
	
	public void writeToTxt (String fileName){
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
			
			for(Entry<Id, TreeMap<Id, TreeMap<Double, SignalGroupState>>> ee : systemGroupTimeState.entrySet()){
				writer.write("signalSystemId" + "\t" + ee.getKey());
				writer.newLine();
				for(Entry<Id, TreeMap<Double, SignalGroupState>> e: ee.getValue().entrySet()){
					writer.write("signalGroupId" + "\t" + e.getKey());
					writer.newLine();
					for (Entry<Double, SignalGroupState> eee : e.getValue().entrySet()){
						writer.write(eee.getKey() +"\t");
					}
					writer.newLine();
					for (Entry<Double, SignalGroupState> eee : e.getValue().entrySet()){
						writer.write(eee.getValue() +"\t");
					}
					writer.newLine();
					writer.newLine();
				}
			}
			
			writer.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Map<Id, TreeMap<Id, TreeMap<Double, SignalGroupState>>> getSystemGroupTimeStateMap(){
		return this.systemGroupTimeState;
	}
	
	@Override
	public void reset(int iteration) {

	}

}

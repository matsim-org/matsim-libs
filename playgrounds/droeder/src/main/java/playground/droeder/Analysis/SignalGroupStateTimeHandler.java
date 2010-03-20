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
package playground.droeder.Analysis;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.SignalGroupStateChangedEvent;
import org.matsim.core.events.handler.SignalGroupStateChangedEventHandler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.signalsystems.control.SignalGroupState;
import org.matsim.signalsystems.systems.SignalGroupDefinition;


/**
 * @author droeder
 *
 */
public class SignalGroupStateTimeHandler implements
		SignalGroupStateChangedEventHandler {
	
	Map <Id, TreeMap<Double, Double>> idGreenStateTime = new TreeMap<Id, TreeMap<Double, Double>>();
	
	public void init(Map<Id, SignalGroupDefinition> groups){
		for (SignalGroupDefinition s: groups.values()){
			idGreenStateTime.put(s.getId(), new TreeMap<Double, Double>());
		}
	}

	@Override
	public void handleEvent(SignalGroupStateChangedEvent e) {
		TreeMap<Double, Double> temp1 = idGreenStateTime.get(e.getSignalGroupId());
		if(e.getNewState().equals(SignalGroupState.GREEN)){
			temp1.put(((int)(e.getTime()*100.00))/100.00, null);
		}else if (e.getNewState().equals(SignalGroupState.RED)){
			temp1.lastEntry().setValue(((int)(e.getTime()*100.00))/100.00);
		}
		idGreenStateTime.put(e.getSignalGroupId(), temp1);

	}
	
	public void writeToTxt (String fileName){
		System.out.println(idGreenStateTime.toString());
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
			for(Entry<Id, TreeMap<Double, Double>> e: idGreenStateTime.entrySet()){
				writer.write("id" + "\t" + e.getKey());
				writer.newLine();
				for (Entry<Double, Double> ee : e.getValue().entrySet()){
					writer.write(ee.getKey() +"\t");
				}
				writer.newLine();
				for (Entry<Double, Double> ee : e.getValue().entrySet()){
					writer.write(ee.getValue() +"\t");
				}
				writer.newLine();
				writer.newLine();
			}
			
			writer.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void reset(int iteration) {

	}

}

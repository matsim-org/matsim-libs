/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractSignalSystemController
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jbischoff.BAsignals;

import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.signalsystems.MatsimSignalSystemsReader;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsDataImpl;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsWriter20;
import org.matsim.signalsystems.systems.SignalGroupDefinition;
import org.matsim.signalsystems.systems.SignalSystemDefinition;
import org.matsim.signalsystems.systems.SignalSystems;
import org.matsim.signalsystems.systems.SignalSystemsImpl;

public class JBSignalSystemConverter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SignalSystems oldsigs = new SignalSystemsImpl();
		MatsimSignalSystemsReader ssr = new MatsimSignalSystemsReader(oldsigs);
		ssr.readFile("/Users/JB/Documents/Work/cottbus/signalSystemsByNodes.xml");
		SignalSystemsData ssd = new SignalSystemsDataImpl();
		
		
		
		for(Entry<Id,SignalGroupDefinition> sg:	oldsigs.getSignalGroupDefinitions().entrySet()){
			//if (sg.getValue().getId()
			System.out.println(sg.getValue().getLinkRefId());
		}

			
		
		
	}

}

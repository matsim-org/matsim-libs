/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationConverterV4ToV5
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.core.utils.io;

import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.LaneDefinitionsImpl;
import org.matsim.lanes.MatsimLaneDefinitionsWriter;
import org.matsim.signalsystems.MatsimSignalSystemConfigurationsReader;
import org.matsim.signalsystems.MatsimSignalSystemConfigurationsWriter;
import org.matsim.signalsystems.MatsimSignalSystemsReader;
import org.matsim.signalsystems.MatsimSignalSystemsWriter;
import org.matsim.signalsystems.config.SignalSystemConfigurations;
import org.matsim.signalsystems.config.SignalSystemConfigurationsImpl;
import org.matsim.signalsystems.systems.SignalSystems;
import org.matsim.signalsystems.systems.SignalSystemsImpl;

/**
 * This class provides helper methods to convert the
 * different MATSim XML versions to other versions.
 *
 * @author dgrether
 *
 */
public class MatsimXMLFormatConverter {

	

	
	/**
	 * Converts a signalsystemdefinition v1.0 to a signalsystemdefinition in the v1.1 version.
	 * As the v1.0 version also includes lane definitions an additional output path for the
	 * separate lane definitions in file format version 1.1 is needed.
	 * @param signalsv10 input path of the v1.0 signal system definition
	 * @param signalsv11 output path for the signal systems v.1.1
	 * @param lanesv11 output path for the lane definitions v.1.1
	 */
	public static void convertSignalSystemsV10ToV11(String signalsv10, String signalsv11, String lanesv11){
		//create containers
		LaneDefinitions lanedefs = new LaneDefinitionsImpl();
		SignalSystems signalSystems = new SignalSystemsImpl();
		//read old format
		MatsimSignalSystemsReader reader = new MatsimSignalSystemsReader(lanedefs, signalSystems);
		reader.readFile(signalsv10);
		//write new formats
		MatsimLaneDefinitionsWriter laneWriter = new MatsimLaneDefinitionsWriter(lanedefs);
		laneWriter.writeFile(lanesv11);
		MatsimSignalSystemsWriter writer = new MatsimSignalSystemsWriter(signalSystems);
		writer.writeFile(signalsv11);
		
	}
	
	/**
	 * Converts a signalsystemconfigurations v1.0 to a signalsystemconfigurations in the v1.1 version.
	 */
	public static void convertSignalSystemsV10ToV11(String signalsystemconfigsv10, String signalsystemconfigsv11){
		//create containers
		SignalSystemConfigurations signalSystemConfigs = new SignalSystemConfigurationsImpl();
		//read old format
		MatsimSignalSystemConfigurationsReader reader = new MatsimSignalSystemConfigurationsReader(signalSystemConfigs);
		reader.readFile(signalsystemconfigsv10);
		//write new formats
		MatsimSignalSystemConfigurationsWriter writer = new MatsimSignalSystemConfigurationsWriter(signalSystemConfigs);
		writer.writeFile(signalsystemconfigsv11);
		
	}
	
	
}

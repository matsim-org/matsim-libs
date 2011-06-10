/* *********************************************************************** *
 * project: org.matsim.*
 * CottbusUtils
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.cottbus;

import java.util.Set;

import org.geotools.feature.Feature;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.ConfigUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * @author dgrether
 *
 */
public class CottbusUtils {

	
	public static ScenarioImpl loadCottbusScenrio(boolean fixedTimeSignals){
		Config c2 = ConfigUtils.createConfig();
		c2.scenario().setUseLanes(true);
		c2.scenario().setUseSignalSystems(true);
		c2.network().setInputFile(DgCottbusScenarioPaths.NETWORK_FILENAME);
		c2.network().setLaneDefinitionsFile(DgCottbusScenarioPaths.LANES_FILENAME);
		c2.signalSystems().setSignalSystemFile(DgCottbusScenarioPaths.SIGNALS_FILENAME);
		c2.signalSystems().setSignalGroupsFile(DgCottbusScenarioPaths.SIGNAL_GROUPS_FILENAME);
		if (fixedTimeSignals){
			c2.signalSystems().setSignalControlFile(DgCottbusScenarioPaths.SIGNAL_CONTROL_FIXEDTIME_FILENAME);
		}
		else {
			c2.signalSystems().setSignalControlFile(DgCottbusScenarioPaths.SIGNAL_CONTROL_SYLVIA_FILENAME);
		}
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.loadScenario(c2);
		return sc;
	}
	
	
	public static Tuple<CoordinateReferenceSystem, Feature> loadCottbusFeature(String shapeFile) {
		ShapeFileReader shapeReader = new ShapeFileReader();
		Set<Feature> features;
		features = shapeReader.readFileAndInitialize(shapeFile);
		CoordinateReferenceSystem crs = shapeReader.getCoordinateSystem();
		for (Feature feature : features) {
			if (feature.getAttribute("NAME").equals("Cottbus")){
				return new Tuple<CoordinateReferenceSystem, Feature>(crs, feature);
			}
		}
		return null;
	}

	
}

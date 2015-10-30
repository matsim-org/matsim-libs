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

import java.util.Collection;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * @author dgrether
 */
public class CottbusUtils {

	public static MutableScenario loadCottbusScenrio(boolean fixedTimeSignals){
		Config c2 = ConfigUtils.createConfig();
		c2.qsim().setUseLanes(true);
		ConfigUtils.addOrGetModule(c2, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setUseSignalSystems(true);
		c2.network().setInputFile(DgCottbusScenarioPaths.NETWORK_FILENAME);
		c2.network().setLaneDefinitionsFile(DgCottbusScenarioPaths.LANES_FILENAME);
		ConfigUtils.addOrGetModule(c2, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setSignalSystemFile(DgCottbusScenarioPaths.SIGNALS_FILENAME);
		ConfigUtils.addOrGetModule(c2, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setSignalGroupsFile(DgCottbusScenarioPaths.SIGNAL_GROUPS_FILENAME);
		if (fixedTimeSignals){
			ConfigUtils.addOrGetModule(c2, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setSignalControlFile(DgCottbusScenarioPaths.SIGNAL_CONTROL_FIXEDTIME_FILENAME);
		}
		else {
			ConfigUtils.addOrGetModule(c2, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setSignalControlFile(DgCottbusScenarioPaths.SIGNAL_CONTROL_SYLVIA_FILENAME);
		}
		MutableScenario sc = (MutableScenario) ScenarioUtils.loadScenario(c2);
		return sc;
	}
	
	public static Tuple<CoordinateReferenceSystem, SimpleFeature> loadCottbusFeature(String shapeFile) {
		ShapeFileReader shapeReader = new ShapeFileReader();
		Collection<SimpleFeature> features = shapeReader.readFileAndInitialize(shapeFile);
		CoordinateReferenceSystem crs = shapeReader.getCoordinateSystem();
		for (SimpleFeature feature : features) {
			if (feature.getAttribute("NAME").equals("Cottbus")){
				return new Tuple<CoordinateReferenceSystem, SimpleFeature>(crs, feature);
			}
		}
		return null;
	}

	public static Tuple<CoordinateReferenceSystem, SimpleFeature> loadFeature(String shapeFile) {
		ShapeFileReader shapeReader = new ShapeFileReader();
		Collection<SimpleFeature> features = shapeReader.readFileAndInitialize(shapeFile);
		CoordinateReferenceSystem crs = shapeReader.getCoordinateSystem();
		SimpleFeature feature = features.iterator().next();
		return new Tuple<CoordinateReferenceSystem, SimpleFeature>(crs, feature);
	}

	
}

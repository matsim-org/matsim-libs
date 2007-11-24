/* *********************************************************************** *
 * project: org.matsim.*
 * ConfigReaderMatsimXml_v1.java
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

package playground.marcel.config;

import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.gbl.Gbl;
import org.matsim.mobsim.Simulation;
import org.matsim.scoring.CharyparNagelScoringFunction;
import org.matsim.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import playground.marcel.config.groups.EventsConfigGroup;
import playground.marcel.config.groups.FacilitiesConfigGroup;
import playground.marcel.config.groups.GlobalConfigGroup;
import playground.marcel.config.groups.MatricesConfigGroup;
import playground.marcel.config.groups.NetworkConfigGroup;
import playground.marcel.config.groups.PlansConfigGroup;
import playground.marcel.config.groups.ScoringConfigGroup;
import playground.marcel.config.groups.SimulationConfigGroup;
import playground.marcel.config.groups.StrategiesConfigGroup;
import playground.marcel.config.groups.WorldConfigGroup;
import playground.marcel.config.groups.StrategiesConfigGroup.StrategySettings;

public class ConfigReaderMatsimXml_v1 extends MatsimXmlParser {

	public static final String XML_ROOT = "config";
	public static final String XML_MODULE = "module";
	public static final String XML_PARAM = "param";
	public static final String XML_NAME = "name";
	public static final String XML_VALUE = "value";

	private static final String CONFIG = "config";

	private static final String GBL = "global";
	private static final String GBL_SEED = "randomSeed";
	private static final String GBL_OUTTIMEFORMAT = "outputTimeFormat";
	private static final String GBL_LOCAL_DTD_BASE = "localDTDBase";

	private static final String WORLD = "world";
	private static final String WORLD_INFILE = "inputWorldFile";
	private static final String WORLD_OUTVERSION = "outputVersion";
	private static final String WORLD_OUTFILE = "outputWorldFile";

	private static final String NETWORK = "network";
	private static final String NETWORK_INFILE = "inputNetworkFile";
	private static final String NETWORK_OUTVERSION = "outputVersion";
	private static final String NETWORK_OUTFILE = "outputNetworkFile";

	private static final String FACILITIES = "facilities";
	private static final String FACILITIES_INFILE = "inputFacilitiesFile";
	private static final String FACILITIES_OUTVERSION = "outputVersion";
	private static final String FACILITIES_OUTFILE = "outputFacilitiesFile";

	private static final String MATRICES = "matrices";
	private static final String MATRICES_INFILE = "inputMatricesFile";
	private static final String MATRICES_OUTVERSION = "outputVersion";
	private static final String MATRICES_OUTFILE = "outputMatricesFile";

	private static final String PLANS = "plans";
	private static final String PLANS_INFILE = "inputPlansFile";
	private static final String PLANS_OUTVERSION = "outputVersion";
	private static final String PLANS_OUTFILE = "outputPlansFile";
	private static final String PLANS_OUTSAMPLE = "outputSample";
	private static final String PLANS_SWITCHOFFSTREAMING = "switchOffPlansStreaming";
	private static final String NO = "no";

	private static final String EVENTS = "events";
	private static final String EVENTS_INFILE = "inputFile";
	private static final String EVENTS_OUTFILE = "outputFile";
	private static final String EVENTS_OUTFORMAT = "outputFormat";

	private static final String STRATEGY = "strategy";
	private static final String PLANCALCSCORE = "planCalcScore";

	private Config config = null;
	private Stack<ConfigGroupI> groups = null;
	private String currentModuleName = null;

	public ConfigReaderMatsimXml_v1(final Config config) {
		super();
		this.config = config;
		this.groups = new Stack<ConfigGroupI>();
	}

	public void readfile(final String filename) throws SAXException, ParserConfigurationException, IOException {
		this.parse(filename);
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		System.out.println("start " + name);
		if (XML_MODULE.equals(name)) {
			this.currentModuleName = name;
			ConfigGroupI group = this.config.getGroup(atts.getValue(XML_NAME));
			if (group == null) {
				throw new IllegalArgumentException("group " + atts.getValue(XML_NAME) + " is unknown!");
			}
			this.groups.push(group);
		} else if (XML_PARAM.equals(name)) {
			handleParam(this.currentModuleName, name, atts.getValue(XML_VALUE));
		} else if (XML_ROOT.equals(name)) {
		} else {
			Gbl.errorMsg(this + "[tag=" + name + " not known]");
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		System.out.println("end " + name);
		if (XML_MODULE.equals(name)) {
			this.groups.pop();
			this.currentModuleName = null;
		}
	}

	private void handleParam(final String moduleName, final String paramName, final String value) {
		if (CONFIG.equals(moduleName)) {

		} else if (GBL.equals(moduleName)) {
			handleGblParam(paramName, value);
		} else if (WORLD.equals(moduleName)) {
			handleWorldParam(paramName, value);
		} else if (NETWORK.equals(moduleName)) {
			handleNetworkParam(paramName, value);
		} else if (FACILITIES.equals(moduleName)) {
			handleFacilitiesParam(paramName, value);
		} else if (MATRICES.equals(moduleName)) {
			handleMatricesParam(paramName, value);
		} else if (PLANS.equals(moduleName)) {
			handlePlansParam(paramName, value);
		} else if (EVENTS.equals(moduleName)) {
			handleEventsParam(paramName, value);
		} else if ("simulation".equals(moduleName)) {
			handleSimulationParam(paramName, value);
		} else if (STRATEGY.equals(moduleName)) {
			handleStrategyParam(paramName, value);
		} else if (PLANCALCSCORE.equals(moduleName)) {
			handleScoringParam(paramName, value);
		} else {

		}
	}

	private void handleGblParam(final String paramName, final String value) {
		GlobalConfigGroup group = this.config.global();
		if (GBL_SEED.equals(paramName)) {
			group.setRandomSeed(Long.parseLong(value));
		} else if (GBL_OUTTIMEFORMAT.equals(paramName)) {
			group.setTimeFormat(value);
		} else if (GBL_LOCAL_DTD_BASE.equals(paramName)) {
			group.setLocalDtdBase(value);
		} else {
			System.err.println("unknown gbl-param " + paramName);
		}
	}

	private void handleWorldParam(final String paramName, final String value) {
		WorldConfigGroup group = this.config.world();
		if (WORLD_INFILE.equals(paramName)) {
			group.setInputFile(value);
		} else if (WORLD_OUTFILE.equals(paramName)) {
			group.setOutputFile(value);
		} else if (WORLD_OUTVERSION.equals(paramName)) {
			group.setOutputFormat(value);
		} else {
			System.err.println("unknown world-param " + paramName);
		}
	}

	private void handleNetworkParam(final String paramName, final String value) {
		NetworkConfigGroup group = this.config.network();
		if (NETWORK_INFILE.equals(paramName)) {
			group.setInputFile(value);
		} else if (NETWORK_OUTFILE.equals(paramName)) {
			group.setOutputFile(value);
		} else if (NETWORK_OUTVERSION.equals(paramName)) {
			group.setOutputFormat(value);
		} else {
			System.err.println("unknown network-param " + paramName);
		}
	}

	private void handleFacilitiesParam(final String paramName, final String value) {
		FacilitiesConfigGroup group = this.config.facilities();
		if (FACILITIES_INFILE.equals(paramName)) {
			group.setInputFile(value);
		} else if (FACILITIES_OUTFILE.equals(paramName)) {
			group.setOutputFile(value);
		} else if (FACILITIES_OUTVERSION.equals(paramName)) {
			group.setOutputFormat(value);
		} else {
			System.err.println("unknown facilities-param " + paramName);
		}
	}

	private void handleMatricesParam(final String paramName, final String value) {
		MatricesConfigGroup group = this.config.matrices();
		if (MATRICES_INFILE.equals(paramName)) {
			group.setInputFile(value);
		} else if (MATRICES_OUTFILE.equals(paramName)) {
			group.setOutputFile(value);
		} else if (MATRICES_OUTVERSION.equals(paramName)) {
			group.setOutputFormat(value);
		} else {
			System.err.println("unknown matrices-param " + paramName);
		}
	}

	private void handlePlansParam(final String paramName, final String value) {
		PlansConfigGroup group = this.config.plans();
		if (PLANS_INFILE.equals(paramName)) {
			group.setInputFile(value);
		} else if (PLANS_OUTFILE.equals(paramName)) {
			group.setOutputFile(value);
		} else if (PLANS_OUTVERSION.equals(paramName)) {
			group.setOutputFormat(value);
		} else if (PLANS_OUTSAMPLE.equals(paramName)) {
			group.setOutputSample(Double.parseDouble(value));
		} else if (PLANS_SWITCHOFFSTREAMING.equals(paramName)) {
			group.usePlansStreaming(NO.equals(value));
		} else {
			System.err.println("unknown plans-param " + paramName);
		}
	}

	private void handleEventsParam(final String paramName, final String value) {
		EventsConfigGroup group = this.config.events();
		if (EVENTS_INFILE.equals(paramName)) {
			group.setInputFile(value);
		} else if (EVENTS_OUTFILE.equals(paramName)) {
			group.setOutputFile(value);
		} else if (EVENTS_OUTFORMAT.equals(paramName)) {
			group.setOutputFormat(value);
		} else {
			System.err.println("unknown events-param " + paramName);
		}
	}

	private void handleSimulationParam(final String paramName, final String value) {
		SimulationConfigGroup group = this.config.simulation();
		if (Simulation.STARTTIME.equals(paramName)) {
			group.setStartTime(Gbl.parseTime(value));
		} else if (Simulation.ENDTIME.equals(paramName)) {
			group.setEndTime(Gbl.parseTime(value));
		} else if ("snapshotperiod".equals(paramName)) {
			group.setSnapshotPeriod(Gbl.parseTime(value));
		} else if ("snapshotFormat".equals(paramName)) {
			group.setSnapshotFormat(value);
		} else if ("snapshotperiod".equals(paramName)) {
			group.setFlowCapFactor(Double.parseDouble(value));
		} else if ("storageCapacityFactor".equals(paramName)) {
			group.setStorageCapFactor(Double.parseDouble(value));
		} else if ("stuckTime".equals(value)) {
			group.setStuckTime(Double.parseDouble(value));
		} else if ("removeStuckVehicles".equals(paramName)) {
			group.removeStuckVehicles("yes".equals(value));
		} else {
			System.err.println("unknown simulation-param: " + paramName);
		}
	}

	private void handleStrategyParam(final String paramName, final String value) {
		StrategiesConfigGroup group = this.config.strategies();
		if ("maxAgentPlanMemorySize".equals(paramName)) {
			group.setMaxAgentPlanMemorySize(Integer.parseInt(value));
		} else if ("ExternalExeConfigTemplate".equals(paramName)) {
			// TODO
		} else if ("ExternalExeTmpFileRootDir".equals(paramName)) {
			// TODO
		} else if (paramName.contains("_")) {
			String[] parts = paramName.split("_");
			String name = parts[0];
			String id = parts[1];
			StrategySettings settings = group.getStrategy(id);
			if (settings == null) {
				settings = group.addStrategy(id);
			}
			if ("Module".equals(name)) {
				// TODO
			} else if ("ModuleProbability".equals(name)) {
				settings.setWeight(Double.parseDouble(value));
			} else if ("ModuleDisableAfterIteration".equals(name)) {
				// TODO
			}
		}
	}

	private void handleScoringParam(final String paramName, final String value) {
		ScoringConfigGroup group = this.config.scoring();
		if (CharyparNagelScoringFunction.CONFIG_LEARNINGRATE.equals(paramName)) {
			group.setLearningRate(Double.parseDouble(value));
		} else if (true) {
			// TODO
		}
	}

}

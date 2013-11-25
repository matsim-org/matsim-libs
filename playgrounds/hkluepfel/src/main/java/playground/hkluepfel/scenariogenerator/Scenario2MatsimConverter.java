/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioGenerator.java
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
package playground.hkluepfel.scenariogenerator;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.IllegalAttributeException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.grips.control.algorithms.FeatureTransformer;
import org.matsim.contrib.grips.experimental.CustomizedOsmNetworkReader;
import org.matsim.contrib.grips.io.GripsConfigDeserializer;
import org.matsim.contrib.grips.model.config.GripsConfigModule;
import org.matsim.contrib.grips.model.events.InfoEvent;
import org.matsim.contrib.grips.scenariogenerator.EvacuationNetworkGenerator;
import org.matsim.contrib.grips.scenariogenerator.PopulationFromESRIShapeFileGenerator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.utils.gis.matsim2esri.network.CapacityBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.LanesBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.LineStringBasedFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.PolygonFeatureGenerator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Grips scenario2matsim converter
 * Workflow:
 * grips.xml+area.shp+population.shp --> ScenarioGenertor --> MATSim Szenario
 * - Wo wird entschied ob 10% oder 100% Scenario erzeugt wird?
 * 
 * @author laemmel
 *
 */
@SuppressWarnings("deprecation")
public class Scenario2MatsimConverter {

	private static final Logger log = Logger.getLogger(Scenario2MatsimConverter.class);
	protected static final boolean DEBUG = false;
	protected final String configFile;
	protected String matsimConfigFile;
	protected Id safeLinkId;
	protected final EventsManager eventsmanager;
	protected Config config;
	protected Scenario scenario;

	public Scenario2MatsimConverter(String config) {
		this.eventsmanager = EventsUtils.createEventsManager();
		this.configFile = config;
	}

	public Scenario2MatsimConverter(String config, EventHandler handler) {
		this.eventsmanager = EventsUtils.createEventsManager();
		this.eventsmanager.addHandler(handler);
		this.configFile = config;
	}

	public void run() {
		log.info("loading config file");
		InfoEvent e = new InfoEvent(System.currentTimeMillis(), "loading config file");
		this.eventsmanager.processEvent(e);

		try {
			this.config = ConfigUtils.createConfig();
			GripsConfigModule gcm = new GripsConfigModule("grips");
			this.config.addModule(gcm);

			this.config.global().setCoordinateSystem("EPSG:3395");

			GripsConfigDeserializer parser = new GripsConfigDeserializer(gcm);
			parser.readFile(this.configFile);
		} catch(Exception ee) {
			//TODO for backwards compatibility should be remove soon
			log.warn("File is not a  grips config file. Guessing it is a common MATSim config file");
			this.config = ConfigUtils.loadConfig(this.configFile);

		}

		this.config.global().setCoordinateSystem("EPSG:3395");
		this.config.controler().setOutputDirectory(getGripsConfig(this.config).getOutputDir()+"/output");

		
		QSimConfigGroup qsim = this.config.qsim();
		qsim.setEndTime(30*3600);
		
		this.config.timeAllocationMutator().setMutationRange(0.);

		this.scenario = ScenarioUtils.createScenario(this.config);
		this.safeLinkId = this.scenario.createId("el1");

		GripsConfigModule gcm = getGripsConfig(this.scenario.getConfig());
		
		// Module scg = this.sc.getConfig().getModule(SimulationConfigGroup.GROUP_NAME);
		double samplesize = gcm.getSampleSize();
		qsim.setFlowCapFactor(samplesize);
		qsim.setStorageCapFactor(samplesize);
		qsim.setRemoveStuckVehicles(false);

		String outputDir = gcm.getOutputDir();
		File outputDirFile = new File(outputDir);
		if (!outputDirFile.exists()) {
			outputDirFile.mkdirs();
		}

		log.info("generating network file");
		e = new InfoEvent(System.currentTimeMillis(), "generating network file");
		this.eventsmanager.processEvent(e);
		generateAndSaveNetwork(this.scenario);
		if (DEBUG) {
			dumpNetworkAsShapeFile(this.scenario);
		}

		log.info("generating population file");
		e = new InfoEvent(System.currentTimeMillis(), "generating population file");
		this.eventsmanager.processEvent(e);
		generateAndSavePopulation(this.scenario);

		log.info("saving simulation config file");
		e = new InfoEvent(System.currentTimeMillis(), "simulation config file");
		this.eventsmanager.processEvent(e);

		this.config.otfVis().setMapOverlayMode(true);

		this.config.controler().setLastIteration(10);
		this.config.controler().setOutputDirectory(getGripsConfig(this.config).getOutputDir()+"/output");

		this.config.strategy().setMaxAgentPlanMemorySize(3);

		this.config.strategy().addParam("ModuleDisableAfterIteration_1", "75");
		this.config.strategy().addParam("maxAgentPlanMemorySize", "3");
		this.config.strategy().addParam("Module_1", "ReRoute");
		this.config.strategy().addParam("ModuleProbability_1", "0.1");
		this.config.strategy().addParam("Module_2", "ChangeExpBeta");
		this.config.strategy().addParam("ModuleProbability_2", "0.9");

		this.config.travelTimeCalculator().setTraveltimeBinSize(120);
		this.config.travelTimeCalculator().setTravelTimeCalculatorType("TravelTimeCalculatorHashMap");

		this.matsimConfigFile = getGripsConfig(this.config).getOutputDir() + "/config.xml";

		new ConfigWriter(this.config).write(this.matsimConfigFile);
		e = new InfoEvent(System.currentTimeMillis(), "scenario generation finished.");
		this.eventsmanager.processEvent(e);

	}


	protected void dumpNetworkAsShapeFile(Scenario sc) {

		final Network network = sc.getNetwork();

		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(network, sc.getConfig().global().getCoordinateSystem());
		builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
		builder.setWidthCoefficient(0.5);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
		new Links2ESRIShape(network,getGripsConfig(this.config).getOutputDir()+"/links_ls.shp", builder).write();

		CoordinateReferenceSystem crs = MGC.getCRS(sc.getConfig().global().getCoordinateSystem());
		builder.setWidthCoefficient(0.003);
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(CapacityBasedWidthCalculator.class);
		builder.setCoordinateReferenceSystem(crs);
		new Links2ESRIShape(network,getGripsConfig(this.config).getOutputDir()+"/links_p.shp", builder).write();

	}

	@SuppressWarnings("unused")
	private void generateAndSaveNetworkChangeEvents(Scenario sc) {
		throw new RuntimeException("This has to be done during network generation. The reason is that at this stage the mapping between original link ids (e.g. from osm) to generated matsim link ids is forgotten!");

	}

	protected void generateAndSavePopulation(Scenario matsimscenario) {
		// for now a simple ESRI shape file format is used to emulated the a more sophisticated not yet defined population meta format
		GripsConfigModule gcm = getGripsConfig(matsimscenario.getConfig());
		String gripsPopulationFile = gcm.getPopulationFileName();
		new PopulationFromESRIShapeFileGenerator(matsimscenario, gripsPopulationFile, this.safeLinkId).run();

		String outputPopulationFile = gcm.getOutputDir() + "/population.xml.gz";
		new PopulationWriter(matsimscenario.getPopulation(), matsimscenario.getNetwork(), gcm.getSampleSize()).write(outputPopulationFile);
		matsimscenario.getConfig().plans().setInputFile(outputPopulationFile);

		ActivityParams pre = new ActivityParams("pre-evac");
		pre.setTypicalDuration(49); // needs to be geq 49, otherwise when running a simulation one gets "java.lang.RuntimeException: zeroUtilityDuration of type pre-evac must be greater than 0.0. Did you forget to specify the typicalDuration?"
		// the reason is the double precision. see also comment in ActivityUtilityParameters.java (gl)
		pre.setMinimalDuration(49);
		pre.setClosingTime(49);
		pre.setEarliestEndTime(49);
		pre.setLatestStartTime(49);
		pre.setOpeningTime(49);


		ActivityParams post = new ActivityParams("post-evac");
		post.setTypicalDuration(49); // dito
		post.setMinimalDuration(49);
		post.setClosingTime(49);
		post.setEarliestEndTime(49);
		post.setLatestStartTime(49);
		post.setOpeningTime(49);
		matsimscenario.getConfig().planCalcScore().addActivityParams(pre);
		matsimscenario.getConfig().planCalcScore().addActivityParams(post);

		matsimscenario.getConfig().planCalcScore().setLateArrival_utils_hr(0.);
		matsimscenario.getConfig().planCalcScore().setPerforming_utils_hr(0.);

		//		sc.getConfig().planCalcScore().addParam("activityPriority_0", "1");
		//		sc.getConfig().planCalcScore().addParam("activityTypicalDuration_0", "00:00:49");
		//		sc.getConfig().planCalcScore().addParam("activityMinimalDuration_0", "00:00:49");
		//		sc.getConfig().planCalcScore().addParam("activityPriority_1", "1");
		//		sc.getConfig().planCalcScore().addParam("activityTypicalDuration_1", "00:00:49");
		//		sc.getConfig().planCalcScore().addParam("activityMinimalDuration_1", "00:00:49");


	}

	protected void generateAndSaveNetwork(Scenario sc) {

		GripsConfigModule gcm = getGripsConfig(sc.getConfig());
		String gripsNetworkFile = gcm.getNetworkFileName();

		// Step 1 raw network input
		// for now grips network meta format is osm
		// Hamburg example UTM32N. In future coordinate transformation should be performed beforehand
		CoordinateTransformation ct =  new GeotoolsTransformation("WGS84", this.config.global().getCoordinateSystem());

		
		if (gcm.getMainTrafficType().equals("vehicular")) {
			OsmNetworkReader reader = new OsmNetworkReader(sc.getNetwork(), ct, true);
			reader.setKeepPaths(true);
			reader.parse(gripsNetworkFile);
		} else if (gcm.getMainTrafficType().equals("pedestrian")) {

			OsmNetworkReader reader = new OsmNetworkReader(sc.getNetwork(), ct, false);
			reader.setKeepPaths(true);
			reader.parse(gripsNetworkFile);
			//capacity per lane and hour 1.3/m/s * 0.6 m * 3600s/h 
			double laneCap = 2808 * 2; // 2 lanes

			//					reader.setHighwayDefaults(1, "motorway",4, 5.0/3.6, 1.0, 10000,true);
			//					reader.setHighwayDefaults(1, "motorway_link", 4,  5.0/3.6, 1.0, 10000,true);
			reader.setHighwayDefaults(2, "trunk",         2,  1.34, 1., laneCap);
			reader.setHighwayDefaults(2, "trunk_link",    2,  1.34, 1.0,laneCap);
			reader.setHighwayDefaults(3, "primary",       2,  1.34, 1.0, laneCap);
			reader.setHighwayDefaults(3, "primary_link",  2,  1.34, 1.0, laneCap);
			reader.setHighwayDefaults(4, "secondary",     2,  1.34, 1.0, laneCap);
			reader.setHighwayDefaults(5, "tertiary",      2,  1.34, 1.0,  laneCap);
			reader.setHighwayDefaults(6, "minor",         2,  1.34, 1.0,  laneCap);
			reader.setHighwayDefaults(6, "unclassified",  2,  1.34, 1.0,  laneCap);
			reader.setHighwayDefaults(6, "residential",   2,  1.34, 1.0,  laneCap);
			reader.setHighwayDefaults(6, "living_street", 2,  1.34, 1.0,  laneCap);
			reader.setHighwayDefaults(6,"path",           2,  1.34, 1.0,  laneCap);
			reader.setHighwayDefaults(6,"cycleway",       2,  1.34, 1.0,  laneCap);
			reader.setHighwayDefaults(6,"footway",        2,  1.34, 1.0,  laneCap);

			// max density is set to 5.4 p/m^2
			((NetworkImpl)sc.getNetwork()).setEffectiveLaneWidth(.6);
			((NetworkImpl)sc.getNetwork()).setEffectiveCellSize(.31);
			reader.setKeepPaths(true);
			reader.parse(gripsNetworkFile);
		}else if (gcm.getMainTrafficType().equals("mixed")) {
			//TODO OSMReader for mixed
			log.warn("You are using an experimental feature. Only use this if you exactly know what are you doing!");
			((NetworkImpl)sc.getNetwork()).setEffectiveLaneWidth(.6);
			((NetworkImpl)sc.getNetwork()).setEffectiveCellSize(.31);
			CustomizedOsmNetworkReader reader = new CustomizedOsmNetworkReader(sc.getNetwork(), ct, true);
			reader.setHighwayDefaults(6,"path",           2,  1.34, 1.0,  1);
			reader.setHighwayDefaults(6,"cycleway",       2,  1.34, 1.0,  1);
			reader.setHighwayDefaults(6,"footway",        2,  1.34, 1.0,  1);
			reader.setHighwayDefaults(6,"steps",        2,  1.34, 1.0,  1);
			reader.setKeepPaths(true);
			reader.parse(gripsNetworkFile);
		}




		// Step 2 evacuation network generator
		// 2a) read the evacuation area
		// for now grips evacuation area meta format is ESRI Shape with no validation etc.
		// TODO switch to gml by writing a  xsd + corresponding parser. may be geotools is our friend her? The xsd has to make sure that the evacuation area consists of one and only one
		// polygon
		@SuppressWarnings("rawtypes")
		FeatureSource fs = ShapeFileReader.readDataFile(gcm.getEvacuationAreaFileName());
		SimpleFeature ft = null;
		try {
			ft = (SimpleFeature) fs.getFeatures().features().next();
			FeatureTransformer.transform(ft,fs.getSchema().getCoordinateReferenceSystem(),this.config);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-2);
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-2);
		} catch (TransformException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-2);
		} catch (IllegalAttributeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-2);

		}
		MultiPolygon mp = (MultiPolygon) ft.getDefaultGeometry();
		Polygon p = (Polygon) mp.getGeometryN(0);
		// 2b) generate network
		new EvacuationNetworkGenerator(sc, p,this.safeLinkId).run();

		String networkOutputFile = gcm.getOutputDir()+"/network.xml.gz";
		//		((NetworkImpl)sc.getNetwork()).setEffectiveCellSize(0.26);
		//		((NetworkImpl)sc.getNetwork()).setEffectiveLaneWidth(0.71);
		new NetworkWriter(sc.getNetwork()).write(networkOutputFile);
		sc.getConfig().network().setInputFile(networkOutputFile);
	}



	public GripsConfigModule getGripsConfig() {
		Module m = this.config.getModule("grips");
		if (m instanceof GripsConfigModule) {
			return (GripsConfigModule) m;
		}
		GripsConfigModule gcm = new GripsConfigModule(m);
		this.config.getModules().put("grips", gcm);
		return gcm;
	}


	@Deprecated //call this w/o parameter
	public GripsConfigModule getGripsConfig(Config c) {

		Module m = c.getModule("grips");
		if (m instanceof GripsConfigModule) {
			return (GripsConfigModule) m;
		}
		GripsConfigModule gcm = new GripsConfigModule(m);
		c.getModules().put("grips", gcm);
		return gcm;
	}

	public String getPathToMatsimConfigXML() {
		return this.matsimConfigFile;
	}



}

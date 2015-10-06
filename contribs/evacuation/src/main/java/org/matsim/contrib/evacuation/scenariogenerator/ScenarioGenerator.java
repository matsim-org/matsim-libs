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
package org.matsim.contrib.evacuation.scenariogenerator;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.evacuation.control.algorithms.FeatureTransformer;
import org.matsim.contrib.evacuation.experimental.CustomizedOsmNetworkReader;
import org.matsim.contrib.evacuation.io.EvacuationConfigReader;
import org.matsim.contrib.evacuation.model.config.EvacuationConfigModule;
import org.matsim.contrib.evacuation.model.events.InfoEvent;
import org.matsim.contrib.evacuation.utils.ScenarioCRSTransformation;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
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
import org.matsim.utils.gis.matsim2esri.network.*;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.io.File;
import java.io.IOException;

/**
 * Evacuation scenario generator workflow: GIS Metaformat --> ScenarioGenertor -->
 * MATSim scenario
 * 
 * @author laemmel
 * 
 */
@SuppressWarnings("deprecation")
public class ScenarioGenerator {

	protected static final boolean DEBUG = false;
	private static final String VIS_CRS = "EPSG:3395";//world mercator for visualization
	private static final Logger log = Logger.getLogger(ScenarioGenerator.class);
	protected final String configFile;
	protected final EventsManager em;
	protected String matsimConfigFile;
	protected Id<Link> safeLinkId;
	protected Config matsimConfig;
	protected Scenario matsimScenario;

	public ScenarioGenerator(String evacuationconfig) {
		this.em = EventsUtils.createEventsManager();
		this.configFile = evacuationconfig;
	}

	public ScenarioGenerator(String evacuationConfig, EventHandler handler) {
		this.em = EventsUtils.createEventsManager();
		this.em.addHandler(handler);
		this.configFile = evacuationConfig;
	}

	public void run() {
		log.info("loading config file");
		InfoEvent e = new InfoEvent(System.currentTimeMillis(),
				"loading config file");
		this.em.processEvent(e);
		EvacuationConfigModule gcm = null; 

		try {
			this.matsimConfig = ConfigUtils.createConfig();
			gcm = new EvacuationConfigModule("evacuation");//, this.configFile);
			this.matsimConfig.addModule(gcm);
			EvacuationConfigReader parser = new EvacuationConfigReader(gcm);
			parser.parse(this.configFile);
//			gcm.setFileNamesAbsolute();
			String crs = getCRSFromEvacArea(gcm.getEvacuationAreaFileName());
			this.matsimConfig.global().setCoordinateSystem(crs);

		
		} catch (Exception ee) {
			// TODO for backwards compatibility should be remove soon
			log.warn("File is not a  evacuation config file. Guessing it is a common MATSim config file");
			this.matsimConfig = ConfigUtils.loadConfig(this.configFile);

		}

		String outdir = gcm.getOutputDir();
		this.matsimConfigFile = outdir + "/config.xml";
		this.matsimConfig.controler().setOutputDirectory(outdir +"/output");
				
		QSimConfigGroup qsim = this.matsimConfig.qsim();
		qsim.setEndTime(30 * 3600);
		this.matsimConfig.timeAllocationMutator().setMutationRange(0.);
		this.matsimScenario = ScenarioUtils.createScenario(this.matsimConfig);
		this.safeLinkId = Id.create("el1", Link.class);

		File outputDirFile = new File(outdir);
		if (!outputDirFile.exists()) {
			outputDirFile.mkdirs();
		}

		generateNetwork(this.matsimScenario);
		if (DEBUG) {
			dumpNetworkAsShapeFile(this.matsimScenario);
		}

		log.info("generating population file");
		e = new InfoEvent(System.currentTimeMillis(),
				"generating population file");
		this.em.processEvent(e);
		generatePopulation(this.matsimScenario);

		//transform coordinate system to world mercator
		ScenarioCRSTransformation.transform(this.matsimScenario, VIS_CRS);
		this.matsimConfig.global().setCoordinateSystem(VIS_CRS);
		//save network
		String networkOutputFile = gcm.getOutputDir() + "/network.xml.gz";
		new NetworkWriter(this.matsimScenario.getNetwork()).write(networkOutputFile);
		this.matsimScenario.getConfig().network().setInputFile(networkOutputFile);
		//save population
		String outputPopulationFile = gcm.getOutputDir() + "/population.xml.gz";
		new PopulationWriter(this.matsimScenario.getPopulation(), this.matsimScenario.getNetwork(),
				gcm.getSampleSize()).write(outputPopulationFile);
		this.matsimScenario.getConfig().plans().setInputFile(outputPopulationFile);


		log.info("saving matsim config file to:" + this.matsimConfigFile);
		e = new InfoEvent(System.currentTimeMillis(), "simulation config file");
		this.em.processEvent(e);


		this.matsimConfig.controler().setLastIteration(10);
		this.matsimConfig.strategy().setMaxAgentPlanMemorySize(3);
		this.matsimConfig.strategy().addParam("ModuleDisableAfterIteration_1", "75");
		this.matsimConfig.strategy().addParam("maxAgentPlanMemorySize", "3");
		this.matsimConfig.strategy().addParam("Module_1", "ReRoute");
		this.matsimConfig.strategy().addParam("ModuleProbability_1", "0.1");
		this.matsimConfig.strategy().addParam("Module_2", "ChangeExpBeta");
		this.matsimConfig.strategy().addParam("ModuleProbability_2", "0.9");

		this.matsimConfig.qsim().setRemoveStuckVehicles(false);

		this.matsimConfig.travelTimeCalculator().setTraveltimeBinSize(120);
		this.matsimConfig.travelTimeCalculator().setTravelTimeCalculatorType(
				"TravelTimeCalculatorHashMap");

		new ConfigWriter(this.matsimConfig).write(this.matsimConfigFile);
		e = new InfoEvent(System.currentTimeMillis(),
				"scenario generation finished.");
		this.em.processEvent(e);

	}

	private String getCRSFromEvacArea(String evacuationAreaFileName) {
		ShapeFileReader r = new ShapeFileReader();
		r.readFileAndInitialize(evacuationAreaFileName);
		ReferencedEnvelope b = r.getBounds();
		double lon = (b.getMaxX()+b.getMinX())/2;
		double lat = (b.getMaxY()+b.getMinY())/2;
		String epsgCode = MGC.getUTMEPSGCodeForWGS84Coordinate(lon, lat);
		return epsgCode;
	}

	protected void dumpNetworkAsShapeFile(Scenario sc) {

		final Network network = sc.getNetwork();

		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(
				network, sc.getConfig().global().getCoordinateSystem());
		builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
		builder.setWidthCoefficient(0.5);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
		new Links2ESRIShape(network, getEvacuationConfig(this.matsimConfig).getOutputDir()
				+ "/links_ls.shp", builder).write();

		CoordinateReferenceSystem crs = MGC.getCRS(sc.getConfig().global()
				.getCoordinateSystem());
		builder.setWidthCoefficient(0.003);
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(CapacityBasedWidthCalculator.class);
		builder.setCoordinateReferenceSystem(crs);
		new Links2ESRIShape(network, getEvacuationConfig(this.matsimConfig).getOutputDir()
				+ "/links_p.shp", builder).write();

	}

	@SuppressWarnings("unused")
	private void generateAndSaveNetworkChangeEvents(Scenario sc) {
		throw new RuntimeException(
				"This has to be done during network generation. The reason is that at this stage the mapping between original link ids (e.g. from osm) to generated matsim link ids is forgotten!");

	}

	protected void generatePopulation(Scenario sc) {
		// for now a simple ESRI shape file format is used to emulated the a
		// more sophisticated not yet defined population meta format
		EvacuationConfigModule gcm = getEvacuationConfig(sc.getConfig());
		String evacuationPopulationFile = gcm.getPopulationFileName();
		new PopulationFromESRIShapeFileGenerator(sc, evacuationPopulationFile,
				this.safeLinkId).run();



		sc.getConfig().qsim().setStorageCapFactor(gcm.getSampleSize());
		sc.getConfig().qsim().setFlowCapFactor(gcm.getSampleSize());

		ActivityParams pre = new ActivityParams("pre-evac");
		pre.setTypicalDuration(49); // needs to be geq 49, otherwise when
									// running a simulation one gets
									// "java.lang.RuntimeException: zeroUtilityDuration of type pre-evac must be greater than 0.0. Did you forget to specify the typicalDuration?"
		// the reason is the double precision. see also comment in
		// ActivityUtilityParameters.java (gl)
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
		sc.getConfig().planCalcScore().addActivityParams(pre);
		sc.getConfig().planCalcScore().addActivityParams(post);

		sc.getConfig().planCalcScore().setLateArrival_utils_hr(0.);
		sc.getConfig().planCalcScore().setPerforming_utils_hr(0.);

		// matsimScenario.getConfig().planCalcScore().addParam("activityPriority_0", "1");
		// matsimScenario.getConfig().planCalcScore().addParam("activityTypicalDuration_0",
		// "00:00:49");
		// matsimScenario.getConfig().planCalcScore().addParam("activityMinimalDuration_0",
		// "00:00:49");
		// matsimScenario.getConfig().planCalcScore().addParam("activityPriority_1", "1");
		// matsimScenario.getConfig().planCalcScore().addParam("activityTypicalDuration_1",
		// "00:00:49");
		// matsimScenario.getConfig().planCalcScore().addParam("activityMinimalDuration_1",
		// "00:00:49");

	}

	protected void generateNetwork(Scenario sc) {
		log.info("generating network file");
		InfoEvent e = new InfoEvent(System.currentTimeMillis(), "generating network file");
		this.em.processEvent(e);

		EvacuationConfigModule gcm = getEvacuationConfig(); // sc.getConfig());
		String evacuationNetworkFile = gcm.getNetworkFileName();

		// Step 1 raw network input
		// for now evacuation network meta format is osm
		// Hamburg example UTM32N. In future coordinate transformation should be
		// performed beforehand
		CoordinateTransformation ct = new GeotoolsTransformation("WGS84",
				this.matsimConfig.global().getCoordinateSystem());

		if (gcm.getMainTrafficType().equals("vehicular")) {
			OsmNetworkReader reader = new OsmNetworkReader(sc.getNetwork(), ct,
					true);
			reader.setKeepPaths(true);
			reader.parse(evacuationNetworkFile);
		} else if (gcm.getMainTrafficType().equals("pedestrian")) {

			OsmNetworkReader reader = new OsmNetworkReader(sc.getNetwork(), ct,
					false);
			reader.setKeepPaths(true);
			reader.parse(evacuationNetworkFile);
			// capacity per lane and hour 1.3/m/s * 0.6 m * 3600s/h
			double laneCap = 2808 * 2; // 2 lanes

			// reader.setHighwayDefaults(1, "motorway",4, 5.0/3.6, 1.0,
			// 10000,true);
			// reader.setHighwayDefaults(1, "motorway_link", 4, 5.0/3.6, 1.0,
			// 10000,true);
			reader.setHighwayDefaults(2, "trunk", 2, 1.34, 1., laneCap);
			reader.setHighwayDefaults(2, "trunk_link", 2, 1.34, 1.0, laneCap);
			reader.setHighwayDefaults(3, "primary", 2, 1.34, 1.0, laneCap);
			reader.setHighwayDefaults(3, "primary_link", 2, 1.34, 1.0, laneCap);
			reader.setHighwayDefaults(4, "secondary", 2, 1.34, 1.0, laneCap);
			reader.setHighwayDefaults(5, "tertiary", 2, 1.34, 1.0, laneCap);
			reader.setHighwayDefaults(6, "minor", 2, 1.34, 1.0, laneCap);
			reader.setHighwayDefaults(6, "unclassified", 2, 1.34, 1.0, laneCap);
			reader.setHighwayDefaults(6, "residential", 2, 1.34, 1.0, laneCap);
			reader.setHighwayDefaults(6, "living_street", 2, 1.34, 1.0, laneCap);
			reader.setHighwayDefaults(6, "path", 2, 1.34, 1.0, laneCap);
			reader.setHighwayDefaults(6, "cycleway", 2, 1.34, 1.0, laneCap);
			reader.setHighwayDefaults(6, "footway", 2, 1.34, 1.0, laneCap);
			reader.setHighwayDefaults(6, "steps", 2, 1.34, 1.0, laneCap);
			reader.setHighwayDefaults(6, "pedestrian", 2, 1.34, 1.0, laneCap);
			// max density is set to 5.4 p/m^2
			((NetworkImpl) sc.getNetwork()).setEffectiveLaneWidth(.6);
			((NetworkImpl) sc.getNetwork()).setEffectiveCellSize(.31);
			reader.setKeepPaths(true);
			reader.parse(evacuationNetworkFile);
		} else if (gcm.getMainTrafficType().equals("mixed")) {
			// TODO OSMReader for mixed
			log.warn("You are using an experimental feature. Only use this if you exactly know what are you doing!");
			((NetworkImpl) sc.getNetwork()).setEffectiveLaneWidth(.6);
			((NetworkImpl) sc.getNetwork()).setEffectiveCellSize(.31);
			CustomizedOsmNetworkReader reader = new CustomizedOsmNetworkReader(
					sc.getNetwork(), ct, true);
			reader.setHighwayDefaults(6, "path", 2, 1.34, 1.0, 1);
			reader.setHighwayDefaults(6, "cycleway", 2, 1.34, 1.0, 1);
			reader.setHighwayDefaults(6, "footway", 2, 1.34, 1.0, 1);
			reader.setHighwayDefaults(6, "steps", 2, 1.34, 1.0, 1);
			reader.setKeepPaths(true);
			reader.parse(evacuationNetworkFile);
		}

		// Step 2 evacuation network generator
		// 2a) read the evacuation area
		// for now evacuation evacuation area meta format is ESRI Shape with no
		// validation etc.
		// TODO switch to gml by writing a xsd + corresponding parser. may be
		// geotools is our friend her? The xsd has to make sure that the
		// evacuation area consists of one and only one
		// polygon
		@SuppressWarnings("rawtypes")
		FeatureSource fs = ShapeFileReader.readDataFile(gcm.getEvacuationAreaFileName());
		SimpleFeature ft = null;
		try {
			ft = (SimpleFeature) fs.getFeatures().features().next();
			FeatureTransformer.transform(ft, fs.getSchema()
					.getCoordinateReferenceSystem(), this.matsimConfig);
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(-2);
		} catch (FactoryException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.exit(-2);
		} catch (TransformException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.exit(-2);
		} catch (IllegalAttributeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.exit(-2);

		}
		MultiPolygon mp = (MultiPolygon) ft.getDefaultGeometry();
		Polygon p = (Polygon) mp.getGeometryN(0);
		// 2b) generate network
		new EvacuationNetworkGenerator(sc, p, this.safeLinkId).run();
		log.info("done generating network file");

	}

	public EvacuationConfigModule getEvacuationConfig() {
		ConfigGroup m = this.matsimConfig.getModule("evacuation");
		if (m instanceof EvacuationConfigModule) {
			return (EvacuationConfigModule) m;
		}
		EvacuationConfigModule gcm = new EvacuationConfigModule(m);
		this.matsimConfig.getModules().put("evacuation", gcm);
		return gcm;
	}

	@Deprecated
	// call this w/origin parameter
	public EvacuationConfigModule getEvacuationConfig(Config c) {

		ConfigGroup m = c.getModule("evacuation");
		if (m instanceof EvacuationConfigModule) {
			return (EvacuationConfigModule) m;
		}
		EvacuationConfigModule gcm = new EvacuationConfigModule(m);
		c.getModules().put("evacuation", gcm);
		return gcm;
	}

	public String getPathToMatsimConfigXML() {
		return this.matsimConfigFile;
	}

}

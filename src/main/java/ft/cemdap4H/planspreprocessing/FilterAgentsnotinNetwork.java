/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package ft.cemdap4H.planspreprocessing;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.GeometryUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

/**
 * @author  saxer
 *
 */
/**
 *
 */
public class FilterAgentsnotinNetwork {

	static String pathNetworkFile = null;
	static String pathInputPlanFile = null;
	static String pathOutputPlanFile = null;
	static String pathResearchAreaShapeFile = null;
	static String shapeFeatureIdentifier = null;
	static String pathNetworBoundingBox = null;
	static Scenario scenario = null;
	static double safetyBufferInMeter = 2000.0;
	
	static CoordinateTransformation coordinateTransformation = null;
	static String shapeFileEPSG = null;
	static List<String> allowedEPSGs = new ArrayList<String>();
	

	static List<Geometry> districtGeometryList = new ArrayList<Geometry>();
	static GeometryFactory geomfactory = JTSFactoryFinder.getGeometryFactory(null);
	static GeometryCollection geometryCollection = geomfactory.createGeometryCollection(null);
	static int removedAgentsCounter = 0;
	static double filterDistance = 60000.00;
	static List<Double> xList = new ArrayList<Double>();
	static List<Double> yList = new ArrayList<Double>();
	

	private final static Set<String> zones = new HashSet<>(); // Districts of
	                                                          // our research
	                                                          // area
	private final static Map<String, Geometry> zoneMap = new HashMap<>(); // Districts
	                                                                      // and
	                                                                      // geometry
	                                                                      // of
	                                                                      // our
	                                                                      // research
	                                                                      // area

	private static final Logger LOG = Logger.getLogger(FilterAgentsnotinNetwork.class);

	static Geometry researchAreaBoundary = null;

	public static void main(String[] args) {

		// In
		shapeFeatureIdentifier = "NO";
		//pathNetworkFile = "E:\\Thiel\\Programme\\MatSim\\00_HannoverModel_1.0\\Input\\Cemdap\\cemdap_output\\Hannover_big_wchildren\\vw216.1.0.output_network.xml.gz";
		pathInputPlanFile = "E:\\Thiel\\Programme\\MatSim\\00_HannoverModel_1.0\\Input\\Cemdap\\cemdap_output\\Hannover_big_wchildren\\mergedPlans_dur.xml.gz";
		pathResearchAreaShapeFile = "E:\\Thiel\\Programme\\MatSim\\00_HannoverModel_1.0\\Input\\Cemdap\\add_data\\shp\\Statistische_Bezirke_Hannover_Region.shp";
		shapeFileEPSG = "EPSG:25832";
		

		
		// Out
		pathNetworBoundingBox = "E:\\Thiel\\Programme\\MatSim\\00_HannoverModel_1.0\\Input\\Cemdap\\cemdap_output\\Hannover_big_wchildren\\requiredNetworkBoundingBox.txt";
		pathOutputPlanFile = "E:\\Thiel\\Programme\\MatSim\\00_HannoverModel_1.0\\Input\\Cemdap\\cemdap_output\\Hannover_big_wchildren\\mergedPlans_dur_dropped.xml.gz";

		FilterAgentsnotinNetwork.run(pathInputPlanFile, pathOutputPlanFile, pathResearchAreaShapeFile,
		        shapeFeatureIdentifier, pathNetworBoundingBox, shapeFileEPSG);

	}

	public static void run(String pathInputPlanFile, String pathOutputPlanFile,
	        String pathResearchAreaShapeFile, String shapeFeatureIdentifier, String pathNetworBoundingBox, String shapeFileEPSG) {
	
		//This list contains all allowed metrical coordinate systems.
		allowedEPSGs.add("EPSG:25832");
		
		if (!allowedEPSGs.contains(shapeFileEPSG))
		{
			throw new RuntimeException("Current coordinate system not in list of allowed EPSGs");
		}
		
		coordinateTransformation = TransformationFactory.getCoordinateTransformation(shapeFileEPSG,TransformationFactory.WGS84);
				
		initalizeInputData(pathInputPlanFile); // Load the input
		readShape(pathResearchAreaShapeFile, shapeFeatureIdentifier); // Load
		                                                              // the
		                                                              // shape
		                                                              // of our
		                                                              // research
		                                                              // area
		getResearchAreaBoundary();
		// Use Iterator in order to be able to drop person while reading them
		for (Iterator<? extends Person> personIter = scenario.getPopulation().getPersons().values()
		        .iterator(); personIter.hasNext();) {
			Person p = personIter.next();

			// Use Iterator in order to be able to drop plans while reading them
			for (Iterator<? extends Plan> planIter = p.getPlans().iterator(); planIter.hasNext();) {

				Plan plan = planIter.next();

				// Check if this plan intersects the network boundary
				// Method: Build a bounding box from activity points and compare
				// this with the network boundary
				// If a plan intersects and any agent activity is not more far
				// away from city center than filterDistance,
				// we keep this plan
				if (agentPlanIntersectsNetwork(plan) && !anyAgentActivityToFarWayFromCentroid(filterDistance, plan)) {

					// Keep plan
				}

				else {
					// Is this plan is not intersecting with our network or any
					// activity leads to far away from the network boundary it
					// will be removed
					planIter.remove();
				}

			}

			// If we have a person which has no more plans left, the person is
			// removed from population
			int remainingPlan = p.getPlans().size();
			if (remainingPlan == 0) {
				// LOG.warn("Person: " + p.getId() + " removed");
				personIter.remove();
				removedAgentsCounter++;
			} else {
				// Don't drop this person. But we store the coordinates of
				// activities in order to get the required network bounding box
				updateCoordLists(p);
			}

		}

		// We add additional safetyBufferInMeter around the bounding box to
		// guarantee that all remaining activities are within our new network
		writeNetworkBoundingBox(pathNetworBoundingBox, Collections.min(xList) ,
		        Collections.min(yList), Collections.max(xList) ,
		        Collections.max(yList));

		new PopulationWriter(scenario.getPopulation()).write(pathOutputPlanFile);
		// writeShiftedAgents(AgentShiftedFilePath, AgentShifted);
		LOG.warn("Complete persons deleted: " + removedAgentsCounter);

	}
	
	private static void updateCoordLists(Person person) {

		for (Plan plan : person.getPlans()) {
			List<Activity> activities = TripStructureUtils.getActivities(plan.getPlanElements(),
			        EmptyStageActivityTypes.INSTANCE);

			for (Activity act : activities) {
				
				
				Double x = act.getCoord().getX();
				Double y = act.getCoord().getY();
				
//				Double x = transformedCoord.getX();
//				Double y = transformedCoord.getY();
				xList.add(x);
				yList.add(y);
			}

		}

	}

	private static void readShape(String shapeFile, String featureKeyInShapeFile) {
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
		for (SimpleFeature feature : features) {
			String id = feature.getAttribute(featureKeyInShapeFile).toString();
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			zones.add(id);
			zoneMap.put(id, geometry);
		}
	}

	public static boolean anyAgentActivityToFarWayFromCentroid(double cirtialMeter, Plan plan) {

		// Returns true if any activity to more far way than cirtialMeter from
		// centroidPoint
		Point centroidPoint = researchAreaBoundary.getCentroid();
		
		double maxDistance = 0.0;

		List<Activity> activities = TripStructureUtils.getActivities(plan.getPlanElements(),
		        EmptyStageActivityTypes.INSTANCE);

		for (Activity act : activities) {
			double actualDistance = NetworkUtils.getEuclideanDistance(act.getCoord(),
			        CoordUtils.createCoord(centroidPoint.getX(), centroidPoint.getY()));

			if (actualDistance > maxDistance) {
				maxDistance = actualDistance;
			}
		}

		return (maxDistance > cirtialMeter);

	}

	public static void initalizeInputData(String pathInputPlanFile) {
		// This class populates the scenario with network and population
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		//new MatsimNetworkReader(scenario.getNetwork()).readFile(pathNetworkFile);
		new PopulationReader(scenario).readFile(pathInputPlanFile);

	}

	public static boolean dropIrrelevantPlans(Person person) {

		return true;
	}

	public static boolean agentPlanIntersectsNetwork(Plan plan) {

		// Converts all coordinates of an agent's plan to an point array
		// If the point array's bounding box intersects with the network
		// boundary, we assume, that this plan is relevant for our MATSim model
		GeometryFactory geomfact = JTSFactoryFinder.getGeometryFactory();
		// GeometryCollection geometryColl =
		// geomfact.createGeometryCollection(null);
		Geometry boundingBox = null;

		List<Activity> activities = TripStructureUtils.getActivities(plan.getPlanElements(),
		        EmptyStageActivityTypes.INSTANCE);

		Point[] actPointArray = new Point[activities.size()];

		int i = 0;
		for (Activity act : activities) {

			Coord coord = act.getCoord();
			Point point = GeometryUtils.createGeotoolsPoint(coord);

			actPointArray[i] = point;
			i++;
		}

		Geometry geometryColl = geomfact.createMultiPoint(actPointArray);
		boundingBox = geometryColl.getEnvelope();

		if (boundingBox.intersects(researchAreaBoundary)) {
			return true;
		} else
			return false;

	}

	public static boolean isActivityWithinNetwork(Activity act) {
		boolean anyActInnetworkBoundary = false;
		Coord coord = act.getCoord();
		Point point = GeometryUtils.createGeotoolsPoint(coord);

		// Gets true, if the act is within the networkBoundary
		if (researchAreaBoundary.contains(point))
			anyActInnetworkBoundary = true;

		return anyActInnetworkBoundary;
	}

	public static void getResearchAreaBoundary() {
		// This class infers the geometric boundary of all network link
		Geometry boundary = null;
		for (Geometry zoneGeom : zoneMap.values()) {
			districtGeometryList.add(zoneGeom);
		}

		geometryCollection = (GeometryCollection) geomfactory.buildGeometry(districtGeometryList);
		boundary = geometryCollection.convexHull();

		researchAreaBoundary = boundary;
	}

	static void writeNetworkBoundingBox(String filename, double minX, double minY, double maxX, double maxY) {
		
		minX = minX - safetyBufferInMeter;
		minY = minY - safetyBufferInMeter;
		
		maxX = maxX + safetyBufferInMeter;
		maxY = maxY + safetyBufferInMeter;
		
		
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		String sep = "\t";
		try {
			bw.append("minX"+sep+"minY"+sep+"maxX"+sep+"maxY");
			bw.newLine();
			bw.append(minX + sep + minY + sep + maxX + sep + maxY);
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}

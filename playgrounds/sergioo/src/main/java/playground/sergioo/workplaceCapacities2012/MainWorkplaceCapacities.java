/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.sergioo.workplaceCapacities2012;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.AStarLandmarks;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.OpeningTime;
import org.matsim.facilities.OpeningTimeImpl;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.opengis.feature.simple.SimpleFeature;

import others.sergioo.util.algebra.Matrix1DImpl;
import others.sergioo.util.algebra.Matrix2DImpl;
import others.sergioo.util.algebra.Matrix3DImpl;
import others.sergioo.util.algebra.MatrixND;
import others.sergioo.util.algebra.PointND;
import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;
import playground.sergioo.visualizer2D2012.networkVisualizer.SimpleNetworkWindow;
import playground.sergioo.visualizer2D2012.networkVisualizer.networkPainters.NetworkPainter;
import playground.sergioo.workplaceCapacities2012.gui.BSSimpleNetworkWindow;
import playground.sergioo.workplaceCapacities2012.gui.ClustersWindow;
import playground.sergioo.workplaceCapacities2012.gui.WeigthsNetworkWindow;
import playground.sergioo.workplaceCapacities2012.gui.WorkersAreaPainter;
import playground.sergioo.workplaceCapacities2012.gui.WorkersBSPainter;
import playground.sergioo.workplaceCapacities2012.hits.PersonSchedule;
import playground.sergioo.workplaceCapacities2012.hits.PointPerson;
import playground.sergioo.workplaceCapacities2012.hits.Trip;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class MainWorkplaceCapacities {

	//Classes
	private static class ModeShareZone {
		private final double modeShare;
		private double difference = 0;
		private final List<Double> capacities = new ArrayList<Double>();
		public ModeShareZone(double modeShare) {
			this.modeShare = modeShare;
		}
	}

	//Constants
	private static final String NETWORK_FILE = "./data/currentSimulation/singapore2.xml";
	private static final String LINKS_MAP_FILE = "./data/facilities/auxiliar/links.map";
	private static final String QUANTITIES_MAP_FILE = "./data/facilities/auxiliar/quantities.map";
	private static final String WEIGHTS_MAP_FILE = "./data/facilities/auxiliar/weightsMap.map";
	private static final String TRAVEL_TIMES_FILE = "./data/facilities/auxiliar/travelTimes.dat";
	private static final String WEIGHTS2_MAP_FILE = "./data/facilities/auxiliar/weights.map";
	private static final String CLUSTERS_FILE = "./data/facilities/auxiliar/clusters.map";
	private static final String AREAS_MAP_FILE = "./data/facilities/auxiliar/areasMPL.map";
	private static final String POLYGONS_FILE = "./data/facilities/Masterplan_Areas.shp";
	private static final String CAPACITIES_FILE = "./data/facilities/auxiliar/capacities.map";
	private static final String BUILDINGS_FILE = "./data/facilities/auxiliar/buildings.xml";
	private static final String INPUT_FILE = "./data/facilities/parametersAu.txt";
	private static final String OUTPUT_FILE = "./data/facilities/matrixAu.dat";
	private static final String SOLUTION_FILE = "./data/facilities/solutionAu.txt";
	private static final String WORK_FACILITIES_FILEO = "./data/facilities/workFacilitiesO.xml";
	private static final String WORK_FACILITIES_FILE = "./data/facilities/workFacilities.xml";
	private static final double WALKING_SPEED = 4/3.6;
	private static final double PRIVATE_BUS_SPEED = 16/3.6;
	private static final double MIN_TRAVEL_TIME = 3*60;
	private static final double MAX_TRAVEL_TIME = 15*60;
	private static final String SEPARATOR = ";;;";
	private static final int NUM_NEAR = 3;

	//Static attributes
	private static int SIZE = 10;
	private static int NUM_ITERATIONS = 100;
	private static List<CentroidCluster<PointPerson>> clusters;
	private static SortedMap<Id<ActivityFacility>, MPAreaData> dataMPAreas = new TreeMap<Id<ActivityFacility>, MPAreaData>();
	private static SortedMap<String, Coord> stopsBase = new TreeMap<String, Coord>();
	private static Network network;
	private static List<List<Double>> travelTimes;
	private static SortedMap<Id<ActivityFacility>, Double> maximumAreaCapacities;
	private static List<List<Double>> stopScheduleCapacities;
	private static ActivityFacilities buildings;
	//private static Coord downLeft = new CoordImpl(103.83355, 1.2814);
	//private static Coord upRight = new CoordImpl(103.8513, 1.2985);
	private static Coord downLeft;

	static {
		final double x = -Double.MAX_VALUE;
		final double y = -Double.MAX_VALUE;
		downLeft = new Coord(x, y);
	}

	private static Coord upRight = new Coord(Double.MAX_VALUE, Double.MAX_VALUE);
	private static HashMap<String, Double> workerAreas = new HashMap<String, Double>();
	//Main
	/**
	 * @param args
	 * @throws NoConnectionException
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws IOException
	 * @throws BadStopException
	 */
	public static void main(String[] args) throws BadStopException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		if(args.length==2) {
			SIZE = Integer.parseInt(args[0]);
			NUM_ITERATIONS = Integer.parseInt(args[1]);
		}
		loadData();
		//calculateOptimizationParameters();
		boolean exception = true;
		while(exception) {
			System.out.println("Run the solver and press Enter when the file is copied in the folder");
			new BufferedReader(new InputStreamReader(System.in)).readLine();
			try {
				readMasterAreaResults();
			}
			catch (Exception e) {
				continue;
			}
			exception = false;
		}
		ActivityFacilitiesImpl facilities = capacitiesToBuildings();
		new FacilitiesWriter(facilities).write(WORK_FACILITIES_FILEO);
	}

	//Static Methods
	private static void loadData() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseAux  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
		ResultSet stopsResult = dataBaseAux.executeQuery("SELECT * FROM stops");
		while(stopsResult.next())
			stopsBase.put(stopsResult.getString(1), new Coord(stopsResult.getDouble(3), stopsResult.getDouble(2)));
		stopsResult.close();
		dataBaseAux.close();
		System.out.println("Stops done!");
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(CLUSTERS_FILE));
			clusters = (List<CentroidCluster<PointPerson>>) ois.readObject();
			ois.close();
		} catch(EOFException e) {
			clusters = clusterWorkActivities(getWorkActivityTimes());
		}
		new ClustersWindow("Work times cluster PCA back: "+getClustersDeviations(clusters)+" "+getWeightedClustersDeviations(clusters), clusters).setVisible(true);
		System.out.println("Clustering done!");
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(NETWORK_FILE);
		network = scenario.getNetwork();
		setMPAreas();
		setWorkerAreas();
		System.out.println("Types done!");
		/*new MatsimFacilitiesReader(scenario).readFile(BUILDINGS_FILE);
		buildings = scenario.getActivityFacilities();*/
	}
	private static void calculateOptimizationParameters() throws BadStopException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		System.out.println("Process starts with "+SIZE+" clusters and "+NUM_ITERATIONS+" iterations.");
		/*CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
		downLeft=coordinateTransformation.transform(downLeft);
		upRight=coordinateTransformation.transform(upRight);*/
		Map<Id<TransitStopFacility>, Double> stopCapacities = new HashMap<Id<TransitStopFacility>, Double>();
		Map<String, Double> quantitiesMap;
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(QUANTITIES_MAP_FILE));
			quantitiesMap = (Map<String, Double>)ois.readObject();
			stopCapacities = (Map<Id<TransitStopFacility>, Double>)ois.readObject();
			ois.close();
		} catch (EOFException e) {
			quantitiesMap = calculateStopClustersQuantities(stopCapacities);
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(QUANTITIES_MAP_FILE));
			oos.writeObject(quantitiesMap);
			oos.writeObject(stopCapacities);
			oos.close();
		}
		MatrixND<Double> quantities = new Matrix2DImpl(new int[]{clusters.size(),stopsBase.size()});
		stopScheduleCapacities = new ArrayList<List<Double>>();
		Iterator<String> stopIdsI = stopsBase.keySet().iterator();
		//Iterator<Coord> stopsI = stopsBase.values().iterator();
		for(int s=0; s<quantities.getDimension(1); s++) {
			String stopId = stopIdsI.next();
			/*Coord stopCoord = stopsI.next();
			boolean inStop = stopCoord.getX()>downLeft.getX() && stopCoord.getX()<upRight.getX() && stopCoord.getY()>downLeft.getY() && stopCoord.getY()<upRight.getY();
			if(inStop)*/
				stopScheduleCapacities.add(new ArrayList<Double>());
			for(int c=0; c<quantities.getDimension(0); c++) {
				Double quantity = quantitiesMap.get(stopId+SEPARATOR+c);
				if(quantity==null)
					quantity = 0.0;
				quantities.setElement(new int[]{c,s}, quantity);
				//if(inStop)
					stopScheduleCapacities.get(stopScheduleCapacities.size()-1).add(quantity);
			}
		}
		System.out.println("Quantities done!");
		MatrixND<Double> maxs = new Matrix1DImpl(new int[]{dataMPAreas.size()}, 60.0);
		Iterator<Id<ActivityFacility>> mPAreaI = dataMPAreas.keySet().iterator();
		maximumAreaCapacities = new TreeMap<Id<ActivityFacility>, Double>();
		for(int f=0; f<maxs.getDimension(0); f++) {
			Id<ActivityFacility> mPId = mPAreaI.next();
			MPAreaData dataMPArea = dataMPAreas.get(mPId);
			double max = getMaxCapacity(dataMPArea)*dataMPArea.getModeShare()/(1+dataMPArea.getModeShare());
			maxs.setElement(new int[]{f}, max);
			Coord areaCoord = dataMPArea.getCoord();
			if(areaCoord.getX()>downLeft.getX() && areaCoord.getX()<upRight.getX() && areaCoord.getY()>downLeft.getY() && areaCoord.getY()<upRight.getY())
				maximumAreaCapacities.put(mPId, max);
		}
		System.out.println("Max areas done!");
		Map<Tuple<Id<TransitStopFacility>, Id<ActivityFacility>>,Tuple<Boolean,Double>> weightsMap;
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(WEIGHTS_MAP_FILE));
			weightsMap = (Map<Tuple<Id<TransitStopFacility>, Id<ActivityFacility>>,Tuple<Boolean,Double>>) ois.readObject();
			ois.close();
			try {
				ois = new ObjectInputStream(new FileInputStream(TRAVEL_TIMES_FILE));
				travelTimes = (List<List<Double>>) ois.readObject();
				ois.close();
			} catch(EOFException e) {
				e.printStackTrace();
			}
		} catch(EOFException e){
			weightsMap= calculateAreaStopTravelTimes(stopsBase, stopCapacities, network);
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(WEIGHTS_MAP_FILE));
			oos.writeObject(weightsMap);
			oos.close();
			oos = new ObjectOutputStream(new FileOutputStream(TRAVEL_TIMES_FILE));
			oos.writeObject(travelTimes);
			oos.close();
		}
		/*Map<Integer, Double> tts = new HashMap<Integer, Double>();
		for(int i=0; i<travelTimes.size(); i++)
			if(travelTimes.get(i).get(8134)<36000)
				tts.put(i, travelTimes.get(i).get(8134));*/
		new WeigthsNetworkWindow("Weights", new NetworkPainter(network), weightsMap, dataMPAreas, stopsBase).setVisible(true);
		System.out.println("Travel times done!");
		/*Matrix2DImpl weights = new Matrix2DImpl(new int[]{dataMPAreas.size(),stopsBase.size()});
		mPAreaI = dataMPAreas.keySet().iterator();
		for(int f=0; f<weights.getDimension(0); f++) {
			Id<ActivityFacility> mPAreaId = mPAreaI.next();
			stopIdsI = stopsBase.keySet().iterator();
			for(int s=0; s<weights.getDimension(1); s++) {
				Id<TransitStopFacility> sId = Id.create(stopIdsI.next(), TransitStopFacility.class);
				Tuple<Boolean, Double> weight = weightsMap.get(new Tuple<Id<TransitStopFacility>, Id<ActivityFacility>>(sId, mPAreaId));
				if(weight==null)
					weights.setElement(f, s, 0.0);
				else
					weights.setElement(f, s, weight.getSecond());
			}
		}*/
		System.out.println("Weights done!");
		Matrix2DImpl proportions = new Matrix2DImpl(new int[]{dataMPAreas.size(),clusters.size()});
		Map<String, List<Double>> proportionsMap = calculateTypeBuildingOptionWeights(clusters);
		mPAreaI = dataMPAreas.keySet().iterator();
		for(int f=0; f<proportions.getDimension(0); f++) {
			Id<ActivityFacility> mPAreaId = mPAreaI.next();
			for(int c=0; c<proportions.getDimension(1); c++)
				proportions.setElement(f, c, proportionsMap.get(dataMPAreas.get(mPAreaId).getType()).get(c));
		}
		System.out.println("Proportions done!");
		writeOptimizationParameters();
	}
	private static double getMaxCapacity(MPAreaData dataMPArea) {
		boolean withBuildings = false;
		GeometryFactory factory = new GeometryFactory();
		if(withBuildings)  {
			double max = 0;
			for(ActivityFacility building:buildings.getFacilities().values())
				if(factory.createPoint(new Coordinate(building.getCoord().getX(), building.getCoord().getY())).within(dataMPArea.getPolygon()))
					max+=building.getActivityOptions().get("work").getCapacity();
			return max;
		}
		else
			return (dataMPArea.getMaxArea()/workerAreas.get(dataMPArea.getType()));
	}

	private static Map<String, PointPerson> getWorkActivityTimes() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseHits  = new DataBaseAdmin(new File("./data/hits/DataBase.properties"));
		Map<String, PersonSchedule> times;
		ResultSet timesResult = dataBaseHits.executeQuery("SELECT pax_idx,trip_id,t6_purpose,t3_starttime,t4_endtime,p6_occup,t5_placetype FROM hits.hitsshort");
		times = new HashMap<String, PersonSchedule>();
		while(timesResult.next()) {
			PersonSchedule timesPerson = times.get(timesResult.getString(1));
			if(timesPerson==null) {
				timesPerson = new PersonSchedule(timesResult.getString(1), timesResult.getString(6));
				times.put(timesResult.getString(1), timesPerson);
			}
			if(timesResult.getInt(2)!=0) {
				Iterator<Entry<Integer, Trip>> timesPersonI = timesPerson.getTrips().entrySet().iterator();
				Entry<Integer, Trip> last = null;
				while(timesPersonI.hasNext())
					last = timesPersonI.next();
				if(last==null || last.getKey()!=timesResult.getInt(2)) {
					int startTime = (timesResult.getInt(4)%100)*60+(timesResult.getInt(4)/100)*3600;
					int endTime = (timesResult.getInt(5)%100)*60+(timesResult.getInt(5)/100)*3600;
					if(last!=null && last.getKey()<timesResult.getInt(2) && last.getValue().getEndTime()>startTime) {
						startTime += 12*3600;
						endTime += 12*3600;
					}
					if(last!=null && last.getKey()<timesResult.getInt(2) && last.getValue().getEndTime()>startTime) {
						startTime += 12*3600;
						endTime += 12*3600;
					}
					timesPerson.getTrips().put(timesResult.getInt(2), new Trip(timesResult.getString(3), startTime, endTime, timesResult.getString(7)));
				}
			}
		}
		timesResult.close();
		Map<String, PointPerson> points = new HashMap<String, PointPerson>();
		for(PersonSchedule timesPerson:times.values()) {
			SortedMap<Integer, Trip> tripsPerson = timesPerson.getTrips();
			boolean startTimeSaved=false;
			double startTime=-1, endTime=-1;
			String placeType=null;
			if(tripsPerson.size()>0) {
				for(int i = tripsPerson.keySet().iterator().next(); i<=tripsPerson.size(); i ++) {
					if(!startTimeSaved && tripsPerson.get(i).getPurpose()!=null && tripsPerson.get(i).getPurpose().equals("work")) {
						startTime = tripsPerson.get(i).getEndTime();
						startTimeSaved = true;
					}
					if(i>tripsPerson.keySet().iterator().next() && tripsPerson.get(i-1).getPurpose().equals("work")) {
						endTime = tripsPerson.get(i).getStartTime();
						placeType = tripsPerson.get(i-1).getPlaceType();
					}
				}
			}
			if(startTime!=-1 && endTime!=-1 && endTime-startTime>=7*3600 && endTime-startTime<=16*3600)
				if(startTime>24*3600)
					points.put(timesPerson.getId(), new PointPerson(timesPerson.getId(), timesPerson.getOccupation(), new Double[]{startTime-24*3600, endTime-24*3600-(startTime-24*3600)}, placeType));
				else
					points.put(timesPerson.getId(), new PointPerson(timesPerson.getId(), timesPerson.getOccupation(), new Double[]{startTime, endTime-startTime}, placeType));
		}
		Map<String, Double> weights;
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(WEIGHTS2_MAP_FILE));
			weights = (Map<String, Double>) ois.readObject();
			ois.close();
		} catch (EOFException e) {
			weights = new HashMap<String, Double>();
			ResultSet weightsR = dataBaseHits.executeQuery("SELECT pax_idx,hipf10  FROM hits.hitsshort_geo_hipf");
			while(weightsR.next())
				weights.put(weightsR.getString(1), weightsR.getDouble(2));
			for(PointPerson pointPerson:points.values()) {
				if(weights.get(pointPerson.getId())!=null)
					pointPerson.setWeight(weights.get(pointPerson.getId()));
				else
					pointPerson.setWeight(100);
			}
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(WEIGHTS2_MAP_FILE));
			oos.writeObject(weights);
			oos.close();
		}
		dataBaseHits.close();
		return points;
	}
	private static List<CentroidCluster<PointPerson>> clusterWorkActivities(Map<String,PointPerson> points) throws FileNotFoundException, IOException, ClassNotFoundException {
		List<CentroidCluster<PointPerson>> clusters = null;
		Set<PointPerson> pointsC = getPCATransformation(points.values());
		clusters = new KMeansPlusPlusClusterer<PointPerson>(SIZE, 1000).cluster(pointsC);
		new ClustersWindow("Work times cluster PCA: "+getClustersDeviations(clusters)+" "+getWeightedClustersDeviations(clusters), clusters).setVisible(true);
		for(Cluster<PointPerson> cluster:clusters)
			for(PointPerson pointPersonT:cluster.getPoints()) {
				PointPerson pointPerson = points.get(pointPersonT.getId());
				for(int d=0; d<pointPersonT.getDimension(); d++)
					pointPersonT.setElement(d, pointPerson.getElement(d));
			}
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(CLUSTERS_FILE));
		oos.writeObject(clusters);
		oos.close();
		return clusters;
	}
	private static double getClustersDeviations(List<CentroidCluster<PointPerson>> clusters) {
		double deviation = 0;
		for(CentroidCluster<PointPerson> cluster:clusters)
			for(PointPerson pointPerson:cluster.getPoints())
				deviation += ((PointPerson)cluster.getCenter()).distanceFrom(pointPerson);
		return deviation;
	}
	private static double getWeightedClustersDeviations(List<CentroidCluster<PointPerson>> clusters) {
		double deviation = 0, totalWeight=0;
		for(CentroidCluster<PointPerson> cluster:clusters) {
			for(PointPerson pointPerson:cluster.getPoints()) {
				deviation += ((PointPerson)cluster.getCenter()).distanceFrom(pointPerson)*pointPerson.getWeight();
				totalWeight = pointPerson.getWeight();
			}
		}
		return deviation/totalWeight;
	}
	private static Set<PointPerson> getPCATransformation(Collection<PointPerson> points) {
		RealMatrix pointsM = new Array2DRowRealMatrix(points.iterator().next().getDimension(), points.size());
		int k=0;
		for(PointND<Double> point:points) {
			for(int f=0; f<point.getDimension(); f++)
				pointsM.setEntry(f, k, point.getElement(f));
			k++;
		}
		RealMatrix means = new Array2DRowRealMatrix(pointsM.getRowDimension(), 1);
		for(int r=0; r<means.getRowDimension(); r++) {
			double mean = 0;
			for(int c=0; c<pointsM.getColumnDimension(); c++)
				mean+=pointsM.getEntry(r,c)/pointsM.getColumnDimension();
			means.setEntry(r, 0, mean);
		}
		RealMatrix deviations = new Array2DRowRealMatrix(pointsM.getRowDimension(), pointsM.getColumnDimension());
		for(int r=0; r<deviations.getRowDimension(); r++)
			for(int c=0; c<deviations.getColumnDimension(); c++)
				deviations.setEntry(r, c, pointsM.getEntry(r, c)-means.getEntry(r, 0));
		RealMatrix covariance = deviations.multiply(deviations.transpose()).scalarMultiply(1/(double)pointsM.getColumnDimension());
		EigenDecomposition eigenDecomposition = new EigenDecomposition(covariance, 0);
		RealMatrix eigenVectorsT = eigenDecomposition.getVT();
		RealVector eigenValues = new ArrayRealVector(eigenDecomposition.getD().getRowDimension());
		for(int r=0; r<eigenDecomposition.getD().getRowDimension(); r++)
			eigenValues.setEntry(r, eigenDecomposition.getD().getEntry(r, r));
		for(int i=0; i<eigenValues.getDimension(); i++) {
			for(int j=i+1; j<eigenValues.getDimension(); j++)
				if(eigenValues.getEntry(i)<eigenValues.getEntry(j)) {
					double tempValue = eigenValues.getEntry(i);
					eigenValues.setEntry(i, eigenValues.getEntry(j));
					eigenValues.setEntry(j, tempValue);
					RealVector tempVector = eigenVectorsT.getRowVector(i);
					eigenVectorsT.setRowVector(i, eigenVectorsT.getRowVector(j));
					eigenVectorsT.setRowVector(j, tempVector);
				}
			eigenVectorsT.setRowVector(i,eigenVectorsT.getRowVector(i).mapMultiply(Math.sqrt(1/eigenValues.getEntry(i))));
		}
		RealVector standardDeviations = new ArrayRealVector(pointsM.getRowDimension());
		for(int r=0; r<covariance.getRowDimension(); r++)
			standardDeviations.setEntry(r, Math.sqrt(covariance.getEntry(r, r)));
		double zValue = standardDeviations.dotProduct(new ArrayRealVector(pointsM.getRowDimension(), 1));
		RealMatrix zScore = deviations.scalarMultiply(1/zValue);
		pointsM = eigenVectorsT.multiply(zScore);
		Set<PointPerson> pointsC = new HashSet<PointPerson>();
		k=0;
		for(PointPerson point:points) {
			PointPerson pointC = new PointPerson(point.getId(), point.getOccupation(), new Double[]{pointsM.getEntry(0, k), pointsM.getEntry(1, k)}, point.getPlaceType());
			pointC.setWeight(point.getWeight());
			pointsC.add(pointC);
			k++;
		}
		return pointsC;
	}
	private static Map<String, Double> calculateStopClustersQuantities(Map<Id<TransitStopFacility>, Double> stopCapacities) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		List<PointPerson> centers = new ArrayList<PointPerson>();
		for(int c=0; c<clusters.size(); c++)
			centers.add(clusters.get(c).getPoints().get(0).centroidOf(clusters.get(c).getPoints()));
		DataBaseAdmin dataBaseAux  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
		Map<String, Double> quantities = new HashMap<String, Double>();
		Map<String, Integer> users = new HashMap<String, Integer>();
		ResultSet tripsResult = dataBaseAux.executeQuery("SELECT * FROM DCM_work_activities");
		int ts=0;
		while(tripsResult.next()) {
			Id<TransitStopFacility> stopId = Id.create(tripsResult.getString(5), TransitStopFacility.class);
			Double quantity = stopCapacities.get(stopId);
			if(quantity==null)
				quantity = 0.0;
			stopCapacities.put(stopId, quantity+1);
			Integer num = users.get(tripsResult.getString(2));
			if(num==null)
				num = 0;
			users.put(tripsResult.getString(2), ++num);
			int nearestCluster = 0;
			PointPerson time = new PointPerson(tripsResult.getString(2)+"_"+num, "", new Double[]{(double) tripsResult.getInt(8), (double) (tripsResult.getInt(12)-tripsResult.getInt(8))}, "");
			for(int c=0; c<clusters.size(); c++)
				if(centers.get(c).distanceFrom(time)<centers.get(nearestCluster).distanceFrom(time))
					nearestCluster = c;
			String key = tripsResult.getString(5)+SEPARATOR+nearestCluster;
			quantity = quantities.get(key);
			if(quantity==null)
				quantity = 0.0;
			quantities.put(key, quantity+1);
			ts++;
		}
		System.out.println(ts);
		tripsResult.close();
		dataBaseAux.close();
		return quantities;
	}
	private static void setMPAreas() throws FileNotFoundException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException, NoConnectionException {
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(AREAS_MAP_FILE));
			dataMPAreas = (SortedMap<Id<ActivityFacility>, MPAreaData>)ois.readObject();
			ois.close();
		} catch(EOFException e) {
			DataBaseAdmin dataBaseAuxiliar  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
			CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_SVY21, TransformationFactory.WGS84_UTM48N);
			ResultSet mPAreasR = dataBaseAuxiliar.executeQuery("SELECT * FROM masterplan_areas WHERE use_for_generation = 1");
			while(mPAreasR.next()) {
				ResultSet mPAreasR2 = dataBaseAuxiliar.executeQuery("SELECT ZoneID,`Pu/Pr` FROM DCM_mplan_zones_modeshares WHERE objectID="+mPAreasR.getInt(1));
				mPAreasR2.next();
				dataMPAreas.put(Id.create(mPAreasR.getString(1), ActivityFacility.class), new MPAreaData(Id.create(mPAreasR.getString(1), ActivityFacility.class), coordinateTransformation.transform(new Coord(mPAreasR.getDouble(6), mPAreasR.getDouble(7))), mPAreasR.getString(2), mPAreasR.getDouble(5), Id.create(mPAreasR2.getInt(1), ActivityFacility.class), mPAreasR2.getDouble(2)));
			}
			mPAreasR.close();
			dataBaseAuxiliar.close();
			//Load polygons
			Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(POLYGONS_FILE);
			for(SimpleFeature feature:features) {
				MPAreaData area = dataMPAreas.get(Id.create((Integer)feature.getAttribute(1), ActivityFacility.class));
				if(area!=null)
					area.setPolygon((Polygon) ((MultiPolygon)feature.getDefaultGeometry()).getGeometryN(0));
			}
			//Find nearest good links
			GeometryFactory factory = new GeometryFactory();
			int i=0;
			for(MPAreaData mPArea:dataMPAreas.values()) {
				if(i%100 == 0)
					System.out.println(i+++" of "+dataMPAreas.size());
				Link nearestLink =null;
				double nearestDistance = Double.MAX_VALUE;
				for(Link link:network.getLinks().values())
					if(link.getAllowedModes().contains("car")) {
						Coord fromNodeCoord = link.getFromNode().getCoord();
						Coord toNodeCoord = link.getToNode().getCoord();
						double distance = CoordUtils.distancePointLinesegment(fromNodeCoord, toNodeCoord, mPArea.getCoord());
						if(distance<MAX_TRAVEL_TIME*WALKING_SPEED)
							if(distance<nearestDistance) {
								nearestDistance = distance;
								nearestLink = link;
							}
							else if(factory.createPoint(new Coordinate(fromNodeCoord.getX(), fromNodeCoord.getY())).within(mPArea.getPolygon()))
								mPArea.addLinkId(link.getId());
							else if(factory.createPoint(new Coordinate(toNodeCoord.getX(), toNodeCoord.getY())).within(mPArea.getPolygon()))
								mPArea.addLinkId(link.getId());
					}
				if(nearestLink==null && mPArea.getLinkIds().size()==0) {
					for(Link link:network.getLinks().values())
						if(link.getAllowedModes().contains("car")) {
							Coord fromNodeCoord = link.getFromNode().getCoord();
							Coord toNodeCoord = link.getToNode().getCoord();
							double distance = CoordUtils.distancePointLinesegment(fromNodeCoord, toNodeCoord, mPArea.getCoord());
							if(distance<nearestDistance) {
								nearestDistance = distance;
								nearestLink = link;
							}
						}
				}
				if(nearestLink!=null)
					mPArea.addLinkId(nearestLink.getId());
				if(mPArea.getLinkIds().size()==0)
					throw new RuntimeException("Error!");
			}
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(AREAS_MAP_FILE));
			oos.writeObject(dataMPAreas);
			oos.close();
		}
		System.out.println("Areas done!");
	}
	private static void setWorkerAreas() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		boolean commonSense = true;
		if(commonSense) {
			DataBaseAdmin dataBaseAux  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
			ResultSet typesResult = dataBaseAux.executeQuery("SELECT * FROM masterplan_areas_types");
			while(typesResult.next())
				workerAreas.put(typesResult.getString(1), typesResult.getDouble(2));
			typesResult.close();
			dataBaseAux.close();
		}
	}
	private static Map<Tuple<Id<TransitStopFacility>, Id<ActivityFacility>>, Tuple<Boolean, Double>> calculateAreaStopTravelTimes(SortedMap<String, Coord> stopsBase, Map<Id<TransitStopFacility>, Double> stopsCapacities, Network network) throws BadStopException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		List<Map<String, Id<Link>>> linksStops;
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(LINKS_MAP_FILE));
			linksStops = (List<Map<String, Id<Link>>>) ois.readObject();
			ois.close();
		} catch (EOFException e) {
			linksStops = new ArrayList<Map<String, Id<Link>>>();
			for(int n=0; n<NUM_NEAR; n++) {
				linksStops.add(new HashMap<String, Id<Link>>());
				for(Entry<String, Coord> stopBase: stopsBase.entrySet()) {
					Id<Link> nearest = network.getLinks().values().iterator().next().getId();
					double nearestDistance = CoordUtils.calcEuclideanDistance(network.getLinks().get(nearest).getCoord(), stopBase.getValue());
					for(Link link:network.getLinks().values())
						if(link.getAllowedModes().contains("car")) {
							boolean selected = false;
							for(int p=0; p<n; p++)
								if(linksStops.get(p).get(stopBase.getKey()).equals(link.getId()))
									selected=true;
							if(!selected && CoordUtils.calcEuclideanDistance(link.getToNode().getCoord(), stopBase.getValue())<nearestDistance) {
								nearest = link.getId();
								nearestDistance = CoordUtils.calcEuclideanDistance(network.getLinks().get(nearest).getCoord(), stopBase.getValue());
							}
						}
					linksStops.get(n).put(stopBase.getKey(), nearest);
				}
			}
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(LINKS_MAP_FILE));
			oos.writeObject(linksStops);
			oos.close();
		}
		//Compute stops facilities weights
		TravelDisutility travelMinCost = new TravelDisutility() {
			@Override
			public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
				return getLinkMinimumTravelDisutility(link);
			}
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return link.getLength()/WALKING_SPEED;
			}
		};
		TravelTime timeFunction = new TravelTime() {

			@Override
			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				return link.getLength()/WALKING_SPEED;
			}
		};
		PreProcessLandmarks preProcessData = new PreProcessLandmarks(travelMinCost);
		preProcessData.run(network);
		AStarLandmarks aStarLandmarks = new AStarLandmarks(network, preProcessData, timeFunction);
		Map<Tuple<Id<TransitStopFacility>, Id<ActivityFacility>>,Tuple<Boolean,Double>> weights = new HashMap<Tuple<Id<TransitStopFacility>, Id<ActivityFacility>>, Tuple<Boolean,Double>>();
		Collection<Tuple<Id<TransitStopFacility>, Integer>> removeStops = new ArrayList<Tuple<Id<TransitStopFacility>, Integer>>();
		travelTimes = new ArrayList<List<Double>>();
		int s=0;
		for(Entry<String, Coord> stop: stopsBase.entrySet()) {
			String stopKey = stop.getKey();
			boolean mrtStop = stopKey.startsWith("STN");
			/*Coord stopCoord = stop.getValue();
			boolean inStop = stopCoord.getX()>downLeft.getX() && stopCoord.getX()<upRight.getX() && stopCoord.getY()>downLeft.getY() && stopCoord.getY()<upRight.getY();
			if(inStop)*/
				travelTimes.add(new ArrayList<Double>());
			double maxTimeFromStop = 0;
			Collection<Id<Link>> linkIds = new ArrayList<Id<Link>>();
			for(int n=0; n<NUM_NEAR; n++)
				if(CoordUtils.calcEuclideanDistance(network.getLinks().get(linksStops.get(n).get(stopKey)).getToNode().getCoord(), stop.getValue())<MAX_TRAVEL_TIME*WALKING_SPEED/5)
					linkIds.add(linksStops.get(n).get(stopKey));
			Id<TransitStopFacility> stopId = Id.create(stopKey, TransitStopFacility.class);
			double maxCapacityNearFacilities = 0;
			int w=0;
			for(MPAreaData mPArea:dataMPAreas.values()) {
				double distance = CoordUtils.calcEuclideanDistance(stopsBase.get(stopKey), mPArea.getCoord());
				/*Coord areaCoord = mPArea.getCoord();
				boolean inArea = areaCoord.getX()>downLeft.getX() && areaCoord.getX()<upRight.getX() && areaCoord.getY()>downLeft.getY() && areaCoord.getY()<upRight.getY();
				if(inStop && inArea) {*/
					travelTimes.get(travelTimes.size()-1).add(getCost(mrtStop, Math.floor(36000.0+distance+0.5)));
					w++;
				//}
				if(distance<MIN_TRAVEL_TIME*WALKING_SPEED) {
					weights.put(new Tuple<Id<TransitStopFacility>, Id<ActivityFacility>>(stopId, mPArea.getId()), new Tuple<Boolean, Double>(true, getCost(mrtStop, distance/WALKING_SPEED)));
					//if(inStop && inArea) {
						travelTimes.get(travelTimes.size()-1).set(w-1, getCost(mrtStop, distance/WALKING_SPEED));
						mPArea.addTravelTime(stopId, distance/WALKING_SPEED);
					//}
					if(distance/WALKING_SPEED>maxTimeFromStop)
						maxTimeFromStop = distance/WALKING_SPEED;
					maxCapacityNearFacilities += maximumAreaCapacities.get(mPArea.getId());
				}
				else if(distance<MAX_TRAVEL_TIME*WALKING_SPEED) {
					double walkingTime = Double.MAX_VALUE;
					for(Id<Link> linkId:linkIds)
						for(Id<Link> linkId2:mPArea.getLinkIds()) {
							double walkingTimeA=aStarLandmarks.calcLeastCostPath(network.getLinks().get(linkId).getToNode(), network.getLinks().get(linkId2).getFromNode(), 0, null, null).travelCost+CoordUtils.calcEuclideanDistance(network.getLinks().get(linkId2).getFromNode().getCoord(), mPArea.getCoord())/WALKING_SPEED;
							if(walkingTimeA<walkingTime)
								walkingTime = walkingTimeA;
						}
					if(walkingTime<=MAX_TRAVEL_TIME) {
						weights.put(new Tuple<Id<TransitStopFacility>, Id<ActivityFacility>>(stopId, mPArea.getId()), new Tuple<Boolean, Double>(true, getCost(mrtStop, walkingTime)));
						//if(inStop && inArea) {
							travelTimes.get(travelTimes.size()-1).set(w-1, getCost(mrtStop, walkingTime));
							mPArea.addTravelTime(stopId, walkingTime);
						//}
						if(walkingTime>maxTimeFromStop)
							maxTimeFromStop = walkingTime;
						maxCapacityNearFacilities += maximumAreaCapacities.get(mPArea.getId());
					}
				}
			}
			if(stopsCapacities.get(stopId)>maxCapacityNearFacilities) {
				double maxCapacityNear2Facilities = maxCapacityNearFacilities;
				w=0;
				for(MPAreaData mPArea:dataMPAreas.values()) {
					/*Coord areaCoord = mPArea.getCoord();
					boolean inArea = areaCoord.getX()>downLeft.getX() && areaCoord.getX()<upRight.getX() && areaCoord.getY()>downLeft.getY() && areaCoord.getY()<upRight.getY();
					if(inStop && inArea)*/
						w++;
					if(CoordUtils.calcEuclideanDistance(stopsBase.get(stopKey), mPArea.getCoord())<(MAX_TRAVEL_TIME*2/3)*PRIVATE_BUS_SPEED) {
						double walkingTime = Double.MAX_VALUE;
						for(Id<Link> linkId:linkIds)
							for(Id<Link> linkId2:mPArea.getLinkIds()) {
								double walkingTimeA=aStarLandmarks.calcLeastCostPath(network.getLinks().get(linkId).getToNode(), network.getLinks().get(linkId2).getFromNode(), 0, null, null).travelCost+CoordUtils.calcEuclideanDistance(network.getLinks().get(linkId2).getFromNode().getCoord(), mPArea.getCoord())/WALKING_SPEED;
								if(walkingTimeA<walkingTime)
									walkingTime = walkingTimeA;
							}
						double privateBusTime = Double.MAX_VALUE;
						for(Id<Link> linkId:linkIds)
							for(Id<Link> linkId2:mPArea.getLinkIds()) {
								double privateBusTimeA=aStarLandmarks.calcLeastCostPath(network.getLinks().get(linkId).getToNode(), network.getLinks().get(linkId2).getFromNode(), 0, null, null).travelCost*WALKING_SPEED/PRIVATE_BUS_SPEED+CoordUtils.calcEuclideanDistance(network.getLinks().get(linkId2).getFromNode().getCoord(), mPArea.getCoord())/WALKING_SPEED;
								if(privateBusTimeA<privateBusTime)
									privateBusTime = privateBusTimeA;
							}
						if(walkingTime>MAX_TRAVEL_TIME && privateBusTime<=(MAX_TRAVEL_TIME*2/3)) {
							weights.put(new Tuple<Id<TransitStopFacility>, Id<ActivityFacility>>(stopId, mPArea.getId()), new Tuple<Boolean, Double>(false, getCost(mrtStop, privateBusTime)));
							//if(inStop && inArea) {
								travelTimes.get(travelTimes.size()-1).set(w-1, getCost(mrtStop, privateBusTime));
								mPArea.addTravelTime(stopId, privateBusTime);
							//}
							if(privateBusTime>maxTimeFromStop)
								maxTimeFromStop = privateBusTime;
							maxCapacityNear2Facilities += maximumAreaCapacities.get(mPArea.getId());
						}
					}
				}
				if(stopsCapacities.get(stopId)>maxCapacityNear2Facilities) {
					System.out.println("Far" + stopId);
					double maxCapacityNear3Facilities = maxCapacityNear2Facilities;
					w=0;
					for(MPAreaData mPArea:dataMPAreas.values()) {
						/*Coord areaCoord = mPArea.getCoord();
						boolean inArea = areaCoord.getX()>downLeft.getX() && areaCoord.getX()<upRight.getX() && areaCoord.getY()>downLeft.getY() && areaCoord.getY()<upRight.getY();
						if(inStop && inArea)*/
							w++;
						if(CoordUtils.calcEuclideanDistance(stopsBase.get(stopKey), mPArea.getCoord())<MAX_TRAVEL_TIME*PRIVATE_BUS_SPEED) {
							double privateBusTime = Double.MAX_VALUE;
							for(Id<Link> linkId:linkIds)
								for(Id<Link> linkId2:mPArea.getLinkIds()) {
									double privateBusTimeA=aStarLandmarks.calcLeastCostPath(network.getLinks().get(linkId).getToNode(), network.getLinks().get(linkId2).getFromNode(), 0, null, null).travelCost*WALKING_SPEED/PRIVATE_BUS_SPEED+CoordUtils.calcEuclideanDistance(network.getLinks().get(linkId2).getFromNode().getCoord(), mPArea.getCoord())/WALKING_SPEED;
									if(privateBusTimeA<privateBusTime)
										privateBusTime = privateBusTimeA;
								}
							if(privateBusTime>(MAX_TRAVEL_TIME*2/3) && privateBusTime<=MAX_TRAVEL_TIME) {
								weights.put(new Tuple<Id<TransitStopFacility>, Id<ActivityFacility>>(stopId, mPArea.getId()), new Tuple<Boolean, Double>(false, getCost(mrtStop, privateBusTime)));
								//if(inStop && inArea) {
									travelTimes.get(travelTimes.size()-1).set(w-1, getCost(mrtStop, privateBusTime));
									mPArea.addTravelTime(stopId, privateBusTime);
								//}
								if(privateBusTime>maxTimeFromStop)
									maxTimeFromStop = privateBusTime;
								maxCapacityNear3Facilities += maximumAreaCapacities.get(mPArea.getId());
							}
						}
					}
					if(stopsCapacities.get(stopId)>maxCapacityNear3Facilities) {
						System.out.println("Very far" + stopId);
						removeStops.add(new Tuple<Id<TransitStopFacility>, Integer>(stopId, s));
					}
				}
			}
			double totalTimeFromStop = 0;
			maxTimeFromStop++;
			for(Entry<Tuple<Id<TransitStopFacility>, Id<ActivityFacility>>,Tuple<Boolean, Double>> weight:weights.entrySet())
				if(weight.getKey().getFirst().equals(stopId)) {
					double correctWeight = maxTimeFromStop-weight.getValue().getSecond();
					weights.put(weight.getKey(), new Tuple<Boolean, Double>(weight.getValue().getFirst(), correctWeight));
					totalTimeFromStop += correctWeight;
				}
			if(totalTimeFromStop!=0)
				for(Entry<Tuple<Id<TransitStopFacility>, Id<ActivityFacility>>,Tuple<Boolean, Double>> weight:weights.entrySet())
					if(weight.getKey().getFirst().equals(stopId))
						weights.put(weight.getKey(), new Tuple<Boolean, Double>(weight.getValue().getFirst(),weight.getValue().getSecond()/totalTimeFromStop));
			if(s++%100 == 0)
				System.out.println(s+" of "+stopsBase.size());
		}
		int num=0;
		for(Tuple<Id<TransitStopFacility>, Integer> stopId:removeStops) {
			num+=stopsCapacities.get(stopId.getFirst());
			/*stopsCapacities.remove(stopId.getFirst());
			stopsBase.remove(stopId.getFirst().toString());
			stopScheduleCapacities.remove(stopId.getSecond().intValue());
			travelTimes.remove(stopId.getSecond().intValue());*/
		}
		System.out.println(num+" workers lost.");
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(AREAS_MAP_FILE));
		oos.writeObject(dataMPAreas);
		oos.close();
		return weights;
	}
	private static Double getCost(boolean mrtStop, double travelTime) {
		double tt1;
		if(mrtStop)
			tt1 = 540;
		else
			tt1 = 270;
		if(travelTime<tt1)
			return 90*travelTime/tt1;
		else
			return travelTime-tt1+90;
	}

	private static Map<String, List<Double>> calculateTypeBuildingOptionWeights(List<CentroidCluster<PointPerson>> clusters) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseAux  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
		Map<String, List<String>> mPTypesAll = new HashMap<String, List<String>>();
		Map<String, List<Double>> proportions = new HashMap<String, List<Double>>();
		Map<String, Double> proportionsT = new HashMap<String, Double>();
		ResultSet mPTypesResult = dataBaseAux.executeQuery("SELECT DISTINCT mp_type FROM mp_types_X_hits_types");
		while(mPTypesResult.next()) {
			List<Double> list=new ArrayList<Double>();
			for(int i=0; i<clusters.size(); i++)
				list.add(0.0);
			proportions.put(mPTypesResult.getString(1), list);
			proportionsT.put(mPTypesResult.getString(1), 0.0);
		}
		for(int c=0; c<clusters.size(); c++)
			for(PointPerson person:clusters.get(c).getPoints()) {
				String type = person.getPlaceType();
				List<String> mPTypes = mPTypesAll.get(type);
				if(mPTypes==null) {
					mPTypes = getMPTypes(type, dataBaseAux);
					mPTypesAll.put(type, mPTypes);
				}
				for(String mPType:mPTypes) {
					List<Double> list = proportions.get(mPType);
					list.set(c, list.get(c)+1);
					proportionsT.put(mPType,proportionsT.get(mPType)+1);
				}
			}
		for(String key:proportions.keySet())
			for(int c=0; c<clusters.size(); c++) {
				Double proportion = proportions.get(key).get(c)/proportionsT.get(key);
				if(proportion.isNaN())
					proportion=0.0;
				proportions.get(key).set(c, proportion);
			}
		dataBaseAux.close();
		return proportions;
	}
	private static List<String> getMPTypes(String placeType, DataBaseAdmin dataBaseAux) throws SQLException, NoConnectionException {
		ResultSet mPTypesResult = dataBaseAux.executeQuery("SELECT mp_type FROM mp_types_X_hits_types WHERE hits_type='"+placeType+"'");
		List<String> mpTypes = new ArrayList<String>();
		while(mPTypesResult.next())
			mpTypes.add(mPTypesResult.getString(1));
		mPTypesResult.close();
		return mpTypes;
	}
	private static void writeOptimizationParameters() throws FileNotFoundException, IOException {
		double[][] travelTimesM = new double[travelTimes.size()][travelTimes.get(0).size()];
		for(int s=0; s<travelTimesM.length; s++)
			for(int w=0; w<travelTimesM[s].length; w++)
				travelTimesM[s][w]=travelTimes.get(s).get(w);
		double[] maximumAreaCapacitiesM = new double[maximumAreaCapacities.size()];
		Iterator<Double> it = maximumAreaCapacities.values().iterator();
		for(int w=0; w<maximumAreaCapacitiesM.length; w++)
			maximumAreaCapacitiesM[w] = it.next();
		double[][] stopScheduleCapacitiesM = new double[stopScheduleCapacities.size()][stopScheduleCapacities.get(0).size()];
		for(int s=0; s<stopScheduleCapacitiesM.length; s++)
			for(int c=0; c<stopScheduleCapacitiesM[s].length; c++)
				stopScheduleCapacitiesM[s][c]=stopScheduleCapacities.get(s).get(c);
		int numStops=travelTimesM.length, numWorkAreas = travelTimesM[0].length, numWorkSchedules=stopScheduleCapacitiesM[0].length;
		List<Integer> workSchedules = new ArrayList<Integer>();
		for(int i=0; i<numWorkSchedules; i++) {
			int sum=0;
			for(int j=0; j<stopScheduleCapacitiesM.length; j++)
				sum+=stopScheduleCapacitiesM[j][i];
			if(sum>0)
				workSchedules.add(i);
		}
		System.out.println("Size: "+numStops*numWorkAreas*workSchedules.size());
		System.out.println("- Stops: "+numStops);
		System.out.println("- Work areas: "+numWorkAreas);
		System.out.println("- Work schedules: "+workSchedules.size());
		PrintWriter writer = new PrintWriter(INPUT_FILE);
		writer.println("numStops = "+numStops+";");
		writer.println("numWorkAreas = "+numWorkAreas+";");
		writer.println("numWorkSchedules = "+workSchedules.size()+";");
		writer.println("travelTimes =");
		writer.println("[");
		for(double[] tts:travelTimesM) {
			writer.print("["+tts[0]);
			for(int i=1; i<tts.length; i++)
				writer.print(","+tts[i]);
			writer.println("]");
		}
		writer.println("];");
		writer.println("maximumAreaCapacities =");
		writer.print("["+maximumAreaCapacitiesM[0]);
		for(int i=1; i<maximumAreaCapacitiesM.length; i++)
			writer.print(","+maximumAreaCapacitiesM[i]);
		writer.println("];");
		writer.println("stopScheduleCapacities =");
		writer.println("[");
		for(double[] sss:stopScheduleCapacitiesM) {
			writer.print("["+sss[workSchedules.get(0)]);
			for(int i=1; i<workSchedules.size(); i++)
				writer.print(","+sss[workSchedules.get(i)]);
			writer.println("]");
		}
		writer.println("];");
		writer.close();
	}
	private static void writeOptimizationParameters2(int numRegions) throws FileNotFoundException, IOException {
		List<double[][]> travelTimes = new ArrayList<double[][]>();
		List<double[]> maximumAreaCapacities = new ArrayList<double[]>();
		List<double[][]> stopScheduleCapacities = new ArrayList<double[][]>();
		Set<StopCoord> pointsC = new HashSet<StopCoord>();
		for(Entry<String,Coord> stop:stopsBase.entrySet())
			pointsC.add(new StopCoord(stop.getValue().getX(), stop.getValue().getY(), Id.create(stop.getKey(), TransitStopFacility.class)));
		List<CentroidCluster<StopCoord>> clusters = new KMeansPlusPlusClusterer<StopCoord>(numRegions, 1000).cluster(pointsC);
		for(int n=0; n<numRegions; n++) {
			double[][] tts = new double[clusters.get(n).getPoints().size()][1];
			for(StopCoord stop:clusters.get(n).getPoints()) {
				for(MPAreaData mPArea:dataMPAreas.values()) {
					Double tt = mPArea.getTravelTime(stop.getId());
					int s=0;
					int w=0;
					if(tt!=null)
						tts[s][w]=tt;
				}
			}
			travelTimes.add(tts);
			maximumAreaCapacities.add(new double[1]);
			stopScheduleCapacities.add(new double[clusters.get(n).getPoints().size()][SIZE]);
		}
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(INPUT_FILE));
		oos.writeObject(travelTimes);
		oos.writeObject(maximumAreaCapacities);
		oos.writeObject(stopScheduleCapacities);
		oos.close();
	}
	private static void fitCapacities(FittingCapacities fittingCapacities) throws FileNotFoundException, IOException, ClassNotFoundException {
		MatrixND<Double> capacities = null;
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(CAPACITIES_FILE));
			capacities = (MatrixND<Double>) ois.readObject();
			ois.close();
		} catch (Exception e2) {
			Runtime.getRuntime().gc();
			capacities = fittingCapacities.run(NUM_ITERATIONS);
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(CAPACITIES_FILE));
			oos.writeObject(capacities);
			oos.close();
			System.out.println("Matrix written!");
		}
		Matrix3DImpl matrix = (Matrix3DImpl)capacities;
		ActivityFacilityImpl fac = ((ActivityFacilitiesImpl)FacilitiesUtils.createActivityFacilities()).createAndAddFacility(Id.create("dummy", ActivityFacility.class), new Coord(0, 0));
		for(int o=0; o<matrix.getDimension(1); o++) {
			double[] center = new double[]{0, 0};
			for(PointPerson pointPerson:clusters.get(o).getPoints())
				for(int i=0; i<2; i++)
					center[i] += pointPerson.getElement(i);
			for(int i=0; i<2; i++)
				center[i] /= clusters.get(o).getPoints().size();
			int minStart = ((int) Math.round(((center[0]%3600.0)/60)/15))*15;
			int minDuration = ((int) Math.round(((center[1]%3600.0)/60)/15))*15;
			NumberFormat numberFormat = new DecimalFormat("00");
			String optionText = "w_"+numberFormat.format(Math.floor(center[0]/3600))+numberFormat.format(minStart)+"_"+numberFormat.format(Math.floor(center[1]/3600))+numberFormat.format(minDuration);
			OpeningTime openingTime = new OpeningTimeImpl(Math.round(center[0]/900)*900, Math.round((center[0]+center[1])/900)*900);
			Iterator<MPAreaData> mPAreaI = dataMPAreas.values().iterator();
			for(int f=0; f<matrix.getDimension(0); f++) {
				MPAreaData mPArea = mPAreaI.next();
				double pTCapacityFO = 0;
				for(int s=0; s<matrix.getDimension(2); s++)
					pTCapacityFO += matrix.getElement(f, o, s);
				if(pTCapacityFO>0) {
					ActivityOptionImpl activityOption = new ActivityOptionImpl(optionText);
					activityOption.setCapacity(pTCapacityFO/mPArea.getModeShare());
					activityOption.addOpeningTime(openingTime);
					mPArea.putActivityOption(activityOption);
				}
			}
		}
	}
	private static void readMasterAreaResults() throws IOException, ClassNotFoundException {
		double[][] matrixCapacities;
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(OUTPUT_FILE));
			matrixCapacities = (double[][]) ois.readObject();
			ois.close();
		} catch(EOFException e) {
			BufferedReader reader = new BufferedReader(new FileReader(SOLUTION_FILE));
			int numStops = new Integer(reader.readLine());
			int numWorkAreas = new Integer(reader.readLine());
			int numWorkSchedules = new Integer(reader.readLine());
			matrixCapacities = new double[numWorkAreas][numWorkSchedules];
			for(int w=0; w<numWorkAreas; w++)
				for(int c=0; c<numWorkSchedules; c++)
					matrixCapacities[w][c]=0;
			for(int s=0; s<numStops; s++)
				for(int w=0; w<numWorkAreas; w++) {
					String line = reader.readLine();
					String[] values = line.replaceAll("\\[","").replaceAll("\\]", "").trim().split(" ");
					for(int c=0; c<numWorkSchedules; c++)
						matrixCapacities[w][c] += new Double(values[c]);
				}
			reader.readLine();
			reader.close();
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(OUTPUT_FILE));
			oos.writeObject(matrixCapacities);
			oos.close();
		}
		WorkersAreaPainter workersPainter = new WorkersAreaPainter(network);
		workersPainter.setData(matrixCapacities, dataMPAreas, stopsBase.values());
		new SimpleNetworkWindow("Capacities", workersPainter).setVisible(true);
		Map<Id<ActivityFacility>, ModeShareZone> modeShareZones = new HashMap<Id<ActivityFacility>, ModeShareZone>();
		Iterator<MPAreaData> mPAreaI = dataMPAreas.values().iterator();
		for(int w=0; w<matrixCapacities.length; w++) {
			MPAreaData mPArea = mPAreaI.next();
			ModeShareZone modeShareZone = modeShareZones.get(mPArea.getZoneId());
			if(modeShareZone == null) {
				modeShareZone = new ModeShareZone(mPArea.getModeShare());
				modeShareZones.put(mPArea.getZoneId(), modeShareZone);
			}
			double capacity = 0;
			for(int c=0; c<matrixCapacities[0].length; c++) {
				if(c==modeShareZone.capacities.size())
					modeShareZone.capacities.add(0.0);
				modeShareZone.capacities.set(c, modeShareZone.capacities.get(c)+matrixCapacities[w][c]);
				capacity += matrixCapacities[w][c];
			}
			modeShareZone.difference += getMaxCapacity(mPArea)-capacity;
		}
		System.out.println("Zones done!");
		ActivityFacilityImpl fac = ((ActivityFacilitiesImpl)FacilitiesUtils.createActivityFacilities()).createAndAddFacility(Id.create("dummy", ActivityFacility.class), new Coord(0, 0));
		for(int c=0; c<matrixCapacities[0].length; c++) {
			double[] center = new double[]{0, 0};
			for(PointPerson pointPerson:clusters.get(c).getPoints())
				for(int i=0; i<2; i++)
					center[i] += pointPerson.getElement(i);
			for(int i=0; i<2; i++)
				center[i] /= clusters.get(c).getPoints().size();
			int minStart = ((int) Math.round(((center[0]%3600.0)/60)/15))*15;
			boolean oneHourMoreStart = false;
			if(minStart==60) {
				oneHourMoreStart = true;
				minStart = 0;
			}
			int minDuration = ((int) Math.round(((center[1]%3600.0)/60)/15))*15;
			boolean oneHourMoreDuration = false;
			if(minDuration==60) {
				oneHourMoreDuration = true;
				minDuration = 0;
			}
			NumberFormat numberFormat = new DecimalFormat("00");
			String optionText = "w_"+numberFormat.format(Math.floor(center[0]/3600)+(oneHourMoreStart?1:0))+numberFormat.format(minStart)+"_"+numberFormat.format(Math.floor(center[1]/3600)+(oneHourMoreDuration?1:0))+numberFormat.format(minDuration);
			OpeningTime openingTime = new OpeningTimeImpl(Math.round(center[0]/900)*900, Math.round((center[0]+center[1])/900)*900);
			mPAreaI = dataMPAreas.values().iterator();
			for(int w=0; w<matrixCapacities.length; w++) {
				MPAreaData mPArea = mPAreaI.next();
				double pTCapacityFO = matrixCapacities[w][c];
				ActivityOptionImpl activityOption = new ActivityOptionImpl(optionText);
				double capacity = 0;
				for(int sc=0; sc<matrixCapacities[0].length; sc++)
					capacity += matrixCapacities[w][sc];
				ModeShareZone zone = modeShareZones.get(mPArea.getZoneId());
				double allCap = (getMaxCapacity(mPArea)-capacity)*zone.capacities.get(c)/zone.difference;
				activityOption.setCapacity(pTCapacityFO+(allCap/mPArea.getModeShare()));
				activityOption.addOpeningTime(openingTime);
				mPArea.putActivityOption(activityOption);
			}
		}
	}
	private static ActivityFacilitiesImpl capacitiesToBuildings() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		ActivityFacilitiesImpl facilities= (ActivityFacilitiesImpl)FacilitiesUtils.createActivityFacilities();
		DataBaseAdmin dataBaseAux  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
		ResultSet buildingsR = dataBaseAux.executeQuery("SELECT objectid, mpb.x as xcoord, mpb.y as ycoord, perc_surf as area_perc, fea_id AS id_building, postal_code as postal_code FROM work_facilities_aux.masterplan_areas mpa LEFT JOIN work_facilities_aux.masterplan_building_perc mpb ON mpa.objectid = mpb.object_id  WHERE use_for_generation = 1");
		int b=0;
		Collection<Link> noCarLinks = new ArrayList<Link>();
		for(Link link:network.getLinks().values())
			if(!link.getAllowedModes().contains("car"))
				noCarLinks.add(link);
		for(Link link:noCarLinks)
			network.removeLink(link.getId());
		Map<String, Double> scheduleCapacities = new HashMap<String, Double>();
		while(buildingsR.next()) {
			Id<ActivityFacility> areaId =  Id.create(buildingsR.getString(1), ActivityFacility.class);
			MPAreaData mPArea = dataMPAreas.get(areaId);
			Id<ActivityFacility> id = Id.create((int)(buildingsR.getFloat(5)), ActivityFacility.class);
			if(facilities.getFacilities().get(id)!=null)
				continue;
			ActivityFacilityImpl building = facilities.createAndAddFacility(id, new Coord(buildingsR.getDouble(2), buildingsR.getDouble(3)));
			building.setLinkId(((NetworkImpl)network).getNearestLinkExactly(building.getCoord()).getId());
			building.setDesc(buildingsR.getString(6)+":"+mPArea.getType().replaceAll("&", "AND"));
			double proportion = buildingsR.getDouble(4);
			for(ActivityOption activityOptionArea:mPArea.getActivityOptions().values()) {
				double capacity = activityOptionArea.getCapacity()*proportion;
				if(capacity>0) {
					Double scheduleCapacity = scheduleCapacities.get(activityOptionArea.getType());
					if(scheduleCapacity==null)
						scheduleCapacity = 0.0;
					scheduleCapacities.put(activityOptionArea.getType(), scheduleCapacity+capacity);
					ActivityOptionImpl activityOption = new ActivityOptionImpl(activityOptionArea.getType());
					activityOption.setCapacity(capacity);
					activityOption.addOpeningTime(activityOptionArea.getOpeningTimes().first());
					building.getActivityOptions().put(activityOption.getType(), activityOption);
				}
			}
			b++;
		}
		System.out.println(b + " buildings");
		int numDesiredSchedules = 10;
		String[] schedules = new String[numDesiredSchedules];
		for(int n=0; n<schedules.length; n++) {
			double maxCap = 0;
			String maxSchedule = "";
			for(Entry<String, Double> cap:scheduleCapacities.entrySet()) {
				boolean in = false;
				for(int c=0; c<n; c++)
					if(schedules[c].equals(cap.getKey()))
						in = true;
				if(!in && cap.getValue()>maxCap) {
					maxCap = cap.getValue();
					maxSchedule = cap.getKey();
				}
			}
			schedules[n] = maxSchedule;
		}
		//capacitiesToIntegers(facilities);
		for(Link link:noCarLinks)
			network.addLink(link);
		WorkersBSPainter painter = new WorkersBSPainter(network);
		painter.setData(facilities, schedules);
		new BSSimpleNetworkWindow("Building capacities", painter).setVisible(true);
		return facilities;
	}
	private static void capacitiesToIntegers(ActivityFacilities facilities) {
		for(ActivityFacility building:facilities.getFacilities().values()) {
			String minOption="", maxOption="";
			double minCapacity=Double.MAX_VALUE, maxCapacity=0;
			double rawRest = 0;
			Set<String> zeroOptions = new HashSet<String>();
			for(ActivityOption activityOption:building.getActivityOptions().values()) {
				double rawCapacity = activityOption.getCapacity();
				double capacity = Math.round(rawCapacity);
				if(capacity==0)
					zeroOptions.add(activityOption.getType());
				activityOption.setCapacity(capacity);
				if(rawCapacity<minCapacity) {
					minCapacity = rawCapacity;
					minOption = activityOption.getType();
				}
				if(rawCapacity>maxCapacity) {
					maxCapacity = rawCapacity;
					maxOption = activityOption.getType();
				}
				rawRest += rawCapacity-capacity;
			}
			double rest = Math.round(rawRest);
			if(rest>0)
				building.getActivityOptions().get(maxOption).setCapacity(Math.round(maxCapacity)+rest);
			else
				while(rest<0) {
					rest = Math.round(minCapacity)+rest;
					if(rest>0)
						building.getActivityOptions().get(minOption).setCapacity(rest);
					else {
						building.getActivityOptions().remove(minOption);
						if(rest<0) {
							minCapacity = Double.MAX_VALUE;
							for(ActivityOption activityOption:building.getActivityOptions().values())
								if(activityOption.getCapacity()<minCapacity) {
									minCapacity = activityOption.getCapacity();
									minOption = activityOption.getType();
								}
						}
					}
				}
			for(String zeroOption:zeroOptions)
				building.getActivityOptions().remove(zeroOption);
		}
	}

}

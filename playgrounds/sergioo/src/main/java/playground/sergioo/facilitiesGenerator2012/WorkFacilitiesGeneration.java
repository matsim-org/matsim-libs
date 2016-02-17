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

package playground.sergioo.facilitiesGenerator2012;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
import java.util.Random;
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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.router.AStarLandmarks;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
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

import others.sergioo.util.algebra.Matrix1DImpl;
import others.sergioo.util.algebra.Matrix2DImpl;
import others.sergioo.util.algebra.Matrix3DImpl;
import others.sergioo.util.algebra.MatrixND;
import others.sergioo.util.algebra.PointND;
import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;
import playground.sergioo.facilitiesGenerator2012.gui.WeigthsNetworkWindow;
import playground.sergioo.facilitiesGenerator2012.hits.PersonSchedule;
import playground.sergioo.facilitiesGenerator2012.hits.PointPerson;
import playground.sergioo.facilitiesGenerator2012.hits.Trip;
import playground.sergioo.visualizer2D2012.networkVisualizer.networkPainters.NetworkPainter;

public class WorkFacilitiesGeneration {

	//Constants
	private static int SIZE = 20;
	private static final String HIERACHY_FILE = "../../Dendogram/files/distancesWorkSchedules.txt";
	private static final String NETWORK_FILE = "./data/currentSimulation/singapore2.xml";
	private static final String LINKS_MAP_FILE = "./data/facilities/auxiliar/links.map";
	private static final String NEAREST_LINKS_MAP_FILE = "./data/facilities/auxiliar/nearestLinks.map";
	private static final String WEIGHTS_MAP_FILE = "./data/facilities/auxiliar/weightsMap.map";
	private static final String WEIGHTS2_MAP_FILE = "./data/facilities/auxiliar/weights2.map";
	private static final String CAPACITIES_FILE = "./data/facilities/auxiliar/finalCapacities.map";
	private static final String WORK_FACILITIES_FILEO = "./data/facilities/workFacilitiesO.xml";
	private static final String WORK_FACILITIES_FILE = "./data/facilities/workFacilities.xml";
	//private static final String MATRIX_AREAS_FILE = "./data/facilities/auxiliar/matrixAreas.map";
	private static final double WALKING_SPEED = 4/3.6;
	private static final double PRIVATE_BUS_SPEED = 16/3.6;
	private static final double MAX_TRAVEL_TIME = 15*60;
	private static final String SEPARATOR = ";;;";
	private static final int NUM_NEAR = 3;
	private static int NUM_ITERATIONS = 100;

	//Attributes

	//Methods
	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException, BadStopException {
		ActivityFacilitiesImpl facilities;
		/*try {
			ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
			new FacilitiesReaderMatsimV1(scenario).readFile(WORK_FACILITIES_FILEO);
			facilities = scenario.getActivityFacilities();
		} catch (Exception e) {*/
			if(args.length==2) {
				SIZE = Integer.parseInt(args[0]);
				NUM_ITERATIONS = Integer.parseInt(args[1]);
			}
			SortedMap<Id<ActivityFacility>, ActivityFacility> mPAreas = new TreeMap<Id<ActivityFacility>, ActivityFacility>();
			Map<Id<ActivityFacility>, MPAreaData> dataMPAreas = new HashMap<Id<ActivityFacility>, MPAreaData>();
			Tuple <FittingCapacities, List<CentroidCluster<PointPerson>>> tuple = getFittingCapacitiesObject(mPAreas, dataMPAreas);
			FittingCapacities fittingCapacities = tuple.getFirst();
			List<CentroidCluster<PointPerson>> clusters = tuple.getSecond();
			MatrixND<Double> capacities = null;
			try {
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(CAPACITIES_FILE));
				capacities = (MatrixND<Double>) ois.readObject();
				ois.close();
			} catch (EOFException e2) {
				Runtime.getRuntime().gc();
				capacities = fittingCapacities.run(NUM_ITERATIONS);
				ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(CAPACITIES_FILE));
				oos.writeObject(capacities);
				oos.close();
				System.out.println("Matrix written!");
			}
			Matrix3DImpl matrix = (Matrix3DImpl)capacities;
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
				Iterator<ActivityFacility> mPAreaI = mPAreas.values().iterator();
				for(int f=0; f<matrix.getDimension(0); f++) {
					ActivityFacilityImpl mPArea = (ActivityFacilityImpl) mPAreaI.next();
					MPAreaData mPAreaData = dataMPAreas.get(mPArea.getId());
					double pTCapacityFO = 0;
					for(int s=0; s<matrix.getDimension(2); s++)
						pTCapacityFO += matrix.getElement(f, o, s);
					if(pTCapacityFO>0) {
						ActivityOptionImpl activityOption = new ActivityOptionImpl(optionText);
						activityOption.setCapacity(pTCapacityFO/mPAreaData.getModeShare());
						activityOption.addOpeningTime(openingTime);
						mPArea.getActivityOptions().put(activityOption.getType(), activityOption);
					}
				}
			}
			/*PrintWriter printWriter = new PrintWriter(MATRIX_AREAS_FILE);
			Iterator<MPAreaData> mPIterator = dataMPAreas.values().iterator();
			int i = 0;
			while(mPIterator.hasNext())
				printWriter.println(i+++" "+mPIterator.next().getId());
			printWriter.close();*/
			DataBaseAdmin dataBaseAux  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
			ResultSet buildingsR = dataBaseAux.executeQuery("SELECT id, area_perc, no_ AS id_building, xcoord AS xcoord_bldg, ycoord AS ycoord_bldg FROM work_facilities_aux.buildings LEFT JOIN work_facilities_aux.building_perc ON FID_master = id WHERE use_for_generation = 1");
			CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
			facilities = (ActivityFacilitiesImpl) FacilitiesUtils.createActivityFacilities();
			while(buildingsR.next()) {
				Id<ActivityFacility> areaId =  Id.create(buildingsR.getString(1), ActivityFacility.class);
				ActivityFacilityImpl mPArea = (ActivityFacilityImpl) mPAreas.get(areaId);
				MPAreaData mPAreaData = dataMPAreas.get(areaId);
				ActivityFacilityImpl building = facilities.createAndAddFacility(Id.create(buildingsR.getString(3), ActivityFacility.class), coordinateTransformation.transform(new Coord(buildingsR.getDouble(4), buildingsR.getDouble(5))));
				building.setDesc(mPAreaData.getType());
				double proportion = buildingsR.getDouble(2);
				for(ActivityOption activityOptionArea:mPArea.getActivityOptions().values()) {
					double capacity = activityOptionArea.getCapacity()*proportion;
					if(capacity>0) {
						ActivityOptionImpl activityOption = new ActivityOptionImpl(activityOptionArea.getType());
						activityOption.setCapacity(capacity);
						activityOption.addOpeningTime(activityOptionArea.getOpeningTimes().first());
						building.getActivityOptions().put(activityOption.getType(), activityOption);
					}
				}
			}
		//}
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
		/*Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(NETWORK_FILE);
		Network network = scenario.getNetwork();
		for(ActivityFacility building:facilities.getFacilities().values()) {
			Link nearestLink =null;
			double nearestDistance = Double.MAX_VALUE;
			for(Link link:network.getLinks().values())
				if(link.getAllowedModes().contains("car")) {
					double distance = CoordUtils.distancePointLinesegment(link.getFromNode().getCoord(), link.getToNode().getCoord(), building.getCoord());
					if(distance<nearestDistance) {
						nearestDistance = distance;
						nearestLink = link;
					}
				}
			((ActivityFacilityImpl)building).setLinkId(nearestLink.getId());
		}*/
		new FacilitiesWriter(facilities).write(WORK_FACILITIES_FILE);
		/*createTripsTables();
		writeHierachicalClustersFile(clusterWorkActivities(getWorkActivityTimes()));
		MatrixND<Double> weights = new MatrixNDImpl<Double>(new int[]{5,4}, 0.5);
		MatrixND<Double> quantities = new MatrixNDImpl<Double>(new int[]{3,4}, 20.0);
		MatrixND<Double> proportions = new MatrixNDImpl<Double>(new int[]{5,3}, 0.3333);
		MatrixND<Double> maxs = new MatrixNDImpl<Double>(new int[]{5}, 60.0);
		new FittingCapacities(new int[]{5,3,4}, weights, quantities, proportions, maxs).run(20);*/
	}
	private static void createTripsTables() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseAux  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
		ResultSet tripsResult = dataBaseAux.executeQuery("SELECT * FROM work_pt_trips_full");
		Map<String, Integer> users = new HashMap<String, Integer>();
		int i=0;
		while(tripsResult.next()) {
			ResultSet stopResult = dataBaseAux.executeQuery("SELECT * FROM stops where id='"+tripsResult.getString(2)+"'");
			if(!stopResult.next())
				dataBaseAux.executeStatement("INSERT INTO stops VALUES ('"+tripsResult.getString(2)+"',"+tripsResult.getDouble(3)+","+tripsResult.getDouble(4)+")");
			stopResult.close();
			stopResult = dataBaseAux.executeQuery("SELECT * FROM stops where id='"+tripsResult.getString(6)+"'");
			if(!stopResult.next())
				dataBaseAux.executeStatement("INSERT INTO stops VALUES ('"+tripsResult.getString(6)+"',"+tripsResult.getDouble(7)+","+tripsResult.getDouble(8)+")");
			stopResult.close();
			Integer num = users.get(tripsResult.getString(1));
			if(num==null)
				num = 0;
			users.put(tripsResult.getString(1), ++num);
			dataBaseAux.executeStatement("INSERT INTO work_pt_trips VALUES ('"+tripsResult.getString(1)+"_"+num+"','"+tripsResult.getString(2)+"',"+((tripsResult.getTime(5).getTime()/1000)+27000)+",'"+tripsResult.getString(6)+"',"+((tripsResult.getTime(9).getTime()/1000)+27000)+")");
			if(i%100==0)
				System.out.println(i);
			i++;
		}
		tripsResult.close();
		dataBaseAux.close();
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
		/*points.clear();
		double a=Math.PI/6;
		double dx=3;
		double dy=7;
		double sx=4;
		double sy=0.5;
		for(int i=0; i<1000; i++) {
			double x=Math.random();
			double y=Math.random();
			points.put(i+"", new PointPerson(i+"", "any", new Double[]{Math.cos(a)*sx*x-Math.sin(a)*sy*y+dx, Math.sin(a)*sx*x+Math.cos(a)*sy*y+dy}));
		}*/
		return points;
	}
	private static List<CentroidCluster<PointPerson>> clusterWorkActivities(Map<String,PointPerson> points) {
		Set<PointPerson> pointsC = getPCATransformation(points.values());
		Random r = new Random();
		List<CentroidCluster<PointPerson>> clusters = new KMeansPlusPlusClusterer<PointPerson>(SIZE, 100).cluster(pointsC);
		//new ClustersWindow("Work times cluster PCA: "+getClustersDeviations(clusters)+" "+getWeightedClustersDeviations(clusters), clusters, pointsC.size()).setVisible(true);
		for(Cluster<PointPerson> cluster:clusters)
			for(PointPerson pointPersonT:cluster.getPoints()) {
				PointPerson pointPerson = points.get(pointPersonT.getId());
				for(int d=0; d<pointPersonT.getDimension(); d++)
					pointPersonT.setElement(d, pointPerson.getElement(d));
			}
		//new ClustersWindow("Work times cluster PCA back: "+getClustersDeviations(clusters)+" "+getWeightedClustersDeviations(clusters), clusters, pointsC.size()).setVisible(true);
		/*List<Cluster<PointPerson>> clusters2 = new KMeansPlusPlusClusterer<PointPerson>(new Random()).cluster(points.values(), SIZE, 100);
		new ClustersWindow("Work times cluster: "+getClustersDeviations(clusters2)+" "+getWeightedClustersDeviations(clusters2), clusters2, points.size()).setVisible(true);
		for(Cluster<PointPerson> clusterE:clusters) {
				double startTime = clusterE.getCenter().getElement(0);
				double endTime = clusterE.getCenter().getElement(1);
				System.out.println();
				System.out.println("    ("+startTime+","+endTime+")");
				System.out.println("    ("+((int)startTime/(15*60))*(15*60)+","+((int)endTime/(15*60))*(15*60)+")");
				System.out.println("    ("+(int)startTime/3600+":"+((int)startTime%3600)/60+","+(int)endTime/3600+":"+((int)endTime%3600)/60+")");
				System.out.println("    ("+((int)startTime/(15*60))*(15*60)/3600+":"+(((int)startTime/(15*60))*(15*60)%3600)/60+","+((int)endTime/(15*60))*(15*60)/3600+":"+(((int)endTime/(15*60))*(15*60)%3600)/60+")");
				System.out.println("    "+clusterE.getPoints().size());
		}*/
		return clusters;
	}
	/*private static Map<Integer, util.clustering.Cluster<Double>> clusterWorkActivities(Set<PointND<Double>> points) {
		Map<Integer, util.clustering.Cluster<Double>> clusters = new KMeans<Double>().getClusters(SIZE, points);
		new ClustersWindow("Work times cluster", clusters, points.size()).setVisible(true);
		for(Entry<Integer, util.clustering.Cluster<Double>> clusterE:clusters.entrySet())
			if(!clusterE.getValue().isEmpty()) {
				double startTime = clusterE.getValue().getMean().getElement(0);
				double endTime = clusterE.getValue().getMean().getElement(1);
				System.out.println();
				System.out.println(clusterE.getKey());
				System.out.println("    ("+startTime+","+endTime+")");
				System.out.println("    ("+((int)startTime/(15*60))*(15*60)+","+((int)endTime/(15*60))*(15*60)+")");
				System.out.println("    ("+(int)startTime/3600+":"+((int)startTime%3600)/60+","+(int)endTime/3600+":"+((int)endTime%3600)/60+")");
				System.out.println("    ("+((int)startTime/(15*60))*(15*60)/3600+":"+(((int)startTime/(15*60))*(15*60)%3600)/60+","+((int)endTime/(15*60))*(15*60)/3600+":"+(((int)endTime/(15*60))*(15*60)%3600)/60+")");
				System.out.println("    "+clusterE.getValue().getPoints().size());
			}
		return clusters;
	}*/
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
	private static void writeHierachicalClustersFile(List<CentroidCluster<PointPerson>> clusters) throws FileNotFoundException {
		PrintWriter printWriter = new PrintWriter(HIERACHY_FILE);
		int i=0;
		for(CentroidCluster<PointPerson> clusterA:clusters) {
			double startTimeA = ((PointPerson)clusterA.getCenter()).getElement(0);
			double endTimeA = ((PointPerson)clusterA.getCenter()).getElement(1);
			String clusterAText = ((int)startTimeA/(15*60))*(15*60)/3600+":"+(((int)startTimeA/(15*60))*(15*60)%3600)/60+"_"+((int)endTimeA/(15*60))*(15*60)/3600+":"+(((int)endTimeA/(15*60))*(15*60)%3600)/60;
			int j=0;
			for(CentroidCluster<PointPerson> clusterB:clusters) {
				double startTimeB = ((PointPerson)clusterB.getCenter()).getElement(0);
				double endTimeB = ((PointPerson)clusterB.getCenter()).getElement(1);
				String clusterBText = ((int)startTimeB/(15*60))*(15*60)/3600+":"+(((int)startTimeB/(15*60))*(15*60)%3600)/60+"_"+((int)endTimeB/(15*60))*(15*60)/3600+":"+(((int)endTimeB/(15*60))*(15*60)%3600)/60;
				if(i<j)
					printWriter.println(clusterAText+"_"+i+" "+clusterBText+"_"+j+" "+((PointPerson)clusterA.getCenter()).distanceFrom(((PointPerson)clusterB.getCenter())));
				j++;
			}
			i++;
		}
		printWriter.close();
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
	private static Tuple<Map<Tuple<Id<TransitStopFacility>, Id<ActivityFacility>>,Tuple<Boolean,Double>>, Network> calculateAreaStopWeights(Map<String, Coord> stopsBase, Map<Id<TransitStopFacility>, Double> stopsCapacities, Map<String, Double> workerAreas, SortedMap<Id<ActivityFacility>, ActivityFacility> mPAreas, Map<Id<ActivityFacility>, MPAreaData> dataMPAreas) throws BadStopException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(NETWORK_FILE);
		Network network = scenario.getNetwork();
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
					for(Link link:network.getLinks().values()) {
						if(link.getAllowedModes().contains("car")) {
							boolean selected = false;
							for(int p=0; p<n; p++)
								if(linksStops.get(p).get(stopBase.getKey()).equals(link.getId()))
									selected=true;
							if(!selected && CoordUtils.calcEuclideanDistance(link.getCoord(), stopBase.getValue())<CoordUtils.calcEuclideanDistance(network.getLinks().get(nearest).getCoord(), stopBase.getValue()))
								nearest = link.getId();
						}
					}
					linksStops.get(n).put(stopBase.getKey(), nearest);
				}
			}
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(LINKS_MAP_FILE));
			oos.writeObject(linksStops);
			oos.close();
		}
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
				return link.getLength();
			}
		};
		PreProcessLandmarks preProcessData = new PreProcessLandmarks(travelMinCost);
		preProcessData.run(network);
		AStarLandmarks aStarLandmarks = new AStarLandmarks(network, preProcessData, timeFunction);
		DataBaseAdmin dataBaseAuxiliar  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
		ResultSet mPAreasR = dataBaseAuxiliar.executeQuery("SELECT * FROM buildings WHERE use_for_generation = 1");
		while(mPAreasR.next()) {
			mPAreas.put(Id.create(mPAreasR.getString(1), ActivityFacility.class), null);//new ActivityFacilityImpl(Id.create(mPAreasR.getString(1), ActivityFacility.class),coordinateTransformation.transform(new CoordImpl(mPAreasR.getDouble(4), mPAreasR.getDouble(5)))));
			dataMPAreas.put(Id.create(mPAreasR.getString(1), ActivityFacility.class), new MPAreaData(mPAreasR.getInt(1),mPAreasR.getString(2), mPAreasR.getDouble(3), mPAreasR.getDouble(6)));
		}
		mPAreasR.close();
		dataBaseAuxiliar.close();
		//Find nearest good links
		Map<String, String> nearestLinks;
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(NEAREST_LINKS_MAP_FILE));
			nearestLinks = (Map<String, String>) ois.readObject();
			ois.close();
		} catch(EOFException e) {
			nearestLinks = new HashMap<String, String>();
			for(ActivityFacility mPArea:mPAreas.values()) {
				Link nearestLink =null;
				double nearestDistance = Double.MAX_VALUE;
				for(Link link:network.getLinks().values())
					if(link.getAllowedModes().contains("car")) {
						double distance = CoordUtils.distancePointLinesegment(link.getFromNode().getCoord(), link.getToNode().getCoord(), mPArea.getCoord());
						if(distance<nearestDistance) {
							nearestDistance = distance;
							nearestLink = link;
						}
					}
				nearestLinks.put(mPArea.getId().toString(), nearestLink.getId().toString());
			}
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(NEAREST_LINKS_MAP_FILE));
			oos.writeObject(nearestLinks);
			oos.close();
		}
		for(ActivityFacility mPArea:mPAreas.values())
			((ActivityFacilityImpl)mPArea).setLinkId(Id.createLinkId(nearestLinks.get(mPArea.getId().toString())));
		//Compute stops facilities weights
		Map<Tuple<Id<TransitStopFacility>, Id<ActivityFacility>>,Tuple<Boolean,Double>> weights;
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(WEIGHTS_MAP_FILE));
			weights = (Map<Tuple<Id<TransitStopFacility>, Id<ActivityFacility>>, Tuple<Boolean, Double>>) ois.readObject();
			ois.close();
		} catch(EOFException e) {
			weights = new HashMap<Tuple<Id<TransitStopFacility>, Id<ActivityFacility>>, Tuple<Boolean, Double>>();
			int i=0;
			for(String stopKey: stopsBase.keySet()) {
				double maxTimeFromStop = 0;
				System.out.println(i+++" of "+stopsBase.size());
				Id<Link>[] links = new Id[NUM_NEAR];
				for(int n=0; n<NUM_NEAR; n++)
					links[n]=linksStops.get(n).get(stopKey);
				Id<TransitStopFacility> stopId = Id.create(stopKey, TransitStopFacility.class);
				double maxCapacityNearFacilities = 0;
				for(ActivityFacility mPArea:mPAreas.values())
					if(CoordUtils.calcEuclideanDistance(stopsBase.get(stopKey), mPArea.getCoord())<MAX_TRAVEL_TIME*WALKING_SPEED) {
						double walkingTime = Double.MAX_VALUE;
						for(int n=0; n<NUM_NEAR; n++) {
							double walkingTimeA=aStarLandmarks.calcLeastCostPath(network.getLinks().get(links[n]).getToNode(), network.getLinks().get(mPArea.getLinkId()).getFromNode(), 0, null, null).travelCost;
							if(walkingTimeA<walkingTime)
								walkingTime = walkingTimeA;
						}
						if(walkingTime<=MAX_TRAVEL_TIME) {
							weights.put(new Tuple<Id<TransitStopFacility>, Id<ActivityFacility>>(stopId, mPArea.getId()), new Tuple<Boolean, Double>(true, walkingTime));
							if(walkingTime>maxTimeFromStop)
								maxTimeFromStop = walkingTime;
							MPAreaData dataMPArea = dataMPAreas.get(mPArea.getId());
							maxCapacityNearFacilities += (dataMPArea.getMaxArea()/workerAreas.get(dataMPArea.getType()))*dataMPArea.getModeShare();
						}
					}
				if(stopsCapacities.get(stopId)>maxCapacityNearFacilities) {
					double maxCapacityNear2Facilities = maxCapacityNearFacilities;
					for(ActivityFacility mPArea:mPAreas.values())
						if(CoordUtils.calcEuclideanDistance(stopsBase.get(stopKey), mPArea.getCoord())<(MAX_TRAVEL_TIME*2/3)*PRIVATE_BUS_SPEED) {
							double walkingTime = Double.MAX_VALUE;
							for(int n=0; n<NUM_NEAR; n++) {
								double walkingTimeA=aStarLandmarks.calcLeastCostPath(network.getLinks().get(links[n]).getToNode(), network.getLinks().get(mPArea.getLinkId()).getFromNode(), 0, null, null).travelCost;
								if(walkingTimeA<walkingTime)
									walkingTime = walkingTimeA;
							}
							double privateBusTime = Double.MAX_VALUE;
							for(int n=0; n<NUM_NEAR; n++) {
								double privateBusTimeA=aStarLandmarks.calcLeastCostPath(network.getLinks().get(links[n]).getToNode(), network.getLinks().get(mPArea.getLinkId()).getFromNode(), 0, null, null).travelCost*WALKING_SPEED/PRIVATE_BUS_SPEED;
								if(privateBusTimeA<privateBusTime)
									privateBusTime = privateBusTimeA;
							}
							if(walkingTime>MAX_TRAVEL_TIME && privateBusTime<=(MAX_TRAVEL_TIME*2/3)) {
								weights.put(new Tuple<Id<TransitStopFacility>, Id<ActivityFacility>>(stopId, mPArea.getId()), new Tuple<Boolean, Double>(false, privateBusTime));
								if(privateBusTime>maxTimeFromStop)
									maxTimeFromStop = privateBusTime;
								MPAreaData dataMPArea = dataMPAreas.get(mPArea.getId());
								maxCapacityNear2Facilities += (dataMPArea.getMaxArea()/workerAreas.get(dataMPArea.getType()))*dataMPArea.getModeShare();
							}
						}
					if(stopsCapacities.get(stopId)>maxCapacityNear2Facilities) {
						System.out.println("Far" + stopId);
						double maxCapacityNear3Facilities = maxCapacityNear2Facilities;
						for(ActivityFacility mPArea:mPAreas.values())
							if(CoordUtils.calcEuclideanDistance(stopsBase.get(stopKey), mPArea.getCoord())<MAX_TRAVEL_TIME*PRIVATE_BUS_SPEED) {
								double privateBusTime = Double.MAX_VALUE;
								for(int n=0; n<NUM_NEAR; n++) {
									double privateBusTimeA=aStarLandmarks.calcLeastCostPath(network.getLinks().get(links[n]).getToNode(), network.getLinks().get(mPArea.getLinkId()).getFromNode(), 0, null, null).travelCost*WALKING_SPEED/PRIVATE_BUS_SPEED;
									if(privateBusTimeA<privateBusTime)
										privateBusTime = privateBusTimeA;
								}
								if(privateBusTime>(MAX_TRAVEL_TIME*2/3) && privateBusTime<=MAX_TRAVEL_TIME) {
									weights.put(new Tuple<Id<TransitStopFacility>, Id<ActivityFacility>>(stopId, mPArea.getId()), new Tuple<Boolean, Double>(false, privateBusTime));
									if(privateBusTime>maxTimeFromStop)
										maxTimeFromStop = privateBusTime;
									MPAreaData dataMPArea = dataMPAreas.get(mPArea.getId());
									maxCapacityNear3Facilities += (dataMPArea.getMaxArea()/workerAreas.get(dataMPArea.getType()))*dataMPArea.getModeShare();
								}
							}
						if(stopsCapacities.get(stopId)>maxCapacityNear3Facilities) {
							System.out.println("Very far" + stopId);
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
			}
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(WEIGHTS_MAP_FILE));
			oos.writeObject(weights);
			oos.close();
		}
		return new Tuple<Map<Tuple<Id<TransitStopFacility>, Id<ActivityFacility>>, Tuple<Boolean, Double>>, Network>(weights, network);
	}
	private static Map<String, Double> calculateStopClustersQuantities(List<CentroidCluster<PointPerson>> clusters, Map<Id<TransitStopFacility>, Double> stops) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseAux  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
		Map<String, Double> quantities = new HashMap<String, Double>();
		ResultSet tripsResult = dataBaseAux.executeQuery("SELECT * FROM work_pt_trips");
		while(tripsResult.next()) {
			Id<TransitStopFacility> stopId = Id.create(tripsResult.getString(2), TransitStopFacility.class);
			Double quantity = stops.get(stopId);
			if(quantity==null)
				quantity = 0.0;
			stops.put(stopId, quantity+0.5);
			stopId = Id.create(tripsResult.getString(4), TransitStopFacility.class);
			quantity = stops.get(stopId);
			if(quantity==null)
				quantity = 0.0;
			stops.put(stopId, quantity+0.5);
			int nearestCluster = 0;
			PointPerson time = new PointPerson(tripsResult.getString(1), "", new Double[]{(double) tripsResult.getInt(3), (double) (tripsResult.getInt(5)-tripsResult.getInt(3))}, "");
			for(int c=0; c<clusters.size(); c++) {
				if(((PointPerson)clusters.get(c).getCenter()).distanceFrom(time)<((PointPerson)clusters.get(nearestCluster).getCenter()).distanceFrom(time))
					nearestCluster = c;
			}
			String key = tripsResult.getString(2)+SEPARATOR+nearestCluster;
			quantity = quantities.get(key);
			if(quantity==null)
				quantity = 0.0;
			quantities.put(key, quantity+0.5);
			key = tripsResult.getString(4)+SEPARATOR+nearestCluster;
			quantity = quantities.get(key);
			if(quantity==null)
				quantity = 0.0;
			quantities.put(key, quantity+0.5);
		}
		tripsResult.close();
		dataBaseAux.close();
		return quantities;
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
	private static Tuple<FittingCapacities, List<CentroidCluster<PointPerson>>> getFittingCapacitiesObject(SortedMap<Id<ActivityFacility>, ActivityFacility> mPAreas,  Map<Id<ActivityFacility>, MPAreaData> dataMPAreas) throws BadStopException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		System.out.println("Process starts with "+SIZE+" clusters and "+NUM_ITERATIONS+" iterations.");
		List<CentroidCluster<PointPerson>> clusters = clusterWorkActivities(getWorkActivityTimes());
		System.out.println("Clustering done!");
		DataBaseAdmin dataBaseAux  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
		SortedMap<String, Coord> stopsBase = new TreeMap<String, Coord>();
		ResultSet stopsResult = dataBaseAux.executeQuery("SELECT * FROM stops");
		while(stopsResult.next())
			stopsBase.put(stopsResult.getString(1), new Coord(stopsResult.getDouble(3), stopsResult.getDouble(2)));
		stopsResult.close();
		System.out.println("Stops done!");
		Map<Id<TransitStopFacility>, Double> stops = new HashMap<Id<TransitStopFacility>, Double>();
		Map<String, Double> quantitiesMap = calculateStopClustersQuantities(clusters, stops);
		MatrixND<Double> quantities = new Matrix2DImpl(new int[]{clusters.size(),stopsBase.size()});
		for(int c=0; c<quantities.getDimension(0); c++) {
			Iterator<String> stopsI = stopsBase.keySet().iterator();
			for(int s=0; s<quantities.getDimension(1); s++) {
				Double quantity = quantitiesMap.get(stopsI.next()+SEPARATOR+c);
				if(quantity==null)
					quantity = 0.0;
				quantities.setElement(new int[]{c,s}, quantity);
			}
		}
		System.out.println("Quantities done!");
		Map<String, Double> workerAreas = new HashMap<String, Double>();
		ResultSet typesResult = dataBaseAux.executeQuery("SELECT * FROM building_types");
		while(typesResult.next())
			workerAreas.put(typesResult.getString(1), typesResult.getDouble(2));
		typesResult.close();
		Tuple<Map<Tuple<Id<TransitStopFacility>, Id<ActivityFacility>>,Tuple<Boolean,Double>>, Network> weightsMap = calculateAreaStopWeights(stopsBase, stops, workerAreas, mPAreas, dataMPAreas);
		WeigthsNetworkWindow weigthsNetworkWindow = new WeigthsNetworkWindow("Weights", new NetworkPainter(weightsMap.getSecond()), weightsMap.getFirst(), mPAreas, stopsBase);
		weigthsNetworkWindow.setVisible(true);
		while(!weigthsNetworkWindow.isReadyToExit())
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		System.out.println("Facilities done!");
		Matrix2DImpl weights = new Matrix2DImpl(new int[]{mPAreas.size(),stopsBase.size()});
		Iterator<Id<ActivityFacility>> mPAreaI = mPAreas.keySet().iterator();
		for(int f=0; f<weights.getDimension(0); f++) {
			Id<ActivityFacility> facilityId = mPAreaI.next();
			Iterator<String> stopsI = stopsBase.keySet().iterator();
			for(int s=0; s<weights.getDimension(1); s++) {
				Double weight = weightsMap.getFirst().get(new Tuple<Id<TransitStopFacility>, Id<ActivityFacility>>(Id.create(stopsI.next(), TransitStopFacility.class), facilityId)).getSecond();
				if(weight==null)
					weight = 0.0;
				weights.setElement(f, s, weight);
			}
		}
		System.out.println("Weights done!");
		Matrix2DImpl proportions = new Matrix2DImpl(new int[]{mPAreas.size(),clusters.size()});
		Map<String, List<Double>> proportionsMap = calculateTypeBuildingOptionWeights(clusters);
		mPAreaI = mPAreas.keySet().iterator();
		for(int f=0; f<proportions.getDimension(0); f++) {
			Id<ActivityFacility> facilityId = mPAreaI.next();
			for(int c=0; c<proportions.getDimension(1); c++)
				proportions.setElement(f, c, proportionsMap.get(dataMPAreas.get(facilityId).getType()).get(c));
		}
		System.out.println("Proportions done!");
		MatrixND<Double> maxs = new Matrix1DImpl(new int[]{mPAreas.size()}, 60.0);
		dataBaseAux.close();
		mPAreaI = mPAreas.keySet().iterator();
		for(int f=0; f<maxs.getDimension(0); f++) {
			MPAreaData dataMPArea = dataMPAreas.get(mPAreaI.next());
			double max = (dataMPArea.getMaxArea()/workerAreas.get(dataMPArea.getType()))*dataMPArea.getModeShare();
			maxs.setElement(new int[]{f}, max);
		}
		System.out.println("Max areas done!");
		return new Tuple<FittingCapacities, List<CentroidCluster<PointPerson>>>(new FittingCapacities(new int[]{mPAreas.size(),clusters.size(),stopsBase.size()}, weights, quantities, proportions, maxs), clusters) ;
	}

}

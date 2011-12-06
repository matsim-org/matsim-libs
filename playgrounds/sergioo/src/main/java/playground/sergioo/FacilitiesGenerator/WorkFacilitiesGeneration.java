package playground.sergioo.FacilitiesGenerator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
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

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.ArrayRealVector;
import org.apache.commons.math.linear.EigenDecomposition;
import org.apache.commons.math.linear.EigenDecompositionImpl;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.linear.RealVector;
import org.apache.commons.math.stat.clustering.Cluster;
import org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.AStarLandmarks;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.algorithms.WorldConnectLocations;

import playground.sergioo.FacilitiesGenerator.gui.ClustersWindow;
import playground.sergioo.FacilitiesGenerator.hits.PersonSchedule;
import playground.sergioo.FacilitiesGenerator.hits.PointPerson;
import playground.sergioo.FacilitiesGenerator.hits.Trip;

import util.algebra.MatrixND;
import util.algebra.MatrixNDImpl;
import util.algebra.PointND;
import util.dataBase.DataBaseAdmin;
import util.dataBase.NoConnectionException;

public class WorkFacilitiesGeneration {

	//Constants
	private static final int SIZE = 20;
	private static final String HIERACHY_FILE = "../../Dendogram/files/distancesWorkSchedules.txt";
	private static final String NETWORK_FILE = "./data/currentSimulation/singapore2.xml";
	private static final double WALKING_SPEED = 4/3.6;
	private static final double PRIVATE_BUS_SPEED = 40/3.6;
	private static final double MAX_TRAVEL_TIME = 15*60;
	private static final String SEPARATOR = ";;;";
	private static final int NUM_NEAR = 3;
	
	//Attributes

	//Methods
	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException, BadStopException {
		//writeHierachicalClustersFile(clusterWorkActivities(getWorkActivityTimes()));
		/*MatrixND<Double> weights = new MatrixNDImpl<Double>(new int[]{5,4}, 0.5);
		MatrixND<Double> quantities = new MatrixNDImpl<Double>(new int[]{3,4}, 20.0);
		MatrixND<Double> proportions = new MatrixNDImpl<Double>(new int[]{5,3}, 0.3333);
		MatrixND<Double> maxs = new MatrixNDImpl<Double>(new int[]{5}, 60.0);
		new FittingCapacities(new int[]{5,3,4}, weights, quantities, proportions, maxs).run(20);*/
	}
	public static Map<String, PointPerson> getWorkActivityTimes() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseHits  = new DataBaseAdmin(new File("./data/hits/DataBase.properties"));
		ResultSet timesResult = dataBaseHits.executeQuery("SELECT pax_idx,trip_id,t6_purpose,t3_starttime,t4_endtime,p6_occup FROM hits.hitsshort");
		Map<String, PersonSchedule> times = new HashMap<String, PersonSchedule>();
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
					timesPerson.getTrips().put(timesResult.getInt(2), new Trip(timesResult.getString(3), startTime, endTime));
				}
			}
		}
		timesResult.close();
		Map<String, PointPerson> points = new HashMap<String, PointPerson>();
		for(PersonSchedule timesPerson:times.values()) {
			SortedMap<Integer, Trip> tripsPerson = timesPerson.getTrips();
			boolean startTimeSaved=false;
			double startTime=-1, endTime=-1;
			if(tripsPerson.size()>0) {
				for(int i = tripsPerson.keySet().iterator().next(); i<=tripsPerson.size(); i ++) {
					if(!startTimeSaved && tripsPerson.get(i).getPurpose()!=null && tripsPerson.get(i).getPurpose().equals("work")) {
						startTime = tripsPerson.get(i).getEndTime();
						startTimeSaved = true;
					}
					if(i>tripsPerson.keySet().iterator().next() && tripsPerson.get(i-1).getPurpose().equals("work"))
						endTime = tripsPerson.get(i).getStartTime();
				}
			}
			if(startTime!=-1 && endTime!=-1 && endTime-startTime>=7*3600 && endTime-startTime<=16*3600)
				if(startTime>24*3600)
					points.put(timesPerson.getId(), new PointPerson(timesPerson.getId(), timesPerson.getOccupation(), new Double[]{startTime-24*3600, endTime-24*3600-(startTime-24*3600)}));
				else
					points.put(timesPerson.getId(), new PointPerson(timesPerson.getId(), timesPerson.getOccupation(), new Double[]{startTime, endTime-startTime}));
		}
		Map<String, Double> weights = new HashMap<String, Double>();
		ResultSet weightsR = dataBaseHits.executeQuery("SELECT pax_idx,hipf10  FROM hits.hitsshort_geo_hipf");
		while(weightsR.next())
			weights.put(weightsR.getString(1), weightsR.getDouble(2));
		for(PointPerson pointPerson:points.values()) {
			if(weights.get(pointPerson.getId())!=null)
				pointPerson.setWeight(weights.get(pointPerson.getId()));
			else
				pointPerson.setWeight(100);
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
	public static List<Cluster<PointPerson>> clusterWorkActivities(Map<String,PointPerson> points) {
		Set<PointPerson> pointsC = getPCATransformation(points.values());
		Random r = new Random();
		List<Cluster<PointPerson>> clusters = new KMeansPlusPlusClusterer<PointPerson>(r).cluster(pointsC, SIZE, 100);
		new ClustersWindow("Work times cluster PCA: "+getClustersDeviations(clusters)+" "+getWeightedClustersDeviations(clusters), clusters, pointsC.size()).setVisible(true);
		for(Cluster<PointPerson> cluster:clusters)
			for(PointPerson pointPersonT:cluster.getPoints()) {
				PointPerson pointPerson = points.get(pointPersonT.getId());
				for(int d=0; d<pointPersonT.getDimension(); d++)
					pointPersonT.setElement(d, pointPerson.getElement(d));
			}
		new ClustersWindow("Work times cluster PCA back: "+getClustersDeviations(clusters)+" "+getWeightedClustersDeviations(clusters), clusters, pointsC.size()).setVisible(true);
		List<Cluster<PointPerson>> clusters2 = new KMeansPlusPlusClusterer<PointPerson>(new Random()).cluster(points.values(), SIZE, 100);
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
		}
		return clusters;
	}
	/*public static Map<Integer, util.clustering.Cluster<Double>> clusterWorkActivities(Set<PointND<Double>> points) {
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
	private static double getClustersDeviations(List<Cluster<PointPerson>> clusters) {
		double deviation = 0;
		for(Cluster<PointPerson> cluster:clusters)
			for(PointPerson pointPerson:cluster.getPoints())
				deviation += cluster.getCenter().distanceFrom(pointPerson);
		return deviation;
	}
	private static double getWeightedClustersDeviations(List<Cluster<PointPerson>> clusters) {
		double deviation = 0, totalWeight=0;
		for(Cluster<PointPerson> cluster:clusters) {
			for(PointPerson pointPerson:cluster.getPoints()) {
				deviation += cluster.getCenter().distanceFrom(pointPerson)*pointPerson.getWeight();
				totalWeight = pointPerson.getWeight();
			}
		}
		return deviation/totalWeight;
	}
	private static void writeHierachicalClustersFile(List<Cluster<PointPerson>> clusters) throws FileNotFoundException {
		PrintWriter printWriter = new PrintWriter(HIERACHY_FILE);
		int i=0;
		for(Cluster<PointPerson> clusterA:clusters) {
			double startTimeA = clusterA.getCenter().getElement(0);
			double endTimeA = clusterA.getCenter().getElement(1);
			String clusterAText = ((int)startTimeA/(15*60))*(15*60)/3600+":"+(((int)startTimeA/(15*60))*(15*60)%3600)/60+"_"+((int)endTimeA/(15*60))*(15*60)/3600+":"+(((int)endTimeA/(15*60))*(15*60)%3600)/60;
			int j=0;
			for(Cluster<PointPerson> clusterB:clusters) {
				double startTimeB = clusterB.getCenter().getElement(0);
				double endTimeB = clusterB.getCenter().getElement(1);
				String clusterBText = ((int)startTimeB/(15*60))*(15*60)/3600+":"+(((int)startTimeB/(15*60))*(15*60)%3600)/60+"_"+((int)endTimeB/(15*60))*(15*60)/3600+":"+(((int)endTimeB/(15*60))*(15*60)%3600)/60;
				if(i<j)
					printWriter.println(clusterAText+"_"+i+" "+clusterBText+"_"+j+" "+clusterA.getCenter().distanceFrom(clusterB.getCenter()));
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
		EigenDecomposition eigenDecomposition = new EigenDecompositionImpl(covariance, 0);
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
			PointPerson pointC = new PointPerson(point.getId(), point.getOccupation(), new Double[]{pointsM.getEntry(0, k), pointsM.getEntry(1, k)});
			pointC.setWeight(point.getWeight());
			pointsC.add(pointC);
			k++;
		}
		return pointsC;
	}
	private static Map<Tuple<Id, Id>,Double> calculateAreaStopWeights(Map<String, Coord> stopsBase, ActivityFacilitiesImpl facilities, Map<Id, Tuple<String,Double>> typeAndMaxCapacityFacilities) throws BadStopException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseTransit(true);
		new MatsimNetworkReader(scenario).readFile(NETWORK_FILE);
		Network network = scenario.getNetwork();
		List<Map<String, Id>> linksStops = new ArrayList<Map<String,Id>>();
		for(int n=0; n<NUM_NEAR; n++) {
			linksStops.add(new HashMap<String, Id>());
			for(Entry<String, Coord> stopBase: stopsBase.entrySet()) {
				Id nearest = network.getLinks().values().iterator().next().getId();
				for(Link link:network.getLinks().values()) {
					boolean selected = false;
					for(int p=0; p<n; p++)
						if(linksStops.get(p).get(stopBase.getKey()).equals(link.getId()))
							selected=true;
					if(!selected && CoordUtils.calcDistance(link.getCoord(), stopBase.getValue())<CoordUtils.calcDistance(network.getLinks().get(nearest).getCoord(), stopBase.getValue()))
						nearest = link.getId();
				}
				linksStops.get(n).put(stopBase.getKey(), nearest);
			}
		}
		TravelMinCost travelMinCost = new TravelMinCost() {
			public double getLinkGeneralizedTravelCost(Link link, double time) {
				return getLinkMinimumTravelCost(link);
			}
			public double getLinkMinimumTravelCost(Link link) {
				return link.getLength()/WALKING_SPEED;
			}
		};
		TravelTime timeFunction = new TravelTime() {	
			public double getLinkTravelTime(Link link, double time) {
				return link.getLength();
			}
		};
		PreProcessLandmarks preProcessData = new PreProcessLandmarks(travelMinCost);
		preProcessData.run(network);
		AStarLandmarks aStarLandmarks = new AStarLandmarks(network, preProcessData, timeFunction);
		DataBaseAdmin dataBaseAuxiliar  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
		ResultSet buildingsR = dataBaseAuxiliar.executeQuery("SELECT * FROM buildings");
		while(buildingsR.next()) {
			facilities.createFacility(new IdImpl(buildingsR.getString(1)),new CoordImpl(buildingsR.getDouble(4), buildingsR.getDouble(5)));
			typeAndMaxCapacityFacilities.put(new IdImpl(buildingsR.getString(1)), new Tuple<String, Double>(buildingsR.getString(2), buildingsR.getDouble(3)));
		}
		buildingsR.close();
		ResultSet stopsR = dataBaseAuxiliar.executeQuery("SELECT * FROM stops");
		Map<Id, Tuple<Integer,Double>> stops = new HashMap<Id, Tuple<Integer,Double>>();
		while(stopsR.next())
			stops.put(new IdImpl(stopsR.getString(1)), new Tuple<Integer, Double>(stopsR.getInt(2), stopsR.getDouble(3)));
		stopsR.close();
		dataBaseAuxiliar.close();
		new WorldConnectLocations(scenario.getConfig()).connectFacilitiesWithLinks(facilities, (NetworkImpl)scenario.getNetwork());
		Map<Tuple<Id, Id>,Double> weights = new HashMap<Tuple<Id,Id>, Double>();
		double totalTimeFromStop = 0;
		for(String stopKey: stopsBase.keySet()) {
			Id link=null;
			Id[] links = new Id[NUM_NEAR];
			for(int n=0; n<NUM_NEAR; n++)
				links[n]=linksStops.get(n).get(stopKey);
			Id stopId = new IdImpl(stopKey);
			double maxCapacityNearFacilities = 0;
			for(ActivityFacility facility:facilities.getFacilities().values()) {
				double walkingTime = Double.MAX_VALUE;
				for(int n=0; n<NUM_NEAR; n++) {
					double walkingTimeA=aStarLandmarks.calcLeastCostPath(network.getLinks().get(links[n]).getToNode(), network.getLinks().get(facility.getLinkId()).getFromNode(), 0).travelCost;
					if(walkingTimeA<walkingTime) {
						walkingTime = walkingTimeA;
						link = links[n];
					}
				}
				if(walkingTime<=MAX_TRAVEL_TIME) {
					weights.put(new Tuple<Id, Id>(stopId, facility.getId()), walkingTime);
					totalTimeFromStop += walkingTime;
					maxCapacityNearFacilities += typeAndMaxCapacityFacilities.get(facility.getId()).getSecond();
				}
			}
			if(stops.get(stopId).getFirst()/stops.get(stopId).getSecond()>maxCapacityNearFacilities) {
				double maxCapacityNear2Facilities = maxCapacityNearFacilities;
				for(ActivityFacility facility:facilities.getFacilities().values()) {
					double walkingTime = aStarLandmarks.calcLeastCostPath(network.getLinks().get(link).getToNode(), network.getLinks().get(facility.getLinkId()).getFromNode(), 0).travelCost;
					double privateBusTime = aStarLandmarks.calcLeastCostPath(network.getLinks().get(link).getToNode(), network.getLinks().get(facility.getLinkId()).getFromNode(), 0).travelCost*WALKING_SPEED/PRIVATE_BUS_SPEED;
					if(walkingTime>MAX_TRAVEL_TIME && privateBusTime<=MAX_TRAVEL_TIME) {
						weights.put(new Tuple<Id, Id>(stopId, facility.getId()), privateBusTime);
						totalTimeFromStop += privateBusTime;
						maxCapacityNear2Facilities += typeAndMaxCapacityFacilities.get(facility.getId()).getSecond();
					}
					if(stops.get(stopId).getFirst()/stops.get(stopId).getSecond()>maxCapacityNear2Facilities)
						throw new BadStopException(stopId);
				}
			}
		}
		for(Entry<Tuple<Id, Id>,Double> weight:weights.entrySet())
			weights.put(weight.getKey(), weight.getValue()/totalTimeFromStop);
		return weights;
	}
	private static Map<String, Double> calculateStopClustersQuantities(List<Cluster<PointPerson>> clusters) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseAux  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
		Map<String, Double> quantities = new HashMap<String, Double>();
		ResultSet tripsResult = dataBaseAux.executeQuery("SELECT * FROM work_stops_activities");
		while(tripsResult.next()) {
			int nearestCluster = 0;
			PointPerson time = new PointPerson(tripsResult.getString(1), "", new Double[]{(double) tripsResult.getInt(3), (double) (tripsResult.getInt(5)-tripsResult.getInt(3))});
			for(int c=0; c<clusters.size(); c++) {
				if(clusters.get(c).getCenter().distanceFrom(time)<clusters.get(nearestCluster).getCenter().distanceFrom(time))
					nearestCluster = c;
			}
			String key = tripsResult.getString(2)+SEPARATOR+nearestCluster;
			Double quantity = quantities.get(key);
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
	public static Map<String, List<Double>> calculateTypeBuildingOptionWeights(List<Cluster<PointPerson>> clusters) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dataBaseAux  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
		ResultSet typesResult = dataBaseAux.executeQuery("SELECT paxid,uraType FROM types");
		Map<String, List<Double>> proportions = new HashMap<String, List<Double>>();
		Map<String, Double> proportionsT = new HashMap<String, Double>();
		while(typesResult.next()) {
			CLUSTERS_SEARCH:
			for(int c=0; c<clusters.size(); c++)
				for(PointPerson person:clusters.get(c).getPoints())
					if(person.getId().equals(typesResult.getString(1))) {
						List<Double> list = proportions.get(typesResult.getString(2));
						if(list==null) {
							list=new ArrayList<Double>(clusters.size());
							proportions.put(typesResult.getString(2), list);
						}
						list.set(c, list.get(c)+1);
						proportionsT.put(typesResult.getString(2),proportionsT.get(typesResult.getString(2))+1);
						break CLUSTERS_SEARCH;
					}
		}
		typesResult.close();
		for(String key:proportions.keySet())
			for(int c=0; c<clusters.size(); c++)
				proportions.get(key).set(c, proportions.get(key).get(c)/proportionsT.get(key));
		dataBaseAux.close();
		return proportions;
	}
	public static void fittingCapacities() throws BadStopException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		List<Cluster<PointPerson>> clusters = clusterWorkActivities(getWorkActivityTimes());
		DataBaseAdmin dataBaseAux  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
		Map<String, Coord> stopsBase = new HashMap<String, Coord>();
		ResultSet stopsResult = dataBaseAux.executeQuery("SELECT * FROM stops");
		while(stopsResult.next())
			stopsBase.put(stopsResult.getString(1), coordinateTransformation.transform(new CoordImpl(stopsResult.getDouble(3), stopsResult.getDouble(2))));
		stopsResult.close();
		Map<Id, Tuple<String,Double>> typeAndMaxCapacityFacilities = new HashMap<Id, Tuple<String,Double>>();
		ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl();
		Map<Tuple<Id, Id>,Double> weightsMap = calculateAreaStopWeights(stopsBase, facilities, typeAndMaxCapacityFacilities);
		MatrixND<Double> weights = new MatrixNDImpl<Double>(new int[]{facilities.getFacilities().size(),stopsBase.size()});
		Iterator<Id> facilityI = facilities.getFacilities().keySet().iterator();
		for(int f=0; f<weights.getDimensions()[0]; f++) {
			Id facilityId = facilityI.next();
			Iterator<String> stopsI = stopsBase.keySet().iterator();
			for(int s=0; s<weights.getDimensions()[1]; s++)
				weights.setElement(new int[]{f,s}, weightsMap.get(new Tuple<Id, Id>(new IdImpl(stopsI.next()), facilityId)));
		}
		Map<String, Double> quantitiesMap = calculateStopClustersQuantities(clusters);
		MatrixND<Double> quantities = new MatrixNDImpl<Double>(new int[]{clusters.size(),stopsBase.size()});
		for(int c=0; c<quantities.getDimensions()[0]; c++) {
			Iterator<String> stopsI = stopsBase.keySet().iterator();
			for(int s=0; s<quantities.getDimensions()[1]; s++)
				quantities.setElement(new int[]{c,s},quantitiesMap.get(stopsI.next()+SEPARATOR+c));
		}
		MatrixND<Double> proportions = new MatrixNDImpl<Double>(new int[]{facilities.getFacilities().size(),clusters.size()});
		Map<String, List<Double>> proportionsMap = calculateTypeBuildingOptionWeights(clusters);
		facilityI = facilities.getFacilities().keySet().iterator();
		for(int f=0; f<proportions.getDimensions()[0]; f++) {
			Id facilityId = facilityI.next();
			for(int c=0; c<proportions.getDimensions()[1]; c++)
				proportions.setElement(new int[]{f,c},proportionsMap.get(typeAndMaxCapacityFacilities.get(facilityId).getFirst()).get(c));
		}
		MatrixND<Double> maxs = new MatrixNDImpl<Double>(new int[]{5}, 60.0);
		Map<String, Double> workerAreas = new HashMap<String, Double>();
		ResultSet typesResult = dataBaseAux.executeQuery("SELECT * FROM building_types");
		while(typesResult.next())
			workerAreas.put(typesResult.getString(1), typesResult.getDouble(2));
		typesResult.close();
		dataBaseAux.close();
		facilityI = facilities.getFacilities().keySet().iterator();
		for(int f=0; f<maxs.getDimensions()[0]; f++) {
			Tuple<String, Double> typeMaxCapacity = typeAndMaxCapacityFacilities.get(facilityI.next());
			maxs.setElement(new int[]{f}, typeMaxCapacity.getSecond()/workerAreas.get(typeMaxCapacity.getFirst()));
		}
		MatrixND<Double> capacities = new FittingCapacities(new int[]{5,3,4}, weights, quantities, proportions, maxs).run(20);
	}
}

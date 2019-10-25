package playgroundMeng.ptAccessabilityAnalysis.activitiesAnalysis;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.google.inject.Inject;

import playgroundMeng.ptAccessabilityAnalysis.areaSplit.AreaSplit;
import playgroundMeng.ptAccessabilityAnalysis.areaSplit.DistrictBasedSplit;
import playgroundMeng.ptAccessabilityAnalysis.areaSplit.GridBasedSplit;
import playgroundMeng.ptAccessabilityAnalysis.prepare.TimeConvert;
import playgroundMeng.ptAccessabilityAnalysis.run.PtAccessabilityConfig;

public class StartActivitiesAnalysis implements ActivitiesAnalysisInterface{
	private static final Logger logger = Logger.getLogger(StartActivitiesAnalysis.class);
	Network network;
	AreaSplit areaSplit;
	PtAccessabilityConfig ptAccessabilityConfig;
	private List<ActivityImp> activities = new LinkedList<ActivityImp>();
	private Map<Id<Person>, List<Trip>> personId2Trips = new HashedMap();
	private Map<String, LinkedList<ActivityImp>> area2Activities = new HashedMap();
	private Map<String, Map<Double, LinkedList<ActivityImp>>> area2Time2Activities = new HashedMap();
	private Collection<SimpleFeature> simpleFeatures = null;
	
	@Inject
	public StartActivitiesAnalysis(Network network, PtAccessabilityConfig ptAccessabilityConfig, AreaSplit areaSplit){
		this.network = network;
		this.ptAccessabilityConfig = ptAccessabilityConfig;
		this.areaSplit = areaSplit;
		if(ptAccessabilityConfig.isConsiderActivities()) {
			this.run();
			this.setArea2Time2Activities();
		}	
	}
	
	public void run() {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		ActivitiesEventHandler activitiesEventHandler = new ActivitiesEventHandler(this.network);
		eventsManager.addHandler(activitiesEventHandler);
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(ptAccessabilityConfig.getEventFile());
		this.personId2Trips = activitiesEventHandler.getPersonId2Trips();
		for(Id<Person> personId: personId2Trips.keySet()) {
			for(Trip trip: personId2Trips.get(personId)) {
				activities.add(trip.getActivityStartImp());
			}
		}
		logger.info("beginn to filter the activities into area");
		this.getArea2Activities();
		for(String string: this.area2Activities.keySet()) {
			logger.info("area "+ string + " has "+ area2Activities.get(string).size()+" activities");
		}
	}
	public void getArea2Activities(){
		if(this.areaSplit instanceof DistrictBasedSplit) {
			ShapeFileReader shapeFileReader = new ShapeFileReader();
			this.simpleFeatures = shapeFileReader.getAllFeatures(ptAccessabilityConfig.getShapeFile());
		
			Geometry geometry = null;
			GeometryFactory gf = new GeometryFactory();
			
			for(ActivityImp activityImp : activities) {
				for(SimpleFeature simpleFeature : this.simpleFeatures) {
					if(!this.area2Activities.keySet().contains(simpleFeature.getAttribute("NAME").toString())) {
						this.area2Activities.put(simpleFeature.getAttribute("NAME").toString(), new LinkedList<ActivityImp>());
					}
					geometry = (Geometry) simpleFeature.getDefaultGeometry();
					boolean bo = geometry.contains(gf.createPoint(new Coordinate(activityImp.getCoord().getX(),activityImp.getCoord().getY())));
					if(bo){
						this.area2Activities.get(simpleFeature.getAttribute("NAME").toString()).add(activityImp);
						break;
					}
				}	
			}	
		} else if (this.areaSplit instanceof GridBasedSplit) {
			this.areaSplit = (GridBasedSplit) areaSplit;
			
			double minx = ((GridBasedSplit) areaSplit).getMinx(); double miny = ((GridBasedSplit) areaSplit).getMiny();
			double maxx = ((GridBasedSplit) areaSplit).getMaxx();  double maxy = ((GridBasedSplit) areaSplit).getMaxy();
			int num = ptAccessabilityConfig.getAnalysisGridSlice();
			double xInterval =((GridBasedSplit) areaSplit).getxInterval();
			double yInterval = ((GridBasedSplit) areaSplit).getyInterval();
			
			for(int a=1; a <= num; a++) {
				for(int b= 1; b<= num; b++) {
					Map<Integer, Integer> coordMap = new HashedMap();
					coordMap.put(a, b);
					this.area2Activities.put(coordMap.toString(), new LinkedList<ActivityImp>());
				}
			}
			for(ActivityImp activityImp: activities) {
				double x = activityImp.getCoord().getX();
				double y = activityImp.getCoord().getY();
				
				int xx = Math.min(num, (int)((x-minx)/xInterval)+1);
				int yy = Math.min(num, (int)((y-miny)/xInterval)+1);
				
				Map<Integer, Integer> coordMap = new HashedMap();
				coordMap.put(xx, yy);
				this.area2Activities.get(coordMap.toString()).add(activityImp);
			}
		}
	}
	public void setArea2Time2Activities() {
		for(String string: this.area2Activities.keySet()) {
			this.area2Time2Activities.put(string, new HashedMap());
			for(double x=ptAccessabilityConfig.getBeginnTime(); x<ptAccessabilityConfig.getEndTime(); x+=ptAccessabilityConfig.getAnalysisTimeSlice()) {
				this.area2Time2Activities.get(string).put(x, new LinkedList<ActivityImp>());
				for(ActivityImp activityImp: this.area2Activities.get(string)) {
					double time = TimeConvert.timeConvert(activityImp.getStartTime());
					if(time>=x && time< x+ptAccessabilityConfig.getAnalysisTimeSlice()) {
						this.area2Time2Activities.get(string).get(x).add(activityImp);
					}
				}
				logger.info(string +" area at time "+x+ " has activities "+ this.area2Time2Activities.get(string).get(x).size());
			}
		}
	}

	@Override
	public Map<String, Map<Double, LinkedList<ActivityImp>>> getArea2time2activities() {
		// TODO Auto-generated method stub
		return this.area2Time2Activities;
	}
}

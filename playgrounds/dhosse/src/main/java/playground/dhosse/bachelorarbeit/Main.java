package playground.dhosse.bachelorarbeit;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.matsim4opus.constants.InternalConstants;
import org.matsim.contrib.matsim4opus.gis.SpatialGrid;
import org.matsim.contrib.matsim4opus.gis.Zone;
import org.matsim.contrib.matsim4opus.gis.ZoneLayer;
import org.matsim.contrib.matsim4opus.interfaces.MATSim4UrbanSimInterface;
import org.matsim.contrib.matsim4opus.utils.io.ReadFromUrbanSimModel;
import org.matsim.contrib.matsim4opus.utils.network.NetworkBoundaryBox;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class Main {

	public static void main(String args[]){
		
		String file1 = "C:/Users/Daniel/Dropbox/bsc/input/config.xml";
//		String file2 = "./input/network_bridge.xml";
		
//		Config config = ConfigUtils.createConfig();
		Config config = ConfigUtils.loadConfig(file1);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Controler ctrl = new Controler(config);
		ctrl.setOverwriteFiles(true);
		
		MATSim4UrbanSimInterface main = new MATSim4UrbanSimInterface() {
			
			@Override
			public boolean isParcelMode() {
				return true;
			}
			
			@Override
			public ReadFromUrbanSimModel getReadFromUrbanSimModel() {
				return null;
			}
			
			@Override
			public double getOpportunitySampleRate() {
				return 0;
			}
		};
		
		NetworkBoundaryBox bbox = new NetworkBoundaryBox();
		
		bbox.setDefaultBoundaryBox(scenario.getNetwork());
		
		SpatialGrid freeSpeedGrid = new SpatialGrid(bbox.getBoundingBox(), 100);
		SpatialGrid carGrid = new SpatialGrid(bbox.getBoundingBox(), 100);
		SpatialGrid bikeGrid = new SpatialGrid(bbox.getBoundingBox(), 100);
		SpatialGrid walkGrid = new SpatialGrid(bbox.getBoundingBox(), 100);
		SpatialGrid ptGrid = new SpatialGrid(bbox.getBoundingBox(), 100);
		
		ActivityFacilitiesImpl parcels = new ActivityFacilitiesImpl();//create facility, aus matsim-bev., strukturdaten etc.
		
		int i=0;
		
		for(Person p : scenario.getPopulation().getPersons().values()){
			PlanElement pe1 = p.getSelectedPlan().getPlanElements().get(0);
			PlanElement pe2 = p.getSelectedPlan().getPlanElements().get(2);
			if(pe1 instanceof Activity){
				Id id = new IdImpl(i);
				parcels.createAndAddFacility(id, ((Activity)pe1).getCoord());
				i++;
			}
			if(pe2 instanceof Activity){
				Id id = new IdImpl(i);
				parcels.createAndAddFacility(id, ((Activity)pe2).getCoord());
				i++;
			}
		}
		
		ScenarioImpl sc = (ScenarioImpl) scenario;
		
		Set<Zone<Id>> zones = new HashSet<Zone<Id>>();
		
		for(ActivityFacility facility : parcels.getFacilities().values()){
			Coordinate[] coord = new Coordinate[1];
			coord[0] = new Coordinate(facility.getCoord().getX(), facility.getCoord().getY());
			CoordinateSequence cs = new CoordinateArraySequence(coord);
			Point p = new Point(cs, new GeometryFactory());
			Zone<Id> zone = new Zone<Id>(p);
			zones.add(zone);
		}
		
		ZoneLayer<Id> startZones = new ZoneLayer<Id>(zones);
		
		ctrl.addControlerListener(new MyParcelBasedAccessibilityControlerListener(main, startZones,
				parcels, freeSpeedGrid, carGrid, bikeGrid, walkGrid, ptGrid, null, null,
				sc));
		ctrl.run();
		
//		Scenario sc = ScenarioUtils.createScenario(config);
//		Scenario sc2 = ScenarioUtils.createScenario(config);
		
//		MatsimNetworkReader nr = new MatsimNetworkReader(sc);
//		nr.readFile(file1);

//		MatsimNetworkReader nr2 = new MatsimNetworkReader(sc2);
//		nr2.readFile(file2);
		
//		NetworkInspector nI = new NetworkInspector(scenario);
//		nI.checkNetworkAttributes(true, true);
//		nI.isRoutable();

//		Grid grid = new Grid();
//		grid.calculateTravelTime(sc.getNetwork());
//		grid.gridComparison(1000, sc.getNetwork(), sc2.getNetwork());
//		grid.generateSHPExport(file1,file2);
		
	}
}
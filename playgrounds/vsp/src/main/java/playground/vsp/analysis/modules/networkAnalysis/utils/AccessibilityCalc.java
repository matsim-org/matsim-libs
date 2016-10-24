package playground.vsp.analysis.modules.networkAnalysis.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.gis.Zone;
import org.matsim.contrib.accessibility.gis.ZoneLayer;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.contrib.accessibility.utils.Distances;
import org.matsim.contrib.accessibility.utils.NetworkUtil;
import org.matsim.contrib.accessibility.utils.ProgressBar;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class AccessibilityCalc {
	
private SpatialGrid freeSpeedGrid;
	
	private MutableScenario scenario;
	
	private ZoneLayer<Id<Zone>> measuringPoints;
	
	protected AggregationObject[] aggregatedOpportunities;
	
	protected double walkSpeedMeterPerHour = 3000.;
	
	private Geometry boundary;
	
	public AccessibilityCalc(ZoneLayer<Id<Zone>> measuringPoints, SpatialGrid freeSpeedGrid, MutableScenario scenario, Geometry boundary) {
		
		this.freeSpeedGrid = freeSpeedGrid;
		this.scenario = scenario;
		this.measuringPoints = measuringPoints;
		this.boundary = boundary;
		
	}
	
	public void runAccessibilityComputation(){
		
		final Network network = (Network)this.scenario.getNetwork();
		
		ProgressBar bar = new ProgressBar( this.measuringPoints.getZones().size() );
		
		for(Zone<Id<Zone>> measurePoint : this.measuringPoints.getZones()){
			
			bar.update();
			
			Coord coord = MGC.point2Coord(measurePoint.getGeometry().getCentroid());
			Point p = measurePoint.getGeometry().getCentroid();
			final Coord coord1 = coord;
			
			Link nearestLink = NetworkUtils.getNearestLinkExactly(network,coord1);
			final Coord coord2 = coord;
			Node nearestNode = NetworkUtils.getNearestNode(network,coord2);
			
			Distances distance = NetworkUtil.getDistances2NodeViaGivenLink(coord, nearestLink, nearestNode);
			double distanceMeasuringPoint2Road_meter 	= distance.getDistancePoint2Intersection();
			
			double walkTravelTime_h 	= distanceMeasuringPoint2Road_meter / this.walkSpeedMeterPerHour; //travel time from coord to network (node or link)
			
			if(boundary.contains(p))
				this.freeSpeedGrid.setValue(walkTravelTime_h, p);
			
		}
		
	}

}

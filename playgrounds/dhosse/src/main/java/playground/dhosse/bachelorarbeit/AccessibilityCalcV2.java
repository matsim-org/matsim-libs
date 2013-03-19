package playground.dhosse.bachelorarbeit;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.matsim4opus.gis.SpatialGrid;
import org.matsim.contrib.matsim4opus.gis.Zone;
import org.matsim.contrib.matsim4opus.gis.ZoneLayer;
import org.matsim.contrib.matsim4opus.matsim4urbansim.AccessibilityControlerListenerImpl.GeneralizedCostSum;
import org.matsim.contrib.matsim4opus.utils.helperObjects.AggregateObject2NearestNode;
import org.matsim.contrib.matsim4opus.utils.io.writer.SpatialGridTableWriter;
import org.matsim.contrib.matsim4opus.utils.misc.ProgressBar;
import org.matsim.contrib.matsim4opus.utils.network.NetworkUtil;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Point;

public class AccessibilityCalcV2 {
	
	private SpatialGrid freeSpeedGrid;
	
	private ScenarioImpl scenario;
	
	private ZoneLayer<Id> measuringPoints;
	
	protected AggregateObject2NearestNode[] aggregatedOpportunities;
	
	protected double walkSpeedMeterPerHour = 3000.;
	protected double logitScaleParameter = 1.;
	protected double inverseOfLogitScaleParameter = 1/this.logitScaleParameter;
	
	protected double VijFreeTT;
	protected double betaCarTT = -12.;
	protected double betaWalkTT = -12.;
	
	public AccessibilityCalcV2(ZoneLayer<Id> measuringPoints, SpatialGrid freeSpeedGrid, ScenarioImpl scenario) {
		
		this.freeSpeedGrid = freeSpeedGrid;
		this.scenario = scenario;
		this.measuringPoints = measuringPoints;
		
	}
	
	public void runAccessibilityComputation(){
		
		final NetworkImpl network = (NetworkImpl)this.scenario.getNetwork();
		
		Iterator<Zone<Id>> measuringPointsIterator = this.measuringPoints.getZones().iterator();
		
		ProgressBar bar = new ProgressBar( this.measuringPoints.getZones().size() );
		
		GeneralizedCostSum gcs = new GeneralizedCostSum();
		
		Map<Id,ArrayList<Zone<Id>>> aggregatedMeasurementPoints = new HashMap<Id, ArrayList<Zone<Id>>>();
		
		while(measuringPointsIterator.hasNext()){
			
			Zone<Id> measurePoint = measuringPointsIterator.next();
			
			Point p = measurePoint.getGeometry().getCentroid();
			
			Coord coordFromZone = new CoordImpl(p.getX(),p.getY());
			
			Link nearestLink = network.getNearestLinkExactly(coordFromZone);
			
			Node fromNode = NetworkUtil.getNearestNode(coordFromZone, nearestLink);
			
			Id id = fromNode.getId();
			
			if(!aggregatedMeasurementPoints.containsKey(id))
				aggregatedMeasurementPoints.put(id, new ArrayList<Zone<Id>>());
			
			aggregatedMeasurementPoints.get(id).add(measurePoint);
			
		}
		
		for(Id nodeId : aggregatedMeasurementPoints.keySet()){
			
			bar.update();
			
			Node nearestNode = network.getNodes().get(nodeId);
			
			for(Zone<Id> measurePoint : aggregatedMeasurementPoints.get(nodeId)){
				
				gcs.reset();
				
				Coord coord = MGC.coordinate2Coord(measurePoint.getGeometry().getCoordinate());
				Point p = measurePoint.getGeometry().getCentroid(); 
				
				double distance_meter 	= NetworkUtil.getEuclidianDistance(coord, nearestNode.getCoord());
				double walkTravelTime_h = distance_meter / this.walkSpeedMeterPerHour;
				
				double Vjk					= Math.exp(this.logitScaleParameter * walkTravelTime_h );
				
				gcs.addFreeSpeedCost(Vjk);
				
				double freeSpeedAccessibility = - this.inverseOfLogitScaleParameter *Math.log(gcs.getFreeSpeedSum());
				
				this.freeSpeedGrid.setValue(freeSpeedAccessibility, p);
				
			}
			
		}
		
		try{
		BufferedWriter writer = new BufferedWriter(new FileWriter("C:/Users/Daniel/Dropbox/bsc/test/freeSpeedAccessibility_cellsize"+this.freeSpeedGrid.getResolution()+"m.txt"));
		
		for(double x = this.freeSpeedGrid.getXmin(); x <= this.freeSpeedGrid.getXmax(); x += this.freeSpeedGrid.getResolution()) {
			writer.write(SpatialGridTableWriter.separator);
			writer.write(String.valueOf(x));
		}
		writer.newLine();
		
		for(double y = this.freeSpeedGrid.getYmin(); y <= this.freeSpeedGrid.getYmax() ; y += this.freeSpeedGrid.getResolution()) {
			writer.write(String.valueOf(y));
			for(double x = this.freeSpeedGrid.getXmin(); x <= this.freeSpeedGrid.getXmax(); x += this.freeSpeedGrid.getResolution()) {
				writer.write(SpatialGridTableWriter.separator);
				Double val = this.freeSpeedGrid.getValue(x, y);
				if(!Double.isNaN(val))
					writer.write(String.valueOf(val));
				else
					writer.write("NaN");
			}
			writer.newLine();
		}
		writer.flush();
		writer.close();
		}
		catch(IOException e){
			e.printStackTrace();
		}
		
	}

}

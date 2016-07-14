package playground.dhosse.bachelorarbeit;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.gis.SpatialGridTableWriter;
import org.matsim.contrib.accessibility.gis.Zone;
import org.matsim.contrib.accessibility.gis.ZoneLayer;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.contrib.matsim4urbansim.utils.io.misc.ProgressBar;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Point;

public class AccessibilityCalcV2 {
	
	private SpatialGrid freeSpeedGrid;
	
	private MutableScenario scenario;
	
	private ZoneLayer<Id> measuringPoints;
	
	protected AggregationObject[] aggregatedOpportunities;
	
	protected double walkSpeedMeterPerHour = 3000.;
	
	private String ouputFolder = null;
	
	public AccessibilityCalcV2(ZoneLayer<Id> measuringPoints, SpatialGrid freeSpeedGrid, MutableScenario scenario, String output) {
		
		this.freeSpeedGrid = freeSpeedGrid;
		this.scenario = scenario;
		this.measuringPoints = measuringPoints;
		this.ouputFolder = output;
		
	}
	
	public void runAccessibilityComputation(){
		
		final NetworkImpl network = (NetworkImpl)this.scenario.getNetwork();
		
		ProgressBar bar = new ProgressBar( this.measuringPoints.getZones().size() );
		
		for(Zone<Id> measurePoint : this.measuringPoints.getZones()){
			
			bar.update();
			
			Coord coord = MGC.point2Coord(measurePoint.getGeometry().getCentroid());
			Point p = measurePoint.getGeometry().getCentroid();
			
			Link nearestLink = network.getNearestLinkExactly(coord);
			
			double distance_meter = CoordUtils.calcEuclideanDistance(coord, nearestLink.getCoord());
			double walkTravelTime_h = distance_meter / this.walkSpeedMeterPerHour;
			
			this.freeSpeedGrid.setValue(walkTravelTime_h, p);
			
		}
		
		try{
		BufferedWriter writer = new BufferedWriter(new FileWriter(this.ouputFolder+"/freeSpeedAccessibility.txt"));
		
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

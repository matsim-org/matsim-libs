package playground.southafrica.freight.digicore.containers;

import java.util.ArrayList;
import java.util.List;

import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

public class DigicoreVehicle implements Vehicle {
	private Id<Vehicle> id;
	private VehicleType type = new VehicleTypeImpl(Id.create("commercial", VehicleType.class));
	private List<DigicoreChain> chains = new ArrayList<DigicoreChain>();
	
	public DigicoreVehicle(final Id<Vehicle> id) {
		this.id = id;
	}

	public Id<Vehicle> getId() {
		return this.id;
	}

	public VehicleType getType() {
		return this.type;
	}
	
	public List<DigicoreChain> getChains(){
		return this.chains;
	}
	
	public void setType(String type){
		this.type = new VehicleTypeImpl(Id.create(type, VehicleType.class));
	}
	
	
	
	
	/**
	 * This method takes a {@link MultiPolygon} and assesses what % of a vehicle's activities
	 * takes place in the area.
	 * 
	 * @param area the MultiPolygon obtained from a shapefile
	 * @param threshold the percentage to distinguish between inter- and intra-provincial vehicles
	 * 		  (we use 0.6)
	 * @return an int value (either 0 for intra-provincial, 1 for inter-provincial, 2 for extra-provincial)
	 */
	
	public int determineIfIntraInterExtraVehicle(MultiPolygon area, double threshold){
		int vehicleType = 4; //just arbitrary value to know if the vehicle couldn't be classified. 
		int inside = 0;
		int allCount = 0;
		
		GeometryFactory gf = new GeometryFactory();
		
		for(DigicoreChain dc : this.getChains()){
			Point p = null;
			int countInside_Chain = 0;
			
			/*
			 * this was changed (QvH-October 2012) to consider ALL activities in
			 * a chain when determining whether in it intra, inter, extra
			 */
			for(DigicoreActivity da : dc.getAllActivities()){
				//if(da.getType().equalsIgnoreCase("minor")){
					p = gf.createPoint(new Coordinate(da.getCoord().getX(),da.getCoord().getY()));
					if(area.getEnvelope().contains(p)){
						if(area.contains(p)){
							countInside_Chain++;
						}
					}
				allCount++;
			}
			inside += countInside_Chain;
		}
		
		if(allCount > 0){
			double percentageValue = (double)inside/(double)allCount;
			
			if(percentageValue > 0.6){
				//this is an intra-provincial vehicle
				vehicleType = 0;
			}else if(percentageValue <= 0.6){
				if(percentageValue > 0){
					//this is an inter-provincial vehicle
					vehicleType = 1;
				}else{
					//this vehicle never enters the area --> extra vehicle
					vehicleType = 2;
				}
			}
			
		}else{
			Log.warn("Vehicle " + this.getId() + " contains no activities.");
		}
				
		return vehicleType;
	}
}

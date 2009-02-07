package playground.jjoubert.CommercialTraffic;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class Vehicle {
	public int vehID;
	public ArrayList<GPSPoint> homeLocation;
	public ArrayList<Chain> chains;
	public int avgActivitesPerChain;
	public int avgChainDuration;
	public final static int DISTANCE_THRESHOLD = 2000;
	
	public Vehicle(int id){
		this.vehID = id;
		this.homeLocation = new ArrayList<GPSPoint>();
		this.chains = new ArrayList<Chain>();
		this.avgActivitesPerChain = 0;
		this.avgChainDuration = 0;
	}
	
	public void writeVehicleXML(){
		//TODO write method
	}
	
	public void updateVehicleStatistics(){
		calcAvgActivitiesPerChain();
		calcAvgChainDuration();
	}
	
	private void calcAvgActivitiesPerChain(){
		int activities = 0;
		for (Chain chain : this.chains) {
			activities += chain.activities.size();
		}
		if(this.chains.size() > 0){
			this.avgActivitesPerChain = (int) ( activities / this.chains.size() );
		} else{
			this.avgActivitesPerChain = 0;
		}
	}
	
	private void calcAvgChainDuration(){
		if(this.chains.size() > 0){
			int duration = 0;
			for (Chain chain : this.chains){
				duration += chain.duration;
			}
			this.avgChainDuration = (int) (duration / this.chains.size() );
		} else{
			this.avgChainDuration = 0;
		}
	}
	
	public void cleanHomeLocations(){
		ArrayList<GPSPoint> home = new ArrayList<GPSPoint>();
		if(this.homeLocation.size() > 0 ){
			home.add(this.homeLocation.get(0) ); // add first home location
			GeometryFactory gf = new GeometryFactory();

			for(int j = 1; j < this.homeLocation.size(); j++){ // only check from second home location
				Point p2 = gf.createPoint( new Coordinate(this.homeLocation.get(j).longitude, this.homeLocation.get(j).latitude ) );
				boolean duplicate = false;
				int i = 0;
				while((i < home.size()) && !duplicate ){
					Point p1 = gf.createPoint(new Coordinate(home.get(i).longitude, home.get(i).latitude) );
					if( convertDistance(p1.distance(p2)) < DISTANCE_THRESHOLD ){
						duplicate = true;
					}
					i++;
				}
				if( !duplicate ){
					home.add(this.homeLocation.get(j) );
				}			
			}
			this.homeLocation = home;
		}
	}

	private int convertDistance(double distance) {
		// Conversion constant (degree to meter) taken from http://www.uwgb.edu/dutchs/UsefulData/UTMFormulas.htm on Sat, 7 Feb 2009 at 11:25 CET
		return ((int) (distance*111200) );
	}
		
	
}

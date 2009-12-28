package playground.jjoubert.CommercialTraffic;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
/**
 * A vehicle container class for the commercial vehicle study in South Africa. Each 
 * vehicle contains major activity locations, minor activity locations, and a list of 
 * all minor activities in the given study area.
 *  
 * @author jwjoubert
 */
public class Vehicle {
	private int vehId;
	private ArrayList<Activity> homeLocation;
	private ArrayList<Chain> chains;
	private ArrayList<Activity> studyAreaActivities;
	private int averageActivitesPerChain;
	private int averageChainDuration;
	private int averageChainDistance; 
	private int numberOfStudyAreaActivities;
	private double percentageStudyAreaActivities;
	private int studyAreaChainDistance;
	private int totalActivities;
	
	/*
	 * A preset threshold that states the distance (expressed in meters) within which
	 * major activities are considered to be the same location. 
	 */
	public final static int DISTANCE_THRESHOLD = 2500; // expressed in meters
	
	/**
	 * Creates a new vehicle with the ID as obtained from the DigiCore data set.
	 * 
	 * @param id is the unique vehicle identification number used by DigiCore
	 */
	public Vehicle(int id){

		this.vehId = id;
		this.homeLocation = new ArrayList<Activity>();
		this.chains = new ArrayList<Chain>();
		this.studyAreaActivities = new ArrayList<Activity>();
		this.averageActivitesPerChain = 0;
		this.averageChainDuration = 0;
		this.averageChainDistance = 0;
		this.numberOfStudyAreaActivities = 0;
		this.percentageStudyAreaActivities = 0;
		this.totalActivities = 0;
	}
	
	/**
	 * Updates a number of statistics for the vehicle:
	 * <ul>
	 * <li> calculates the average number of activities per chain;
	 * <li> calculates the average chain duration (in minutes);
	 * <li> calculates the average chain distance (in meters); and
	 * <li> calculates the average number of activities in the study  area.
	 * </ul>
	 *
	 * @param studyArea of type <code>MultiPolygon</code>, assumed to be given in the 
	 * 		  WGS84_UTM35S coordinate system.
	 */
	public void updateVehicleStatistics(MultiPolygon studyArea){
		setAverageActivitiesPerChain();
		setAverageChainDuration();
		setAverageChainDistance();
		setStudyAreaActivities(studyArea);
	}
	
	private void setAverageActivitiesPerChain(){
		int totalActivities = 0;
		if(this.chains.size() > 0){
			for (Chain chain : this.chains) {
				totalActivities += (chain.getActivities().size() - 2);
			}
			this.averageActivitesPerChain = ((int) ( totalActivities / this.chains.size() ));
		} else{
			this.averageActivitesPerChain = (0);
		}
		this.totalActivities = totalActivities;
	}

	private void setAverageChainDuration(){
		if(this.chains.size() > 0){
			int duration = 0;
			for (Chain chain : this.chains){
				duration += chain.getDuration();
			}
			this.averageChainDuration = ((int) (duration / this.chains.size() ));
		} else{
			this.averageChainDuration = (0);
		}
	}

	private void setAverageChainDistance() {
		if(this.chains.size() > 0){
			int totalDistance = 0;
			for(Chain chain: this.chains ){
				totalDistance += chain.getDistance();
			}
			this.averageChainDistance = ((totalDistance / this.chains.size() ));
		} else{
			this.averageChainDistance = (0);
		}
	}

	/**
	 * The method checks each chain, and each activity of that chain, and identifies if
	 * the activities are within the given study area. All activities within the study
	 * area are added to the vehicles container (an <code>ArrayList</code>) called 
	 * <code>studyAreaActivities</code>.
	 * @param studyArea, assumed to be in the <i>WGS_UTM35S</i> coordinate system.
	 */
	private void setStudyAreaActivities(MultiPolygon studyArea){
		GeometryFactory gf = new GeometryFactory();

		/*
		 * Calculate the maximum distance that any corner of the envelope of the study 
		 * area is from the centroid of the study area.  
		 */
		Geometry studyAreaEnvelope = studyArea.getEnvelope();
		Point studyAreaCentroid = studyArea.getCentroid();
		Coordinate[] cornerPoints = studyAreaEnvelope.getCoordinates();
		double maxDistance = 0.0;
		for (Coordinate c : cornerPoints) {
			maxDistance = Math.max(maxDistance, c.distance(studyAreaCentroid.getCoordinate()));
		}
		if(this.chains.size() > 0){
			for (Chain chain : this.chains) {
				if(chain.getActivities().size() > 0){
					for (int i = 1; i < chain.getActivities().size() - 1; i++ ) { // don't count first and last major locations
						Activity thisActivity = chain.getActivities().get(i);
						Point p = gf.createPoint( thisActivity.getLocation().getCoordinate() );
						/*
						 * For efficiency (well, I HOPE), I first check if the point is 
						 * closer than the calculated maximum distance. This is a 
						 * 'cheaper' calculation than immediately checking if the point 
						 * is within the study area. 
						 */
						if(p.distance(studyAreaCentroid) < maxDistance){
							if( studyArea.contains(p) ){
								this.studyAreaActivities.add( thisActivity );
								chain.setInStudyArea(true);
							}
						}
					}
				}
				if( chain.isInStudyArea() ){
					/*
					 * TODO Currently the total chain length is added to the study area
					 * distance. In future we can/could revisit to ensure that only the
					 * portion INSIDE the study are is actually added.
					 */					
					this.studyAreaChainDistance += chain.getDistance();
				}
			}
		}
		this.numberOfStudyAreaActivities = this.studyAreaActivities.size();
		this.percentageStudyAreaActivities = (((double) this.numberOfStudyAreaActivities) / 
										((double) this.totalActivities ) );
	}
		
	/**
	 * <p>This method identifies the major activity locations. The first chain's first major 
	 * location is identified. All other major locations are then checked, and only if it 
	 * is further than a predefined threshold, <code>this.DISTANCE_THRESHOLD</code> 
	 * (default 2500m), will it be considered a <i>new</i> location.</p> 
	 * 
	 * <p>The method adjusts all major location coordinates to the weighted centre of the 
	 * location clusters. A second check is then done to see whether cluster centroids are 
	 * within the distance threshold. If so, the dominating cluster is the one with the 
	 * most activity locations, and all dominated clusters' locations are adjusted accordingly.
	 */
	public void extractMajorLocations(){
		if(this.chains.size() > 0){
			// First find all possible major locations
			ArrayList<ArrayList<Activity>> majorLocationList = new ArrayList<ArrayList<Activity>>();
			ArrayList<Activity> newMajorLocation = new ArrayList<Activity>();
			majorLocationList.add(newMajorLocation);
			newMajorLocation.add( this.chains.get(0).getActivities().get(0) ); // add first home activity of first chain
			
			for(int i = 0; i < this.chains.size(); i++ ){
				Activity a1 = this.chains.get(i).getActivities().get( 
						this.chains.get(i).getActivities().size() - 1 ); // get the last activity of the chain
				Coordinate c1 = a1.getLocation().getCoordinate(); 
				
				// Check it against each potential major location
				boolean duplicate = false;
				int j = 0;
				while( (!duplicate) && (j < majorLocationList.size() ) ){
					ArrayList<Activity> thisMajorLocation = majorLocationList.get(j);
					int k = 0;
					while( (!duplicate) && (k < thisMajorLocation.size() ) ){
						Activity a2 = thisMajorLocation.get(k);
						Coordinate c2 = a2.getLocation().getCoordinate();
						int distance = (int)( c1.distance(c2) );
						if( distance < DISTANCE_THRESHOLD ){
							duplicate = true;
							majorLocationList.get(j).add(a1);
						} else{
							k++;
						}
					}
					j++;
				}
				if( !duplicate ){ 
					// Create a new major location
					newMajorLocation = new ArrayList<Activity>();
					newMajorLocation.add(a1);
					majorLocationList.add( newMajorLocation );
				}
			}
			
			/* 
			 * Find the centre for each major location (using a gravity method) and set 
			 * the points for all locations the same as the centre.
			 */
			for(int i = 0; i < majorLocationList.size(); i++ ){
				ArrayList<Activity> thisList = majorLocationList.get(i);
				double xSum = 0;
				double ySum = 0;
				for(int j = 0; j < thisList.size(); j++ ){
					GPSPoint point = thisList.get(j).getLocation();
					xSum += point.getCoordinate().x;
					ySum += point.getCoordinate().y;
				}
				Coordinate center = new Coordinate( (xSum / thisList.size()), (ySum / thisList.size()) );
				
				for(int j = 0; j < thisList.size(); j++ ){
					thisList.get(j).getLocation().setCoordinate(center);
				}				
			}
			
			/* 
			 * Perform one last pass if your first points turned out to be outliers.
			 */
			if(majorLocationList.size() > 1){
				int i = 0; 
				while( i < majorLocationList.size() - 1 ){
					boolean deleted = false;
					Coordinate c1 = majorLocationList.get(i).get(0).getLocation().getCoordinate();
					int w1 = majorLocationList.get(i).size();
					int j = (i+1); 
					while( j < majorLocationList.size() && !deleted ){
						Coordinate c2 = majorLocationList.get(j).get(0).getLocation().getCoordinate();
						int w2 = majorLocationList.get(j).size();
						int dist = (int) c1.distance(c2);
						if( dist < DISTANCE_THRESHOLD ){
							if( w1 > w2 ){ // move to point 1
								for(int k = 0; k < majorLocationList.get(j).size(); k++ ){
									majorLocationList.get(j).get(k).getLocation().setCoordinate(c1);
								}
								majorLocationList.get(i).addAll( majorLocationList.get(j) );
								majorLocationList.remove(j);
							} else {
								for(int k = 0; k < majorLocationList.get(i).size(); k++ ){
									majorLocationList.get(i).get(k).getLocation().setCoordinate(c2);
								}
								majorLocationList.get(j).addAll( majorLocationList.get(i) );
								majorLocationList.remove(i);
								deleted = true;
							} 
						} else{
							j++;
						}
					}
					if( !deleted ){
						i++;
					}
				}					
			}
			
			for( int i = 0; i < majorLocationList.size(); i++ ){
				homeLocation.add( majorLocationList.get(i).get(0) );
			}
			
		}
	}

	/**
	 * Considers each chain of the vehicle. If a chain does not start <i><b>AND</b></i> 
	 * end at a major location, or if a chain does not contain at least one minor 
	 * activity between two major activities, the chain is removed.
	 */
	public void cleanChains() {
		int i = 0;
		while (i < this.getChains().size() ){
			Activity first = this.getChains().get(i).getActivities().get(0);
			Activity last = this.getChains().get(i).getActivities().get(this.getChains().get(i).getActivities().size() - 1);
			if( (first.getDuration() < ActivityLocations.getMajorActivityMinimumDuration()) ||
			    (last.getDuration() < ActivityLocations.getMajorActivityMinimumDuration()) ||
			    (this.getChains().get(i).getActivities().size() <= 2 ) ){
				this.getChains().remove(i);
			} else{
				this.getChains().get(i).setDuration();
				this.getChains().get(i).setDistance();
				i++;
			}
		}
	}
	

	/**
	 * Returns the list of major activities. 
	 * 
	 * @return ArrayList < Activity > where <code>Activity<code> represents major activities.
	 */
	public ArrayList<Activity> getHomeLocation() {
		return homeLocation;
	}

	/**
	 * Returns the list of chains.
	 * 
	 * @return ArrayList < Chain >
	 */
	public ArrayList<Chain> getChains() {
		return chains;
	}

	/**
	 * Returns the list of all activities in Gauteng.
	 * 
	 * @return {@code ArrayList<Activity>}
	 */
	public ArrayList<Activity> getStudyAreaActivities() {
		return studyAreaActivities;
	}
	
	public int getTotalActivities() {
		return totalActivities;
	}

	public int getVehID() {
		return vehId;
	}

	public int getAvgActivitesPerChain() {
		return averageActivitesPerChain;
	}

	public int getAvgChainDuration() {
		return averageChainDuration;
	}

	public int getAvgChainDistance() {
		return averageChainDistance;
	}

	public double getPercentStudyAreaActivities() {
		return percentageStudyAreaActivities;
	}
		
	public int getNumberOfStudyAreaActivities() {
		return numberOfStudyAreaActivities;
	}

	public int getStudyAreaChainDistance() {
		return studyAreaChainDistance;
	}

}

package playground.jjoubert.CommercialTraffic;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

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
public class CommercialVehicle {
	private final Logger log = Logger.getLogger(CommercialVehicle.class);
	private int vehID;
	private List<Coordinate> majorLocationList;
	private List<Chain> chains;
	private List<Activity> studyAreaActivities;
	private int avgActivitesPerChain;
	private int avgChainDuration;
	private int avgChainDistance; 
	private int numberOfStudyAreaActivities;
	private double percentageStudyAreaActivities;
	private int studyAreaChainDistance;
	private int totalActivities;
		
	/**
	 * Creates a new vehicle with the ID as obtained from the DigiCore data set.
	 * 
	 * @param id is the unique vehicle identification number used by DigiCore
	 */
	public CommercialVehicle(int id){

		this.vehID = id;
		this.majorLocationList = new ArrayList<Coordinate>();
		this.chains = new ArrayList<Chain>();
		this.studyAreaActivities = new ArrayList<Activity>();
		this.avgActivitesPerChain = 0;
		this.avgChainDuration = 0;
		this.avgChainDistance = 0;
		this.numberOfStudyAreaActivities = 0;
		this.percentageStudyAreaActivities = 0;
		this.totalActivities = 0;
	}
	
	/**
	 * Updates a number of statistics for the vehicle:
	 * <ul>
	 * <li> calculates the average number of activities per chain;
	 * <li> calculates the average chain duration (in minutes);
	 * <li> calculates the average chain distance (in meters); 
	 * <li> extracts all the <code>major</code> location coordinates; and
	 * <li> calculates the average number of activities in the study area.
	 * </ul>
	 *
	 * @param studyArea of type <code>MultiPolygon</code>, assumed to be given in the 
	 * 		  WGS84_UTM35S coordinate system.
	 */
	public void updateVehicleStatistics(MultiPolygon studyArea){
		setAverageActivitiesPerChain();
		setAverageChainDuration();
		setAverageChainDistance();
		if(studyArea != null){
			setStudyAreaActivities(studyArea);
		}
		setMajorLocations();
	}
	
	
	/**
	 * Updates a number of statistics for the vehicle:
	 * <ul>
	 * <li> calculates the average number of activities per chain;
	 * <li> calculates the average chain duration (in minutes);
	 * <li> calculates the average chain distance (in meters); and
	 * <li> extracts all the <code>major</code> location coordinates.
	 * </ul>
	 */
	public void updateVehicleStatistics(){
		this.updateVehicleStatistics(null);
	}
	
	private void setAverageActivitiesPerChain(){
		int totalActivities = 0;
		if(this.chains.size() > 0){
			for (Chain chain : this.chains) {
				totalActivities += (chain.getActivities().size() - 2);
			}
			this.avgActivitesPerChain = (int) ( totalActivities / (double) this.chains.size() );
		} else{
			this.avgActivitesPerChain = (0);
			log.warn("Vehicle " + vehID + " has an empty chain!");
		}
		this.totalActivities = totalActivities;
	}

	private void setAverageChainDuration(){
		if(this.chains.size() > 0){
			int duration = 0;
			for (Chain chain : this.chains){
				duration += chain.getDuration();
			}
			this.avgChainDuration = ((int) (duration / this.chains.size() ));
		} else{
			this.avgChainDuration = (0);
		}
	}

	private void setAverageChainDistance() {
		if(this.chains.size() > 0){
			int totalDistance = 0;
			for(Chain chain: this.chains ){
				totalDistance += chain.getDistance();
			}
			this.avgChainDistance = ((totalDistance / this.chains.size() ));
		} else{
			this.avgChainDistance = (0);
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
	 * <p>This method extract all the <code>major</code> activity locations. The first 
	 * activity of each chain is added, as well as the last activity of the last chain.
	 */
	private void setMajorLocations(){
		for (Chain chain : this.chains) {
			majorLocationList.add(chain.getActivities().get(0).getLocation().getCoordinate());
		}
		majorLocationList.add(this.chains.get(this.chains.size()-1).getActivities().get(0).getLocation().getCoordinate());
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
	public List<Coordinate> getMajorLocationList() {
		return majorLocationList;
	}

	/**
	 * Returns the list of chains.
	 * 
	 * @return ArrayList < Chain >
	 */
	public List<Chain> getChains() {
		return chains;
	}

	/**
	 * Returns the list of all activities in Gauteng.
	 * 
	 * @return {@code ArrayList<Activity>}
	 */
	public List<Activity> getStudyAreaActivities() {
		return studyAreaActivities;
	}
	
	public int getTotalActivities() {
		return totalActivities;
	}

	public int getVehID() {
		return vehID;
	}

	public int getAvgActivitesPerChain() {
		return avgActivitesPerChain;
	}

	public int getAvgChainDuration() {
		return avgChainDuration;
	}

	public int getAvgChainDistance() {
		return avgChainDistance;
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

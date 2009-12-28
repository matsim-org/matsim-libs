package playground.jjoubert.CommercialTraffic;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

/**
 * A vehicle container class for the commercial vehicle study in South Africa. Each 
 * vehicle contains a unique vehicle Id (as provided by <i>DigiCore</i>); a <code>List</code>
 * of <code>Chain</code>s, and some summary statistics about the activities and the chains.
 *  
 * @author jwjoubert
 */
public class CommercialVehicle {
	private int vehID;	
	private List<Chain> chains;
	private int averageActivitesPerChain;
	private int averageChainDuration;
	private int averageChainDistance; 
	private double fractionMinorInStudyArea;
	private double fractionMajorInStudyArea;
	private double kilometerPerAreaActivityStat;
		
	/**
	 * Creates a new vehicle with the ID as obtained from the DigiCore data set.
	 * 
	 * @param id is the unique vehicle identification number used by <i>DigiCore<i>.
	 */
	public CommercialVehicle(int id){

		this.vehID = id;
		this.chains = new ArrayList<Chain>();
		this.averageActivitesPerChain = 0;
		this.averageChainDuration = 0;
		this.averageChainDistance = 0;
		this.fractionMinorInStudyArea = 0;
		this.fractionMajorInStudyArea = 0;
		this.kilometerPerAreaActivityStat = 0;
	}
	
	/**
	 * Calculates a number of statistics for the vehicle. If the study area parameters is
	 * passed as <code>null</code>, then the associated statistics will not be calculated.
	 * Rather, it is then suggested the <code>updateVehicleStatistics()</code> method be 
	 * used.
	 * <ul>
	 * <li> calculates the average number of activities per chain;
	 * <li> calculates the average chain duration (in minutes);
	 * <li> calculates the average chain distance (in meters); 
	 * <li> calculates the fraction of <i>minor</i> activities in the study area;
	 * <li> calculates the fraction of <i>major</i> activities in the study area;
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
	}
	
	
	/**
	 * Calculates a number of statistics for the vehicle:
	 * <ul>
	 * <li> calculates the average number of activities per chain;
	 * <li> calculates the average chain duration (in minutes);
	 * <li> calculates the average chain distance (in meters); and
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
			this.averageActivitesPerChain = (int) ( totalActivities / (double) this.chains.size() );
		} else{
			this.averageActivitesPerChain = (0);
		}
	}

	private void setAverageChainDuration(){
		if(this.chains.size() > 0){
			int duration = 0;
			for (Chain chain : this.chains){
				duration += chain.getDuration();
			}
			this.averageChainDuration = (int) (duration / (double) this.chains.size());
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
		Geometry studyAreaEnvelope = studyArea.getEnvelope();
		
		List<Coordinate> majorList = new ArrayList<Coordinate>();

		int totalMinor = 0;
		int minorInStudyArea = 0;
		double studyAreaDistance = 0;
		
		if(this.chains.size() > 0){
			for (Chain chain : this.chains) {
				if(chain.getActivities().size() > 0){
					for (int i = 1; i < chain.getActivities().size() - 1; i++ ) { // don't count first and last major locations
						Activity thisActivity = chain.getActivities().get(i);
						if(i == 1){
							/*
							 * Check for 'major' activities.
							 */
							Coordinate c = thisActivity.getLocation().getCoordinate();
							if(!majorList.contains(c)){
								majorList.add(c);
							}
				
						} else{
							/*
							 * Check for 'minor' activities.
							 */
							Point p = gf.createPoint( thisActivity.getLocation().getCoordinate() );
							/*
							 * For efficiency, I first check if the point is within the 
							 * envelope of the study area. This is a 'cheaper' calculation 
							 * than immediately checking if the point is within the study 
							 * area. 
							 */
							if(studyAreaEnvelope.contains(p)){
								if( studyArea.contains(p) ){
									minorInStudyArea++;
									chain.setInStudyArea(true);
								}
							}
							totalMinor++;
						}
					}
				}
				if(chain.isInStudyArea()){
					studyAreaDistance += chain.getDistance();
				}
			}
		}
		fractionMinorInStudyArea = minorInStudyArea / ((double) totalMinor );
		
		// Calculate the descriptive statistic I developed.
		if(minorInStudyArea > 0){
			kilometerPerAreaActivityStat = ((double) (studyAreaDistance / (double) 1000)) / ((double) minorInStudyArea);
		}
		

		/*
		 * Second, we check the 'major' activities.
		 */
		int majorInStudyArea = 0;
		for (Coordinate c : majorList) {
			Point p = gf.createPoint(c);
			if(studyAreaEnvelope.contains(p)){
				if(studyArea.contains(p)){
					majorInStudyArea++;
				}
			}
		}
		fractionMajorInStudyArea = majorInStudyArea / ((double) majorList.size());
	}
			
	/**
	 * Returns the list of chains.
	 * 
	 * @return {@code ArrayList<Chain>}
	 */
	public List<Chain> getChains() {
		return chains;
	}

	public int getVehID() {
		return vehID;
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

	public double getFractionMinorInStudyArea() {
		return fractionMinorInStudyArea;
	}
		
	public double getFractionMajorInStudyArea() {
		return fractionMajorInStudyArea;
	}
	
	public double getKilometerPerAreaActivityStat() {
		return kilometerPerAreaActivityStat;
	}

}

package playground.southafrica.freight.digicore.analysis.chain.chainSimilarity.binary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;

public class ChainComparator {
	final private DigicoreVehicle vehicle;
	private List<Id> facilities;
	private QuadTree<Id> qt;
	
	public ChainComparator(DigicoreVehicle vehicle) {
		this.vehicle = vehicle;
		this.facilities = new ArrayList<Id>();
		this.qt = null;
	}
	
	public void buildActivityFacilityList(){
		/* Set up the QuadTree. */
		double xMin = Double.POSITIVE_INFINITY;
		double xMax = Double.NEGATIVE_INFINITY;
		double yMin = Double.POSITIVE_INFINITY;
		double yMax = Double.NEGATIVE_INFINITY;
		
		Map<Id, Coord> map = new HashMap<Id, Coord>();
		int idCounter = 0;
		
		for(DigicoreChain chain : this.vehicle.getChains()){
			for(DigicoreActivity activity : chain.getAllActivities()){
				/* Check the QT extents. */
				xMin = Math.min(xMin, activity.getCoord().getX());
				xMax = Math.max(xMax, activity.getCoord().getX());
				yMin = Math.min(yMin, activity.getCoord().getY());
				yMax = Math.max(yMax, activity.getCoord().getY());
				
				Id id = null;
				if(activity.getFacilityId() != null){
					id = activity.getFacilityId();
				} else{
					/* Get an artificial Id, and place the activity in the QT. */
					Id artificialId = new IdImpl("a" + idCounter++);
					id = artificialId;
					map.put(artificialId, activity.getCoord());
				}
				
				/* Add only if not already in the list. */
				if(!facilities.contains(id)){
					facilities.add(id);
				}
			}
		}
		
		/* Now populate the QuadTree with those activities that do not have a
		 * facility Id associated with it. */
		this.qt = new QuadTree<Id>(xMin, yMin, xMax, yMax);
		for(Id id : map.keySet()){
			this.qt.put(map.get(id).getX(), map.get(id).getY(), id);
		}
	}
	
	/**
	 * @param chain1
	 * @param chain2
	 * @return
	 */
	public double compareChains(DigicoreChain chain1, DigicoreChain chain2, double penalty){
		double comparison = 0.0;
		
		/* Calculate the presence factor of each facility. */
		for(Id facilityId : this.facilities){
			Integer i1 = this.getActivityPosition(chain1, facilityId);
			Integer i2 = this.getActivityPosition(chain2, facilityId);
			
			double overlap;
			if(i1 == null || i2 == null){
				overlap = penalty;
			} else{
				overlap = Math.pow((double)i1 - (double)i2, 2);
			}
			comparison += overlap;
		}
		return comparison;
	}
	
	public QuadTree<Id> getQuadTree(){
		return this.qt;
	}

	public List<Id> getFacilityIds() {
		return this.facilities;
	}
	
	
	public Integer getActivityPosition(DigicoreChain chain, Id id){
		Integer i = null;
		
		int index = 0;
		boolean found = false;
		while(!found & index < chain.size()){
			DigicoreActivity activity = chain.get(index);
			if(activity.getFacilityId() != null){
				if( activity.getFacilityId().toString().equalsIgnoreCase(id.toString()) ){
					found = true;
					i = index;
				} else{
					index++;
				}
			} else{
				/* The activity doesn't have a facility Id, so we should get 
				 * the Id from the QuadTree using the activity's coordinate. */
				Id artificialId = this.qt.get(activity.getCoord().getX(), activity.getCoord().getY());
				if( artificialId.toString().equalsIgnoreCase(id.toString()) ){
					found = true;
					i = index;
				} else{
					index++;
				}
			}
		}
		
		return i;
	}
	
	
}

package playground.southafrica.freight.digicore.algorithms.postclustering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.ObjectAttributes;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreFacility;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;


public class CallableChainReconstructor implements Callable<DigicoreVehicle> {
	private Logger log = Logger.getLogger(CallableChainReconstructor.class);
	private DigicoreVehicle vehicle;
	private QuadTree<DigicoreFacility> facilityTree;
	private ObjectAttributes facilityAttributes;
	private Counter threadCounter;
	private boolean inStudyArea;
	private Geometry studyArea;

	public CallableChainReconstructor(DigicoreVehicle vehicle, QuadTree<DigicoreFacility> facilityTree, 
			ObjectAttributes facilityAttributes, Counter threadCounter, Geometry studyArea) {
		this.vehicle = vehicle;
		this.threadCounter = threadCounter;
		this.facilityTree = facilityTree;
		this.facilityAttributes = facilityAttributes;
		this.studyArea = studyArea;
	}


	@Override
	public DigicoreVehicle call() throws Exception {
		GeometryFactory gf = new GeometryFactory();

		for(DigicoreChain dc : this.vehicle.getChains()){
			for(DigicoreActivity da : dc.getAllActivities()){
				/* Convert activity coordinate to Point. */
				Point dap = gf.createPoint(new Coordinate(da.getCoord().getX(), da.getCoord().getY()));

				/* Check if it is inside the study area. But only check if it 
				 * has not already been flagged as inside the area. */
				if(!inStudyArea){
					if(this.studyArea.covers(dap)){
						inStudyArea = true;
					}
				}

				/* Get all the facilities in a 1000m radius around the activity. 
				 * This seems to be a fairly arbitrary threshold. If a point is
				 * not within 1000m, chances are it is NOT AT any facility. */
				Collection<DigicoreFacility> col = this.facilityTree.getDisk(da.getCoord().getX(), da.getCoord().getY(), 1000);
				List<DigicoreFacility> neighbours = new ArrayList<DigicoreFacility>(col.size());
				neighbours.addAll(col);

				if(neighbours.size() > 0){
					boolean found = false;
					int i = 0;
					while(!found && i < neighbours.size()){
						Id<ActivityFacility> thisFacilityId = neighbours.get(i).getId();
						Geometry g = null;
						Object o = this.facilityAttributes.getAttribute(thisFacilityId.toString(), "concaveHull");
						if(o instanceof Geometry){
							g = (Geometry) o;

							/* Check if the activity is inside the geometry. */
							if(g.covers(dap)){
								found = true;

								/* Adapt the facility Id, as well as the coordinate. 
								 * The coordinate will be the centroid of the hull
								 * geometry. That is, NOT the weighted average of 
								 * all the points in the original cluster/hull. 
								 * 
								 * Note that ONLY the activity is changed, the 
								 * traces between facilities are left as is. */
								da.setFacilityId(thisFacilityId);
								if(!g.getCentroid().isEmpty()){
									da.setCoord(new Coord(g.getCentroid().getX(), g.getCentroid().getY()));
								} else{
									log.warn("The geometry is empty and has no centroid. Activity location not changed.");
								}
							} else{
								i++;
							}
						} else{
							/* This should never happen!! If it does, ignore 
							 * checking the point in this area. */
							log.error("The object attribute 'concaveHull' is not a geometry!!");
							log.error("   --> Facility id: " + thisFacilityId.toString());
							i++;
						}
					}
				}
			}
		}
		threadCounter.incCounter();

		if(inStudyArea){
			return this.vehicle;
		} else{
			return null;
		}
	}	
}

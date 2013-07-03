package playground.southafrica.freight.digicore.analysis.postClustering;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreFacility;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader_v1;
import playground.southafrica.freight.digicore.io.DigicoreVehicleWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;


public class RunnableChainReconstructor implements Runnable {
	private Logger log = Logger.getLogger(RunnableChainReconstructor.class);
	private File vehicleFile;
	private String outputFolder;
	private QuadTree<DigicoreFacility> facilityTree;
	private ObjectAttributes facilityAttributes;
	private Counter threadCounter;
	private int activitiesAtFacilities;

	public RunnableChainReconstructor(File vehicleFile, QuadTree<DigicoreFacility> facilityTree, ObjectAttributes facilityAttributes, Counter threadCounter, String outputFolder) {
		this.vehicleFile = vehicleFile;
		this.threadCounter = threadCounter;
		this.outputFolder = outputFolder;
		this.facilityTree = facilityTree;
		this.facilityAttributes = facilityAttributes;
		
		this.activitiesAtFacilities = 0;
	}

	
	@Override
	public void run() {
		GeometryFactory gf = new GeometryFactory();
		
		DigicoreVehicleReader_v1 dvr = new DigicoreVehicleReader_v1();
		dvr.parse(vehicleFile.getAbsolutePath());
		DigicoreVehicle dv = dvr.getVehicle();
		
		for(DigicoreChain dc : dv.getChains()){
			for(DigicoreActivity da : dc.getAllActivities()){
				/* Convert activity coordinate to Point. */
				Point dap = gf.createPoint(new Coordinate(da.getCoord().getX(), da.getCoord().getY()));
				
				/*TODO Remove after debugging. */
				if(dap == null){
					log.error("Null Point for activity location.");
				}
				
				/* Get all the facilities in a 1000m radius around the activity. */
				Collection<DigicoreFacility> col = this.facilityTree.get(da.getCoord().getX(), da.getCoord().getY(), 1000);
				List<DigicoreFacility> neighbours = new ArrayList<DigicoreFacility>(col.size());
				neighbours.addAll(col);
				
				if(neighbours.size() > 0){
					boolean found = false;
					int i = 0;
					while(!found && i < neighbours.size()){
						Point gPoint = null;
						LineString gLineString = null;
						Polygon gPolygon = null;
						
						Id thisFacilityId = neighbours.get(i).getId();
						Geometry g = null;
						Object o = this.facilityAttributes.getAttribute(thisFacilityId.toString(), "concaveHull");
						if(o instanceof Geometry){
							g = (Geometry) o;
							if(g instanceof Point){
								gPoint = (Point) g;
							} else if(g instanceof LineString){
								gLineString = (LineString) g;
							} else if(g instanceof Polygon){
								gPolygon = (Polygon) g;
							} else{
								log.warn("Funny type of Geometry: " + g.getClass().toString());
							}
							
							/* Check if the activity is inside the geometry. */
							if(g.covers(dap)){
								found = true;
								
//							log.info("  --> " + dv.getId().toString());
								
								/* Adapt the facility Id, as well as the coordinate. 
								 * The coordinate will be the centroid of the hull
								 * geometry. That is, NOT the weighted average of 
								 * all the points in the original cluster/hull. */
								da.setFacilityId(thisFacilityId);
								if(!g.getCentroid().isEmpty()){
									da.setCoord(new CoordImpl(g.getCentroid().getX(), g.getCentroid().getY()));
								} else{
									log.warn("The geometry is empty and has no centroid. Activity location not changed.");
								}
								activitiesAtFacilities++;
							} else{
								i++;
							}
						} else{
							/*FIXME This should never happen!! */
							log.error("The object attribute 'concaveHull' is not a geometry!!");
							log.error("   --> Facility id: " + thisFacilityId.toString());
							i++;
						}
					}
				}
			}
		}
		
		/* Write the (possibly) adapted vehicle to file. */
		if(activitiesAtFacilities > 0){
			DigicoreVehicleWriter dvw = new DigicoreVehicleWriter();
			dvw.write(this.outputFolder + dv.getId().toString() + ".xml.gz", dv);		
		}
		
		threadCounter.incCounter();
	}	
}

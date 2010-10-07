package playground.andreas.utils.ana.filterActsPerShape;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

public class WorkHomeShapeCounter extends AbstractPersonAlgorithm{
	
	private static final Logger log = Logger.getLogger(WorkHomeShapeCounter.class);
	private static final Level logLevel = Level.INFO;
	
	private final Coord minXY;
	private final Coord maxXY;
	private String actTypeOne;
	private String actTypeTwo;
	
	private boolean initDone = false;
	private String shapeFile;
	private FeatureSource featureSource;
	private HashMap<Polygon, Integer> polygonCountMap = new HashMap<Polygon, Integer>();
	private HashMap<Polygon, String> polyNameMap = new HashMap<Polygon, String>();
	
	@Override
	public void run(Person person) {
		
		if(!this.initDone){
			init();
		}
		
		int nofPlans = person.getPlans().size();
	
		for (int planId = 0; planId < nofPlans; planId++) {
			Plan plan = person.getPlans().get(planId);
			
			// search selected plan
			if (plan.isSelected()) {
				
				// do something				
				for (PlanElement pEOne : plan.getPlanElements()) {
					if(pEOne instanceof Activity){
						
						// check - actTypeOne in search area? 
						Activity actOne = (Activity) pEOne;						
						if(actOne.getType().equalsIgnoreCase(this.actTypeOne)){
							// it is of type actTypeOne -> check coords
							
							if(actIsSituatedInGivenSearchArea(actOne)){
								// act one ok, check type two;
								
								for (PlanElement pETwo : plan.getPlanElements()) {
									if(pETwo instanceof Activity){
										
										Activity actTwo = (Activity) pETwo;										
										if(actTwo.getType().equalsIgnoreCase(this.actTypeTwo)){
											
											// act two ok - search corresponding shape											
											searchCorrespondingShape(actTwo);
											break;
										}
										
									}									
								}								
								break;
							}							
							
						}						
						
					}
				}
				
			}
		}		
	}

	public WorkHomeShapeCounter(Coord minXY, Coord maxXY, String actTypeOne, String actTypeTwo, String shapeFile){
		super();
		this.minXY = minXY;
		this.maxXY = maxXY;
		this.actTypeOne = actTypeOne;
		this.actTypeTwo = actTypeTwo;
		this.shapeFile = shapeFile;
		log.setLevel(logLevel);
		log.info("Added...");
	}

	@Override
	public String toString() {
		StringBuffer strB = new StringBuffer();
		strB.append("Results with " + this.actTypeOne + " at " + this.minXY + ", " + this.maxXY + " and " + this.actTypeTwo + " at...\n");
		strB.append("Shape, Number of agents\n");
		for (Polygon poly : this.polygonCountMap.keySet()) {
			strB.append(this.polyNameMap.get(poly) + ", " + this.polygonCountMap.get(poly) + "\n");
		}
		return strB.toString();
	}
	
	public void toFile(String filename){
		
		int numberOfActs = 0;
		for (Polygon poly : this.polygonCountMap.keySet()) {
			numberOfActs += this.polygonCountMap.get(poly).intValue();
		}
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filename)));
			writer.write("#Results with " + this.actTypeOne + " act in minXY " + this.minXY + " maxXY " + this.maxXY + " and " + this.actTypeTwo + " act located in"); writer.newLine();
			writer.write("#Includes all acts moved from TXL and SXF to BBI. " + numberOfActs + " agents in total"); writer.newLine();
			writer.write("Shape, Number of agents, Share"); writer.newLine();
			for (Polygon poly : this.polygonCountMap.keySet()) {
				writer.write(this.polyNameMap.get(poly) + ", " + this.polygonCountMap.get(poly) + ", " + (this.polygonCountMap.get(poly).doubleValue() / numberOfActs)); writer.newLine();
			}
			writer.flush();
			writer.close();
			log.info("Results written to " + filename);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Put all polygon in an iterable list
	 */
	private void init() {
		
		try {
			this.featureSource = ShapeFileReader.readDataFile(this.shapeFile);			
	
			for (Object obj : this.featureSource.getFeatures()) {				
				Feature ft = (Feature) obj;
				for (int i = 0; i < ft.getDefaultGeometry().getNumGeometries(); i++) {
					Geometry geometry = ft.getDefaultGeometry().getGeometryN(i);
					if(geometry instanceof Polygon){
						Polygon poly = (Polygon) geometry;
						this.polygonCountMap.put(poly, new Integer(0));
						this.polyNameMap.put(poly, (String) ft.getAttribute("Bezirk"));
					}
				}
			}			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.initDone = true;
	}

	private boolean actIsSituatedInGivenSearchArea(Activity act) {
		if(this.minXY.getX() > act.getCoord().getX()){ return false;}
		if(this.minXY.getY() > act.getCoord().getY()){ return false;}
		if(this.maxXY.getX() < act.getCoord().getX()){ return false;}
		if(this.maxXY.getY() < act.getCoord().getY()){ return false;}
		
		return true;		
	}

	private void searchCorrespondingShape(Activity actTwo) {
		for (Polygon poly : this.polygonCountMap.keySet()) {
			if(poly.contains(MGC.coord2Point(actTwo.getCoord()))){
				this.polygonCountMap.put(poly, new Integer(this.polygonCountMap.get(poly).intValue() + 1));
				break;
			}
		}
	}

}

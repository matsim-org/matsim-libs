package playground.southafrica.freight.digicore.analysis.postClustering;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.misc.Counter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

public class RunnableCoordInAreaDeterminer implements Runnable{
	boolean mustWrite;
	private Counter mapCounter;
	private MultiPolygon area;
	private BufferedWriter bw;
	private GeometryFactory gf = new GeometryFactory();
	private Point p;
	private HashMap<Coord, Integer> smallMap;
	private int mergedOutputThreshold;
	
	
	public RunnableCoordInAreaDeterminer(int mergedOutputThreshold, HashMap<Coord,Integer> smallMap, BufferedWriter bw, MultiPolygon area, Counter mapCounter){
		this.mergedOutputThreshold = mergedOutputThreshold;
		this.smallMap = smallMap;
		this.bw = bw;
		this.area = area;
		this.mapCounter = mapCounter;
	}
	
	@Override
	public void run() {
		Iterator<Coord> coordIterator = smallMap.keySet().iterator();
		
		while(coordIterator.hasNext()){
			Coord coord = coordIterator.next(); 
			//check if number of merged activities exceed specified threshold
			if(smallMap.get(coord) >= mergedOutputThreshold){
				if(!(area==null)){
					//check if entry is in area
					p = gf.createPoint(new Coordinate(coord.getX(), coord.getY()));
					if(area.getEnvelope().contains(p)){
						if(area.contains(p)){
							//write to file 
							try {
								bw.write(String.format("%.0f,%.0f,%d\n",coord.getX(),coord.getY(),smallMap.get(coord)));
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
					
				}else{
					//there is no shapefile/study area specified - only write
					try {
						bw.write(String.format("%.0f,%.0f,%d\n",coord.getX(),coord.getY(),smallMap.get(coord)));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		mapCounter.incCounter();
		}	
	}
}

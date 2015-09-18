package playground.dhosse.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;

import org.matsim.core.utils.io.IOUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class GeometryUtils {
	
	public static Geometry readPolygonFile(String file){
		
		BufferedReader reader = IOUtils.getBufferedReader(file);
		
		String line = null;
//		int idx = 0;
//		String geometryType = null;
		LinkedList<Coordinate> coordinateList = new LinkedList<>();
		
		try {
			
			while((line = reader.readLine()) != null){
				
				String[] parts = line.split("\t");
				
//				if(idx == 0){
//					
//					geometryType = line;
//					
//				}
				
				if(parts.length > 1){
					
					coordinateList.addLast(new Coordinate(Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
					
				}
				
//				idx++;
				
			}
			
			reader.close();
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
		Coordinate[] coordinates = new Coordinate[coordinateList.size()];
		coordinateList.toArray(coordinates);
		
		return new GeometryFactory().createPolygon(coordinates);
		
	}
	
}

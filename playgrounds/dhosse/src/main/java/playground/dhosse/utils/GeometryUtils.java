package playground.dhosse.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.matsim.core.utils.io.IOUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class GeometryUtils {
	
	private static final String END = "END";
	
	public static Geometry createGeometryFromPolygonFile(String file){
		
		Set<LinkedList<Coordinate>> additiveGeometriesSet = new HashSet<>();
		Set<LinkedList<Coordinate>> subtractiveGeometriesSet = new HashSet<>();
		
		BufferedReader reader = IOUtils.getBufferedReader(file);
		
		String line = null;
		LinkedList<Coordinate> coordinateList = new LinkedList<>();
		String type = null;
		
		try {
			
			while((line = reader.readLine()) != null){

				String[] parts = line.split("\t");
				
				if(parts[0].equals(END) || type == null){
					
					if(type == null){
					
						if(parts[0].startsWith("!")){
							
							type = "sub";
							
						} else{
							
							type = "add";
							
						}
						
					} else if(parts[0].equals(END)){

						if(type.equals("add")){

							additiveGeometriesSet.add(coordinateList);
							
						} else {
							
							subtractiveGeometriesSet.add(coordinateList);
							
						}
						
						coordinateList = new LinkedList<>();
						type = null;
						
					}
					
				} else{

					if(parts.length < 2){
						
						String[] subParts = parts[0].split("   ")[1].split(" ");
						coordinateList.addLast(new Coordinate(Double.parseDouble(subParts[0]), Double.parseDouble(subParts[1])));
						
					} else {
						
						coordinateList.addLast(new Coordinate(Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
						
					}
					
				}
				
			}
			
			reader.close();
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
		LinearRing[] holes = new LinearRing[subtractiveGeometriesSet.size()];

		List<Coordinate> shellCoordinatesList = new ArrayList<>();
		
		for(List<Coordinate> coordinatesList : additiveGeometriesSet){
			
			shellCoordinatesList.addAll(coordinatesList);
			
		}
		
		int index = 0;
		for(List<Coordinate> hole : subtractiveGeometriesSet){
			
			Coordinate[] coordinates = new Coordinate[hole.size()];
			holes[index] = new GeometryFactory().createLinearRing(hole.toArray(coordinates));
			index++;
			
		}
		
		Coordinate[] coordinates = new Coordinate[shellCoordinatesList.size()];

		Polygon p1 = new GeometryFactory().createPolygon(new GeometryFactory().createLinearRing(shellCoordinatesList.toArray(coordinates)), holes);
		
		return new GeometryFactory().createMultiPolygon(new Polygon[]{p1});
		
	}
	
}

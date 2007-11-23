/* *********************************************************************** *
 * project: org.matsim.*
 * Shape2Lineu.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.alexander;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class Shape2Lineu {
	
	private FeatureCollection collection;
	
	public Shape2Lineu(FeatureCollection collection){
		this.collection = collection;
	}

	public void createLinie(Geometry geo){
		
		Line polyline = new Line();
//		polyline.setId((String)geo.getUserData());
		
		final double ANGLEDELTA = (Math.PI/2);
		Coordinate [] coord = geo.getCoordinates();
		GeometryFactory geofac = new GeometryFactory();
		List<Coordinate> coordinates = new ArrayList<Coordinate>();
		List widths = new ArrayList();
	
		Coordinate coordPre = coord[0];
		Coordinate coordCurrent = coord[0]; 
		Coordinate coordNext = coord[0];
		boolean next = true;
		polyline.addPoint(coordCurrent);
		int ii=0;
					
////Konstruktion der Linien innerhalb des Polygons
		while(next){			
			double tmpLength=0;
			for(int i=0 ; i<coord.length ; i++){
					
				Coordinate [] c = new Coordinate[]{coordCurrent, coord[i]};
				CoordinateSequence seq = new CoordinateArraySequence(c);
				LineString line = new LineString(seq, geofac);
				Geometry line2 = (Geometry) line; 
								
				if (line2.within(geo)){ //!geo.crosses(line)&&
					double currentAngle = getAngle(subCoord(coordCurrent,coord[i]),subCoord(coordCurrent,coordPre));
					if(((ii==0) || (currentAngle > ANGLEDELTA)) && (line.getLength() > tmpLength)){																						
						tmpLength = line.getLength();
						coordNext = coord[i];					
					}	
				}
			}									
			if(coordCurrent.equals2D(coordNext)){
				next = false;
				break;
			}
			polyline.addPoint(coordNext);	
//			coordinates.add(coordNext);
			coordPre = coordCurrent;
			coordCurrent = coordNext;				
			ii++;
		}
////
////(Schnittpunkte) & Breite
		coordinates = polyline.getPoints();
		
		for (int i=0 ; i < polyline.getPoints().size()-1 ; i++){
			final double STEP = 0.5;
			
			Coordinate coord1 = (Coordinate) coordinates.get(i);
			Coordinate coord2 = (Coordinate) coordinates.get(i+1);			
			Coordinate normiert12 = new Coordinate(subCoord(coord2,coord1).x/getLength(subCoord(coord2,coord1)),subCoord(coord2,coord1).y/getLength(subCoord(coord2,coord1)));			
			double currentLength = getLength(subCoord(coord1,coord2));			
			Coordinate [] inter = new Coordinate[2]; 
									
			for (double iv=0 ; iv < currentLength ; iv = iv + STEP){
																
				Coordinate ortho = new Coordinate(-normiert12.y, normiert12.x);
				Coordinate coordA = subCoord(coord1, multiCoord(ortho, 40));
				Coordinate coordB = subCoord(coord1, multiCoord(ortho, -40));
							
				Coordinate [] d = new Coordinate[]{coordA, coordB};
				CoordinateSequence seqd = new CoordinateArraySequence(d);
				LineString orthoLine = new LineString(seqd, geofac);			
				
				Geometry intersections = geo.intersection(orthoLine);
				inter = intersections.getCoordinates();
								
				if(inter.length == 2){
					double width = getLength(subCoord(inter[0],inter[1]));
					polyline.addWidth(width);
				}
				coord1 = addCoord(coord1,multiCoord(normiert12,0.5));											
			}						
			for(Iterator it = polyline.getWidths().iterator() ; it.hasNext() ; ){System.out.println(it.next());}
		}
		
//////    Merken von Kreuzungen 		            	      	
    	FeatureIterator iteratorII = collection.features();		            	
    	for (;iteratorII.hasNext();){
    			boolean overlaps = false;
            	Feature featureII = iteratorII.next();
    			Geometry tmpGeo = featureII.getDefaultGeometry();		            			
    			overlaps = geo.overlaps(tmpGeo);	            				            					            			
    			if (overlaps) {    				
			
//     				Geometry inter = geo.intersection(tmpGeo);
////    				Coordinate [] interCoord = inter.getCoordinates();
//    				Point pCenter = inter.getCentroid();
//    				Coordinate pCoord = pCenter.getCoordinate();
//    				Coordinate coord1 = new Coordinate();
//    				double length1 = 10000000;
//    				Coordinate coord2 = new Coordinate();
//    				double length2 = 10000000;
//       				for(int i = 0 ; i < coordinates.size() ; i++){
//    					if (length1 < getLength(subCoord(pCoord,coordinates.get(i)))){
//    						length1 = getLength(subCoord(pCoord,coordinates.get(i)));
//    						coord1 = coordinates.get(i);
//    					}    					
//    				}       				
//    				for(int i = 0 ; i < coordinates.size() ; i++){
//    					double currentAngle = getAngle(subCoord(coord1,coord[i]),subCoord(coord1,pCoord));
//    					if ((length2 < getLength(subCoord(pCoord,coordinates.get(i))))&&(currentAngle > ANGLEDELTA)){
//    						length2 = getLength(subCoord(pCoord,coordinates.get(i)));
//    						coord2 = coordinates.get(i);
//    					}    					
//    				}
//    				/////Einf�gen
//    				
//    				
////    				System.out.println(interCoord);		            				
////    				System.out.println(pCenter.getCoordinate());	
////    				for (int it = 0; it < interCoord.length ;it++){
////    					System.out.println(interCoord[it]);		            					
////    				}
////    				String typeII = featureII.getAttribute(2).toString();
////    				System.out.println(overlap + " " + typeII );
    			}
    	}		
							
		for (Iterator it = coordinates.iterator();it.hasNext();){
			System.out.println("  " +it.next());	
		}	
	}
	
	public static Coordinate multiCoord(Coordinate coordI, double skalar){
		Coordinate coord = new Coordinate(coordI.x*skalar, coordI.y*skalar);
		return coord;
	}
	
	public static Coordinate addCoord(Coordinate coordI, Coordinate coordII){
		Coordinate coord = new Coordinate(coordI.x + coordII.x, coordI.y + coordII.y);
		return coord;
	}
	
	public static Coordinate subCoord(Coordinate coordI, Coordinate coordII){
		Coordinate coord = new Coordinate(coordI.x - coordII.x, coordI.y - coordII.y);
		return coord;
	}
	
	public static double skalarMultiCoord(Coordinate coordI, Coordinate coordII){
		double skalarprodukt = (coordI.x*coordII.x) + (coordI.y*coordII.y);
		return skalarprodukt;
	}
	
	public static double getLength(Coordinate coordI){
		double length = Math.sqrt((coordI.x*coordI.x)+(coordI.y*coordI.y));
		return length;
	}
	
	public static double getAngle(Coordinate coordI, Coordinate coordII){
		double angle = Math.acos(skalarMultiCoord(coordI,coordII)/(getLength(coordI)*getLength(coordII)));
		return angle;
	}
	
}

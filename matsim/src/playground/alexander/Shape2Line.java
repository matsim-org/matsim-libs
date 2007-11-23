/* *********************************************************************** *
 * project: org.matsim.*
 * Shape2Line.java
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

import java.util.Iterator;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class Shape2Line {
		
	public Line createLinie(Geometry geo){
		
//		final double ANGLEDELTA = (Math.PI/90);
		final double DISTDELTA = 20;
		
		
		Line line = new Line();
		Coordinate [] coord = geo.getCoordinates();
		Coordinate normalInA = new Coordinate(0,0);
		Coordinate normalInALast = new Coordinate(0,0);
		
		for(int i=0 ; i<coord.length-1 ; i++){
			Coordinate coordA = coord[i];
			Coordinate coordB = coord[i+1];
			
			normalInALast.x = normalInA.x;
			normalInALast.y = normalInA.y;
			normalInA.x = -(coordB.y-coordA.y);
			normalInA.y = (coordB.x-coordA.x);
				
//			Coordinate normalInANorm = new Coordinate(normalInA.x/getLength(normalInA),normalInA.y/getLength(normalInA));
//			Coordinate normalInANormLast;			
//			System.out.println(normalInANorm);
			
			double width = 0;
			double angle = Math.PI;
			Coordinate schnittpunkt = new Coordinate();
			
			for(int ii=0 ; ii<coord.length-1 ; ii++){
								
				Coordinate coordC = coord[ii];
				Coordinate coordD = coord[ii+1];								
				//Schnittpunkt
				double skalar1 = skalarMultiCoord(subCoord(coordB,coordA),coordA); 
				double skalar2 = skalarMultiCoord(subCoord(coordB,coordA),coordC);
				double skalar3 = skalarMultiCoord(subCoord(coordD,coordC),subCoord(coordB,coordA)); 
				Coordinate CurrentSchnittpunkt = addCoord(coordC,multiCoord(subCoord(coordD,coordC),((skalar1-skalar2)/skalar3)));
//				System.out.println("X: "+Schnittpunkt);
				//Abstand
				double distance = getLength(subCoord(coordA,CurrentSchnittpunkt));
				//Winkel
				double currentAngle = getAngle(subCoord(coordB,coordA),subCoord(coordC,coordD));
				if(currentAngle < angle){
					angle = currentAngle;
					width = distance;
					schnittpunkt = CurrentSchnittpunkt;
				}								
			}
			//Abbruch
			if((normalInALast.x < 0 && normalInA.x > 0) || (normalInALast.x > 0 && normalInA.x < 0) || (normalInALast.y < 0 && normalInA.y > 0) || (normalInALast.y > 0 && normalInA.y < 0)){				
//				System.out.println("break");
				break;				
			}
			//Punkt berechnen und hinzuf�gen
			if(width < DISTDELTA){
				Coordinate point = subCoord(coordA,multiCoord(subCoord(schnittpunkt, coordA),-0.5));
				line.addPoint(point);
				line.addWidth(width);				
			}else{
				//f�r das viertel, dass so nicht erfasst wird, andere L�sung suchen
			}
		}		
		//Ausgabe und Test
		List linienCoord = line.getPoints();
		List widths = line.getWidths();
		int iiii = 0;		
		for(Iterator it = linienCoord.iterator(); it.hasNext();){			
			Coordinate cor = (Coordinate) it.next();
			GeometryFactory geoFac = new GeometryFactory();
			Geometry point = geoFac.createPoint(cor);			
			boolean inside = geo.isWithinDistance(point, 130);
//			if (!inside){ System.out.println(cor+" "+inside);}
			double tmpWidth = (Double) widths.get(iiii);
//			System.out.println(cor+" "+inside+" width: "+tmpWidth);
			iiii++;
		}
		return(line);
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

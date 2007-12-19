/* *********************************************************************** *
 * project: org.matsim.*
 * Line2Link.java
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
import org.matsim.network.NetworkLayer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class Line2Link {
	
	FeatureCollection polygons;
	FeatureCollection links;
	NetworkLayer network;
		
	public Line2Link(FeatureCollection polygons, FeatureCollection links, NetworkLayer network){
		this.polygons = polygons;
		this.links = links;
		this.network = network;
	}
	
	public Line2Link(FeatureCollection polygons, FeatureCollection links){
		this.polygons = polygons;
		this.links = links;
	}
	
	public void writeWidth(){
	
		FeatureIterator linksIterator = links.features();
		int count = 0;

		try{			
			while(linksIterator.hasNext()){
			
				Feature link = linksIterator.next();
				MultiLineString linkGeo = (MultiLineString) link.getDefaultGeometry();
				Coordinate [] linkCoord = linkGeo.getCoordinates();	
				List<Geometry> polys = new ArrayList<Geometry>();
				List<Double> widths = new ArrayList<Double>();
				GeometryFactory geofac = new GeometryFactory();
						
				//Breiten �ber Linienpunkte (mit Identifizierung der relevanten Polygone)
				
				for(int i=0 ; i < linkCoord.length ; i++){												
					
					Coordinate [] g = new Coordinate[]{linkCoord[i]};
					
					CoordinateSequence seq = new CoordinateArraySequence(g);
	        		Point point = new Point(seq,geofac);
					
	        		FeatureIterator polygonIterator = polygons.features();
	        		
					try{
						while(polygonIterator.hasNext()){
							Feature polygon = polygonIterator.next();
							Geometry polygonGeo = polygon.getDefaultGeometry();
							if(polygonGeo.covers(point)||polygonGeo.crosses(linkGeo)){
								
								polys.add(polygonGeo);																
								Coordinate coord1;
								Coordinate coord2;
								
								if(i==0){
									coord1 = new Coordinate(linkCoord[i]);
									coord2 = new Coordinate(linkCoord[i+1]);
								} else {
									coord1 = new Coordinate(linkCoord[i]);
									coord2 = new Coordinate(linkCoord[i-1]);
								}
								
								Coordinate normiert12 = new Coordinate(subCoord(coord2,coord1).x/getLength(subCoord(coord2,coord1)),subCoord(coord2,coord1).y/getLength(subCoord(coord2,coord1)));
								
								Coordinate ortho = new Coordinate(-normiert12.y, normiert12.x);
								Coordinate coordA = subCoord(coord1, multiCoord(ortho, 40));
								Coordinate coordB = subCoord(coord1, multiCoord(ortho, -40));
											
								Coordinate [] d = new Coordinate[]{coordA, coordB};
								CoordinateSequence seqd = new CoordinateArraySequence(d);
								LineString orthoLine = new LineString(seqd, geofac);			
																
								if(polygonGeo.intersects(orthoLine)){
									Coordinate [] inter = polygonGeo.intersection(orthoLine).getCoordinates();
									for(int iii = 0 ; iii < inter.length-1 ; iii++ ){
										widths.add(getLength(subCoord(inter[iii],inter[iii+1])));																			
									}
											
								}																				
							}
						}
					}
					finally{
						polygonIterator.close();
					}
					
				}
				//Breiten �ber Polygonpunkte
				
				for(Iterator<Geometry> it = polys.iterator() ; it.hasNext() ; ){
					
					Geometry polygonGeo = (Geometry)it.next();
					Coordinate [] polyCoord = polygonGeo.getCoordinates();

					for(int i=0 ; i<polyCoord.length-1 ; i++){																		
						
						Coordinate polyCoordX = polyCoord[i];
						
						for(int ii=0 ; ii<linkCoord.length-1 ; ii++){
							
							Coordinate coord1 = new Coordinate(linkCoord[ii]);
							Coordinate coord2 = new Coordinate(linkCoord[ii+1]);
														
							Coordinate normiert12 = new Coordinate(subCoord(coord2,coord1).x/getLength(subCoord(coord2,coord1)),subCoord(coord2,coord1).y/getLength(subCoord(coord2,coord1)));
							Coordinate ortho = new Coordinate(-normiert12.y, normiert12.x);
							
							Coordinate coordA = subCoord(polyCoordX, multiCoord(ortho, 40));
							Coordinate coordB = subCoord(polyCoordX, multiCoord(ortho, -40));
							
							Coordinate [] d = new Coordinate[]{coordA, coordB};
							CoordinateSequence seqd = new CoordinateArraySequence(d);
							LineString orthoLine = new LineString(seqd, geofac);
							
							Coordinate [] e = new Coordinate[]{coord1, coord2};
							CoordinateSequence seqe = new CoordinateArraySequence(e);
							LineString line = new LineString(seqe, geofac);
															
							if(polygonGeo.intersects(orthoLine) && orthoLine.intersects(line)){
								try{
									Coordinate [] inter = polygonGeo.intersection(orthoLine).getCoordinates();
									for(int iii = 0 ; iii < inter.length-1 ; iii++ ){
										widths.add(getLength(subCoord(inter[iii],inter[iii+1])));									
									}
								}catch(com.vividsolutions.jts.geom.TopologyException e1){
									System.out.println("TopologyException [Geometry.intersection()]: link:" +link.getAttribute(1).toString());
								}catch(Exception e1){
									System.out.println(e1);
								}
								
							}																					
						}
					}					
				}
				
				//Breite
				double minWidth = 40;
				for(Iterator<Double> it = widths.iterator() ; it.hasNext() ; ){					
					Double tmpWidth = (Double) it.next();
					double tmpWidthValue = tmpWidth.doubleValue();
					if(tmpWidthValue < minWidth)minWidth = tmpWidthValue;
				}

				String linkId = link.getAttribute(1).toString();
				String linkId2 = Integer.toString((Integer)(link.getAttribute(1))+100000);
				if (network.getLink(linkId)!=null){
					network.getLink(linkId).setCapacity(minWidth);				
					network.getLink(linkId2).setCapacity(minWidth);	

				}
			count++;
			if(count%1000 == 0){ 
				System.out.println(count);
			}
			}
	
		}
		finally{
			linksIterator.close();
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

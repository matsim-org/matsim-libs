/* *********************************************************************** *
 * project: org.matsim.*
 * tmp.java
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

package playground.gregor.shapeFileToMATSim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

public class tmp {

}
private HashMap<Integer,Polygon> mergePolygons(){
	
	log.info("merging polygons ...");
	HashMap<Integer,Polygon> returnPolys = new HashMap<Integer, Polygon>();
	
	for (Iterator lsIt = lineStrings.keySet().iterator() ; lsIt.hasNext() ; ){
	
		Integer id = (Integer) lsIt.next();
					
		LineString ls = lineStrings.get(id);
		 
		HashSet<Polygon> neighborhood = new HashSet<Polygon>();
		Collection<Polygon> polys = polygonTree.get(ls.getCentroid().getX(),ls.getCentroid().getY() , 900);

		
    		for (Polygon po : polys){
				if(ls.crosses(po) || po.covers(ls)) { 
					neighborhood.add(po);
				}	
			}
    		List<Polygon> extNeighborhood = new ArrayList<Polygon>();
    		extNeighborhood.addAll(neighborhood);
    		for (Polygon po : polys) {
    			if (!neighborhood.contains(po)){
    				for (Polygon tmp : neighborhood) {
    					if (po.crosses(tmp)){
    						extNeighborhood.add(po);
    					}
    				}
    			}
    		}
//		}
		
		if(extNeighborhood.isEmpty()){	
//			log.warn("cant find any polygon belonging to LineString " + ls + " this should not happen!");
			continue;
		}
			   			   			
		Geometry [] gA = new Geometry[0];
		Geometry [] geoArray = extNeighborhood.toArray(gA);
			GeometryCollection geoColl = new GeometryCollection(geoArray,geofac);	
			   			 			
			try{
			Geometry retPoly = geoColl.buffer(0.05);//magic number!!
			
			for (int i = 0; i < retPoly.getNumGeometries(); i++) {
			Polygon polygon = (Polygon) retPoly.getGeometryN(i);
			if(!polygon.isEmpty()){					
				returnPolys.put( id ,polygon);
			}
		}
			
			}catch(Exception e){
				e.printStackTrace();
			}
			 

	}

	log.info("done.");
	return returnPolys;
}
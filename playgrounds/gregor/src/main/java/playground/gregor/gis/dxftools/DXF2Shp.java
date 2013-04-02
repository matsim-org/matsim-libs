/* *********************************************************************** *
 * project: org.matsim.*
 * DXF2Shp.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor.gis.dxftools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.kabeja.dxf.DXFDocument;
import org.kabeja.dxf.DXFEntity;
import org.kabeja.dxf.DXFLayer;
import org.kabeja.dxf.DXFLine;
import org.kabeja.dxf.DXFPolyline;
import org.kabeja.dxf.DXFSolid;
import org.kabeja.dxf.DXFVertex;
import org.kabeja.dxf.helpers.Point;
import org.kabeja.parser.DXFParser;
import org.kabeja.parser.ParseException;
import org.kabeja.parser.Parser;
import org.kabeja.parser.ParserBuilder;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class DXF2Shp {

	public static void main (String [] args) throws ParseException {

		CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:21781");
		PolylineFeatureFactory lf  = new PolylineFeatureFactory.Builder().
				setCrs(targetCRS).
				addAttribute("layerID", String.class).
				addAttribute("id", String.class).
				addAttribute("color", Integer.class).
				create();
		Collection<SimpleFeature> lts = new ArrayList<SimpleFeature>();
		
		
		Parser parser = ParserBuilder.createDefaultParser();
		parser.parse("/Users/laemmel/devel/burgdorf/dxf/konzept/04_Verkehrskonzept.dxf", DXFParser.DEFAULT_ENCODING);
		DXFDocument doc = parser.getDocument();
		Iterator it = doc.getDXFLayerIterator();
		while (it.hasNext()) {
			DXFLayer l = (DXFLayer) it.next();
			Iterator eit = l.getDXFEntityTypeIterator();
			while (eit.hasNext()) {
				String key = (String)eit.next();
				List<DXFEntity> es = l.getDXFEntities(key);
				for (DXFEntity e : es) {
					if (e instanceof DXFLine) {
						DXFLine dxfl = (DXFLine) e;
						Point start = dxfl.getStartPoint();
						Point end = dxfl.getEndPoint();
						String id = dxfl.getID();
						String layerId = dxfl.getLayerName();
						SimpleFeature ft = lf.createPolyline(new Coordinate[]{new Coordinate(start.getX(),start.getY(),start.getZ()),new Coordinate(end.getX(),end.getY(),end.getZ())}, new Object[]{layerId,id,dxfl.getColor()}, id);
						lts.add(ft);
					} else if (e instanceof DXFPolyline) {
						DXFPolyline dxfl = (DXFPolyline) e;
						Iterator vit = dxfl.getVertexIterator();
						List<Coordinate> coords = new ArrayList<Coordinate>();
						while (vit.hasNext()) {
							DXFVertex v = (DXFVertex) vit.next();
							Coordinate c = new Coordinate(v.getX(),v.getY(),v.getZ());
							coords.add(c);
						}
						Coordinate[] koords = coords.toArray(new Coordinate[0]);
						String id = dxfl.getID();
						String layerId = dxfl.getLayerName();
						SimpleFeature ft = lf.createPolyline(koords, new Object[]{layerId,id,dxfl.getColor()}, id);
						lts.add(ft);						
					} else if (e instanceof DXFSolid) {
						DXFSolid dxfs = (DXFSolid) e;
						String id = dxfs.getID();
						String layerId = dxfs.getLayerName();
						Coordinate [] coords = new Coordinate[4];
						coords[0] = new Coordinate(Math.abs(dxfs.getPoint1().getX()),dxfs.getPoint1().getY());
						coords[1] = new Coordinate(Math.abs(dxfs.getPoint2().getX()),dxfs.getPoint2().getY());
						coords[2] = new Coordinate(Math.abs(dxfs.getPoint3().getX()),dxfs.getPoint3().getY());
						coords[3] = new Coordinate(Math.abs(dxfs.getPoint4().getX()),dxfs.getPoint4().getY());
						
//						if (layerId.equals("Projekt")) {
//							System.out.println("sss");
//						}
							
						SimpleFeature ft = lf.createPolyline(coords, new Object[]{layerId,id,dxfs.getColor()}, id);
						Geometry geo = (Geometry) ft.getDefaultGeometry();
						Geometry hull = geo.convexHull();
						Coordinate[] koords = hull.getCoordinates();
						SimpleFeature ft2 = lf.createPolyline(koords, new Object[]{layerId,id,dxfs.getColor()}, id);
						lts.add(ft2);
						
					}
				}
			}
			//			System.out.println(l.getName());
			//			List<DXFL>;
		}
		
		ShapeFileWriter.writeGeometries(lts, "/Users/laemmel/devel/burgdorf/dxf/verk_lines.shp");
		//		DXFlayer layer = doc.getDXFLayer("layer_name");
		//		List<DXFCircle> arcs = layer.getDXFEntities(DXFConstants.ENTITY_TYPE_CIRCLE);
	}
}

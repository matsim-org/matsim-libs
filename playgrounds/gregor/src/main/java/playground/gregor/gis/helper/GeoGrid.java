package playground.gregor.gis.helper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class GeoGrid {
	
//	private static final double INCR = 2500;
//	private static final double INCR2 = 1250;
//	private static final double CROSS = 50;
//	private static final double minX = 647500;
//	private static final double maxX = 657500;
//	private static final double minY = 9890000;
//	private static final double maxY = 9905000;
	
	private static final double INCR = 1000;
	private static final double INCR2 = 500;
	private static final double CROSS = 20;
	private static final double minX = 650000;
	private static final double maxX = 653000;
	private static final double minY = 9893000;
	private static final double maxY = 9896000;

	public static void main(String [] args) {
		FeatureType ft = initFeatureTypeLs();
		GeometryFactory geofac = new GeometryFactory();
		Collection<Feature> fts = new ArrayList<Feature>();
		
		for (double x = minX; x <= maxX; x += INCR) {
			Coordinate c1 = new Coordinate(x,minY+CROSS);
			for (double y = minY+INCR; y <= maxY; y += INCR) {
				Coordinate c2 = new Coordinate(x,y-CROSS);
				int id = 5;
				LineString ls = geofac.createLineString(new Coordinate[]{c1,c2});
				try {
					fts.add(ft.create(new Object[]{ls,id,(int)c2.x,(int)c2.y}));
				} catch (IllegalAttributeException e) {
					e.printStackTrace();
				}
				c1 = new Coordinate(x,y+CROSS);
			}
			
			
		}
		
		for (double y = minY; y <= maxY; y += INCR) {
			Coordinate c1 = new Coordinate(minX+CROSS,y);
			for (double x = minX+INCR; x <= maxX; x += INCR) {
				Coordinate c2 = new Coordinate(x-CROSS,y);
				int id = 5;
				LineString ls = geofac.createLineString(new Coordinate[]{c1,c2});
				try {
					fts.add(ft.create(new Object[]{ls,id,(int)c2.x,(int)c2.y}));
				} catch (IllegalAttributeException e) {
					e.printStackTrace();
				}
				c1 = new Coordinate(x+CROSS,y);
			}
			
			
		}
		
		for (double x = minX+INCR2; x < maxX; x += INCR2) {
			for (double y = minY+INCR2; y < maxY; y += INCR2) {
				Coordinate c1 = new Coordinate(x-CROSS,y);
				Coordinate c2 = new Coordinate(x+CROSS,y);
				int id = 0;
				LineString ls = geofac.createLineString(new Coordinate[]{c1,c2});
				try {
					fts.add(ft.create(new Object[]{ls,id,(int)c2.x,(int)c2.y}));
				} catch (IllegalAttributeException e) {
					e.printStackTrace();
				}

				Coordinate c3 = new Coordinate(x,y-CROSS);
				Coordinate c4 = new Coordinate(x,y+CROSS);
				LineString ls2 = geofac.createLineString(new Coordinate[]{c3,c4});
				try {
					fts.add(ft.create(new Object[]{ls2,id,(int)c4.x,(int)c4.y}));
				} catch (IllegalAttributeException e) {
					e.printStackTrace();
				}
			}
			
			
		}
//		
//		for (double x = minX; x <= maxX; x += INCR) {
//			int id = 1;
//			if (x/1000 % 5 == 0) {
//				id = 5;
//			}
//			Coordinate c1 = new Coordinate(x,minY);
//			Coordinate c2 = new Coordinate(x,maxY);
//			LineString ls = geofac.createLineString(new Coordinate[]{c1,c2});
//			try {
//				fts.add(ft.create(new Object[]{ls,id,(int)c2.x,(int)c2.y}));
//			} catch (IllegalAttributeException e) {
//				e.printStackTrace();
//			}
//		}
//		for (double y = minY; y <= maxY; y += INCR) {
//			int id = 1;
//			if (y/1000 % 5 == 0) {
//				id = 5;
//			}
//			Coordinate c1 = new Coordinate(minX,y);
//			Coordinate c2 = new Coordinate(maxX,y);
//			LineString ls = geofac.createLineString(new Coordinate[]{c1,c2});
//			try {
//				fts.add(ft.create(new Object[]{ls,id,(int)c2.x,(int)c2.y}));
//			} catch (IllegalAttributeException e) {
//				e.printStackTrace();
//			}
//		}
		try {
//			ShapeFileWriter.writeGeometries(fts, "/home/laemmel/arbeit/diss/qgis/geogrid.shp");
			ShapeFileWriter.writeGeometries(fts, "/home/laemmel/arbeit/svn/shared-svn/projects/LastMile/berichte/abschlussworkshop/gis/geogrid_small.shp");
		} catch (IOException e) {
			e.printStackTrace();
		}

		Collection<Feature> fts2 = new ArrayList<Feature>();
		
		FeatureType ft2 = initFeatureTypeP();
		Coordinate c1 = new Coordinate(minX, minY);
		Coordinate c2 = new Coordinate(minX, maxY);
		Coordinate c3 = new Coordinate(maxX, maxY);
		Coordinate c4 = new Coordinate(maxX, minY);
		Coordinate [] hole = new Coordinate[] {c1,c2,c3,c4,c1};
		LinearRing l2H = geofac.createLinearRing(hole);
		Coordinate c5 = new Coordinate(minX-10000, minY-10000);
		Coordinate c6 = new Coordinate(minX-10000, maxY+10000);
		Coordinate c7 = new Coordinate(maxX+10000, maxY+10000);
		Coordinate c8 = new Coordinate(maxX+10000, minY-10000);
		Coordinate [] shell = new Coordinate[] {c5,c6,c7,c8,c5};
		LinearRing lr = geofac.createLinearRing(shell);
		Polygon p = geofac.createPolygon(lr, new LinearRing[]{l2H});
		try {
			fts2.add(ft2.create(new Object[]{p,0}));
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		}
		try {
//			ShapeFileWriter.writeGeometries(fts2, "/home/laemmel/arbeit/diss/qgis/box.shp");
			ShapeFileWriter.writeGeometries(fts2, "/home/laemmel/arbeit/svn/shared-svn/projects/LastMile/berichte/abschlussworkshop/gis/box_small.shp");
		} catch (IOException e) {
			e.printStackTrace();
		}		
				
		FeatureType ft3 = initFeatureTypePoi();
		Collection<Feature> fts3 = new ArrayList<Feature>();
		for (double x = minX; x <= maxX; x += INCR) {
			Coordinate c= new Coordinate (x, maxY);
			Point poi = geofac.createPoint(c);
			try {
				fts3.add(ft3.create(new Object[]{poi,90,x}));
			} catch (IllegalAttributeException e) {
				e.printStackTrace();
			}
		}
		for (double y = minY; y <= maxY; y += INCR) {
			Coordinate c= new Coordinate (maxX, y);
			Point poi = geofac.createPoint(c);
			try {
				fts3.add(ft3.create(new Object[]{poi,0,y}));
			} catch (IllegalAttributeException e) {
				e.printStackTrace();
			}
		}
		try {
//			ShapeFileWriter.writeGeometries(fts3, "/home/laemmel/arbeit/diss/qgis/labels.shp");
			ShapeFileWriter.writeGeometries(fts3, "/home/laemmel/arbeit/svn/shared-svn/projects/LastMile/berichte/abschlussworkshop/gis/labels_small.shp");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private static FeatureType initFeatureTypePoi() {
		CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.WGS84_UTM47S);
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, targetCRS);
		AttributeType or = AttributeTypeFactory.newAttributeType("or", Integer.class);
		AttributeType val = AttributeTypeFactory.newAttributeType("val", Integer.class);
		Exception ex;
		try {
			return FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, or,val}, "Point");
		} catch (FactoryRegistryException e) {
			ex = e;
		} catch (SchemaException e) {
			ex = e;
		}
		throw new RuntimeException(ex);
	}	
	
	private static FeatureType initFeatureTypeP() {
		CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.WGS84_UTM47S);
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("Polygon",Polygon.class, true, null, null, targetCRS);
		AttributeType id = AttributeTypeFactory.newAttributeType("id", Integer.class);
		Exception ex;
		try {
			return FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, id}, "Polygon");
		} catch (FactoryRegistryException e) {
			ex = e;
		} catch (SchemaException e) {
			ex = e;
		}
		throw new RuntimeException(ex);
	}	
	private static FeatureType initFeatureTypeLs() {
		CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.WGS84_UTM47S);
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("LineString",LineString.class, true, null, null, targetCRS);
		AttributeType id = AttributeTypeFactory.newAttributeType("id", Integer.class);
		AttributeType xT = AttributeTypeFactory.newAttributeType("xT", Integer.class);
		AttributeType yT = AttributeTypeFactory.newAttributeType("yT", Integer.class);
		Exception ex;
		try {
			return FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, id,xT,yT}, "GridLine");
		} catch (FactoryRegistryException e) {
			ex = e;
		} catch (SchemaException e) {
			ex = e;
		}
		throw new RuntimeException(ex);
	}
}

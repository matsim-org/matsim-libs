package playground.gregor.gis.polygonfrombounderyline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.geotools.data.FeatureSource;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class PolygonFromBounderyLine {

	private static final double FETCH_RADIUS = 20;

	private static final double MIN_X = 647250;
	private static final double MAX_X = 659250;
	private static final double MIN_Y = 9886100;
	private static final double MAX_Y = 9906000;

	private String in;

	private String out;

	private GeometryFactory geofac;

	

	public PolygonFromBounderyLine(String in, String out) {
		this.in = in;
		this.out = out;
		this.geofac = new GeometryFactory();
	}

	private void run() {
		QuadTree<LineString> lsTree = new QuadTree<LineString>(MIN_X,MIN_Y,MAX_X,MAX_Y);
		try {
			buildLsTree(lsTree);
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<LineString> ls = new ArrayList<LineString>();
		Set<LineString> rm = new HashSet<LineString>();
		Queue<LineString> queue = new ConcurrentLinkedQueue<LineString>(lsTree.values());
		
		while (queue.size() > 0) {
			LineString l = queue.poll();
			if (rm.contains(l)) {
				continue;
			}
			rm.add(l);
			rm(lsTree,l);
			//backward
			Collection<LineString> tmp1 = lsTree.get(l.getStartPoint().getX(), l.getStartPoint().getY(),FETCH_RADIUS);
			Collection<LineString> tmp2 = lsTree.get(l.getEndPoint().getX(), l.getEndPoint().getY(),FETCH_RADIUS);
			if ( tmp1.size() == 1 ) {
				//				throw new RuntimeException("hm?? something went wrong?!!");
				LineString other = tmp1.iterator().next();
				if (other.getStartPoint().distance(l.getStartPoint()) < FETCH_RADIUS) {

					l = cat(l,false,other,true);
				} else {
					l = cat(l,false,other,false);
				}
				rm.add(other);
				queue.add(l);
				rm(lsTree,other);
				add(lsTree,l);
			} else if ( tmp2.size() == 1 ) {
				//				throw new RuntimeException("hm?? something went wrong?!!");
				LineString other = tmp2.iterator().next();
				if (other.getStartPoint().distance(l.getEndPoint()) < FETCH_RADIUS) {

					l = cat(l,true,other,true);
				} else {
					l = cat(l,true,other,false);
				}
				rm.add(other);
				queue.add(l);
				rm(lsTree,other);
				add(lsTree,l);
			} else {
				ls.add(l);
			}
		}
		System.out.println(ls.size());
		try {
			createFeatures(ls);
		} catch (FactoryRegistryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAttributeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void createFeatures(List<LineString> ls) throws FactoryRegistryException, SchemaException, FactoryException, IllegalAttributeException, IOException {
		FeatureType ft = initFeatureType();
		List<Feature> fts = new ArrayList<Feature>();
		LineString hullLs = null;
		double maxLength = 0;
		for (int i = 0; i < ls.size(); i++) {
			LineString l = ls.get(i);
			if (l.getLength() > maxLength ) {
				maxLength = l.getLength();
				hullLs = l;
			}
		}
		Point start = hullLs.getStartPoint();
		Point end = hullLs.getEndPoint();
		
		Coordinate [] coords = new Coordinate[hullLs.getNumPoints() + 3];
		for (int i = 0; i < hullLs.getNumPoints(); i++) {
			coords[i] = hullLs.getCoordinateN(i);
		}
		
		Coordinate tl = new Coordinate(MIN_X,MAX_Y);
		Coordinate bl = new Coordinate(MIN_X,MIN_Y);
		
		int pos = hullLs.getNumPoints();
		if (start.getY() > end.getY()) {
			coords[pos++] = bl;
			coords[pos++] = tl;
			
		}
		

		
		coords[pos++] = start.getCoordinate();
		LinearRing shell = this.geofac.createLinearRing(coords);
	
		Polygon tmp = this.geofac.createPolygon(shell, null);
		
		int size = 0;
		for (LineString l : ls) {
			if (l == hullLs) {
				continue;
			}
			if (tmp.intersects(l) && l.getNumPoints() > 2) {
				size++;
			}
		}		
		LinearRing [] holes = new LinearRing[size];
		pos = 0;
		for (LineString l : ls) {
			if (l == hullLs) {
				continue;
			}
			if (tmp.intersects(l)&& l.getNumPoints() > 2) {
				Coordinate [] ring = new Coordinate[l.getNumPoints()+1];
				for (int i = 0 ; i < ring.length -1; i++) {
					ring[i] = l.getCoordinateN(i);
				}
				ring[ring.length-1] = l.getCoordinateN(0);
				holes[pos++] = this.geofac.createLinearRing(ring);
			}
		}		
		
		Polygon p = this.geofac.createPolygon(shell, holes);
		fts.add(ft.create(new Object[]{p,0}));
		ShapeFileWriter.writeGeometries(fts, out);
		
	}

	private LineString cat(LineString l1, boolean fwdL1, LineString l2, boolean fwdL2) {
		Coordinate [] coords = new Coordinate[l1.getNumPoints() + l2.getNumPoints() -1];
		int pos = 0;
		Coordinate[] l1Coords = l1.getCoordinates();
		int current = 0, incr = 1, end = l1Coords.length -1;
		if (!fwdL1) {
			incr = -1;
			current = l1Coords.length - 1;
			end = 0;
		}
		while (current != (end+incr) ) {
			coords[pos] = l1Coords[current];
			pos++;
			current += incr;
		}

		Coordinate[] l2Coords = l2.getCoordinates();
		current = 1;
		incr = 1;
		end = l2Coords.length -1;
		if (!fwdL2) {
			current = l2Coords.length -2;
			end = 0;
			incr = -1;
		}
		while (current != (end+incr)) {
			coords[pos] = l2Coords[current];
			pos++;
			current += incr;
		}

		return this.geofac.createLineString(coords);
	}

	private void buildLsTree(QuadTree<LineString> lsTree) throws IOException {
		FeatureSource fs = ShapeFileReader.readDataFile(this.in);
		Iterator it = fs.getFeatures().iterator();
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			if (ft.getDefaultGeometry() instanceof MultiLineString) {
				MultiLineString ms = (MultiLineString) ft.getDefaultGeometry();
				for (int i = 0; i < ms.getNumGeometries(); i++) {

					LineString ls = (LineString) ms.getGeometryN(i);
					Point start = ls.getStartPoint();
					if (start.getX() > MAX_X || start.getX() < MIN_X || start.getY() > MAX_Y || start.getY() < MIN_Y) {
						continue;
					}
					add(lsTree,ls);
				}
			} else {
				throw new RuntimeException("only LineString is supported");
			}
		}

	}
	
	private void add(QuadTree<LineString> lsTree, LineString ls) {
		Point start = ls.getStartPoint();
		Point end = ls.getEndPoint();
		lsTree.put(start.getX(), start.getY(), ls);
		lsTree.put(end.getX(), end.getY(), ls);		
	}
	private void rm(QuadTree<LineString> lsTree, LineString ls) {
		Point start = ls.getStartPoint();
		Point end = ls.getEndPoint();
		lsTree.remove(start.getX(), start.getY(), ls);
		lsTree.remove(end.getX(), end.getY(), ls);		
	}
	
	
	private FeatureType initFeatureType() throws FactoryRegistryException, SchemaException, FactoryException {

		final CoordinateReferenceSystem targetCRS =  MGC.getCRS(TransformationFactory.WGS84_UTM47S);
		AttributeType mp = DefaultAttributeTypeFactory.newAttributeType("MultiPolygon",MultiPolygon.class, true, null, null, targetCRS);
		AttributeType p = DefaultAttributeTypeFactory.newAttributeType("Polygon",Polygon.class, true, null, null, targetCRS);
		AttributeType l = DefaultAttributeTypeFactory.newAttributeType("LineString",LineString.class, true, null, null, targetCRS);
		AttributeType ml = DefaultAttributeTypeFactory.newAttributeType("MultiLineString",MultiLineString.class, true, null, null, targetCRS);
		AttributeType point = DefaultAttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, targetCRS);
		AttributeType mpoint = DefaultAttributeTypeFactory.newAttributeType("MultiPoint",MultiPoint.class, true, null, null, targetCRS);
		AttributeType id = AttributeTypeFactory.newAttributeType("ID", Integer.class);

//		ftMultiPolygon = FeatureTypeFactory.newFeatureType(new AttributeType[] {mp, id}, "geometry");
//		ftPolygon = FeatureTypeFactory.newFeatureType(new AttributeType[] {p, id}, "geometry");
		return FeatureTypeFactory.newFeatureType(new AttributeType[] {p, id}, "geometry");
//		ftMultiLineString = FeatureTypeFactory.newFeatureType(new AttributeType[] {ml, id}, "geometry");
//		ftPoint = FeatureTypeFactory.newFeatureType(new AttributeType[] {point, id}, "geometry");
//		ftMultiPoint = FeatureTypeFactory.newFeatureType(new AttributeType[] {mpoint, id}, "geometry");
	}
	
	public static void main(String [] args) {
		String in = "/home/laemmel/devel/padang_evac/Hazard_Line.shp";
		String out = "/home/laemmel/devel/padang_evac/Evac_Zone.shp";
		new PolygonFromBounderyLine(in,out).run();


	}


}

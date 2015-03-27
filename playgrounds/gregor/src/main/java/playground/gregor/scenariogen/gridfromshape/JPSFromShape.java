/* *********************************************************************** *
 * project: org.matsim.*
 * JPSFromShape.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.gregor.scenariogen.gridfromshape;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.StringUtils;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class JPSFromShape {

	private static double EPSILON = 0.01;
	private final List<List<Coordinate>> obstacles = new ArrayList<>();
	private final List<Coordinate> room = new ArrayList<>();

	private final ShapeFileReader r;
	private final String jpsFile;
	private BufferedWriter br;
	private final String traIn;
	private final String traOut;

	public JPSFromShape(String shapeFile, String jpsFile, String traIn, String traOut) {
		this.r = new ShapeFileReader();
		this.r.readFileAndInitialize(shapeFile);
		this.jpsFile = jpsFile;
		this.traIn = traIn;
		this.traOut = traOut;
	}


	private void run() throws IOException {

		initXML();

		createRoomAndObstacles();

		addSubroom();

		close();
		this.br.close();


		cleanTra();

//		createMATSimSzenario();
	}




//	private void createMATSimSzenario() throws IOException {
//		BufferedReader tBr = new BufferedReader(new FileReader(new File(this.traOut)));
//		String l = tBr.readLine();
//		Set<String> ids = new HashSet<>();
//		List<Coordinate> coords = new ArrayList<Coordinate>();
//		Envelope boundary = null;
//		while (l != null) {
//			String[] expl = StringUtils.explode(l, ' ');
//			if (expl.length == 5) {
//				String id = expl[0];
//				if (!ids.contains(id)){
//					ids.add(id);
//					double x = Double.parseDouble(expl[2]);
//					double y = Double.parseDouble(expl[3]);
//					Coordinate c = new Coordinate(x,y);
//					coords.add(c);
//					if (boundary == null) {
//						boundary = new Envelope(c);
//					} else {
//						boundary.expandToInclude(c);
//					}
//				}
//			}
//			l = tBr.readLine();
//		}
//		tBr.close();
//		QuadTree<Coordinate> qt = new QuadTree<>(boundary.getMinX(), boundary.getMinY(), boundary.getMaxX(), boundary.getMaxY());
//		for (Coordinate c : coords) {
//			qt.put(c.x, c.y, c);
//		}
//		{
//			List<Coordinate> box1 = new ArrayList<>();
//			qt.get(new Rect(-6, 0, -.37, -6.5), box1);
//			//		System.out.println(box1.size());
//			Collections.sort(box1, new  Comparator<Coordinate>() {
//				@Override
//				public int compare(Coordinate o1, Coordinate o2) {
//					return o1.x < o2.x ? -1 : 1;
//				}
//			});
//			Coordinate medX1 = box1.get(box1.size()/2);
//			Collections.sort(box1, new  Comparator<Coordinate>() {
//				@Override
//				public int compare(Coordinate o1, Coordinate o2) {
//					return o1.y < o2.y ? -1 : 1;
//				}
//			});
//			Coordinate medY1 = box1.get(box1.size()/2);
//			System.out.println("med x:" + medX1.x + " med y:" + medY1.y + " min y:" + box1.get(0).y + " max y: " + box1.get(box1.size()-1).y);
//		}
//		{
//			List<Coordinate> box2 = new ArrayList<>();
//			qt.get(new Rect(-1, 5,5.5 , 0), box2);
//			//		System.out.println(box1.size());
//			Collections.sort(box2, new  Comparator<Coordinate>() {
//				@Override
//				public int compare(Coordinate o1, Coordinate o2) {
//					return o1.x < o2.x ? -1 : 1;
//				}
//			});
//			Coordinate medX1 = box2.get(box2.size()/2);
//			Collections.sort(box2, new  Comparator<Coordinate>() {
//				@Override
//				public int compare(Coordinate o1, Coordinate o2) {
//					return o1.y < o2.y ? -1 : 1;
//				}
//			});
//			Coordinate medY1 = box2.get(box2.size()/2);
//			Collections.sort(box2, new  Comparator<Coordinate>() {
//				@Override
//				public int compare(Coordinate o1, Coordinate o2) {
//					return o1.x < o2.x ? -1 : 1;
//				}
//			});
//			System.out.println("med x:" + medX1.x + " med y:" + medY1.y + " min x:" + box2.get(0).x + " max x: " + box2.get(box2.size()-1).x);
//		}
//
//	}


	private void cleanTra() throws IOException {
		Polygon lr = getRing();
		lr = (Polygon) lr.buffer(-0.1);
		BufferedReader tBr = new BufferedReader(new FileReader(new File(this.traIn)));
		BufferedWriter oW = new BufferedWriter(new FileWriter(new File(this.traOut)));
		String l = tBr.readLine();
		while (l != null) {
			String[] expl = StringUtils.explode(l, '\t');
			if (expl.length == 5 && !l.startsWith("#")) {
				double x = Double.parseDouble(expl[2]);
				double y = Double.parseDouble(expl[3]);

				Point p = MGC.xy2Point(x, y);
				if (lr.contains(p)) {
					oW.append(l);
				} else {
					l = tBr.readLine();
					continue;
				}
			} else {
				oW.append(l);
			}
			oW.append('\n');
			l = tBr.readLine();
		}
		tBr.close();
		oW.close();
	}

	private Polygon getRing() {
		GeometryFactory geoFac = new GeometryFactory();
		Coordinate [] coords = new Coordinate[this.room.size()];
		int idx = 0;
		for (Coordinate c : this.room) {
			coords[idx] = c;
			idx++;
		}

		LinearRing lr = geoFac.createLinearRing(coords);
		Polygon p = geoFac.createPolygon(lr, null);
		return p;
	}


	private void createRoomAndObstacles() {
		//1. obstacles
		Iterator<SimpleFeature> it = this.r.getFeatureSet().iterator();
		while (it.hasNext()){
			SimpleFeature ft = it.next();
			Geometry geo = (Geometry) ft.getDefaultGeometry();
			Coordinate[] cs = geo.getCoordinates();
			if (dist(cs[0],cs[cs.length-1]) < EPSILON) {
				List<Coordinate> obs = new ArrayList<>();
				this.obstacles.add(obs);
				for (Coordinate c: cs) {
					obs.add(c);
				}
				it.remove();
			} 
		}
		//2. boundary
		LinkedList<Coordinate> ring = new LinkedList<>();
		Iterator<SimpleFeature> it2 = this.r.getFeatureSet().iterator();
		SimpleFeature first = it2.next();
		it2.remove();
		Geometry firstGeo = (Geometry) first.getDefaultGeometry();
		for (Coordinate c : firstGeo.getCoordinates()) {
			ring.add(c);
		}

		while (dist(ring.getFirst(),ring.getLast()) > EPSILON) {
			while (it2.hasNext()) {
				SimpleFeature next = it2.next();
				Geometry nextGeo = (Geometry) next.getDefaultGeometry();
				if (dist(ring.getLast(),nextGeo.getCoordinates()[0]) < EPSILON) {
					addStraightAway(ring,nextGeo);
					it2.remove();
					break;
				} else if (dist(ring.getLast(),nextGeo.getCoordinates()[nextGeo.getCoordinates().length-1]) < EPSILON) {
					addReverse(ring,nextGeo);
					it2.remove();
					break;
				}

			}
			System.out.println(this.r.getFeatureSet().size());
			it2 = this.r.getFeatureSet().iterator();
		}
		ring.getLast().setCoordinate(ring.getFirst());
		this.room.addAll(ring);

	}


	private void addReverse(LinkedList<Coordinate> ring, Geometry nextGeo) {
		for (int i = nextGeo.getCoordinates().length-2; i >= 0; i--) {
			ring.add(nextGeo.getCoordinates()[i]);
		}

	}


	private void addStraightAway(LinkedList<Coordinate> ring, Geometry nextGeo) {
		for (int i = 1; i < nextGeo.getCoordinates().length; i++) {
			ring.add(nextGeo.getCoordinates()[i]);
		}

	}


	private double dist(Coordinate c1, Coordinate c2) {
		double dx = c2.x-c1.x;
		double dy = c2.y-c1.y;
		return Math.sqrt(dx*dx+dy*dy);
	}


	private void addSubroom() throws IOException {
		this.br.append("\t\t\t<subroom id=\"0\" closed=\"0\" class=\"subroom\">\n");
		this.br.append("\t\t\t\t<polygon caption=\"wall\">\n");
		for (Coordinate c : this.room) {
			this.br.append("\t\t\t\t\t<vertex px=\"" + c.x + "\" py=\"" + c.y +"\" />\n");	
		}
		this.br.append("\t\t\t\t</polygon>\n");
		for (List<Coordinate> obs : this.obstacles) {
			this.br.append("\t\t\t\t<obstacle>\n");
			this.br.append("\t\t\t\t\t<polygon caption=\"pillar\">\n");
			for (Coordinate c : obs) {
				this.br.append("\t\t\t\t\t\t<vertex px=\"" + c.x + "\" py=\"" + c.y +"\" />\n");
			}
			this.br.append("\t\t\t\t\t</polygon>\n");
			this.br.append("\t\t\t\t</obstacle>\n");
		}
		this.br.append("\t\t\t</subroom>\n");
	}
	private void close() throws IOException {
		this.br.append("\t\t</room>\n");
		this.br.append("\t</rooms>\n");
		this.br.append("</geometry>\n");
	}


	private void initXML() throws IOException {
		this.br = new BufferedWriter(new FileWriter(new File(this.jpsFile)));
		this.br.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
				+ "<geometry version =\"0.5\" caption=\"second life\" gridSizeX=\"20\" "
				+ "gridSizeY=\"20\" unit=\"m\" xmlns:xsi=\"http://www.w3.org/2001/XMLSche"
				+ "ma-instance\"xsi:noNamespaceSchemaLocation=\"http://xsd.jupedsim.org"
				+ "/0.6/jps_geoemtry.xsd\">\n");
		this.br.append("\t<rooms>\n");
		this.br.append("\t\t<room id=\"0\" caption=\"hall\">\n");

	}

	public static void main(String [] args) throws IOException {
		//		String shapeFile = "/Users/laemmel/svn/shared-svn/projects/120multiDestPeds/floor_plan/boundaries_closed_transformed.shp";
		String shapeFile = "/Users/laemmel/devel/plaueexp/simpleGeo.shp";
		String jpsFile ="/Users/laemmel/devel/plaueexp/jps.xml";
		String traIn = "/Users/laemmel/devel/plaueexp/dec2010_trajectories/trajectories/agentTrajectoriesFlippedTranslated.txt";
		String traOut = "/Users/laemmel/devel/plaueexp/dec2010_trajectories/trajectories/agentTrajectoriesCleaned.txt";
		new JPSFromShape(shapeFile,jpsFile,traIn,traOut).run();
	}

}

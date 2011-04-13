package playground.gregor.gis.flsimple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.geotools.feature.Feature;
import org.geotools.feature.IllegalAttributeException;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

public class Simplifier {
	
	private String in;
	private String out;
	private Map<Integer, ArrayList<Feature>> map;
	
	private List<Feature> features = new ArrayList<Feature>();
	private Envelope envelope;
	public Simplifier(String input, String output) {
		this.in = input;
		this.out = output;
	}

	
	private void run() throws IOException {
		
		this.map = new HashMap<Integer,ArrayList<Feature>>();
		Set<Feature> fs = new ShapeFileReader().readFileAndInitialize(in);
		this.envelope = new Envelope(fs.iterator().next().getDefaultGeometry().getCoordinate());
		
		for (Feature f : fs) {
			this.envelope.expandToInclude(f.getDefaultGeometry().getCoordinate());
//			Double time = (Double) f.getAttribute("floodingTi");
//			if (time >= 1 && time <= 45) {
//				addToMap((int)(time+0.5),f);
//			}
			addToMap(0,f);
//			if (time <= 30) {
//				addToMap(30,f);
//			} else if (time <= 35) {
//				addToMap(35,f);
//			} else if (time <= 40) {
//				addToMap(40,f);
//			} else if (time <= 45) {
//				addToMap(45,f);
//			} 
//			else {
//				addToMap(46,f);
//			}
		}
		
		for (Entry<Integer, ArrayList<Feature>> e : this.map.entrySet()) {
			createFeature(e);
		}
		ShapeFileWriter.writeGeometries(features, this.out);
	}
	private void createFeature(Entry<Integer, ArrayList<Feature>> e) {
		Feature ft = e.getValue().get(0);
		Geometry geo = e.getValue().get(0).getDefaultGeometry().buffer(0.75);
		QuadTree<Geometry> geos = new QuadTree<Geometry>(this.envelope.getMinX(),this.envelope.getMinY(),this.envelope.getMaxX(),this.envelope.getMaxY());
		Geometry curr = geo;
		for (int i = 1; i < e.getValue().size(); i ++) {
			Feature ff = e.getValue().get(i);
			Geometry tmp = ff.getDefaultGeometry();
			geos.put(tmp.getCentroid().getX(), tmp.getCentroid().getY(), tmp);
		}
		System.out.println("=============================");
		System.out.println("-----" + e.getKey() + "-----");
//		int ii = 0;
		while (geos.size() > 0) {
//			if (ii++ >= 1000) {
//				break;
//			}
			
			curr = geos.get(curr.getCentroid().getX(), curr.getCentroid().getY());
			try {
				geo = geo.union(curr);
			} catch (Exception e1) {
//				e1.printStackTrace();
				try {
					geo = geo.union(curr.buffer(0.1));
				} catch (Exception e2) {
//					e2.printStackTrace();
					try {
						geo = geo.union(curr.buffer(4));
					} catch (Exception e3) {
						// TODO Auto-generated catch block
						e3.printStackTrace();
					}
				}
			}
			
			
			geos.remove(curr.getCentroid().getX(), curr.getCentroid().getY(), curr);
			if (geos.size() % 100 == 0) {
				System.out.println("to go:" + geos.size());
				geo.buffer(0);
			}
		}
		try {
			ft.setDefaultGeometry(geo);
//			ft.setAttribute("floodingTi", e.getKey());
		} catch (IllegalAttributeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		this.features.add(ft);
		
	}
	

	private void addToMap(int i, Feature f) {
		ArrayList<Feature> l = this.map.get(i);
		if (l == null) {
			l = new ArrayList<Feature>();
			this.map.put(i,l);
		}
		l.add(f);
	}


	public static void main(String [] args) {
		String input = "/Users/laemmel/svn/shared-svn/studies/countries/id/padang/network/evac_zone_buildings_v20100517.shp";
		String output = "/Users/laemmel/tmp/buildings.shp";
		try {
			new Simplifier(input,output).run();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

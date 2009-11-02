package playground.gregor.otf.readerwriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
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
import org.matsim.evacuation.flooding.FloodingInfo;
import org.matsim.evacuation.flooding.FloodingReader;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.gregor.MY_STATIC_STUFF;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class BasicInundationGeometryLoader {

	private static final Logger log = Logger.getLogger(BasicInundationGeometryLoader.class);

	@Deprecated
	private final boolean DEBUG;

	private final String netcdf;
	private String outShape;
	private FeatureType ft;

	private final Map<Integer,GeoColl> geoColls = new HashMap<Integer, GeoColl>();
	private Map<Integer, Integer> mapping;
	private List<FloodingInfo> fis;
	private ArrayList<Feature> fts;
	private final ArrayList<List<FloodingInfo>> fgis = new ArrayList<List<FloodingInfo>>(); 
	private GeometryFactory geofac;

	private boolean initialized = false;

	@Deprecated
	public BasicInundationGeometryLoader(String netcdf, String outShape) {
		this.DEBUG = true;
		this.netcdf = netcdf;
		this.outShape = outShape;
	}

	public BasicInundationGeometryLoader(String netcdf) {
		this.DEBUG = false;
		this.netcdf = netcdf;
	}
	
	public ArrayList<List<FloodingInfo>> getInundationGeometries(){
		if (!this.initialized ) {
			run();
		}
		
		return this.fgis;
	}
	
	private void run(){
		initFeatureType();
		this.geofac = new GeometryFactory();
		this.fts = new ArrayList<Feature>();
		FloodingReader fl = new FloodingReader(this.netcdf);
		fl.setReadTriangles(true);
		fl.setReadFloodingSeries(true);
		double offsetEast = 632968.461027224;
		double offsetNorth = 9880201.726;
		fl.setOffset(offsetEast, offsetNorth);
		
		
		List<int[]> tris = fl.getTriangles();
		this.fis = fl.getFloodingInfos();
		this.mapping = fl.getIdxMapping();

		log.info("trying to compose convex geometries");
		tris = findConvexGeometries(tris);
		log.info("done." + this.fgis.size() + " geometries so far.");

		log.info("geometries remaining: " + tris.size());
		log.info("trying to compose convex geometries");
		handleRemaining(tris);
		log.info("done. " + this.fgis.size() + " geometries.");

		if (this.DEBUG) {
			try {
				ShapeFileWriter.writeGeometries(this.fts, this.outShape);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		this.initialized = true;


	}

	private void handleRemaining(List<int[]> tris) {

		allocateGeometries(tris);
		Comparator<GeoColl> c = new GeoCollComparator();
		Queue<GeoColl> geoColls = new PriorityQueue<GeoColl>(tris.size(),c);
		geoColls.addAll(this.geoColls.values());

		Set<int[]> removed = new HashSet<int[]>();

		while (geoColls.size() > 0) {
			GeoColl geoColl = geoColls.poll();
			for (int[]geo : geoColl.geos) {
				if (removed.contains(geo)) {
					continue;
				}
				removed.add(geo);
				if (!fuseGeo(geo,removed)) {
					List<FloodingInfo> infos = new ArrayList<FloodingInfo>();
					Polygon p = getPolygon(infos,geo);
					createInundationGeometry(infos,p);
				}


			}
		}

	}

	private boolean fuseGeo(int [] geo, Set<int[]> removed) {
		for (int i = 0; i < geo.length; i++) {
			int key = geo[i];
			GeoColl tmpColl = this.geoColls.get(key);
			for (int[] tmpGeo : tmpColl.geos) {
				if (tmpGeo == geo || removed.contains(tmpGeo)) {
					continue;
				}
				int [] newGeo = null;
				//				boolean match = false;
				for (int j = 0; j < tmpGeo.length; j++) {
					if (tmpGeo[j] == key) {
						//						match = true;
						continue;
					}
					for (int k = 0; k < geo.length; k++) {
						if (k == i) {
							continue;
						}
						if (tmpGeo[j] == geo[k]) {
							newGeo = new int[geo.length + tmpGeo.length - 2];

							int idx = 0;
							int pred = j - 1 < 0 ? tmpGeo.length - 1 : j - 1;
							//							int succ = j + 1 == tmpGeo.length ? 0 : j + 1;

							if (tmpGeo[pred] == key) {
								while (idx < tmpGeo.length-1) {
									newGeo[idx++] = tmpGeo[j++];
									if (j == tmpGeo.length) {
										j = 0;
									}
								}

							} else {
								while (idx < tmpGeo.length-1) {
									newGeo[idx++] = tmpGeo[j--];
									if (j == -1) {
										j = tmpGeo.length - 1;
									}
								}	

							}
							pred = i - 1 < 0 ? geo.length - 1 : i - 1;
							if (pred == k) {
								while (idx < newGeo.length) {
									newGeo[idx++] = geo[i++];
									if ( i == geo.length) {
										i = 0;
									}
								}
							} else {
								while (idx < newGeo.length) {
									newGeo[idx++] = geo[i--];
									if ( i == -1) {
										i = geo.length - 1;
									}
								}
							}
							List<FloodingInfo> infos = new ArrayList<FloodingInfo>();
							Polygon p = getPolygon(infos,newGeo);
							if (createInundationGeometry(infos,p)) {
								removed.add(tmpGeo);
								return true;
							}
						}
					}

				}
			}
		}

		return false;

	}

	private void createFeature(Geometry g, double z, double tm) {
		try {
			this.fts.add(this.ft.create(new Object []{g,z,tm}));
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		}

	}

	private boolean createInundationGeometry(List<FloodingInfo> infos, Polygon p) {


		if (p != null && isConvex(p)) {
			if(this.DEBUG) {
				double z = 0, tm = 0;
				for (FloodingInfo fi : infos) {
					z += fi.getCoordinate().z;
					tm += fi.getFloodingTime();
				}
				createFeature(p, z/infos.size(), tm/infos.size());
			}
			this.fgis.add(infos);
			return true;
		}

		return false;
	}

	private Polygon getPolygon(List<FloodingInfo> infos, int[] newGeo) {
		Coordinate [] coords = new Coordinate[newGeo.length+1];
		boolean cancel = false;

		for (int i = 0; i < newGeo.length; i++) {

			Integer idx  = this.mapping.get(newGeo[i]);
			if (idx == null) {
				cancel = true;
				break;
			}
			FloodingInfo fi = this.fis.get(idx);
			infos.add(fi);
			coords[i] = fi.getCoordinate();
		}
		if (cancel) {
			return null;
		}
		coords[newGeo.length] = coords[0];
		LinearRing lr = this.geofac.createLinearRing(coords);
		return this.geofac.createPolygon(lr, null);
	}

	private List<int[]> findConvexGeometries(List<int[]> tris) {
		allocateGeometries(tris);
		GeoColl triColl = getInitialTriColl();
		Queue<GeoColl> triColls = new ConcurrentLinkedQueue<GeoColl>();
		triColls.add(triColl);
		this.geoColls.remove(triColl.key);

		Set<int[]> removedTriangles = new HashSet<int[]>();
		Set<int[]> unhandledTriangles = new HashSet<int[]>();

		int next = 1;

		while (triColls.size() > 0) {
			if (this.fgis.size() >= next) {
				log.info("geometries so far: " + this.fgis.size());
				next *= 2;
			}
			triColl = triColls.poll();
			this.geoColls.remove(triColl.key);

			List<Geometry> polys = new ArrayList<Geometry>();

			Map<Coordinate,FloodingInfo> fiMapping = new HashMap<Coordinate, FloodingInfo>();

			for (int[] tri : triColl.geos) {
				if (removedTriangles.contains(tri)) {
					continue;
				}

				List<FloodingInfo> infos = new ArrayList<FloodingInfo>();				
				int j = 0;
				int [] tmpKeys = {-1,-1};
				for (int i = 0; i < 3; i++) {
					if (tri[i] != triColl.key ) {
						tmpKeys[j++] = tri[i];
					}
					
					Integer idx  = this.mapping.get(tri[i]);
					if (idx == null) {
						infos.clear();
						break;
					}
					FloodingInfo info = this.fis.get(idx);
					infos.add(info);
					fiMapping.put(info.getCoordinate(), info);
				}

				if (infos.size() == 0) {
					removedTriangles.add(tri);
					continue;
				}
				Polygon p = getPolygon(infos, tri);
				polys.add(p);
				GeoColl t = getNeighbor(tmpKeys,triColl.key);
				if (t != null) {
					triColls.add(t);
					this.geoColls.remove(t.key);
				}

			}

			Geometry geo = getUnion(polys);
			List<FloodingInfo> infos = new ArrayList<FloodingInfo>();
			boolean fail = true;
			Polygon p = null;
			if (geo instanceof Polygon) {
				p = (Polygon)geo;
			} else if (geo instanceof MultiPolygon) {
				if (geo.getNumGeometries() == 1) {
					p = (Polygon) ((MultiPolygon)geo).getGeometryN(0);
				}
			}
			
			if (p != null) {
				Coordinate[] coords = p.getExteriorRing().getCoordinates();
				for (int i = 0; i < coords.length-1; i++) {
					FloodingInfo fi = fiMapping.get(coords[i]);
					infos.add(fi);
				}
				if (createInundationGeometry(infos, (Polygon)geo)) { 
					removedTriangles.addAll(triColl.geos);
					fail = false;
				}

			}


			if (fail) {
				for (int[] tri : triColl.geos) {
					unhandledTriangles.add(tri);
				}				
			}

			if (triColls.size() == 0) {
				triColls.addAll(this.geoColls.values());
			}
		}
		unhandledTriangles.removeAll(removedTriangles);
		List<int []> ret = new ArrayList<int[]>(unhandledTriangles);
		return ret;
	}

	private Geometry getUnion(List<Geometry> polys) {
		Geometry p = null;
		if (polys.size() > 1) {
			for (Geometry tmp : polys) {
				if (p == null) {
					p = tmp;
				} else {
					p = p.union(tmp);
				}

			}
		}
		return p;
	}

	private boolean isConvex(Geometry p) {
		return Math.abs(p.convexHull().getArea() - p.getArea()) <= 0.01; 
	}

	private GeoColl getNeighbor(int[] tmpKeys, int center) {
		GeoColl t = this.geoColls.get(tmpKeys[0]);
		if (t == null) {
			return null;
		}
		for (int [] tri : t.geos) {
			int newKey = -1;
			boolean found = false;
			for (int i = 0; i < 3; i++) {
				if (tri[i] == center) {
					found = false;
					break;
				} else if (tri[i] == tmpKeys[1]) {
					found = true;
				} else if (tri[i] != tmpKeys[0]) {
					newKey = tri[i];
				}
			}
			if (found) {
				return this.geoColls.get(newKey);
			}

		}


		return null;
	}

	private GeoColl getInitialTriColl() {
		for (GeoColl geoColl : this.geoColls.values()) {
			if (geoColl.geos.size() == 5) {
				return geoColl;
			}
		}
		return null;
	}

	private void allocateGeometries(List<int[]> tris){
		this.geoColls.clear();
		for (int [] tri : tris) {
			for (int i = 0; i < tri.length; i++) {
				addGeometry(tri[i],tri);
			}

		}		
	}

	private void addGeometry(int i, int[] tri) {
		GeoColl tris = this.geoColls.get(i);
		if (tris == null) {
			tris = new  GeoColl();
			tris.key = i;
			this.geoColls.put(i, tris);
		}
		tris.geos.add(tri);
	}

	private static class GeoCollComparator implements Comparator<GeoColl> {

		public int compare(GeoColl o1, GeoColl o2) {
			if (o1.geos.size() > o2.geos.size()) {
				return 1;
			} else if (o1.geos.size() < o2.geos.size()) {
				return 1;
			}  
			return 0;
		}



	}




	private void initFeatureType() {
		CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.WGS84_UTM47S);
		AttributeType p = DefaultAttributeTypeFactory.newAttributeType(
				"Polygon", Polygon.class, true, null, null, targetCRS);
		AttributeType z = AttributeTypeFactory.newAttributeType(
				"dblAvgZ", Double.class);
		AttributeType t = AttributeTypeFactory.newAttributeType(
				"dblAvgT", Double.class);

		Exception ex;
		try {
			this.ft = FeatureTypeFactory.newFeatureType(new AttributeType[] { p, z, t }, "Polygon");
			return;
		} catch (FactoryRegistryException e) {
			ex = e;
		} catch (SchemaException e) {
			ex = e;
		}
		throw new RuntimeException(ex);
	}

	private static class GeoColl{
		int key;
		List<int []> geos = new ArrayList<int[]>();
	}
	@Deprecated
	public static void main (String [] args) {
		String netcdf = MY_STATIC_STUFF.SWW_ROOT + "/" + MY_STATIC_STUFF.SWW_PREFIX + "2" + MY_STATIC_STUFF.SWW_SUFFIX; 

		//"./test/input/org/matsim/evacuation/data/flooding.sww";
		String outShape = "../../analysis/mesh/meshIII.shp";
		new BasicInundationGeometryLoader(netcdf,outShape).run();
	}


}

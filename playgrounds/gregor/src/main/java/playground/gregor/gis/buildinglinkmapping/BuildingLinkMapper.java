package playground.gregor.gis.buildinglinkmapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.evacuation.base.Building;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

public class BuildingLinkMapper {

	
	private static FeatureType ft;

	public static void main(String [] args) {
		String conf = "/Users/laemmel/arbeit/papers/2012/lastMile/matsim/config.xml";
		String runBase = "/Users/laemmel/svn/runs-svn/run1390/";
		Config c = ConfigUtils.loadConfig(conf);
		Scenario sc = ScenarioUtils.loadScenario(c);
		Loader loader = new Loader(sc);
		loader.loadData();
		List<Building> buildings = loader.getBuildings();
		System.out.println(buildings.size());
		ShapeFileReader reader = new ShapeFileReader();
		reader.readFileAndInitialize("/Users/laemmel/svn/shared-svn/studies/countries/id/padang/gis/network_v20080618/links_v20090728.shp");
		QuadTree<Feature> quad = buildQuadTree(reader);
		
		Collection<Feature> fts = new ArrayList<Feature>();
		
		initFeatures();
		for (Building b : buildings) {
			Collection<Feature> coll = quad.get(b.getGeo().getCentroid().getX(), b.getGeo().getCentroid().getY(), 100);
			if (coll.size() == 0) {
				coll = quad.get(b.getGeo().getCentroid().getX(), b.getGeo().getCentroid().getY(), 500);
			} 
			
			Set<Feature> killed = new HashSet<Feature>();
			double minDist = Double.POSITIVE_INFINITY;
			Feature nearest = null;
			for (Feature ft : coll) {
				
				
				if (killed.contains(ft)) {
					continue;
				}
				
				killed.add(ft);
				
				long intId = (Long)ft.getAttribute("ID");
				Id lId = new IdImpl(intId);
				if (sc.getNetwork().getLinks().get(lId) == null) {
					continue;
				}
				
				double dist = b.getGeo().distance(ft.getDefaultGeometry());
				if (dist < minDist) {
					minDist = dist;
					nearest = ft;
				}
			}
			if (nearest == null) {
				System.out.println("skiped: " + b.getPopDay());
				continue;
			}
			int intId = Integer.parseInt(b.getId().toString());
			int qp = b.isQuakeProof() ? 1 : 0;
			Feature bft = null;
			try {
				bft = ft.create(new Object[]{b.getGeo(),intId,b.getPopNight(),b.getPopDay(),b.getPopAf(),b.getFloor(),b.getShelterSpace(),qp,b.getMinWidth(),nearest.getAttribute("ID")});
			} catch (IllegalAttributeException e) {
				e.printStackTrace();
			}
			fts.add(bft);
			if (fts.size() % 100 == 0) {
				System.out.println(fts.size());
			}
		}
		
//		ShapeFileWriter.writeGeometries(fts, "/Users/laemmel/svn/shared-svn/studies/countries/id/padang/network/evac_zone_buildings_v20120206.shp");
		ShapeFileWriter.writeGeometries(fts, "/Users/laemmel/tmp/aaaa.shp");
		
	}

	
	private static void initFeatures() {
		CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG: 32747");
		AttributeType p = DefaultAttributeTypeFactory.newAttributeType(
				"MultiPolygon", MultiPolygon.class, true, null, null, targetCRS);
		AttributeType id = AttributeTypeFactory.newAttributeType(
				"ID", Integer.class);
		AttributeType popNight = AttributeTypeFactory.newAttributeType(
				"popNight", Integer.class);
		AttributeType popDay = AttributeTypeFactory.newAttributeType(
				"popDay", Integer.class);
		AttributeType popAf = AttributeTypeFactory.newAttributeType(
				"popAf", Integer.class);
		AttributeType floor = AttributeTypeFactory.newAttributeType(
				"floor", Integer.class);
		AttributeType cap = AttributeTypeFactory.newAttributeType(
				"capacity", Integer.class);
		AttributeType quakeProof = AttributeTypeFactory.newAttributeType(
				"quakeProof", Integer.class);
		AttributeType minWidth = AttributeTypeFactory.newAttributeType(
				"minWidth", Double.class);
		AttributeType linkId = AttributeTypeFactory.newAttributeType(
				"linkId", Integer.class);

		Exception ex;
		try {
			ft = FeatureTypeFactory.newFeatureType(new AttributeType[] { p, id,popNight, popDay, popAf,
					floor, cap, quakeProof, minWidth, linkId}, "MultiPolygon");
			return;
		} catch (FactoryRegistryException e) {
			ex = e;
		} catch (SchemaException e) {
			ex = e;
		}
		throw new RuntimeException(ex);

	}
	
	private static QuadTree<Feature> buildQuadTree(ShapeFileReader reader) {
		Envelope e = reader.getBounds();
		QuadTree<Feature> quad = new QuadTree<Feature>(e.getMinX(),e.getMinY(),e.getMaxX(),e.getMaxY());
		for (Feature ft : reader.getFeatureSet()) {
			Geometry geo = ft.getDefaultGeometry();
			for (int i = 0; i < geo.getCoordinates().length; i++) {
				Coordinate c = geo.getCoordinates()[i];
				quad.put(c.x, c.y, ft);
			}
			
		}
		return quad;
	}
}

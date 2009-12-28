package playground.gregor.gis.sheltershape;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
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
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

public class ShelterShapeGenerator {

	private final static Logger log = Logger.getLogger(ShelterShapeGenerator.class);

	private final String popBase;
	private final String quakeProof;
	private final String livingSpace;
	private final String out;

	private QuadTree<Feature> quadTree;

	private CoordinateReferenceSystem crs;

	private FeatureType featureType;

	public ShelterShapeGenerator(String popBase, String quakeProof,
			String livingSpace, String out) {
		this.popBase = popBase;
		this.quakeProof = quakeProof;
		this.livingSpace = livingSpace;
		this.out = out;
	}


	public void run() {
		log.info("reading base file and build up QuadTree");
		try {
			readBase();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (FactoryRegistryException e) {
			throw new RuntimeException(e);
		} catch (IllegalAttributeException e) {
			throw new RuntimeException(e);
		} catch (SchemaException e) {
			throw new RuntimeException(e);
		}
		log.info("done.");

		log.info("reading quake proof file");
		try {
			readQuakeProof();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (IllegalAttributeException e) {
			throw new RuntimeException(e);
		}
		log.info("done.");
		
		log.info("reading space file");
		try {
			readSpace();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (IllegalAttributeException e) {
			throw new RuntimeException(e);
		}		
		log.info("done.");
		
		log.info("writing features");
		try {
			ShapeFileWriter.writeGeometries(this.quadTree.values(), this.out);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		log.info("done.");


	}

	private void initFeatures() throws FactoryRegistryException, SchemaException {
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("MultiPolygon",MultiPolygon.class, true, null, null, this.crs);
		AttributeType id = AttributeTypeFactory.newAttributeType("ID", Integer.class);
		AttributeType night = AttributeTypeFactory.newAttributeType("popNight", Integer.class);
		AttributeType day = AttributeTypeFactory.newAttributeType("popDay", Integer.class);
		AttributeType floor = AttributeTypeFactory.newAttributeType("floor", Integer.class);
		AttributeType space = AttributeTypeFactory.newAttributeType("space", Double.class);
		AttributeType quakeProof = AttributeTypeFactory.newAttributeType("quakeProof", Integer.class);
		this.featureType = FeatureTypeFactory.newFeatureType(new AttributeType[] { geom, id, night, day, floor, space, quakeProof}, "building");
	}


	private void readQuakeProof() throws IOException, IllegalAttributeException {
		FeatureSource fts = ShapeFileReader.readDataFile(this.quakeProof);
		Iterator it = fts.getFeatures().iterator();
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			Geometry geo = ft.getDefaultGeometry();
			Collection<Feature> tmp = this.quadTree.get(geo.getCentroid().getX(), geo.getCentroid().getY(), 0.5);
			if (tmp.size() == 0) {
				log.error("No corresponding feature found");
			} else if (tmp.size() > 1) {
				log.error("Found "+ tmp.size() + " corresponding features");
			} else {
				for (Feature f : tmp) {
					f.setAttribute("quakeProof", 1);
				}
			}
		}

	}

	private void readSpace() throws IOException, IllegalAttributeException {
		FeatureSource fts = ShapeFileReader.readDataFile(this.livingSpace);
		Iterator it = fts.getFeatures().iterator();
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			Geometry geo = ft.getDefaultGeometry();
			double area = geo.getArea();
			Collection<Feature> tmp = this.quadTree.get(geo.getCentroid().getX(), geo.getCentroid().getY(), 0.5);
			if (tmp.size() == 0) {
				log.error("No corresponding feature found");
			} else if (tmp.size() > 1) {
				log.error("Found "+ tmp.size() + " corresponding features");
			} else {
				for (Feature f : tmp) {
					f.setAttribute("space", ft.getAttribute("livingSpac"));
					double space = (Double) ft.getAttribute("livingSpac");
					int floor = (int) Math.round(space/area);
					
					f.setAttribute("floor", floor);
				}
			}
		}

	}
	
	private void readBase() throws IOException, IllegalAttributeException, FactoryRegistryException, SchemaException {
		FeatureSource fts = ShapeFileReader.readDataFile(this.popBase);
		this.crs = fts.getSchema().getDefaultGeometry().getCoordinateSystem();
		Envelope en = fts.getBounds();

		log.info("init features");
		initFeatures();
		log.info("done.");


		this.quadTree  = new QuadTree<Feature>(en.getMinX(),en.getMinY(),en.getMaxX(),en.getMaxY());

		Iterator it = fts.getFeatures().iterator();


		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			MultiPolygon geo = (MultiPolygon) ft.getDefaultGeometry();
			Integer id = (Integer) ft.getAttribute("OBJECTID");
			Integer night = (Integer) ft.getAttribute("popBdNt");
			Integer day = (Integer) ft.getAttribute("popBd_day");
			Integer floor = 0;
			Double space = 0.;
			Integer quakeProof = 0;
			Feature f = this.featureType.create(new Object [] {geo,id,night,day,floor,space,quakeProof});
			this.quadTree.put(geo.getCentroid().getX(), geo.getCentroid().getY(), f);
		}
	}


	public static void main(String [] args) {
		String root = "../../inputs/gis/";
		String popBase = root + "buildings/buildingmask_population.shp";
		String quakeProof = root + "buildings/Stable_Structures_after_earthquake.shp";
		String livingSpace = root + "buildings/SemanticClassification_1.shp";
		String out = "./tmp/buildings_v20090403.shp";

		new ShelterShapeGenerator(popBase,quakeProof,livingSpace,out).run();
	}

}

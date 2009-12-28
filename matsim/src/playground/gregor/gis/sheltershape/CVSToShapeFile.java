package playground.gregor.gis.sheltershape;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.StringUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.MultiPolygon;

import playground.gregor.MY_STATIC_STUFF;

public class CVSToShapeFile {

	
	
	private final String output;
	private final String shapeFile;
	private final String cvs;
	private FeatureType featureType;
	private QuadTree<Feature> quad;
	private HashMap<Integer, Feature> map;
	public CVSToShapeFile(String cvs, String shapeFile, String output) {
		this.cvs = cvs;
		this.shapeFile = shapeFile;
		this.output = output;
	}

	private void run() {
		try {
			initFeatures();
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
		try {
			readFeatures();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		}
		
		try {
			parseCVS();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		}

		try {
			ShapeFileWriter.writeGeometries(this.map.values(), this.output);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	private void parseCVS() throws IOException, IllegalAttributeException {
		BufferedReader in = new BufferedReader(new FileReader(new File(this.cvs)));
		
		//header
		String header = in.readLine();
		
		String line = in.readLine();
		
		CoordinateTransformation tr = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_UTM47S");
		int allCap = 0;
		double coef = 0.25;
		while (line != null) {
			String [] expl = StringUtils.explode(line, ',');
			Integer id1 = Integer.parseInt(expl[0]);
			Double doorWidth = Double.parseDouble(expl[2]);
			Double cap = Double.parseDouble(expl[4]);
			Double lat = Double.parseDouble(expl[5]);
			Double lon = Double.parseDouble(expl[6]);
			
			Feature ft = null;
			if (id1 != -1) {
				ft = this.map.get(id1);
				if (ft == null) {
					line = in.readLine();
					continue;
				}
				this.quad.remove(ft.getDefaultGeometry().getCentroid().getX(), ft.getDefaultGeometry().getCentroid().getY(), ft);
				int i = (Integer) ft.getAttribute("quakeProof");
				if (i != 0) {
					System.err.println("alr");
				}
			} else {
				Coord tmp = new CoordImpl(lat,lon);
				Coord c = tr.transform(tmp);
				ft = this.quad.get(c.getX(),c.getY());
				this.quad.remove(ft.getDefaultGeometry().getCentroid().getX(), ft.getDefaultGeometry().getCentroid().getY(), ft);
				int i = (Integer) ft.getAttribute("quakeProof");
				if (i != 0) {
					System.err.println("alr");
				}
			}
			if (cap == -1) {
				double floor = (Integer) ft.getAttribute("floor");
				double area = ft.getDefaultGeometry().getArea();
				cap = (floor -1) * area * coef;
			}
			ft.setAttribute("capacity", cap);
			allCap += cap;
			ft.setAttribute("quakeProof", 1);
			ft.setAttribute("minWidth", doorWidth);
			line = in.readLine();
		}
		
		System.out.println(allCap);
	}

	private void readFeatures() throws IOException, IllegalAttributeException {
		FeatureSource fs = ShapeFileReader.readDataFile(this.shapeFile);
		Envelope e = fs.getBounds();
		this.quad = new QuadTree<Feature>(e.getMinX(),e.getMinY(),e.getMaxX(),e.getMaxY());
		this.map = new HashMap<Integer,Feature>();
		Iterator it = fs.getFeatures().iterator();
		while(it.hasNext()) {
			Feature tmp = (Feature) it.next();
			Feature ft = this.featureType.create(new Object []{tmp.getDefaultGeometry(),tmp.getAttribute("ID"),tmp.getAttribute("popNight"),tmp.getAttribute("popDay"),tmp.getAttribute("floor"),0,0,0.});
			this.quad.put(ft.getDefaultGeometry().getCentroid().getX(), ft.getDefaultGeometry().getCentroid().getY(), ft);
			this.map.put((Integer) ft.getAttribute("ID"),ft);
		}
		
		
	}

	private void initFeatures() throws FactoryRegistryException, SchemaException {
		CoordinateReferenceSystem crs = MGC.getCRS("WGS84_UTM47S");
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("MultiPolygon",MultiPolygon.class, true, null, null, crs);
		AttributeType id = AttributeTypeFactory.newAttributeType("ID", Integer.class);
		AttributeType night = AttributeTypeFactory.newAttributeType("popNight", Integer.class);
		AttributeType day = AttributeTypeFactory.newAttributeType("popDay", Integer.class);
		AttributeType floor = AttributeTypeFactory.newAttributeType("floor", Integer.class);
		AttributeType space = AttributeTypeFactory.newAttributeType("capacity", Integer.class);
		AttributeType quakeProof = AttributeTypeFactory.newAttributeType("quakeProof", Integer.class);
		AttributeType doorWidth = AttributeTypeFactory.newAttributeType("minWidth", Double.class);
		this.featureType = FeatureTypeFactory.newFeatureType(new AttributeType[] { geom, id, night, day, floor, space, quakeProof,doorWidth}, "building");
		
	}

	public static void main(String [] args) {
		String cvs = MY_STATIC_STUFF.PADANG_SVN_DATA + "/sheltersSurvey_2009/survey.csv";
//		String config = "../../inputs/configs/shapeFileEvac.xml";
		String config = "test/input/org/matsim/evacuation/riskaversion/RiskCostFromFloodingDataTest/testRiskCostFromFloodingData/config.xml";
		ScenarioImpl sc = new ScenarioLoaderImpl(config).getScenario();
		String shapeFile = sc.getConfig().evacuation().getBuildingsFile();
		String output = "buildings.shp";
		new CVSToShapeFile(cvs,shapeFile,output).run();
	}

}

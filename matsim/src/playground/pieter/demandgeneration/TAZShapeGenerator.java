package playground.pieter.demandgeneration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

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
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class TAZShapeGenerator {
	private Collection<Feature> inPolygons;
	private Collection<Feature> outFeatures;
	private HashMap<String,MultiPolygon> inPolygonHashMap;
	private GeometryFactory geofac;
	private FeatureType ftTAZ;
	private CoordinateReferenceSystem coordRefSystem;
	private String inputShape;
	private String outputShape;
	private String mappingInfo;

	public TAZShapeGenerator(String inputShape, String outputShape, String mappingInfo) {
		this.inputShape = inputShape;
		this.outputShape = outputShape;
		this.inPolygonHashMap = new HashMap<String, MultiPolygon>();
		this.geofac = new GeometryFactory();
		this.inPolygons = getFeatures(this.inputShape);
		this.mappingInfo = mappingInfo;
		this.outFeatures = new ArrayList<Feature>();
		initFeatures();
	}

	public Collection<Feature> getFeatures(final String inputShape) {
		FeatureSource featSrc = null;
		try {
			featSrc = ShapeFileReader.readDataFile(inputShape);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		this.coordRefSystem = featSrc.getSchema().getDefaultGeometry().getCoordinateSystem();

		final Collection<Feature> featColl = new ArrayList<Feature>();
		org.geotools.feature.FeatureIterator ftIterator = null;
		try {
			ftIterator = featSrc.getFeatures().features();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		//add features to arraylist collection
		while (ftIterator.hasNext()) {
			final Feature feature = ftIterator.next();
			featColl.add(feature);
		}
		return featColl;
	}

	private void initFeatures() {
		//define the output collection
		final AttributeType[] TAZs = new AttributeType[2];
		TAZs[0] = DefaultAttributeTypeFactory.newAttributeType("MultiPolygon",MultiPolygon.class, true, null, null, this.coordRefSystem);
		TAZs[1] = AttributeTypeFactory.newAttributeType("TAZ", Integer.class);

		try {
			this.ftTAZ = FeatureTypeFactory.newFeatureType(TAZs, "TAZ");//
		} catch (final FactoryRegistryException e) {
			e.printStackTrace();
		} catch (final SchemaException e) {
			e.printStackTrace();
		}
	}

	private void indexPolygons() {
		//goes through the collection of polygons
		for (Feature ft : this.inPolygons){
			Geometry geo = ft.getDefaultGeometry();
			//converts geometry to Multipolygon, if not already
			MultiPolygon multiPoly = null;
			if ( geo instanceof MultiPolygon ) {
				multiPoly = (MultiPolygon) geo;
			} else if (geo instanceof Polygon ) {
				multiPoly = this.geofac.createMultiPolygon(new Polygon[] {(Polygon) geo});
			} else {
				throw new RuntimeException("Feature does not contain a polygon/multipolygon!");
			}
			String EA_CODE = ((Long)ft.getAttribute(1)).toString();
			this.inPolygonHashMap.put(EA_CODE, multiPoly);
		}
	}

	private void run() throws IllegalAttributeException, IOException {
		indexPolygons();
		//reads the input file which lists SP_CODE , number of persons in SP who travel by car
		Scanner inputReader = new Scanner(new File(this.mappingInfo));
		//skip first line
		inputReader.nextLine();
		int EAPSU=0, currentTAZ=0, nextTAZ=0;
		ArrayList<Polygon> polyArray = new ArrayList<Polygon>();
		while(inputReader.hasNext())// first reads each line, catches relevant fields
		{
			//create an arraylist to store all the polygons for this TAZ

			if(nextTAZ == 0){
				currentTAZ = inputReader.nextInt();
				EAPSU = inputReader.nextInt();
				MultiPolygon multiPoly = this.inPolygonHashMap.get(Integer.toString(EAPSU));
				if (multiPoly == null) {
					//in case the text file references a EA we dont have in  shapefile
					continue;
				}
				//feed the Multipolygons polys into the PolyArray
				getPolysFromMultiPolyAndPutInArrayList(multiPoly,polyArray);
			}
			nextTAZ = inputReader.nextInt();

			if(nextTAZ != currentTAZ){
				//got a new TAZ, so first write the previous one
				writeTAZGeometry(polyArray, currentTAZ);
				//carry on with next TAZ
				currentTAZ = nextTAZ;
				EAPSU = inputReader.nextInt();
				polyArray = new ArrayList<Polygon>();
				MultiPolygon multiPoly = this.inPolygonHashMap.get(Integer.toString(EAPSU));
				if (multiPoly == null) {
					//in case the text file references a EA we dont have in  shapefile
					continue;
				}
				getPolysFromMultiPolyAndPutInArrayList(multiPoly,polyArray);
			}else{
				EAPSU = inputReader.nextInt();
				MultiPolygon multiPoly = this.inPolygonHashMap.get(Integer.toString(EAPSU));
				if (multiPoly == null) {
					//in case the text file references a EA we dont have in  shapefile
					continue;
				}
				getPolysFromMultiPolyAndPutInArrayList(multiPoly,polyArray);
			}
		}
		writeTAZGeometry(polyArray, currentTAZ);//write the final TAZ
		System.out.println(EAPSU);
		ShapeFileWriter.writeGeometries(this.outFeatures, this.outputShape);
	}

	private void writeTAZGeometry(ArrayList<Polygon> polyArray, int tazNumber) throws IllegalAttributeException {
		//first, create a MultiPoly from the PolyArray
		Polygon[] outArray = new Polygon[polyArray.size()];
		//Now add all the polygons from the PolyArray
		Iterator<Polygon> polyIterator = polyArray.iterator();
		int index = 0;
		while (polyIterator.hasNext()){
			outArray[index++] = polyIterator.next();
		}
		MultiPolygon outPoly = this.geofac.createMultiPolygon(outArray);
		Object [] fta = {outPoly,tazNumber};
		Feature ft = this.ftTAZ.create(fta);
		this.outFeatures.add(ft);
	}

	private void getPolysFromMultiPolyAndPutInArrayList(MultiPolygon inPolys, ArrayList<Polygon> outArray) {
		for(int i = 0; i < inPolys.getNumGeometries(); i++){
			outArray.add((Polygon)inPolys.getGeometryN(i));
		}
	}

	public static void main( String[] args ) throws Exception {
		final String inputShape = "./southafrica/SA_UTM/EA_UTM.shp";
		String outputShape = "./southafrica/TAZ_UTM.shp";
		String mappingInfo = "./southafrica/truncEAtoTAZ.csv";
		new TAZShapeGenerator(inputShape, outputShape, mappingInfo).run();
		System.out.printf("Done! TAZ shapefile output to %s", outputShape);
	}


}

package playground.gregor.gis.coordinatetransform;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import playground.mzilske.osm.WGS84ToOSMMercator;
import playground.mzilske.osm.WGS84ToOSMMercator.Project;

public class ShapeFileTransformer {


	public static void main(String [] args) throws IOException {
		String shapeFile = "/Users/laemmel/svn/shared-svn/studies/countries/de/hh/hafen_fest_evacuation/GDIToMATSimData/population.shp";
		CoordinateTransformation transform1 = TransformationFactory.getCoordinateTransformation("EPSG: 32632", "EPSG: 4326");
		Project transform2 = new WGS84ToOSMMercator.Project();
		FeatureSource fs = ShapeFileReader.readDataFile(shapeFile);

		List<Feature> fts= new ArrayList<Feature>();

		Iterator it = fs.getFeatures().iterator();
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			fts.add(ft);
			Geometry geo = ft.getDefaultGeometry();
			for (int i = 0; i < geo.getNumGeometries(); i++) {

				Geometry ggeo = geo.getGeometryN(i);
				Coordinate[] coordinates = ggeo.getCoordinates();
				for (Coordinate coordinate : coordinates) {
					Coord c = MGC.coordinate2Coord(coordinate);
					Coord cc = transform1.transform(c);
					Coord ccc = transform2.transform(cc);
					coordinate.setCoordinate(MGC.coord2Coordinate(ccc));
				}
			}
		}
		ShapeFileWriter.writeGeometries(fts, shapeFile);
	}

}

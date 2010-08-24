package playground.anhorni.choiceSetGeneration.filters;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class AreaReader {
	
	private List<Polygon> areaPolygons = null;
		
	public AreaReader() {
	}
	
	public void readShapeFile(final String shapeFile)  {
		
		this.areaPolygons = new Vector<Polygon>();
		
		try {
			FeatureSource fs = ShapeFileReader.readDataFile(shapeFile);
			FeatureCollection collectionPolygon = fs.getFeatures();
			FeatureIterator it = collectionPolygon.features();
			while (it.hasNext()) {
				Feature feature = it.next();
				
				MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
				for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
					Polygon polygon = (Polygon) multiPolygon.getGeometryN(i);
					
					/*
					try {
						// Javadoc vividsolutions: Computes a buffer area around this geometry having the given width.
						// NUMERICS?
						polygon = (Polygon) polygon.buffer(0.01);
					} catch (RuntimeException e) {
						e.printStackTrace();
					}
					*/
					this.areaPolygons.add(polygon);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}

	public List<Polygon> getAreaPolygons() {
		return areaPolygons;
	}

	public void setAreaPolygons(List<Polygon> areaPolygons) {
		this.areaPolygons = areaPolygons;
	}
}

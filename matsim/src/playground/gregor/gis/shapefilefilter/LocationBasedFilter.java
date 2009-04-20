package playground.gregor.gis.shapefilefilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;

import com.vividsolutions.jts.geom.Geometry;

public class LocationBasedFilter {

	public static void main (String [] args) throws IOException {
		String zone01 = "../../../workspace/vsp-cvs/studies/padang/gis/evac_zone/zone_0_5.shp";
		String zone02 = "../../../workspace/vsp-cvs/studies/padang/gis/evac_zone/zone_5_10.shp";

		String toFilter = "../../../workspace/vsp-cvs/studies/padang/gis/buildings_v20090403/buildings_v20090403.shp";
		String out = "../../../workspace/vsp-cvs/studies/padang/gis/buildings_v20090403/evac_zone_buildings_v20090403.shp";

		List<Geometry> evacZone = new ArrayList<Geometry>();

		FeatureSource fts = null;
		fts = ShapeFileReader.readDataFile(zone01);
		Iterator it = null;
		it = fts.getFeatures().iterator();
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			evacZone.add(ft.getDefaultGeometry());

		}

		fts = ShapeFileReader.readDataFile(zone02);
		it = fts.getFeatures().iterator();
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			evacZone.add(ft.getDefaultGeometry());

		}
		
		 Collection<org.geotools.feature.Feature> buildings = new ArrayList<Feature>();

		fts = ShapeFileReader.readDataFile(toFilter);
		it = fts.getFeatures().iterator();
		
		int count = 0;
		while (it.hasNext()) {
			if (++count % 1000 == 0) {
				System.out.println(count);
			}
			Feature ft = (Feature) it.next();
			Geometry geo = ft.getDefaultGeometry();
			for (Geometry tmp : evacZone) {
				if (tmp.contains(geo)) {
					buildings.add(ft);
					break;
				}
			}

		}

		ShapeFileWriter.writeGeometries(buildings, out);
	}

}


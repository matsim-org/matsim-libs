package playground.gregor.gis.buildinglinkmapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.geotools.feature.Feature;
import org.geotools.feature.IllegalAttributeException;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;

public class EvacTimeComp {
	
	public static void main(String [] args) {
		String r1994 = "/Users/laemmel/arbeit/papers/2012/lastMile/matsim/buildings_b.shp";
		String r1992 = "/Users/laemmel/arbeit/papers/2012/lastMile/matsim/buildings.shp";
		
		ShapeFileReader reader94 = new ShapeFileReader();
		ShapeFileReader reader92 = new ShapeFileReader();
		reader94.readFileAndInitialize(r1994);
		reader92.readFileAndInitialize(r1992);
		Map<String,Feature> r92m = new HashMap<String, Feature>();
		for (Feature ft : reader92.getFeatureSet()) {
			String name = (String) ft.getAttribute("name");
			r92m.put(name, ft);
		}
		
		Collection<Feature> fts = new ArrayList<Feature>();
		for (Feature ft : reader94.getFeatureSet()) {
			String name = (String) ft.getAttribute("name");
			Feature ft2 = r92m.get(name);
			Double t94 = (Double) ft.getAttribute("dblAvgZ");
			Double t92 = (Double) ft2.getAttribute("dblAvgZ");
			double diff = t94-t92;
			try {
				ft.setAttribute("dblAvgZ", diff);
			} catch (IllegalAttributeException e) {
				e.printStackTrace();
			}
			fts.add(ft);
		}
		ShapeFileWriter.writeGeometries(fts, "/Users/laemmel/arbeit/papers/2012/lastMile/matsim/buildings_diff.shp");
	}

}

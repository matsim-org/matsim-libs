package playground.gregor.gis.shapefilefilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;

public class FeatureBasedFilter {
	
	public static void main(String [] args) {
		String root = "../../../workspace/vsp-cvs/studies/padang/gis/";
		final String input = root +"./buildings_v20090403/buildings_v20090403.shp";
		final String output = root +"./buildings_v20090403/shelters_v20090403.shp";
		
		FeatureSource fs = null;
		try {
			fs = ShapeFileReader.readDataFile(input);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Iterator it  = null;
		try {
			it= fs.getFeatures().iterator();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ArrayList<Feature> fts = new ArrayList<Feature>();
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			Integer quakeProof = (Integer) ft.getAttribute("quakeProof");
			if (quakeProof == 1) {
				fts.add(ft);
			}
		}
		
		try {
			ShapeFileWriter.writeGeometries(fts, output);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}

}

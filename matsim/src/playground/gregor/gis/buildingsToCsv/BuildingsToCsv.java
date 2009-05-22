package playground.gregor.gis.buildingsToCsv;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Coordinate;


public class BuildingsToCsv {

	
	public static void main (String [] args) throws IOException {
		String in = "/home/laemmel/arbeit/svn/shared-svn/studies/countries/id/padang/data/population/Population_Padang.shp";
		String out = "./tmp/buildings_v20090403_table.csv";
		FeatureSource fs = ShapeFileReader.readDataFile(in);

		BufferedWriter writer = new BufferedWriter(new FileWriter(out));
		writer.append("OBJECTID");
		writer.append(',');
		writer.append("popBdNt");
		writer.append(',');
		writer.append("popBd_day");
		writer.append(',');
		writer.append("x,y");
		writer.append("\n");
		
		Iterator it = fs.getFeatures().iterator();
		while (it.hasNext()) {
			Feature f = (Feature) it.next();
			Coordinate c = f.getDefaultGeometry().getCentroid().getCoordinate();
			writer.append(f.getAttribute("OBJECTID").toString());
			writer.append(',');
			writer.append(f.getAttribute("popBdNt").toString());
			writer.append(',');
			writer.append(f.getAttribute("popBd_day").toString());
			writer.append(',');
			writer.append(c.x + "," + c.y);
			writer.append("\n");
			
		}
		writer.close();
		
		
	}
}

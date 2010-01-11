package playground.gregor.gis.points2plans;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Coordinate;


public class Points2Plans {
	
	
	public static void main (String [] args) throws IOException {
		String shp = "/home/laemmel/devel/sim2d/plans.shp";
		FeatureSource fs = ShapeFileReader.readDataFile(shp);
		
		Iterator it = fs.getFeatures().iterator();
		Feature ft = (Feature)it.next();
		Coordinate c0 = ft.getDefaultGeometry().getCoordinate();
		
		Writer out = new FileWriter(new File("/home/laemmel/devel/inputs/networks/plans2d_evac.xml"));
		BufferedWriter bf = new BufferedWriter(out );
		bf.append("<?xml version=\"1.0\" ?>\n");
		bf.append("<!DOCTYPE plans SYSTEM \"http://www.matsim.org/files/dtd/plans_v4.dtd\">\n");
		bf.append("<plans>\n");
		int i = 0;
		double stTime = 9*3600;
		double evacTime = 9*3600 +20*60;
		while (it.hasNext()) {
			 ft = (Feature)it.next();
			 Coordinate c1 = ft.getDefaultGeometry().getCoordinate();
			 bf.append("<person id=\"" + i++ + "\">\n");
			 bf.append("\t<plan>\n");
//			 bf.append("\t\t<act type=\"h\" x=\"" + c0.x + "\" y=\"" + c0.y + "\" end_time=\"" + stTime + "\" />\n");
//			 stTime += 10;
//			 
//			bf.append("\t\t<leg mode=\"car\">\n");
//			bf.append("\t\t</leg>\n");
			
			bf.append("\t\t<act type=\"w\" x=\"" + c1.x + "\" y=\"" + c1.y + "\" end_time=\"" + evacTime + "\" />\n");
			bf.append("\t\t<leg mode=\"car\">\n");
			bf.append("\t\t</leg>\n");
			
			bf.append("\t\t<act type=\"h\" x=\"" + c0.x + "\" y=\"" + c0.y + "\" end_time=\"" + evacTime + "\" />\n");
			bf.append("\t</plan>\n");
			bf.append("</person>\n");
			
		}
		bf.append("</plans>\n");
		bf.close();
	}

}

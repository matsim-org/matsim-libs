package playground.droeder.gis;

import java.io.IOException;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.core.utils.gis.ShapeFileReader;

public class AnalyzeMyShapeFile {
	
	public static void main(String[] args) throws IOException{
		new AnalyzeMyShapeFile().run("D:/VSP/output/BerlinShape/sechwy.shp");
	}
	
	public void run(String shapeFile) throws IOException{
		FeatureSource features = ShapeFileReader.readDataFile(shapeFile);
		Feature ft = (Feature) features.getFeatures().iterator().next();
		
		for(int i = 0; i < ft.getNumberOfAttributes(); i++){
			System.out.print(i + " " + ft.getFeatureType().getAttributeType(i).getName() + "\t" +ft.getAttribute(i).getClass().toString() );
			System.out.println();
		}
	}

}

package playground.balac.carsharinginput;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class GreaterZurichStations {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		BufferedReader read = IOUtils.getBufferedReader(args[0]);
		
		BufferedWriter output = new BufferedWriter(new FileWriter(new File(args[1])));
		double coordX = 683217.0;
		double coordY = 247300.0;
		CoordinateReferenceSystem crs = MGC.getCRS("EPSG:21781");
		Coord center = new Coord(coordX, coordY);
		Collection<SimpleFeature> featuresMovedIncrease = new ArrayList<SimpleFeature>();
        featuresMovedIncrease = new ArrayList<SimpleFeature>();
        PointFeatureFactory nodeFactory = new PointFeatureFactory.Builder().
                setCrs(crs).
                setName("nodes").
                addAttribute("ID", String.class).
               //addAttribute("Customers", Integer.class).
                //addAttribute("Be Af Mo", String.class).
                
                create();
		output.write(read.readLine());
		output.newLine();
		String s = read.readLine();
		int count = 0;
		
		//used to count members in the area
		while (s != null) {
			
			String[] arr = s.split("\t");

			Coord coord = new Coord(Double.parseDouble(arr[5]), Double.parseDouble(arr[4]));
			
			WGS84toCH1903LV03 b = new WGS84toCH1903LV03();  //transforming coord from WGS84 to CH1903LV03
			coord = b.transform(coord);
			
			if (CoordUtils.calcEuclideanDistance(coord, center) <= 30000.0) {
				
				count++;
				SimpleFeature ft = nodeFactory.createPoint(coord, new Object[] {Integer.toString(count)}, null);
				featuresMovedIncrease.add(ft);
				
			}

			s = read.readLine();
		}
        ShapeFileWriter.writeGeometries(featuresMovedIncrease, "C:/Users/balacm/Desktop/SHP_files/Zurich Members_part1.shp");

		System.out.println(count);
		
		while (s != null) {
			
			String[] arr = s.split("\t");

			Coord coord = new Coord(Double.parseDouble(arr[8]), Double.parseDouble(arr[7]));
			
			WGS84toCH1903LV03 b = new WGS84toCH1903LV03();  //transforming coord from WGS84 to CH1903LV03
			coord = b.transform(coord);
			
			if (CoordUtils.calcEuclideanDistance(coord, center) <= 30000.0) {
				
				output.write(arr[3] + "\t" + arr[1] + "\t" + arr[5] + "\t" + arr[6] + "\t" + arr[7] + "\t" + arr[8] + "\t" + arr[9]);
				output.newLine();
				
				
			}
			s = read.readLine();
		}
		
		output.flush();
		output.close();		

	}

}

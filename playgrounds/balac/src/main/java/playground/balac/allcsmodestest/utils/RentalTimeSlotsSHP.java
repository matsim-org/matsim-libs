package playground.balac.allcsmodestest.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class RentalTimeSlotsSHP {

	public static void main(String[] args) throws IOException {
		double centerX = 683217.0;  //Belvue coordinates
		double centerY = 247300.0;
		final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/1.250.OW_CS");
		int[] rentalTimes = new int[30];
		int[] distance = new int[50];

		int[] rentalStart = new int[35];
		Set<Double> bla = new HashSet<Double>();
		Set<String> usedCars = new HashSet<String>();

		String s = readLink.readLine();
		s = readLink.readLine();
		int count = 0;
		int count1 = 0;
		int countZero = 0;
		double di = 0.0;
		double time1 = 0.0;
		
		int[] rentalStartIntervals = new int[3];
		
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[0]);
		
		Network network = scenario.getNetwork();
        CoordinateReferenceSystem crs = MGC.getCRS("EPSG:21781");    // EPSG Code for Swiss CH1903_LV03 coordinate system


		 Collection<SimpleFeature> featuresFirstInterval= new ArrayList<SimpleFeature>();
		 featuresFirstInterval = new ArrayList<SimpleFeature>();
	        PointFeatureFactory nodeFactory = new PointFeatureFactory.Builder().
	                setCrs(crs).
	                setName("nodes").
	                addAttribute("ID", String.class).
	                create();
		
        Collection<SimpleFeature> featuresSecondInterval= new ArrayList<SimpleFeature>();
        featuresSecondInterval = new ArrayList<SimpleFeature>();
	       
        Collection<SimpleFeature> featuresThirdInterval= new ArrayList<SimpleFeature>();
        featuresThirdInterval = new ArrayList<SimpleFeature>();   
	        
		int i = 0;
				
		while(s != null) {
			String[] arr = s.split("\\s");
			if (Double.parseDouble(arr[5]) != 0.0) {
				double time = Double.parseDouble(arr[6]);
				distance[(int)(time * 0.9 / 130.0)]++;
				bla.add(Double.parseDouble(arr[0]));
				double startTime = Double.parseDouble(arr[1]);
				rentalStart[(int)((startTime) / 3600)]++;			
	
				double endTime = Double.parseDouble(arr[2]);
				rentalTimes[(int)((endTime - startTime) / 3600)]++;
				di += Double.parseDouble(arr[5]);
				time1 += endTime -startTime;
				if (endTime - startTime < 1800) 
					count1++;
				count++;
				usedCars.add(arr[8]);
				if (startTime >= 21600 && startTime <= 36000) {
					SimpleFeature ft = nodeFactory.createPoint(network.getLinks().get(Id.create(arr[3], Link.class)).getCoord(), new Object[] {Integer.toString(i)}, null);
					featuresFirstInterval.add(ft);
					i++;
				}
				else if (startTime >= 36000 && startTime <= 57600) {
					SimpleFeature ft = nodeFactory.createPoint(network.getLinks().get(Id.create(arr[3], Link.class)).getCoord(), new Object[] {Integer.toString(i)}, null);
					featuresSecondInterval.add(ft);
					i++;
					
				}
				else if (startTime >= 57600 && startTime <= 72000) {
					SimpleFeature ft = nodeFactory.createPoint(network.getLinks().get(Id.create(arr[3], Link.class)).getCoord(), new Object[] {Integer.toString(i)}, null);
					featuresThirdInterval.add(ft);
					i++;
					
				}
				
				
				
				
				

			}
			s = readLink.readLine();		
			
		}
		
        ShapeFileWriter.writeGeometries(featuresFirstInterval, "C:/Users/balacm/Desktop/SHP_files/TRB2016/ow_6_10_start_rentals.shp");
        ShapeFileWriter.writeGeometries(featuresSecondInterval, "C:/Users/balacm/Desktop/SHP_files/TRB2016/ow_10_16_start_rentals.shp");
        ShapeFileWriter.writeGeometries(featuresThirdInterval, "C:/Users/balacm/Desktop/SHP_files/TRB2016/ow_16_20_start_rentals.shp");

		
	}

}

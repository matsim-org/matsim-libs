package city2000w;

import gis.arcgis.NutsRegionShapeReader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import kid.KiDDataReader;
import kid.KiDSchema;
import kid.KiDShapeFileWriter;
import kid.ScheduledVehicles;
import kid.Vehicle;
import kid.filter.And;
import kid.filter.BusinessSectorFilter;
import kid.filter.GeoRegionFilter;
import kid.filter.LkwKleiner3Punkt5TFilter;
import kid.filter.LogicVehicleFilter;
import kid.filter.StuttgartRegionFilter;
import kid.filter.VehicleFilter;

import org.opengis.feature.simple.SimpleFeature;





public class KiDDataGeoCoder {
	
	public static class MobileVehicleFilter implements VehicleFilter{

		public boolean judge(Vehicle vehicle) {
			int mobilityIndex = Integer.parseInt(vehicle.getAttributes().get(KiDSchema.VEHICLE_MOBILITY));
			if(mobilityIndex == 1){
				return true;
			}
			return false;
		}
		
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		ScheduledVehicles vehicles = new ScheduledVehicles();
		KiDDataReader kidReader = new KiDDataReader(vehicles);
		
		String directory = "/Volumes/projekte/2000-Watt-City/Daten/KiD/";
		kidReader.setVehicleFile(directory + "KiD_2002_Fahrzeug-Datei.txt");
		kidReader.setTransportChainFile(directory + "KiD_2002_Fahrtenketten-Datei.txt");
		kidReader.setTransportLegFile(directory + "KiD_2002_(Einzel)Fahrten-Datei.txt");
		LogicVehicleFilter andFilter = new And();
		andFilter.addFilter(new MobileVehicleFilter());
		andFilter.addFilter(new LkwKleiner3Punkt5TFilter());
		andFilter.addFilter(new BusinessSectorFilter());
		kidReader.setVehicleFilter(andFilter);
		List<SimpleFeature> regions = new ArrayList<SimpleFeature>();
		NutsRegionShapeReader regionReader = new NutsRegionShapeReader(regions, new StuttgartRegionFilter(),null);
		regionReader.read(directory + "regions_europe_wgsUtm32N.shp");
		kidReader.setScheduledVehicleFilter(new GeoRegionFilter(regions));
		kidReader.run();
		
		KiDShapeFileWriter shapeFileWriter = new KiDShapeFileWriter(vehicles);
		shapeFileWriter.setShapeFileName(directory + "tours_lkw_kl3p5_stuttgart.shp");
		shapeFileWriter.setNodeFileName(directory + "nodes_lkw_lk3p5_stuttgart.shp");
		shapeFileWriter.run();
	}

}

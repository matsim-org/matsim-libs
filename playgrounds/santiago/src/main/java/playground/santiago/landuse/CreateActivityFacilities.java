package playground.santiago.landuse;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;


public class CreateActivityFacilities {
	
	
	final String STGO_SHAPE_FILE = "../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/4_finalShapeSantiago/finalShapeSantiago.shp" ;
	final String OUTPUT_FACILITIES_FILE = "../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/";

	
	private CreateActivityFacilities() {
		
}
	
	
	private void Run(){

		
		ArrayList<String> attributeCaptions = collectAttributeCaptions();
		File output = new File(OUTPUT_FACILITIES_FILE);
		if(!output.exists()) output.mkdirs();		
		
		for (String attributeCaption : attributeCaptions){
			
			ActivityFacilities af = createFacilities(STGO_SHAPE_FILE, attributeCaption, attributeCaption);
			writeFacilitiesFile(af, OUTPUT_FACILITIES_FILE + attributeCaption + ".xml" );
			
		}
	
}
	
	
	public static void main(String[] args) {
		 CreateActivityFacilities createActivityFacilitiesTry = new CreateActivityFacilities();
		 createActivityFacilitiesTry.Run();
	}
	


	private Coord shoot(Geometry geometry) {
		Random rnd = new Random();
		//Point point = getRandomPointInFeature(r, zone);
		Point point = null;
		double x, y;
		
		do {
			x = geometry.getEnvelopeInternal().getMinX() + rnd.nextDouble() * (geometry.getEnvelopeInternal().getMaxX() - geometry.getEnvelopeInternal().getMinX());
			y = geometry.getEnvelopeInternal().getMinY() + rnd.nextDouble() * (geometry.getEnvelopeInternal().getMaxY() - geometry.getEnvelopeInternal().getMinY());
			point = MGC.xy2Point(x, y);
		} while (!geometry.contains(point));

		Coord coord = new Coord(point.getX(), point.getY());
		return coord;
	}	


	private ArrayList <String> collectAttributeCaptions (){
	
	ArrayList <String> attributeCaptions = new ArrayList <>();
	
	attributeCaptions.add("comByArea"); 
	attributeCaptions.add("admByArea"); 
	attributeCaptions.add("bodByArea");
	attributeCaptions.add("cultByArea");
	attributeCaptions.add("depByArea");
	attributeCaptions.add("edByArea");
	attributeCaptions.add("estByArea");
	attributeCaptions.add("hogByArea");
	attributeCaptions.add("hotByArea");
	attributeCaptions.add("indByArea");
	attributeCaptions.add("minByArea");
	attributeCaptions.add("ofByArea");
	attributeCaptions.add("otrByArea");
	attributeCaptions.add("salByArea");
	attributeCaptions.add("sitByArea");
	attributeCaptions.add("traByArea");
	
	return attributeCaptions;
	
	}
		
	//attributeCaption should not be "CMN_MZ_AR"	
	private Map<String,String> collectFeatures(String shapeFileName , String attributeCaption) {
		
		ShapeFileReader reader = new ShapeFileReader();
		Collection<SimpleFeature> features = reader.readFileAndInitialize(shapeFileName);
		Map <String, String> utilInfo = new HashMap<>();
		
		for (SimpleFeature feature : features) {
			
			utilInfo.put((String) feature.getAttribute("CMN_MZ_AR"), feature.getAttribute(attributeCaption).toString());
						
		}
		
		return utilInfo;
	}
	
	private Map<String,Geometry> getGeometriesById (String shapeFileName){
		
		ShapeFileReader reader = new ShapeFileReader();
		Map<String,Geometry>geometriesById = new HashMap<>();
		
		Collection<SimpleFeature> features = reader.readFileAndInitialize(shapeFileName);
		for (SimpleFeature feature : features) {
			geometriesById.put((String) feature.getAttribute("CMN_MZ_AR"),(Geometry) feature.getDefaultGeometry());
		}	
		
		return geometriesById;
	}
	
	//attributeCaption should not be "CMN_MZ_AR"
	private ActivityFacilities createFacilities(String shapeFileName, String nameOfFacility, String attributeCaption) {
		
		Map<String,String> utilInfo = collectFeatures (shapeFileName , attributeCaption);
		Map<String,Geometry> geometriesById = getGeometriesById(shapeFileName);
		
		ActivityFacilities activityFacilities = FacilitiesUtils.createActivityFacilities(nameOfFacility);
		ActivityFacilitiesFactory activityFacilitiesFactory = new ActivityFacilitiesFactoryImpl();
		

		
		for (String blockId : utilInfo.keySet()) {

			
			int numberOfBuildingsById = Integer.parseInt(utilInfo.get(blockId));
			Geometry geometry = geometriesById.get(blockId);
			
			
			
			if(numberOfBuildingsById!=0){
				
				for (int n=1 ; n<=numberOfBuildingsById; n++){
					
					Id<ActivityFacility> id = Id.create(blockId+"-"+nameOfFacility.substring(0,2)+n, ActivityFacility.class);
					Coord coord = shoot(geometry); 	
					ActivityFacility activityFacility = activityFacilitiesFactory.createActivityFacility(id, coord);
					activityFacilities.addActivityFacility(activityFacility);

				}
			}

		}
		return activityFacilities;
	}
	
	private void writeFacilitiesFile(ActivityFacilities activityFacilities, String facilitiesOutputFile) {

		FacilitiesWriter fw = new FacilitiesWriter(activityFacilities);
		fw.write(facilitiesOutputFile);
		
	}
	
	
	}
	
	
	
	

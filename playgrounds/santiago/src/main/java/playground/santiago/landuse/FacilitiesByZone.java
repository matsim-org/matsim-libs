package playground.santiago.landuse;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.opengis.feature.simple.SimpleFeature;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class FacilitiesByZone {

	
	Collection <SimpleFeature> features;
	
	
	public FacilitiesByZone(Collection<SimpleFeature> features){
		this.features=features;
	}
	
	private long getEODZone (Coord coord){
		
		Point point = MGC.xy2Point(coord.getX(), coord.getY());		
		Map<Long,Geometry>geometriesById = new HashMap<>();
		
		for (SimpleFeature feature : features) {
			
			geometriesById.put((Long) feature.getAttribute("ID"),(Geometry) feature.getDefaultGeometry());
			
		}
		
		long zone=1;
		for (long id : geometriesById.keySet()){
			if(geometriesById.get(id).contains(point)){
				
				zone = id ;
				break;

			}
		}
		
		return zone;
		
	}	
		
	public Multimap <Long,ActivityFacility> build (String activityType){

		/**************/
		String IDAct = activityType.substring(0,2);
		Config config = ConfigUtils.createConfig();
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		/*Only necessary when the activity type has multiple activity options*/
		Scenario scenarioAux1 = ScenarioUtils.createScenario(config);
		Scenario scenarioAux2 = ScenarioUtils.createScenario(config);
		Scenario scenarioAux3 = ScenarioUtils.createScenario(config);
		/*************************************************************/
		
		MatsimFacilitiesReader fr = new MatsimFacilitiesReader(scenario);
		
		/*Only necessary when the activity type has multiple activity options*/
		MatsimFacilitiesReader frAux1 = new MatsimFacilitiesReader(scenarioAux1);
		MatsimFacilitiesReader frAux2 = new MatsimFacilitiesReader(scenarioAux2);
		MatsimFacilitiesReader frAux3 = new MatsimFacilitiesReader(scenarioAux3);
		/************************************************************/
		
		switch (IDAct){
		
		case ("ho"):
			fr.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/hogByArea.xml");
			break;
			
		case ("wo"):
			fr.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/admByArea.xml");
			frAux1.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/indByArea.xml");
			frAux2.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/minByArea.xml");
			frAux3.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/ofByArea.xml");
			break;
			
		case ("bu"):
			fr.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/admByArea.xml");
			frAux1.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/indByArea.xml");
			frAux2.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/minByArea.xml");
			frAux3.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/ofByArea.xml");
			break;
			
		case("ed"):
			fr.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/edByArea.xml");
			break;
			
		case("he"):
			fr.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/salByArea.xml");
			break;
			
		case("vi"):
			fr.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/hogByArea.xml");
			break;
			
		case("sh"):
			fr.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/comByArea.xml");
			break;
			
		case("le"):
			fr.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/cultByArea.xml");
			frAux1.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/depByArea.xml");
			frAux2.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/hotByArea.xml");
			break;
			
		case("ot"):
			fr.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/otrByArea.xml");
			break;
		}
		/**************/
		
		
		Multimap<Long, ActivityFacility > actFacilitiesByTAZ = HashMultimap.create();
		
		

		/*Simple cases*/
		if (IDAct.equals("ho")||IDAct.equals("ed")||IDAct.equals("he")||IDAct.equals("sh")||IDAct.equals("vi")||IDAct.equals("ot")){
		
		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			
				Coord coord = facility.getCoord();
				long TAZId = getEODZone (coord);				
				actFacilitiesByTAZ.put(TAZId,facility);

				
			}
		
		/*work and busy*/
		} else if (IDAct.equals("wo")||IDAct.equals("bu")){
			
				for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
				
					Coord coord = facility.getCoord();
					long TAZId = getEODZone (coord);				
					actFacilitiesByTAZ.put(TAZId,facility);
				}
			
				for (ActivityFacility facility : scenarioAux1.getActivityFacilities().getFacilities().values()) {
					
					Coord coord = facility.getCoord();
					long TAZId = getEODZone (coord);				
					actFacilitiesByTAZ.put(TAZId,facility);
				}
				
				for (ActivityFacility facility : scenarioAux2.getActivityFacilities().getFacilities().values()) {
					
					Coord coord = facility.getCoord();
					long TAZId = getEODZone (coord);				
					actFacilitiesByTAZ.put(TAZId,facility);
				}
				
				for (ActivityFacility facility : scenarioAux3.getActivityFacilities().getFacilities().values()) {
					
					Coord coord = facility.getCoord();
					long TAZId = getEODZone (coord);				
					actFacilitiesByTAZ.put(TAZId,facility);
				}
				
		/*leisure*/
		} else {
						
			for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			
				Coord coord = facility.getCoord();
				long TAZId = getEODZone (coord);				
				actFacilitiesByTAZ.put(TAZId,facility);
			}
		
			for (ActivityFacility facility : scenarioAux1.getActivityFacilities().getFacilities().values()) {
				
				Coord coord = facility.getCoord();
				long TAZId = getEODZone (coord);				
				actFacilitiesByTAZ.put(TAZId,facility);
			}
			
			for (ActivityFacility facility : scenarioAux2.getActivityFacilities().getFacilities().values()) {
				
				Coord coord = facility.getCoord();
				long TAZId = getEODZone (coord);				
				actFacilitiesByTAZ.put(TAZId,facility);
			}
			
		}
		
	
		return actFacilitiesByTAZ;
		
		
	}
	
	
	
	
}

package playground.julia.emissions;

import java.util.Map;

import org.matsim.core.utils.collections.Tuple;

import playground.vsp.emissions.types.HbefaVehicleAttributes;
import playground.vsp.emissions.types.HbefaVehicleCategory;

public class CommentsOnColdEmissionAnalysisModule {
	
	

	/**
	 * Aenderungen von convertString2Tuple
	 * 
	 * 1. orginal
	 * 2. notizen
	 * 3. vorschlag
	 * 
	 */
	
	//original
	private Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> convertString2Tuple(String vehicleInformation) {
		Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple;
		HbefaVehicleCategory hbefaVehicleCategory = null;
		HbefaVehicleAttributes hbefaVehicleAttributes = new HbefaVehicleAttributes();

		String[] vehicleInformationArray = vehicleInformation.split(";");

		for(HbefaVehicleCategory vehCat : HbefaVehicleCategory.values()){
			if(vehCat.toString().equals(vehicleInformationArray[0])){
				hbefaVehicleCategory = vehCat;
			} else continue;
		}

		if(vehicleInformationArray.length == 4){
			hbefaVehicleAttributes.setHbefaTechnology(vehicleInformationArray[1]);
			hbefaVehicleAttributes.setHbefaSizeClass(vehicleInformationArray[2]);
			hbefaVehicleAttributes.setHbefaEmConcept(vehicleInformationArray[3]);
		} else{
			// interpretation as "average vehicle"
		}

		vehicleInformationTuple = new Tuple<HbefaVehicleCategory, HbefaVehicleAttributes>(hbefaVehicleCategory, hbefaVehicleAttributes);
		return vehicleInformationTuple;
	}
	
	//split trennt am ';'
	// wirft aber alles nach dem letzten eintrag weg
	// ';;a;' wird zu [][][a]
	// ";" erzeugt leeres array
	// fehlende werte koennten zu verschiebungen bzgl der hbefaattributes fuehren...
	// koennte sowas auftreten? "vehcat; technology;;emconcept;anderes" 
	// dann wuerde sizeclass= emconcept und emconcept= anderes gesetzt
	
	Map <String, HbefaVehicleCategory> vehcatMap;
	{
		this.vehcatMap.clear();
		for(HbefaVehicleCategory vehcat: HbefaVehicleCategory.values()){
			vehcatMap.put(vehcat.toString(), vehcat);
		}
	}
	
	//vorschlag
	private Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> convertString2TupleVorschlag(String vehicleInformation) {
		Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple;
		HbefaVehicleCategory hbefaVehicleCategory = null;
		HbefaVehicleAttributes hbefaVehicleAttributes = new HbefaVehicleAttributes();
	
		//aus dem String zuerst die vehcat auslesen
		int techStart= vehicleInformation.indexOf(";")+1;
		if (techStart==0) techStart = vehicleInformation.length()+1;
		String potVehcat = vehicleInformation.substring(0, techStart-1);
		
		if(vehcatMap.containsKey(potVehcat)){
			hbefaVehicleCategory = vehcatMap.get(potVehcat);			
			int sizeStart= vehicleInformation.indexOf(";", techStart)+1;
			int conceptStart= vehicleInformation.indexOf(";", sizeStart)+1;	

			// if one of the indices is -1 => not enough matches of ";"
			// => interpretation as "average vehicle"
			if (!(sizeStart==-1||conceptStart==-1)) {
				int end= vehicleInformation.indexOf(";", conceptStart);
				if (end==-1)end=vehicleInformation.length();

				// all substrings with content
				// otherwise => interpretation as "average vehicle"
				if (sizeStart - techStart > 1 && conceptStart - sizeStart > 1 && end - conceptStart > 1) {
					hbefaVehicleAttributes.setHbefaTechnology(vehicleInformation.substring(techStart, sizeStart-1));
					hbefaVehicleAttributes.setHbefaSizeClass(vehicleInformation.substring(sizeStart, conceptStart-1));
					hbefaVehicleAttributes.setHbefaEmConcept(vehicleInformation.substring(conceptStart, end));
				} 		
			}
		}else{//veh category not set
			return new Tuple<HbefaVehicleCategory, HbefaVehicleAttributes>(null, null);
		}
		
		vehicleInformationTuple = new Tuple<HbefaVehicleCategory, HbefaVehicleAttributes>(hbefaVehicleCategory, hbefaVehicleAttributes);
		return vehicleInformationTuple;
		
	}

	
}

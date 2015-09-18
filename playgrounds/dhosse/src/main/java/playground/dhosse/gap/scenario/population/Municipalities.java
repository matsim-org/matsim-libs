package playground.dhosse.gap.scenario.population;

import java.util.HashMap;
import java.util.Map;

public class Municipalities {
	
	static final Map<String, Municipality> municipalities = new HashMap<String, Municipality>();

	public static void addEntry(String name, int nStudents, int nAdults, int nPensioners){
		
		if(!municipalities.containsKey(name)){
			
			municipalities.put(name, new Municipality(nStudents, nAdults, nPensioners));
			
		}
		
	}
	
	public static Municipality getEntry(String name){
		
		return municipalities.get(name);
		
	}
	
	public static Map<String, Municipality> getMunicipalities(){
		
		return municipalities;
		
	}
	
}
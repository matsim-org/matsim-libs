package playground.gregor.gis.buildinglinkmapping;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.misc.StringUtils;

public class PersonBuildingMappingParser {

	
	public Map<Id,Id> getPersonBuildingMappingFromFile(String file) {
		Map<Id,Id> ret = new HashMap<Id,Id>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(new File(file)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		String str;
		try {
			str = br.readLine();
			str = br.readLine();
			while (str != null) {
				String[] item = StringUtils.explode(str, ',');
				Id pers = new IdImpl(item[0]);
				Id building = new IdImpl(item[1]);
				ret.put(pers, building);
				str = br.readLine();	
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return ret;
	}
}

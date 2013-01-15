package playground.anhorni.csestimation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;

public class UniversalChoiceSetReader {
	
	public TreeMap<Id, ShopLocation> readUniversalCS(String file) {
		TreeMap<Id, ShopLocation> shops = new TreeMap<Id, ShopLocation>();
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			br.readLine(); // skip header
			String curr_line;
			while ((curr_line = br.readLine()) != null) {
				String[] entrs = curr_line.split("\t", -1);
				
				Id id = new IdImpl(Integer.parseInt(entrs[0].trim()));
				ShopLocation shop = new ShopLocation(id);
				CoordImpl coord = new CoordImpl(Double.parseDouble(entrs[4]), Double.parseDouble(entrs[5]));
				shop.setCoord(coord);
				shops.put(id, shop);				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return shops;
	}

}

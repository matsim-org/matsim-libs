package playground.artemc.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by artemc on 1/2/15.
 */
public class MapWriter {

	String path;

	public MapWriter(String path){
		this.path = path;
	}

	public void writeBetaFactors(HashMap<Id<Person>, Double> map, String row1, String row2){
		File file = new File(this.path);

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write(row1+","+row2);
			writer.newLine();
			for (Id<Person> personId:map.keySet()){
				writer.write(personId.toString()+","+map.get(personId).toString());
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

package playground.artemc.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by artemc on 1/2/15.
 */
public class MapWriter {

	String path;

	public MapWriter(String path){
		this.path = path;
	}

	public void write(HashMap<Id<Person>, Double> map, String row1, String row2){
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

	public void writeArray(HashMap<Id<Person>, ArrayList<String>> map, ArrayList<String> head){
		File file = new File(this.path);

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write(head.get(0));
			for(int i=1;i<head.size();i++){
				writer.write(","+head.get(i));
			}

			writer.newLine();
			for (Id<Person> personId:map.keySet()){
				String values = "";
				for(String value:map.get(personId)){
					values=values + value + ",";
				}
				values.substring(0,values.length()-2);

				writer.write(personId.toString() + "," + values);
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

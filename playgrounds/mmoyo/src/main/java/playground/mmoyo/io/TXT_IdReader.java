package playground.mmoyo.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class TXT_IdReader {

	/**reads id's from a text file*/
	public List<Id<Person>> readAgentFromTxtFile(final String idsFilePath  ){
		//reads idFilePaths
		List<Id<Person>> idList = new ArrayList<Id<Person>>();
		try {
		    BufferedReader in = new BufferedReader(new FileReader(idsFilePath));
		    String str_row;
		    while ((str_row = in.readLine()) != null) {
		    	idList.add(Id.create(str_row, Person.class));
		    }
		    in.close();
		} catch (IOException e) {
		}
		
		return idList;
	}
	
	public static void main(String[] args) {
		

	}

}

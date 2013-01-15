package playground.anhorni.csestimation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

public class Writer {
	
	public void write(Population population, TreeMap<Id, ShopLocation> shops, String personsFile, String shopsFile) {
		this.writePersons(population, shops, personsFile);
		this.writeShops(shops, shopsFile);
	}
	
	private void writePersons(Population population, TreeMap<Id, ShopLocation> shops, String personsFile) {
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(personsFile)); 
			
			for (Person p:population.getPersons().values()) {
				EstimationPerson person = (EstimationPerson)p;
				
				if (!person.getShoppingTrips().isEmpty()) {				
					bufferedWriter.write(person.getId() + "\t");
					bufferedWriter.write(person.getAge() + "\t");
					bufferedWriter.write(person.getHhIncome() + "\t");
					
					// TODO: trips and choice rausfummeln
					int choice = -99;				
					bufferedWriter.write(choice + "\t");
					bufferedWriter.write("\n");
				}
				
			}				
			bufferedWriter.flush();
		    bufferedWriter.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writeShops(TreeMap<Id, ShopLocation> shops, String shopsFile) {
		
	}
}

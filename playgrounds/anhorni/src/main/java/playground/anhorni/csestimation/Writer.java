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
				
				for (ShoppingTrip st:person.getShoppingTrips()) {
					bufferedWriter.write(person.getId() + ",");
					bufferedWriter.write(person.getAge() + ",");
					bufferedWriter.write(person.getHhIncome() + ",");				
					bufferedWriter.write(st.getShop().getId() + ","); // choice
					bufferedWriter.write(st.getStart().getX() + "," + st.getStart().getY() + ",");
					bufferedWriter.write(st.getEnd().getX() + "," + st.getEnd().getY() + ",");
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
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(shopsFile)); 			
			for (ShopLocation shop:shops.values()) {
				bufferedWriter.write(shop.getId() + ",");
				bufferedWriter.write(shop.getCoord().getX() + "," + shop.getCoord().getX() + ",");
				bufferedWriter.write(shop.getSize() + ",");
				bufferedWriter.write(shop.getPrice() + ",");
				bufferedWriter.write("\n");				
			}				
			bufferedWriter.flush();
		    bufferedWriter.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}

package playground.anhorni.csestimation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

public class Writer {
		
	public void write(Population population, TreeMap<Id<Location>, ShopLocation> shops, String personsFile, String shopsFile, String bzFile) {
		this.writePersons(population, shops, personsFile);
		this.writeShops(shops, shopsFile, bzFile);
	}
	
	private void writePersons(Population population, TreeMap<Id<Location>, ShopLocation> shops, String personsFile) {
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(personsFile)); 
			
			for (Person p:population.getPersons().values()) {
				EstimationPerson person = (EstimationPerson)p;
				
				for (ShoppingTrip st:person.getShoppingTrips()) {
					bufferedWriter.write(person.getId() + ",");
					bufferedWriter.write(person.getAge() + ",");
					bufferedWriter.write(person.getHhIncome() + ",");				
					bufferedWriter.write(st.getShop().getId() + ","); // choice
					Coord coordStartTrafo = st.getStartCoord();	
					Coord coordEndTrafo = st.getEndCoord();
					bufferedWriter.write(coordStartTrafo.getX() + "," + coordStartTrafo.getY() + ",");
					bufferedWriter.write(coordEndTrafo.getX() + "," + coordEndTrafo.getY() + ",");
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
	
	public void writeShops(TreeMap<Id<Location>, ShopLocation> shops, String shopsFile, String bzFile) {
		
		ShopsEnricher enricher = new ShopsEnricher();
		enricher.enrich(shops, bzFile);
		
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(shopsFile)); 			
			for (ShopLocation shop:shops.values()) {
				bufferedWriter.write(shop.getId() + ",");
				Coord coordtrafo = shop.getCoord();
				bufferedWriter.write(coordtrafo.getX() + "," + coordtrafo.getY() + ",");
				bufferedWriter.write(shop.getSize() + ",");
				bufferedWriter.write(shop.getPrice() + "\n");
			}				
			bufferedWriter.flush();
		    bufferedWriter.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}

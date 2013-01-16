package playground.anhorni.csestimation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;

public class Writer {
	
	private WGS84toCH1903LV03 trafo = new WGS84toCH1903LV03();
	
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
					Coord coordStartTrafo = this.trafo.transform(st.getStart());	
					Coord coordEndTrafo = this.trafo.transform(st.getEnd());
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
	
	public void writeShops(TreeMap<Id, ShopLocation> shops, String shopsFile) {
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(shopsFile)); 			
			for (ShopLocation shop:shops.values()) {
				bufferedWriter.write(shop.getId() + ",");
				Coord coordtrafo = this.trafo.transform(shop.getCoord());
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

package playground.andreas.utils.dummy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.core.gbl.MatsimRandom;

public class dummyDataCreator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File("dummy.dat")));
			
			writer.write("person id; cars; bikes; ships; busses; airplanes; trains");
			writer.newLine();
			
			for (int i = 1; i <= 2000000; i++) {
				
				double cars = (int) (MatsimRandom.getRandom().nextDouble() * 10);
				double bikes = (int) (MatsimRandom.getRandom().nextDouble() * 20);
				double ships = (int) (MatsimRandom.getRandom().nextDouble() * 5);
				double busses = (int) (MatsimRandom.getRandom().nextDouble() * 4);
				double airplanes = (int) (MatsimRandom.getRandom().nextDouble() * 2);
				double trains = (int) (MatsimRandom.getRandom().nextDouble() * 2);
				
				writer.write(i + "; " + cars + "; " + bikes + "; " + ships + "; " + busses + "; " + airplanes + "; " + trains);
				writer.newLine();
				
			}
			
			writer.flush();
			writer.close();
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		

	}

}

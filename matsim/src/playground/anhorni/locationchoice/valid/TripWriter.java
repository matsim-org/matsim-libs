package playground.anhorni.locationchoice.valid;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import playground.anhorni.locationchoice.preprocess.analyzeMZ.PersonTrips;

public class TripWriter {
	
	private final static Logger log = Logger.getLogger(TripWriter.class);
	
	public void write(TreeMap<Id, PersonTrips> personTrips, String region) {	
		String header = "Person_id\tStart_X\tStart_Y\tEnd_X\tEnd_Y\tDist\texact_type\n";
		try {
			BufferedWriter outShopping = 
				IOUtils.getBufferedWriter("output/valid/"+ region + "_shoppingTrips.txt");
			BufferedWriter outLeisure = 
				IOUtils.getBufferedWriter("output/valid/" + region + "_leisure.txt");
			
			outShopping.write(header);
			outLeisure.write(header);
			
			Iterator<PersonTrips> personTrips_it = personTrips.values().iterator();
			while (personTrips_it.hasNext()) {
				PersonTrips pt = personTrips_it.next();
				pt.finish();			
				String out = pt.getPersonId().toString() + "\t";
				for (int i = 0; i < pt.getShoppingTrips().size(); i++) {
					CoordImpl coordStart = pt.getShoppingTrips().get(i).getCoordStart();
					CoordImpl coordEnd = pt.getShoppingTrips().get(i).getCoordEnd();
					
					outShopping.write(out + coordStart.getX() + "\t"+ coordStart.getY() + "\t" 
							+ coordEnd.getX() + "\t" + coordEnd.getY() + "\t" +
							coordStart.calcDistance(coordEnd) +  "\t" +
							pt.getShoppingTrips().get(i).getPurposeCode() + "\n");
				}

				for (int i = 0; i < pt.getLeisureTrips().size(); i++) {
					CoordImpl coordStart = pt.getLeisureTrips().get(i).getCoordStart();
					CoordImpl coordEnd = pt.getLeisureTrips().get(i).getCoordEnd();
					
					outLeisure.write(out + coordStart.getX() + "\t" +  coordStart.getY() + "\t" + 
							coordEnd.getX() +"\t" + coordEnd.getY() +"\t" + 
							coordStart.calcDistance(coordEnd)  + "\t" +
							pt.getLeisureTrips().get(i).getPurposeCode() + "\n");
				}			
			}
			outShopping.flush();
			outLeisure.flush();
			
			outShopping.close();	
			outLeisure.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

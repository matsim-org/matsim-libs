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
		String header = "Person_id\tStart_X\tStart_Y\tShop_X\tShop_Y\tEnd_X\tEnd_Y\tDist_0\tDist_1\n";
		try {
			BufferedWriter outShoppingIntermediate = 
				IOUtils.getBufferedWriter("output/valid/"+ region + "_shoppingIntermediate.txt");
			BufferedWriter outShoppingRoundTrip = 
				IOUtils.getBufferedWriter("output/valid/" + region + "_shoppingRoundTrip.txt");
			BufferedWriter outLeisure = 
				IOUtils.getBufferedWriter("output/valid/" + region + "_leisure.txt");
			
			outShoppingIntermediate.write(header);
			outShoppingRoundTrip.write(header);
			outLeisure.write(header);
			
			Iterator<PersonTrips> personTrips_it = personTrips.values().iterator();
			while (personTrips_it.hasNext()) {
				PersonTrips pt = personTrips_it.next();
				pt.finish();			
				String out = pt.getPersonId().toString() + "\t";
				for (int i = 0; i < pt.getIntermediateShoppingTrips().size(); i=i+2) {
					CoordImpl coordStart = pt.getIntermediateShoppingTrips().get(i).getCoordStart();
					CoordImpl coordShop = pt.getIntermediateShoppingTrips().get(i).getCoordEnd();
					CoordImpl coordEnd = pt.getIntermediateShoppingTrips().get(i+1).getCoordEnd();
					
					outShoppingIntermediate.write(out + coordStart.getX() + "\t"+ coordStart.getY() + "\t" 
							+ coordShop.getX() + "\t" +  coordShop.getY() + "\t" 
							+ coordEnd.getX() + "\t" + coordEnd.getY() + "\t" +
							coordStart.calcDistance(coordShop) + "\t" + coordShop.calcDistance(coordEnd) +"\n");
				}
				for (int i = 0; i < pt.getRoundTripShoppingTrips().size(); i=i+2) {
					CoordImpl coordStart = pt.getRoundTripShoppingTrips().get(i).getCoordStart();
					CoordImpl coordShop = pt.getRoundTripShoppingTrips().get(i).getCoordEnd();
					CoordImpl coordEnd = pt.getRoundTripShoppingTrips().get(i+1).getCoordEnd();
					
					outShoppingRoundTrip.write(out + coordStart.getX() + "\t" + coordStart.getY() + "\t" + 
							coordShop.getX() + "\t" + coordShop.getY() + "\t" +
							coordEnd.getX() + "\t" + coordEnd.getY() + "\t" +
							coordStart.calcDistance(coordShop) + "\t" + coordShop.calcDistance(coordEnd) +"\n");
				}
				for (int i = 0; i < pt.getLeisureShoppingTrips().size(); i=i+2) {
					CoordImpl coordStart = pt.getLeisureShoppingTrips().get(i).getCoordStart();
					CoordImpl coordShop = pt.getLeisureShoppingTrips().get(i).getCoordEnd();
					CoordImpl coordEnd = pt.getLeisureShoppingTrips().get(i+1).getCoordEnd();
					
					outLeisure.write(out + coordStart.getX() + "\t" +  coordStart.getY() + "\t" + 
							coordShop.getX() + "\t" +  coordShop.getY() + "\t" + 
							coordEnd.getX() +"\t" + coordEnd.getY() +"\t" + 
							coordStart.calcDistance(coordShop) + "\t" + coordShop.calcDistance(coordEnd) +"\n");
				}			
			}
			outShoppingIntermediate.flush();
			outShoppingRoundTrip.flush();
			outLeisure.flush();
			
			outShoppingIntermediate.close();	
			outShoppingRoundTrip.close();
			outLeisure.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

package playground.anhorni.locationchoice.preprocess.analyzeMZ;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.TreeMap;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

public class TripWriter {
		
	public void write(TreeMap<Id, PersonTrips> personTrips, String region, String outpath) {	
		String header = "Person_id\tStart_X\tStart_Y\tEnd_X\tEnd_Y\tDist(km)\texact_type\n";
		try {
			BufferedWriter outShopping = 
				IOUtils.getBufferedWriter(outpath + region + "_shoppingTrips.txt");
			BufferedWriter outLeisure = 
				IOUtils.getBufferedWriter(outpath + region + "_leisure.txt");
			
			outShopping.write(header);
			outLeisure.write(header);
			
			DecimalFormat formatter = new DecimalFormat("0.000");
			
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
							formatter.format(coordStart.calcDistance(coordEnd)/1000.0) +  "\t" +
							pt.getShoppingTrips().get(i).getPurposeCode() + "\n");
				}

				for (int i = 0; i < pt.getLeisureTrips().size(); i++) {
					CoordImpl coordStart = pt.getLeisureTrips().get(i).getCoordStart();
					CoordImpl coordEnd = pt.getLeisureTrips().get(i).getCoordEnd();
					
					outLeisure.write(out + coordStart.getX() + "\t" +  coordStart.getY() + "\t" + 
							coordEnd.getX() +"\t" + coordEnd.getY() +"\t" + 
							formatter.format(coordStart.calcDistance(coordEnd)/1000.0) + "\t" +
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

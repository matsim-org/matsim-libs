package playground.qvanheerden.freight;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.utilities.Header;

public class MyDemandGenerator {
	public static Logger log = Logger.getLogger(MyDemandGenerator.class);
	/**
	 * This class will create a csv file containing shipments that will be 
	 * read in when running the {@link MyCarrierPlanGenerator}.
	 * 
	 * For now only generates two customers with a shipment each.  Will eventually 
	 * contain a demand generation model.
	 * 
	 * @param args
	 * 
	 * @author qvanheerden
	 */
	public static void main(String[] args) {
		Header.printHeader(MyDemandGenerator.class.toString(), args);
		
		String outputFile = args[0];
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		
		/* For now just write two shipments to work with
		 * - not all these fields are used in the model, but I may want to use 
		 * 	 some of the values eventually for my own analyses.
		 */
		try {
			bw.write("customer,long,lat,product,mass,sale,duration,start,end");
			bw.newLine();
			bw.write("customer_1,148340.842,-3708473.484,product_1,2500,392,500,0,86400");
			bw.newLine();
			bw.write("customer_2,140308.0297,-3704398.509,product_2,1000,577.5,500,0,86400");
			
		} catch (IOException e) {
			log.error("Could not write to file " + outputFile);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				log.error("Could not close writer.");
			}
		}

		Header.printFooter();
	}
}
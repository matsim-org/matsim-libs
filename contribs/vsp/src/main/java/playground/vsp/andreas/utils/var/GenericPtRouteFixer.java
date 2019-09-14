package playground.vsp.andreas.utils.var;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.routes.ExperimentalTransitRoute;

/**
 * 
 * Old routed plans may contain pt routes of type generic.
 * This fix substitutes them by the proper {@link ExperimentalTransitRoute} route type.
 * Note that the current route type cannot be read from {@link ExperimentalTransitRoute}.
 * Hence there are no automatic updates. 
 * 
 * @author aneumann
 *
 */
public class GenericPtRouteFixer {

	private final static Logger log = Logger.getLogger(GenericPtRouteFixer.class);
	
	/**
	 * The route type in {@link ExperimentalTransitRoute} is not visible. 
	 */
	private static String newRouteType = "experimentalPt1";
	private static String oldRouteType = "generic";
	private static String searchPattern = ">PT1===";
	
	public static void fixPlansFile(String inFile, String outFile){
		
		if (inFile.equalsIgnoreCase(outFile)) {
			log.warn("In file and out file are the same. Aborting!");
		} else {
			log.info("Reading from " + inFile);
			log.info("Writing to " + outFile);
			log.info("Substituting all occurences of \"" + GenericPtRouteFixer.oldRouteType + "\" by \"" + GenericPtRouteFixer.newRouteType + "\" in lines containing the pattern \"" + GenericPtRouteFixer.searchPattern + "\".");
			
			long counter = 0;
			long nextCounterMsg = 1;
			
			long substitutions = 0;
			
			try {
				BufferedReader reader = IOUtils.getBufferedReader(inFile);;
				BufferedWriter writer = IOUtils.getBufferedWriter(outFile);;

				String line;
				while ((line = reader.readLine()) != null) {
					
					counter++;
					if (counter == nextCounterMsg) {
						nextCounterMsg *= 2;
						log.info(" line # " + counter);
					}
					
					// read one line
					if (line.contains(GenericPtRouteFixer.searchPattern)) {
						// this is a pt route and not a e.g. transit walk
						line = line.replace(GenericPtRouteFixer.oldRouteType, GenericPtRouteFixer.newRouteType);
						substitutions++;
					} else {
						// this is some other line, e.g. transit walk, car route, xml
					}
					
					writer.write(line); writer.newLine();
				}

				reader.close();
				writer.flush();
				writer.close();

				log.info("Substituted " + substitutions + " out of " + counter + " lines");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		GenericPtRouteFixer.fixPlansFile(args[0], args[1]);
	}
}

package playground.southafrica.projects.digicore.aggregation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.projects.digicore.DigicoreUtils;
import playground.southafrica.utilities.Header;

/**
 * Aggregates 5Hz acceleration and speed data to 1Hz my taking the the mean
 * values of each of the 5Hz fields, namely x, y and z-acceleration as well as
 * speed. 
 *
 * @author jwjoubert
 */
public class MaxVector1HzDigiAggregator implements DigiAggregator {
	private int maxLines = Integer.MAX_VALUE;
	
	public MaxVector1HzDigiAggregator() {
	}

	@Override
	public void aggregate(String input, String output) {
		LOG.info("Aggregating records by second...");
		Counter counter = new Counter("   lines # ");
		Map<String,Map<String,List<String>>> mapDynamic = new TreeMap<>();
		Map<String,Map<String,String>> mapStatic = new TreeMap<>();

		Map<String, String> hashToPerson = new HashMap<>();
		Map<String, String> personToHash = new HashMap<>();

		BufferedReader br = IOUtils.getBufferedReader(input);
		try{
			String line = null;
			while( (line = br.readLine()) != null && counter.getCounter() < maxLines){
				String[] sa = line.split(",");
				String idString = sa[1];

				/* Convert the person Id to a simpler integer. */
				if(!hashToPerson.containsKey(idString)){
					String id = new Integer(hashToPerson.size()).toString();
					hashToPerson.put(idString, id);
					personToHash.put(id, idString);
				}
				String id = hashToPerson.get(idString);

				String dateString = DigicoreUtils.getDateToSecondsSince1996(Long.parseLong(sa[2]));

				/* Split string into its static and dynamic components:
				 * Static:
				 *    sa[3]: longitude;
				 *    sa[4]: latitude;
				 *    sa[9]: road type;
				 *   sa[10]: historic road speed;
				 *   sa[11]: speed limit.
				 * 
				 * Dynamic:
				 * 	  sa[5]: x-acceleration;
				 * 	  sa[6]: y-acceleration;
				 *    sa[7]: z-acceleration;
				 *    sa[8]: speed
				 */
				String staticString = String.format("%s,%s,%s,%s,%s", sa[3], sa[4], sa[9], sa[10], sa[11]);
				String dynamicString = String.format("%s,%s,%s,%s", sa[5], sa[6], sa[7], sa[8]);

				/* Handle the static data. */
				if(!mapStatic.containsKey(dateString)){
					Map<String, String> personMapStatic = new TreeMap<>();
					mapStatic.put(dateString, personMapStatic);					
				}
				Map<String, String> dateMapStatic = mapStatic.get(dateString);
				if(!dateMapStatic.containsKey(id)){
					dateMapStatic.put(id, staticString);
				}

				/* Handle the dynamic data. */
				if(!mapDynamic.containsKey(dateString)){
					Map<String, List<String>> personMapDynamic = new TreeMap<>();
					mapDynamic.put(dateString, personMapDynamic);
				}
				Map<String,List<String>> dateMapDynamic = mapDynamic.get(dateString);
				if(!dateMapDynamic.containsKey(id)){
					List<String> personList = new ArrayList<>();
					dateMapDynamic.put(id, personList);
				}
				List<String> list = dateMapDynamic.get(id);
				list.add(dynamicString);
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + input);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + input);
			}
		}
		counter.printCounter();

		LOG.info("Writing aggregation (mean acceleration and speed) to file.");
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		int recordIndex = 0;
		int[] counts = {0,0,0,0,0,0};
		try{
			for(String date : mapDynamic.keySet()){
				for(String person : mapDynamic.get(date).keySet()){
					List<String> list = mapDynamic.get(date).get(person);

					/* Just count how many records. */
					int i = list.size();
					int oldValue = counts[i];
					counts[i] = oldValue+1;

					/* Aggregate by using the record of the largest vector,
					 * measured from the origin (0,0,1000) */
					double maxVector = Double.NEGATIVE_INFINITY;
					String maxString = null;
					
					Iterator<String> it = list.iterator();
					while(it.hasNext()){
						String record = it.next();
						String[] sa = record.split(",");
						double x = Double.parseDouble(sa[0]);
						double y = Double.parseDouble(sa[1]);
						double z = Double.parseDouble(sa[2]);
						
						/* Calculate the vector. */
						double v = Math.sqrt( 	Math.pow(x - 0.0, 2.0) + Math.pow(y - 0.0, 2.0) + Math.pow(z - 1000.0, 2.0));
						if(v > maxVector){
							maxVector = v;
							maxString = record;
						}
					}
					
					/* Break up the static link into its components again. This
					 * ensures that the final records written are the exact same
					 * layout as the input file. */
					String[] sa = mapStatic.get(date).get(person).split(",");
					String lon = sa[0];
					String lat = sa[1];
					String road = sa[2];
					String hist = sa[3];
					String limit = sa[4];
					
					/* Break up the largest vector into its components. */
					String[] maxSa = maxString.split(",");
					String maxX = maxSa[0];
					String maxY = maxSa[1];
					String maxZ = maxSa[2];
					String maxSpeed = maxSa[3];
					
					/* Convert time back to milliseconds since 1996. */
					String dateMs = String.valueOf(DigicoreUtils.getLongSince1996FromSeconds(date));

					/* Write the aggregate records to file. */
					try{
						bw.write(String.format("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", 
								recordIndex++, personToHash.get(person), dateMs, lon, lat, 
								maxX, maxY, maxZ, maxSpeed, 
								road, hist, limit));
						bw.newLine();
					} catch (IOException e) {
						e.printStackTrace();
						throw new RuntimeException("Cannot write to from " + output);
					}
				}
			}
		} finally{
			try {
				br.close();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + input);
			}
		}
		for(int i = 0; i < 6; i++){
			LOG.info("Counts:");
			LOG.info(String.format("  %d: %d", i, counts[i]));
		}
	}
	
	
	/**
	 * Set the maximum number of lines to read from the input file.
	 * 
	 * @param maxNumberOfLines
	 */
	public void setMaxLines(int maxNumberOfLines){
		this.maxLines = maxNumberOfLines;
	}

	
	public static void main(String[] args) {
		Header.printHeader(MaxVector1HzDigiAggregator.class.toString(), args);
		
		MaxVector1HzDigiAggregator ma = new MaxVector1HzDigiAggregator();
		
		String input = args[0];
		String output = args[1];
		if(args.length == 3){
			ma.setMaxLines(Integer.parseInt(args[2]));
		}
		
		ma.aggregate(input, output);
		
		Header.printFooter();
	}

}

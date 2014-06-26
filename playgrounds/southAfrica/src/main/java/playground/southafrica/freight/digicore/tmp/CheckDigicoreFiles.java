package playground.southafrica.freight.digicore.tmp;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.utilities.Header;

public class CheckDigicoreFiles {
	private final static Logger LOG = Logger.getLogger(CheckDigicoreFiles.class); 

	public static void main(String[] args) {
		Header.printHeader(CheckDigicoreFiles.class.toString(), args);
		
		String filename = args[0];

		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;
		Counter counter = new Counter("  lines # ");
		
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = br.readLine();
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				Long l = Long.parseLong(sa[0]);
				min = Math.min(min, l);
				max = Math.max(max, l);
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		counter.printCounter();
		
		GregorianCalendar first = new GregorianCalendar(TimeZone.getTimeZone("GMT+2"));
		first.setTimeInMillis(min*1000);

		GregorianCalendar last = new GregorianCalendar(TimeZone.getTimeZone("GMT+2"));
		last.setTimeInMillis(max*1000);
		
		
		LOG.info("First date: " + getPrettyDate(first));
		LOG.info("Last date: " + getPrettyDate(last));
		
		Header.printFooter();
	}
	
	private static String getPrettyDate(GregorianCalendar gc){
		int year = gc.get(Calendar.YEAR);
		int month = gc.get(Calendar.MONTH)+1; // Seems to be a java thing that month is started at 0... 
		int day = gc.get(Calendar.DAY_OF_MONTH);
		
		return String.format("%04d%02d%02d", year, month, day);
	}

}

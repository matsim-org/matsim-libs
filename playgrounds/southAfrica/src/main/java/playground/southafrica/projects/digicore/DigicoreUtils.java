package playground.southafrica.projects.digicore;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

public class DigicoreUtils {
	final private static Logger LOG = Logger.getLogger(DigicoreUtils.class);
	
	public static String getDateSince1996(Long l){
		/* Get the epoch offset: 1 Jan 1996 */
		long offset = 820447200000l;
		
		Calendar c = new GregorianCalendar(TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		c.setTimeInMillis(l + offset);
		String date = String.format("%d/%02d/%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH));
		String time = String.format("%02d:%02d:%02d.%03d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND), c.get(Calendar.MILLISECOND));
		String full = date + " " + time;
		return full;
	}
	
	public static void main(String[] args) throws IOException{
		BufferedReader br = IOUtils.getBufferedReader("/home/jwjoubert/workspace/JoubertEtAl2014a-accelerometerProfiles-Manuscript/data/TwentyLines.csv");
		try{
			String line = null;
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				long date1996 = Long.parseLong(sa[2]);
				LOG.info(DigicoreUtils.getDateSince1996(date1996));
			}
		} finally{
			br.close();
		}
	}
	
	
	
}

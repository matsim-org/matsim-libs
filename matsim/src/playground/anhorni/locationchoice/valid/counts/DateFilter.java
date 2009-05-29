package playground.anhorni.locationchoice.valid.counts;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
//import org.apache.log4j.Logger;

public class DateFilter {
	
	GregorianCalendar calendar = new GregorianCalendar();
	//private final static Logger log = Logger.getLogger(DateFilter.class);
	
	public List<RawCount> filter(List<RawCount> rawCounts) {		
		List<RawCount> filteredRawCounts = new Vector<RawCount>();	
		Iterator<RawCount> rawCount_it = rawCounts.iterator();
		while (rawCount_it.hasNext()) {
			RawCount rawCount = rawCount_it.next();
		
			// filter DI-DO
			// filter summer holidays
			// filter public holidays			
			if (this.isDIDO(rawCount) && !this.inSummerHolidays(rawCount) && !this.isPublicHoliday(rawCount)) {
				filteredRawCounts.add(rawCount);	
			}
		}
		return filteredRawCounts;
	}
	
	// SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
	// 1		2		3		4		   5		6		7
	private boolean isDIDO(RawCount count) {
		
		/*
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		try {
			Date date = (Date) format.parse(count.getYear() + "–" + count.getMonth() + "-" + count.getDay());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		*/
				
	    calendar.set(count.getYear(), count.getMonth() -1, count.getDay());
	    int weekday = calendar.get(Calendar.DAY_OF_WEEK);
	    calendar.clear();
	    	    
	    if (weekday >= 3 && weekday <= 5) return true;
	    else return false; 
	}
	
	private boolean inSummerHolidays(RawCount count) {
		if ((count.getMonth()== 7 && count.getDay() > 15) || (count.getMonth()== 8 && count.getDay() < 16)) return true;
		else return false;
	}
	
	private boolean isPublicHoliday(RawCount count) {
		
		// 2004: DI-DO
		if (count.getYear() == 2004 && count.getMonth()== 1 && count.getDay()== 1) {
			return true; // Neujahrstag
		}
		if (count.getYear() == 2004 && count.getMonth()== 5 && count.getDay()== 20) {
			return true; // Auffahrt
		}
		
		// 2005: DI-DO!
		if (count.getYear() == 2005 && count.getMonth()== 5 && count.getDay()== 5) {
			return true; // Auffahrt
		}
		
		// 2006: DI-DO!
		if (count.getYear() == 2006 && count.getMonth()== 5 && count.getDay()== 25) {
			return true; // Auffahrt
		}
		if (count.getYear() == 2006 && count.getMonth()== 8 && count.getDay()== 1) {
			return true; // 1. August
		}
		if (count.getYear() == 2006 && count.getMonth()== 12 && count.getDay()== 26) {
			return true; // Stephanstag
		}
		else return false;
	}
}

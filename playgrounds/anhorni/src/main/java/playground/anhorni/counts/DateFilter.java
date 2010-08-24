package playground.anhorni.counts;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

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
//			if (this.isDIDO(rawCount) && !this.inSummerHolidays(rawCount) && !this.isPublicHoliday(rawCount)) {
//				filteredRawCounts.add(rawCount);	
//			}
			
			if (this.isMOFRI(rawCount) && !this.inSummerHolidays(rawCount) && !this.isPublicHoliday(rawCount)) {
				filteredRawCounts.add(rawCount);	
			}
			
		}
		return filteredRawCounts;
	}
	
	// SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
	// 1		2		3		4		   5		6		7
	private boolean isDIDO(RawCount count) {	
	    calendar.set(count.getYear(), count.getMonth() -1, count.getDay());
	    int weekday = calendar.get(Calendar.DAY_OF_WEEK);
	    calendar.clear();
	    	    
	    if (weekday >= 3 && weekday <= 5) return true;
	    else return false; 
	}
	
	private boolean isMOFRI(RawCount count) {
	 	calendar.set(count.getYear(), count.getMonth() -1, count.getDay());
	    int weekday = calendar.get(Calendar.DAY_OF_WEEK);
	    calendar.clear();
	    	    
	    if (weekday >= 2 && weekday <= 6) return true;
	    else return false; 
	}
	
	private boolean inSummerHolidays(RawCount count) {
		if ((count.getMonth()== 7 && count.getDay() > 15) || (count.getMonth()== 8 && count.getDay() < 16)) return true;
		else return false;
	}
	
	private boolean isPublicHoliday(RawCount count) {
		
		if (count.getYear() == 2004 || count.getYear() == 2005 || count.getYear() == 2006) {
			if (count.getMonth()== 4 && count.getDay()== 9) {
				return true; // Karfreitag
			}
			if (count.getMonth()== 4 && count.getDay()== 12) {
				return true; // Ostermontag
			}
			if (count.getMonth()== 5 && count.getDay()== 31) {
				return true; // Pfingstmontag
			}
		}		
		// 2004:MO-FR
		if (count.getYear() == 2004) {
			if (count.getMonth()== 1 && count.getDay()== 1) {
				return true; // Neujahrstag
			}
			if (count.getMonth()== 1 && count.getDay()== 2) {
				return true; // Berchtoldstag
			}
			if (count.getMonth()== 5 && count.getDay()== 20) {
				return true; // Auffahrt
			}
			if (count.getMonth()== 12 && count.getDay()== 31) {
				return true; // Silvester
			}
		}		
		// 2005: 
		if (count.getYear() == 2005) {
			if (count.getMonth()== 5 && count.getDay()== 5) {
				return true; // Auffahrt
			}
			if (count.getMonth()== 8 && count.getDay()== 1) {
				return true; // 1. August
			}
			if (count.getMonth()== 12 && count.getDay()== 26) {
				return true; // Stephanstag
			}
		}		
		// 2006:
		if (count.getYear() == 2006) {
			if (count.getMonth()== 1 && count.getDay()== 2) {
				return true; // Berchtoldstag
			}
			if (count.getMonth()== 5 && count.getDay()== 1) {
				return true; // 1. Mai
			}
			if (count.getMonth()== 5 && count.getDay()== 25) {
				return true; // Auffahrt
			}
			if (count.getMonth()== 8 && count.getDay()== 1) {
				return true; // 1. August
			}
			if (count.getMonth()== 12 && count.getDay()== 25) {
				return true; // Weihnachten
			}
			if (count.getMonth()== 12 && count.getDay()== 26) {
				return true; // Stephanstag
			}
		}
		return false;
	}
}

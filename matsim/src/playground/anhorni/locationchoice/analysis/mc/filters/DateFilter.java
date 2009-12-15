package playground.anhorni.locationchoice.analysis.mc.filters;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class DateFilter {
	
	GregorianCalendar calendar = new GregorianCalendar();
	
	public boolean passedFilter(String y, String m, String d) {		
			// filter DI-DO
			// filter summer holidays
			// filter public holidays	
			int year = (int)Integer.parseInt(y);
			int month = Integer.parseInt(m);
			int day = Integer.parseInt(d);
		
			// removing additional filters for comparison with plan-based MC
			// if (this.isMOFRI(year, month, day) && !this.inSummerHolidays(year, month, day) && !this.isPublicHoliday(year, month, day)) {
			if (this.isMOFRI(year, month, day)) {
				return true;	
			}
			return false;
	}
	
	
	// SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
	// 1		2		3		4		   5		6		7
	// calendar.set(...)
	/*@param year the value used to set the <code>YEAR</code> calendar field.
    * @param month the value used to set the <code>MONTH</code> calendar field.
    * Month value is 0-based. e.g., 0 for January.
    * @param date the value used to set the <code>DAY_OF_MONTH</code> calendar field.*/
	
	
	private boolean isDIDO(int year, int month, int day) {			
	    calendar.set(year, month -1 , day);
	    int weekday = calendar.get(Calendar.DAY_OF_WEEK);
	    calendar.clear();
	    	    
	    if (weekday >= 3 && weekday <= 5) return true;
	    else return false; 
	}
	
	private boolean isMOFRI(int year, int month, int day) {			
	    calendar.set(year, month -1 , day);
	    int weekday = calendar.get(Calendar.DAY_OF_WEEK);
	    calendar.clear();
	    	    
	    if (weekday >= 2 && weekday <= 6) return true;
	    else return false; 
	}
	
	private boolean inSummerHolidays(int year, int month, int day) {
		if ((month== 7 && day > 15) || 
				(month== 8 && day < 16)) return true;
		else return false;
	}
	
	private boolean isPublicHoliday(int year, int month, int day) {				
		// 2005: DI-DO!
		if (year == 2005 && month== 5 && day== 5) {
			return true; // Auffahrt
		}
		else return false;
	}
}

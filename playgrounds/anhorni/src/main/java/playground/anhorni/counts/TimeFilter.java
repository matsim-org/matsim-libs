package playground.anhorni.counts;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class TimeFilter {
	
	GregorianCalendar calendar = new GregorianCalendar();
	//private final static Logger log = Logger.getLogger(DateFilter.class);
	
	String dayFilter = "MOFRI";
	int monthFilter = -1;
	private boolean removeSummerHolidays = false;
	private boolean removeXmasDays = false;
	
	public String getDayFilter() {
		return dayFilter;
	}
	
	public void setDayFilter(String dayFilter) {
		this.dayFilter = dayFilter;
	}
	
	public void setMonthFilter(int monthFilter) {
		this.monthFilter = monthFilter;
	}

	public void setSummerHolidaysFilter(boolean removeSummerHolidays) {
		this.removeSummerHolidays = removeSummerHolidays;
	}
	
	public void setXmasDays(boolean removeXmasDays) {
		this.removeXmasDays = removeXmasDays;
	}
	
	public List<RawCount> filter(List<RawCount> rawCounts) {
		List<RawCount> filteredRawCounts = new Vector<RawCount>();	
		Iterator<RawCount> rawCount_it = rawCounts.iterator();
		while (rawCount_it.hasNext()) {
			RawCount rawCount = rawCount_it.next();
		
			// filter DI-DO
			// filter summer holidays
			// filter public holidays
			if (dayFilter.equals("DIDO") && this.isDIDO(rawCount) ) {
				if (!this.removeSummerHolidays || !this.inSummerHolidays(rawCount)) {
					if (!this.isPublicHoliday(rawCount)) {
						if (!this.removeXmasDays || !this.isXmasDay(rawCount)) {
							if (monthFilter <= 0 || rawCount.month == this.monthFilter) {
								filteredRawCounts.add(rawCount);
							}
						}
					}
				}	
			}
			
			if (dayFilter.equals("MOFRI") && this.isMOFRI(rawCount) ) {
				if (!this.removeSummerHolidays ||  !this.inSummerHolidays(rawCount)) {
					if (!this.isPublicHoliday(rawCount)) {
						if (!this.removeXmasDays || !this.isXmasDay(rawCount)) {
							if (monthFilter <= 0 || rawCount.month == this.monthFilter) {
								filteredRawCounts.add(rawCount);
							}
						}
					}
				}	
			}
		}
		return filteredRawCounts;
	}
	
	private boolean isXmasDay(RawCount count) {
	    if (count.getDay() >= 24 && count.getMonth() == 12) return true;
	    else return false; 
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

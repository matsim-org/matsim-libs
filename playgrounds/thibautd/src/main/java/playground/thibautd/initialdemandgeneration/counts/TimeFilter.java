package playground.thibautd.initialdemandgeneration.counts;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Vector;

public class TimeFilter {
	
	private final GregorianCalendar calendar = new GregorianCalendar();
	
	private DayFilter dayFilter = new DayFilter( Day.MONDAY , Day.FRIDAY );
	private int monthFilter = -1;
	private boolean removeSummerHolidays = false;
	private boolean removeXmasDays = false;
	
	public DayFilter getDayFilter() {
		return dayFilter;
	}
	
	public void setDayFilter(final DayFilter dayFilter) {
		this.dayFilter = dayFilter;
	}

	public void setDayFilter(
			final Day start,
			final Day end) {
		setDayFilter( new DayFilter( start , end ) );
	}
	
	public void setMonthFilter(final int monthFilter) {
		this.monthFilter = monthFilter;
	}

	public void setSummerHolidaysFilter(final boolean removeSummerHolidays) {
		this.removeSummerHolidays = removeSummerHolidays;
	}
	
	public void setXmasDays(final boolean removeXmasDays) {
		this.removeXmasDays = removeXmasDays;
	}
	
	public List<RawCount> filter(final List<RawCount> rawCounts) {
		List<RawCount> filteredRawCounts = new Vector<RawCount>();	

		for (RawCount rawCount : rawCounts) {
			// filter DI-DO
			if ( isAcceptableDay(rawCount) ) {
				// filter summer holidays
				if (!this.removeSummerHolidays || !this.inSummerHolidays(rawCount)) {
					// filter public holidays
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
	
	private boolean isXmasDay(final RawCount count) {
	    return (count.getDay() >= 24 && count.getMonth() == 12);
	}
	
	// SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
	// 1		2		3		4		   5		6		7
	private boolean isAcceptableDay(final RawCount count) {	
	    calendar.set(count.getYear(), count.getMonth() -1, count.getDay());
	    int weekday = calendar.get(Calendar.DAY_OF_WEEK);
	    calendar.clear();
	    	    
		return dayFilter.retain( weekday );
	}
	
	private boolean inSummerHolidays(final RawCount count) {
		return ((count.getMonth()== 7 && count.getDay() > 15) ||
				(count.getMonth()== 8 && count.getDay() < 16));
	}
	
	private boolean isPublicHoliday(final RawCount count) {
		
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

	// /////////////////////////////////////////////////////////////////////////
	// classes
	// /////////////////////////////////////////////////////////////////////////
	public static enum Day {
		MONDAY( 2 ),
		TUESDAY( 3 ),
		WEDNESDAY( 4 ),
		THURSDAY( 5 ),
		FRIDAY( 6 ),
		SATURDAY( 7 ),
		SUNDAY( 1 );

		private final int index;

		private Day(final int i) {
			index = i;
		}
	}

	public static class DayFilter {
		private final Day start;
		private final Day end;

		public DayFilter(
				final Day start,
				final Day end) {
			this.start = start;
			this.end = end;
		}

		private boolean retain(final int day) {
			if (start.index <= end.index) {
				return day >= start.index && day <= end.index;
			}
			else {
				return day >= start.index || day <= end.index;
			}
		}
	}
}

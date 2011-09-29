package freight.utils;


public class TimeSchema {
	
	public static TimePeriod MONDAY = new TimePeriod(0.0, 24*3600);
	public static TimePeriod TUESDAY = new TimePeriod(24*3600, 2*24*3600);
	public static TimePeriod WEDNESDAY = new TimePeriod(2*24*3600, 3*24*3600);
	public static TimePeriod THURSDAY = new TimePeriod(3*24*3600, 4*24*3600);
	public static TimePeriod FRIDAY = new TimePeriod(4*24*3600, 5*24*3600);
	public static TimePeriod SATURDAY = new TimePeriod(5*24*3600, 6*24*3600);

}

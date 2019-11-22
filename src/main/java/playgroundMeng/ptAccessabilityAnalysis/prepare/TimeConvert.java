package playgroundMeng.ptAccessabilityAnalysis.prepare;

public class TimeConvert {
	public static double timeConvert(double time) {
		if(time>24*3600) {
			return time-24*3600;
		} else {
			return time;
		}
	}
}

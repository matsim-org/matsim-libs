package playground.acmarmol.Avignon;

import java.util.Arrays;
import java.util.List;

public final class MyTransportMode {

	public static final String car = "car";
	public static final String ride = "ride";
	public static final String bike = "bike";
	public static final String pt = "pt";
	public static final String walk = "walk";
	public static final String transit_walk = "transit_walk";
	public final static List<String> MODES = Arrays.asList("car","ride","bike","pt","walk","transit_walk");
	public final static List<String> PURPOSES = Arrays.asList("toWork", "toShop","toLeisure", "toEducation");

	private MyTransportMode() {
		// prevent creating instances of this class
	}
}

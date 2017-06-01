package playground.balac.test;

import org.matsim.core.config.ReflectiveConfigGroup;

public class BJActivityScoringConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "BJactivityscoring";
	
	private double slopeHome1 = 0.0;
	private double slopeHome2 = 0.0;
	
	public BJActivityScoringConfigGroup() {
		super(GROUP_NAME);
	}
	
	@StringGetter( "slopeHome1" )
	public double getSlopeHome1() {
		return slopeHome1;
	}
	
	@StringSetter( "slopeHome1" )
	public void setSlopeHome1(double slopeHome1) {
		this.slopeHome1 = slopeHome1;
	}
	
	@StringGetter( "slopeHome2" )
	public double getSlopeHome2() {
		return slopeHome2;
	}
	
	@StringSetter( "slopeHome2" )
	public void setSlopeHome2(double slopeHome2) {
		this.slopeHome2 = slopeHome2;
	}

}

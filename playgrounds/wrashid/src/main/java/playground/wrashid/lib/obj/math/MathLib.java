package playground.wrashid.lib.obj.math;

public class MathLib {

	public static boolean equals(double numberA, double numberB, double epsilon){
		return Math.abs(numberA-numberB)<Math.abs(epsilon);
	}
	
}

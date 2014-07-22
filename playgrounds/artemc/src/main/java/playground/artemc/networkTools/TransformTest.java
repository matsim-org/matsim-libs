package playground.artemc.networkTools;

import playground.artemc.networkTools.jcoord.LatLng;
import playground.artemc.networkTools.jcoord.UTMRef;

public class TransformTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		UTMRef utm1 = new UTMRef(682142.8335999995, 4820616.274800001, 'N', 14);
		System.out.println("UTM Reference: " + utm1.toString());
		LatLng ll3 = utm1.toLatLng();
		System.out.println("Converted to Lat/Long: " + ll3.toString());
		System.out.println();

	}

}

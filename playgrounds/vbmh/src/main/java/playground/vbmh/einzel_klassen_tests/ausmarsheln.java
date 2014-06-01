package playground.vbmh.einzel_klassen_tests;

import java.io.File;

import javax.xml.bind.JAXB;

import playground.vbmh.vmParking.ParkingMap;

public class ausmarsheln {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String filename="input/parkings_demo.xml";
		File file = new File( filename );
		ParkingMap karte = JAXB.unmarshal( file, ParkingMap.class );
		System.out.println(karte.getParkings().get(0).type);

	}

}

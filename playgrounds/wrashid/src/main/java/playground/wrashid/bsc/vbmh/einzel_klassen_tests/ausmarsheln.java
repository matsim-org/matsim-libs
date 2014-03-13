package playground.wrashid.bsc.vbmh.einzel_klassen_tests;

import java.io.File;

import javax.xml.bind.JAXB;

import playground.wrashid.bsc.vbmh.vm_parking.ParkingMap;

public class ausmarsheln {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String filename="input/parkings_demo.xml";
		File file = new File( filename );
		ParkingMap karte = JAXB.unmarshal( file, ParkingMap.class );
		System.out.println(karte.getParkings().get(0).type);

	}

}

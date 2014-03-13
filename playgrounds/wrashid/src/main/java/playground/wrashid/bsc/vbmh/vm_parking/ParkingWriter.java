package playground.wrashid.bsc.vbmh.vm_parking;



import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

/**
 * Does some jaxb magic to write parkings into a xml file
 * 
 * 
 * 
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */


public class ParkingWriter {
	public int write(ParkingMap parkingMap, String filename){
		try{
		File file = new File( filename );
		JAXBContext context = JAXBContext.newInstance( ParkingMap.class );
		Marshaller m = context.createMarshaller();
		m.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
		m.marshal( parkingMap, file );}
		catch (Exception e){
			System.out.println(e.toString());
			System.out.println(e.getMessage());
			System.out.println("Writing could not be finished");
			return 0;
			}
		return 1;
	}
}

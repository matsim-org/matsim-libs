package playground.wrashid.bsc.vbmh.vm_parking;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

public class Parking_writer {
	public int write(Parking_Map parking_map, String filename){
		try{
		File file = new File( filename );
		JAXBContext context = JAXBContext.newInstance( Parking_Map.class );
		Marshaller m = context.createMarshaller();
		m.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
		m.marshal( parking_map, file );}
		catch (Exception e){
			System.out.println(e.toString());
			System.out.println(e.getMessage());
			System.out.println("fucked up");
			return 0;
			}
		return 1;
	}
}

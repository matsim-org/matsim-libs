package playground.vbmh.vmParking;



import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

/**
 * Does some jaxb magic to write a parkingMap with all its parking lots into a XML file.
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

		File gisfile=new File (filename+".csv");
		FileWriter gisFileWriter = null;
		try {
			gisFileWriter = new FileWriter(gisfile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
		
		gisSchreiben(gisFileWriter, "ParkingID, X, Y, Type, FacActType, CapacityEV, CapacityNEV \n");
		for (Parking parking : parkingMap.getParkings()){
			gisSchreiben(gisFileWriter, parking.id+","+parking.getCoordinate().getX() + "," + parking.getCoordinate().getY()+ "," + parking.type + "," + parking.facilityActType + "," + parking.capacityEV + "," + parking.capacityNEV+"\n");


		}
		gisEnd(gisFileWriter);




		return 1;
	}

	public void gisEnd(FileWriter fwriter){
		try {
			fwriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	void gisSchreiben(FileWriter fwriter, String text){
		try {
			fwriter.write(text);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}




}

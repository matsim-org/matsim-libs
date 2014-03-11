package playground.wrashid.bsc.vbmh.vm_parking;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

/**
 * Writes Pricing Models using jaxb
 * 
 * 
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */


public class Pricing_Models_Writer {
	public int write(Pricing_Models models, String filename){
		try{
		File file = new File( filename );
		JAXBContext context = JAXBContext.newInstance( Pricing_Models.class );
		Marshaller m = context.createMarshaller();
		m.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
		m.marshal( models, file );}
		catch (Exception e){
			System.out.println(e.toString());
			System.out.println(e.getMessage());
			System.out.println("Writing could not be finished");
			return 0;
			}
		return 1;
	}
}

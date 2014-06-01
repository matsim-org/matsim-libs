package playground.vbmh.vmParking;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

/**
 * Writes Pricing Models into an XML file using jaxb
 * 
 * 
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */


public class PricingModelsWriter {
	public int write(PricingModels models, String filename){
		try{
		File file = new File( filename );
		JAXBContext context = JAXBContext.newInstance( PricingModels.class );
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

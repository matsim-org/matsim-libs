package playground.vbmh.vmEV;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;


public class EVListWriter {
	public int write(EVList evList, String filename){
		try{
			File file = new File( filename );
			JAXBContext context = JAXBContext.newInstance( EVList.class );
			Marshaller m = context.createMarshaller();
			m.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
			m.marshal( evList, file );
			
			}
		catch (Exception e){
			System.out.println(e.toString());
			System.out.println(e.getMessage());
			System.out.println("Writing could not be finished");
			return 0;
		}
		return 1;
	}
		
}

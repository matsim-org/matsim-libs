package org.matsim.contrib.matsim4urbansim.config;

import java.io.File;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.SchemaFactory;

import org.apache.log4j.Logger;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfigv3.Matsim4UrbansimConfigType;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfigv3.ObjectFactory;

public class JAXBUnmarshalV3 {

	// logger
	private static final Logger log = Logger.getLogger(JAXBUnmarshalV3.class);

	/**
	 * reads matsim config generated from urbansim and inits jaxbv3 object structure
	 * @param matsimConfigFile
	 * @return Matsim4UrbansimConfigType
	 */
	@SuppressWarnings("unchecked")
	public Matsim4UrbansimConfigType unmarshal(String matsimConfigFile) {

		Matsim4UrbansimConfigType m4uConfigType = null;

		log.info("Unmaschalling MATSim configuration from: " + matsimConfigFile);
		log.info("...");
		try {

			JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
			// create an unmaschaller (write xml file)
			Unmarshaller unmarschaller = jaxbContext.createUnmarshaller();
			unmarschaller.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(getClass().getResource("/dtd/matsim4urbansim_v3.xsd")));

			File inputFile = new File( matsimConfigFile );
			isFileAvailable(matsimConfigFile, inputFile);
			// contains the content of the MATSim config.
			Object object = unmarschaller.unmarshal(inputFile);
			
			// The structure of both objects must match.
			if(object.getClass() == Matsim4UrbansimConfigType.class)
				m4uConfigType = (Matsim4UrbansimConfigType) object;
			else
				m4uConfigType = (( JAXBElement<Matsim4UrbansimConfigType>) object).getValue();

		} catch (Exception e) {
			System.out.flush() ;
			e.printStackTrace();
			throw new RuntimeException("unmarschalling failed; aborting ...") ;
		}

		log.info("... finished unmarschallig");
		// return initialized object representation of matsim4urbansim config
		// file
		return m4uConfigType;

	}
	
	
	private void isFileAvailable(String matsimConfigFile, File file){
		if(!file.exists()){
			log.error(matsimConfigFile + " not found!!!");
			System.exit(-1);
		}
	}
	
	
}

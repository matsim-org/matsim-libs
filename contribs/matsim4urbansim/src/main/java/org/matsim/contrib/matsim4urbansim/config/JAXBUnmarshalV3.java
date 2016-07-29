package org.matsim.contrib.matsim4urbansim.config;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.contrib.matsim4urbansim.constants.InternalConstants;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfigv3.Matsim4UrbansimConfigType;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfigv3.ObjectFactory;
import org.matsim.core.utils.io.MatsimJaxbXmlParser;
import org.xml.sax.SAXException;

public class JAXBUnmarshalV3 extends MatsimJaxbXmlParser{

	// logger
	private static final Logger log = Logger.getLogger(JAXBUnmarshalV3.class);
	
	public JAXBUnmarshalV3(){
		super(InternalConstants.CURRENT_MATSIM_4_URBANSIM_XSD_MATSIMORG);
	}
	
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
			
			// validate file
			super.validateFile(matsimConfigFile, unmarschaller);
			
			File inputFile = new File( matsimConfigFile );
			isFileAvailable(matsimConfigFile, inputFile);
			// contains the content of the MATSim config.
			Object object = unmarschaller.unmarshal(inputFile);
			
			// The structure of both objects must match.
			if(object.getClass() == Matsim4UrbansimConfigType.class)
				m4uConfigType = (Matsim4UrbansimConfigType) object;
			else
				m4uConfigType = (( JAXBElement<Matsim4UrbansimConfigType>) object).getValue();

		} catch (JAXBException je) {
			System.out.flush() ;
			je.printStackTrace();
			throw new RuntimeException("unmarschalling failed; aborting ...") ;
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

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
	
	private String matsimConfigFile = null;
	
	public JAXBUnmarshalV3(String configFile){
		super(InternalConstants.CURRENT_MATSIM_4_URBANSIM_XSD_MATSIMORG);
		this.matsimConfigFile = configFile;
	}
	
	public Matsim4UrbansimConfigType unmarshal() {

		Matsim4UrbansimConfigType m4uConfigType = null;

		log.info("Unmaschalling MATSim configuration from: " + matsimConfigFile);
		log.info("...");
		try {
			
			JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
			// create an unmaschaller (write xml file)
			Unmarshaller unmarschaller = jaxbContext.createUnmarshaller();
			
			// validate file
			super.validateFile(this.matsimConfigFile, unmarschaller);
			
			File inputFile = new File( matsimConfigFile );
			isFileAvailable(inputFile);
			// contains the content of the MATSim config.
			Object object = unmarschaller.unmarshal(inputFile);
			
			// The structure of both objects must match.
			if(object.getClass() == Matsim4UrbansimConfigType.class)
				m4uConfigType = (Matsim4UrbansimConfigType) object;
			else
				m4uConfigType = (( JAXBElement<Matsim4UrbansimConfigType>) object).getValue();

		} catch (JAXBException je) {
			je.printStackTrace();
			return null;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		log.info("... finished unmarschallig");
		// return initialized object representation of matsim4urbansim config
		// file
		return m4uConfigType;

	}
	
	
	private void isFileAvailable(File file){
		if(!file.exists()){
			log.error(matsimConfigFile + " not found!!!");
			System.exit(-1);
		}
	}
	
	@Override
	public void readFile(String filename) throws JAXBException, SAXException,
			ParserConfigurationException, IOException {
		throw new UnsupportedOperationException("Use unmaschalMATSimConfig()");
	}
	
}

package playground.tnicolai.matsim4opus.matsimTestData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.matsim.core.utils.io.IOUtils;
import org.xml.sax.SAXException;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.org.matsim.config.ConfigType;
import playground.tnicolai.matsim4opus.org.matsim.config.ControlerType;
import playground.tnicolai.matsim4opus.org.matsim.config.InputPlansFileType;
import playground.tnicolai.matsim4opus.org.matsim.config.Matsim4UrbansimType;
import playground.tnicolai.matsim4opus.org.matsim.config.MatsimConfigType;
import playground.tnicolai.matsim4opus.org.matsim.config.NetworkType;
import playground.tnicolai.matsim4opus.org.matsim.config.ObjectFactory;
import playground.tnicolai.matsim4opus.org.matsim.config.PlanCalcScoreType;
import playground.tnicolai.matsim4opus.org.matsim.config.UrbansimParameterType;
import playground.tnicolai.matsim4opus.utils.io.LoadFile;
import playground.tnicolai.matsim4opus.utils.io.TempDirectoryUtil;

public class GenerateMATSimConfig {
	
	private String destination;
	private boolean isTestRun;
	private String network;
	private String warmStartPlansFile;
	private String hotStartPlansFile;
	
	/**
	 * Constructor
	 * @param destination
	 * @param isTestRun
	 * @param network
	 * @param warmStartPlansFile
	 */
	public GenerateMATSimConfig(boolean isTestRun, String network, String warmStartPlansFile){
		this.destination = Constants.MATSIM_4_OPUS_CONFIG + "matsim_config.xml";
		this.isTestRun = isTestRun;
		this.network = network;
		this.warmStartPlansFile = warmStartPlansFile;
		this.hotStartPlansFile = "";
	}
	
	/**
	 * Constructor
	 * @param isTestRun
	 * @param network
	 * @param warmStartPlansFile
	 * @param hotStartPlansFile
	 */
	public GenerateMATSimConfig(boolean isTestRun, String network, String warmStartPlansFile, String hotStartPlansFile){
		this.destination = Constants.MATSIM_4_OPUS_CONFIG + "matsim_config.xml";
		this.isTestRun = isTestRun;
		this.network = network;
		this.warmStartPlansFile = warmStartPlansFile;
		this.hotStartPlansFile = hotStartPlansFile;
	}
	
	/**
	 * Constructor
	 * @param destination
	 * @param isTestRun
	 * @param network
	 */
	public GenerateMATSimConfig(String destination, boolean isTestRun, String network){
		this.destination = destination;
		this.isTestRun = isTestRun;
		this.network = network;
		this.warmStartPlansFile = "";
		this.hotStartPlansFile = "";
	}
	
	/**
	 * Constructor
	 * @param isTestRun
	 * @param network
	 */
	public GenerateMATSimConfig(boolean isTestRun, String network){
		this.destination = Constants.MATSIM_4_OPUS_CONFIG + "matsim_config.xml";
		this.isTestRun = isTestRun;
		this.network = network;
		this.warmStartPlansFile = "";
		this.hotStartPlansFile = "";
	}
	
	/**
	 * generates a MATSim config for testing purposes
	 */
	public void generate(){
		
		BufferedWriter bw = IOUtils.getBufferedWriter( this.destination );
		ObjectFactory of = new ObjectFactory();	
		
		// create "ConfigType"
		ControlerType controlerType = of.createControlerType();
		InputPlansFileType inputPlansFileType = of.createInputPlansFileType();
		InputPlansFileType hotStartPlansFileType = of.createInputPlansFileType();
		NetworkType networkType = of.createNetworkType();
		PlanCalcScoreType planCalcSoreType = of.createPlanCalcScoreType();
		controlerType.setFirstIteration(new BigInteger("0"));
		controlerType.setLastIteration(new BigInteger("1"));
		inputPlansFileType.setInputFile( this.warmStartPlansFile );
		hotStartPlansFileType.setInputFile( this.hotStartPlansFile );
		networkType.setInputFile( this.network );
		planCalcSoreType.setActivityType0( "home" );
		planCalcSoreType.setActivityType1( "work" );
		
		ConfigType configType = of.createConfigType();
		configType.setControler( controlerType );
		configType.setInputPlansFile( inputPlansFileType );
		configType.setHotStartPlansFile( hotStartPlansFileType );
		configType.setNetwork( networkType );
		configType.setPlanCalcScore( planCalcSoreType );
		
		// Create "Matsim4UrbanSimType"
		UrbansimParameterType urbansimParameterType = of.createUrbansimParameterType();
		urbansimParameterType.setYear(new BigInteger("2001"));
		urbansimParameterType.setOpusHome( Constants.OPUS_HOME );
		urbansimParameterType.setOpusDataPath( Constants.OPUS_DATA_PATH );
		urbansimParameterType.setMatsim4Opus( Constants.MATSIM_4_OPUS );
		urbansimParameterType.setMatsim4OpusConfig( Constants.MATSIM_4_OPUS_CONFIG );
		urbansimParameterType.setMatsim4OpusOutput( Constants.MATSIM_4_OPUS_OUTPUT );
		urbansimParameterType.setMatsim4OpusTemp( Constants.MATSIM_4_OPUS_TEMP );
		urbansimParameterType.setSamplingRate( 0.01 );		// just 1% random sample for testing ...
		urbansimParameterType.setIsTestRun( this.isTestRun );
		
		Matsim4UrbansimType matsim4UrbanSimType = of.createMatsim4UrbansimType();
		matsim4UrbanSimType.setUrbansimParameter(urbansimParameterType);
		
		// assign both to "MatsimConfigType"
		MatsimConfigType matsimConfigType = of.createMatsimConfigType();
		matsimConfigType.setConfig( configType );
		matsimConfigType.setMatsim4Urbansim( matsim4UrbanSimType );
		
		try {
			String tempDir = TempDirectoryUtil.createCustomTempDirectory("tmp");
			// init loadFile object: it downloads a xsd from matsim.org into a temp directory
			LoadFile loadFile = new LoadFile(Constants.MATSIM_4_URBANSIM_XSD, tempDir , Constants.XSD_FILE_NAME);
			File file2XSD = loadFile.loadMATSim4UrbanSimXSD(); // trigger loadFile
			if(file2XSD == null || !file2XSD.exists()){
				System.err.println("Did not find xml schema!");
				System.exit(1);
			}
			System.out.println("Using following xsd schema: " + file2XSD.getCanonicalPath());
			
			// crate a schema factory ...
			SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
			// create a schema object via the given xsd to validate the MATSim xml config.
			Schema schema = schemaFactory.newSchema(file2XSD);
			
			JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
			Marshaller m = jaxbContext.createMarshaller();
			m.setSchema(schema);
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			JAXBElement elem = new JAXBElement( new QName("","matsim_config"), MatsimConfigType.class, matsimConfigType); // this is because there is no XMLRootElemet annotation
			m.marshal(elem, bw );
			
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String getMATSimConfigPath(){
		return this.destination;
	}
	
	/**
	 * Test GenerateMATSimConfig
	 * @param args
	 */
	public static void main(String args[]){
		
		TempDirectoryUtil.createOPUSDirectories();
		String destination = TempDirectoryUtil.createCustomTempDirectory( "jaxbOutput" );
		
		GenerateMATSimConfig g = new GenerateMATSimConfig(destination + "/test", true, "");
		
		g.generate();
		
		TempDirectoryUtil.cleaningUpCustomTempDirectories();
		TempDirectoryUtil.cleaningUpOPUSDirectories();
	}

}

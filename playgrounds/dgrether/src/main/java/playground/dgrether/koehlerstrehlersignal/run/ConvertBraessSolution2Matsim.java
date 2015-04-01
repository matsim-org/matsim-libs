package playground.dgrether.koehlerstrehlersignal.run;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsScenarioLoader;
import org.matsim.signalsystems.data.SignalsScenarioWriter;

import playground.dgrether.DgPaths;
import playground.dgrether.koehlerstrehlersignal.analysis.AnalyzeBraessSimulation;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdPool;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2010CrossingSolution;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2010Solution2Matsim;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2015BraessSolutionOffsetsXMLParser;

/**
 * Class to start the conversion of BTU solutions for the braess scenario into MATSim signal format.
 * 
 * @author tthunig
 *
 */
public class ConvertBraessSolution2Matsim {

	private static final Logger log = Logger.getLogger(ConvertBraessSolution2Matsim.class);
	
	private void convert(String directory, String inputFile) {
		// read btu solution
		KS2015BraessSolutionOffsetsXMLParser solutionParser = new KS2015BraessSolutionOffsetsXMLParser();
		solutionParser.readFile(directory + inputFile);
		Map<String, List<KS2010CrossingSolution>> crossingSolutions = 
				solutionParser.getBraessOffsets();
		
		//convert and write btu solution into matsim format
		SignalsData signalsData = loadSignalsData(directory);
		DgIdPool idPool = DgIdPool.readFromFile(directory
				+ "id_conversions.txt");

		for (String coordination : crossingSolutions.keySet()){
			//convert solution
			KS2010Solution2Matsim converter = new KS2010Solution2Matsim(idPool);
			converter.convertSolution(signalsData.getSignalControlData(),
					crossingSolutions.get(coordination));
			//write solution
			writeBraessOffsetsSignalControl(directory, inputFile, signalsData, coordination);
		}
	}
	
	private SignalsData loadSignalsData(String directory) {
		Config config = ConfigUtils.createConfig();
		config.signalSystems().setSignalSystemFile(
				directory + "output_signal_systems_v2.0.xml.gz");
		config.signalSystems().setSignalGroupsFile(
				directory + "output_signal_groups_v2.0.xml.gz");
		config.signalSystems().setSignalControlFile(
				directory + "output_signal_control_v2.0.xml.gz");
		SignalsScenarioLoader signalsLoader = new SignalsScenarioLoader(
				config.signalSystems());
		SignalsData signals = signalsLoader.loadSignalsData();
		return signals;
	}
	
	private void writeBraessOffsetsSignalControl(String directoryPath, String inputFilename,
			SignalsData signalsData, String currentCoord) {
				
		SignalsScenarioWriter writer = new SignalsScenarioWriter();
		String basefilename = inputFilename.substring(inputFilename.lastIndexOf("/")+1,
				inputFilename.lastIndexOf("."));
		String subdirectory = inputFilename.substring(0,inputFilename.lastIndexOf("/")+1);
		
		writer.setSignalSystemsOutputFilename(directoryPath + subdirectory
				+ "signal_systems_" + currentCoord + "_" + basefilename + ".xml");
		writer.setSignalGroupsOutputFilename(directoryPath + subdirectory
				+ "signal_groups_" + currentCoord + "_" + basefilename + ".xml");
		writer.setSignalControlOutputFilename(directoryPath + subdirectory
				+ "signal_control_" + currentCoord + "_" + basefilename + ".xml");
		writer.writeSignalsData(signalsData);
	}

	public static void main(String[] args) {
		String directory =DgPaths.REPOS + "shared-svn/projects/cottbus/data/optimization/braess2ks/"
				+ "2015-02-24_minflow_1.0_morning_peak_speedFilter1.0_SP_tt_cBB100.0_sBB500.0/";
		String fileName = "btu/fix_coordinations.xml";
		
		new ConvertBraessSolution2Matsim().convert(directory, fileName);
		
		log.info("Output written to " + directory + "btu/signal_systems_*_fix_coordinations.xml");
	}

}

/**
 * @author Claudio Ruch
 *
 */

package playground.clruch.io.fleet;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.queuey.datalys.MultiFileReader;
import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.ScenarioOptions;
import playground.clruch.data.ReferenceFrame;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.StorageUtils;
import playground.clruch.utils.NetworkLoader;
import playground.clruch.utils.PropertiesExt;

/** @author Claudio Ruch */
enum StandaloneFleetConverterSF {
	;
	public static void main(String[] args) throws Exception {
		File workingDirectory = MultiFileTools.getWorkingDirectory();
		PropertiesExt simOptions = PropertiesExt.wrap(ScenarioOptions.load(workingDirectory));
		File configFile = new File(workingDirectory, simOptions.getString("simuConfig"));
		GlobalAssert.that(configFile.exists());

		Network network = NetworkLoader.loadNetwork(configFile);
		GlobalAssert.that(network != null);
		System.out.println("NETWORK has " + network.getNodes().size() + " nodes");

		File outputDirectory = new File(workingDirectory, "output/001");
		File dataDirectory = new File(workingDirectory, "taxiTraces");

		System.out.println("INFO working folder: " + workingDirectory.getAbsolutePath());
		System.out.println("INFO data folder: " + dataDirectory.getAbsolutePath());
		System.out.println("INFO output folder: " + outputDirectory.getAbsolutePath());

		File dir = new File(dataDirectory, "usefordata");
		dir.mkdir();
		FileUtils.cleanDirectory(dir);

		List<File> trailFilesComplete = (new MultiFileReader(dataDirectory, "new_")).getFolderFiles();
		System.out.println("NUMBER of data files = " + trailFilesComplete.size());

		// extract data from file and put into dayTaxiRecord
		DayTaxiRecord dayTaxiRecord = new DayTaxiRecord();
		CsvFleetReader reader = new CsvFleetReader(dayTaxiRecord);

		for (int num = 1; num <= trailFilesComplete.size(); num++) {
			System.out.println("Now processing: " + trailFilesComplete.get(num - 1).getName());
			reader.populateFrom(trailFilesComplete.get(num - 1), dataDirectory, num);
		}

		ReferenceFrame referenceFrame = ReferenceFrame.IDENTITY;
		 // STEP 2: DayTaxiRecord to MATSimStaticDatabase
		 MatsimStaticDatabase.initializeSingletonInstance(network, referenceFrame);
		
		 // generate sim objects and store
		 StorageUtils storageUtils = new StorageUtils(outputDirectory);
		 SimulationFleetDump.of(dayTaxiRecord, network, MatsimStaticDatabase.INSTANCE,
		 storageUtils);
		 dir.delete();
	}
}
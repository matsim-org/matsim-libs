/**
 * @author Claudio Ruch
 *
 */

package playground.clruch.io.fleet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.queuey.datalys.MultiFileReader;
import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import ch.ethz.idsc.queuey.datalys.csv.CSVUtils;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.ScenarioOptions;
import playground.clruch.data.ReferenceFrame;
import playground.clruch.net.IdIntegerDatabase;
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
		File headerDirectory = new File(dataDirectory, "HEADER");
		File idDirectory = new File(dataDirectory, "_cabs.txt");

		System.out.println("INFO working folder: " + workingDirectory.getAbsolutePath());
		System.out.println("INFO data folder: " + dataDirectory.getAbsolutePath());
		System.out.println("INFO output folder: " + outputDirectory.getAbsolutePath());
		
		/////////////////////////
		List<File> listOfFiles = (new MultiFileReader(dataDirectory, "new_")).getFolderFiles();
		List<File> listOfFiles2 = (new MultiFileReader(dataDirectory, "anew_")).getFolderFiles();
		
		if (listOfFiles2.isEmpty()) {
		for (File file : listOfFiles) {
			if (file.isFile()) {
				String source = File.separator + file.getAbsolutePath();
				String dest = File.separator + dataDirectory +"/a"+file.getName();
				File fin = new File(source);
				PrintWriter pw = new PrintWriter(dest);
				pw.close();
				FileInputStream fis = new FileInputStream(fin);
				BufferedReader in = new BufferedReader(new InputStreamReader(new ReverseLineInputStream(file)));
				FileWriter fstream = new FileWriter(dest, true);
				BufferedWriter out = new BufferedWriter(fstream);
				String aLine = null;
				while ((aLine = in.readLine()) != null) {
					java.util.List<String> lista = CSVUtils.csvLineToList(aLine, " ");
					Long timeStamp = Long.parseLong(lista.get(3));
					if (timeStamp < 1211147969 && timeStamp > 1211061600) {
						out.write(aLine);
						out.newLine();
					}
				}
				in.close(); fis.close(); out.close();
			}
		}
		}	
		////////////////////////////

		List<File> trailFilesComplete = (new MultiFileReader(dataDirectory, "anew_")).getFolderFiles();
		System.out.println("NUMBER of data files = " + trailFilesComplete.size());

		List<File> trailFiles = new ArrayList<>();

		// ID & HEADERfor Taxis
		ChangeDataSF.head(trailFilesComplete, headerDirectory, trailFiles);
		ArrayList<String> people;
		people = ChangeDataSF.name(idDirectory);

		// extract data from file and put into dayTaxiRecord
		DayTaxiRecord dayTaxiRecord = new DayTaxiRecord();
		CsvFleetReader reader = new CsvFleetReader(dayTaxiRecord);
		for (int num = 0; num < trailFilesComplete.size(); num++) {
			reader.populateFrom(trailFilesComplete.get(num), people, num);
		}

		ReferenceFrame referenceFrame = ReferenceFrame.IDENTITY;
		// STEP 2: DayTaxiRecord to MATSimStaticDatabase
		MatsimStaticDatabase.initializeSingletonInstance(network, referenceFrame);
		
		// generate sim objects and store
		StorageUtils storageUtils = new StorageUtils(outputDirectory);
		SimulationFleetDump.of(dayTaxiRecord, network, MatsimStaticDatabase.INSTANCE, storageUtils);
		for (File file : trailFilesComplete) {
			file.delete();
		}
	}
}
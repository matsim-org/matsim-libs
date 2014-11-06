package playground.smeintjes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import playground.southafrica.freight.digicore.utils.DigicoreUtils;
import playground.southafrica.utilities.Header;

	/**
	 * This class takes a text file listing vehicle ids and 
	 * creates a folder with the corresponding vehicle xml files.
	 * @author sumarie
	 *
	 */
public class DigicoreVehicleIdsToFiles {
	private final static Logger LOG = Logger.getLogger(DigicoreVehicleIdsToFiles.class);
	
	/**
	 * @param the path to the .txt file containing the list of vehicle ids
	 * @param the path to the xml vehicle folder
	 * @param the path to the output folder (where vehicle files should be written)
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Header.printHeader(DigicoreVehicleIdsToFiles.class.toString(), args);
		String inputFile = args[0];
		String xmlFolder = args[1];
		String outputFolder = args[2]; //Note: output folder must already exist!
		List<File> vehicleList = getVehicleFiles(inputFile, xmlFolder);
		writeOutput(vehicleList, xmlFolder, outputFolder);
		Header.printFooter();

	}

	public static List<File> getVehicleFiles(String vehicleIds, String vehicleFiles) {
		List<File> vehicleList = new ArrayList<File>();
		try {
			vehicleList = DigicoreUtils.readDigicoreVehicleIds(vehicleIds, vehicleFiles);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return vehicleList;
	}
	
	public static void writeOutput(List<File> vehicleList, String pathToSource, String pathToDestination) throws IOException{
		for (File file : vehicleList) {
			File oldLocation = new File(pathToSource + file.getName());
			File newLocation = new File(pathToDestination + file.getName());
			FileUtils.copyFile(oldLocation, newLocation);
		}
	}
}

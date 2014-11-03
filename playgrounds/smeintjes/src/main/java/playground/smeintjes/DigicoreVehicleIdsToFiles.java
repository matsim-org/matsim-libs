package playground.smeintjes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.google.common.io.Files;

import playground.southafrica.freight.digicore.utils.DigicoreUtils;
import playground.southafrica.utilities.Header;
import scala.util.parsing.combinator.PackratParsers.Head;

public class DigicoreVehicleIdsToFiles {
	private final static Logger LOG = Logger.getLogger(DigicoreVehicleIdsToFiles.class);
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Header.printHeader(DigicoreVehicleIdsToFiles.class.toString(), args);
		String inputFile = args[0];
		String xmlFolder = args[1];
		String outputFolder = args[2]; //Note: output folder must already exist!
		List<File> vehicleList = getVehicleFiles(inputFile, xmlFolder);
		writeOutput(vehicleList, outputFolder);
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
	
	public static void writeOutput(List<File> vehicleList, String outputFolder) throws IOException{
		String pathToSource = "C:/Users/sumarie/Documents/Honneurs/SVC 791/Data/sampled/";
		String pathToDestination = "C:/Users/sumarie/Documents/Honneurs/SVC 791/Data/inter_Gauteng/inter_vehicles/";
		for (File file : vehicleList) {
			File oldLocation = new File(pathToSource + file.getName());
			File newLocation = new File(pathToDestination + file.getName());
			FileUtils.copyFile(oldLocation, newLocation);
		}
	}
}

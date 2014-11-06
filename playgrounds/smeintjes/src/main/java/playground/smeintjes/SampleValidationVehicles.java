package playground.smeintjes;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;




/** This class takes files as input, and randomly extracts a validation sample
 *  from it. The size of the validation sample is determined by the user and is 
 *  expressed as a percentage of the input folders.
 * @param the folder (type {@link File}) from where files should be sampled.
 * @param the output folder (type {@link File}) where the validation sample will be
 * 		  written to.
 * @param the output folder (type {@link File}) where the remaining files will be
 * 		  written to.
 * @param the size (type {@link Double}) of the validation sample.
 * @return a {@link List} of {@link File}s sampled.
 * 
 * @author sumarie
 */
public class SampleValidationVehicles{
private final static Logger LOG = Logger.getLogger(SampleValidationVehicles.class);
	
	public static void main(String[] args) {
		Header.printHeader(SampleValidationVehicles.class.toString(), args);
		
		String inputFolder = args[0];
		String outputValidationFolder = args[1];
		String outputRemainingFolder = args[2];
		String validationSize = args[3];

		List<File> validationSampleList = readVehicleFiles(inputFolder, validationSize);
		
		writeValidationSample(validationSampleList, outputValidationFolder);
		writeRemainingFiles(outputRemainingFolder, validationSampleList, inputFolder);
		
		Header.printFooter();
	}
	
	
	/** This method samples random files from the input folder according to
	 *  the chosen validation size.
	 * @param vehicleFolder the input folder
	 * @param validationSize the size of the validation sample (expressed as
	 * 		  a percentage)
	 * @return vehicleFiles a {@link List} of {@link File}s sampled
	 */
	public static List<File> readVehicleFiles(String vehicleFolder, String validationSize) {
		int numberOfFiles = new File(vehicleFolder).list().length;
		double doubleValidationSize = Double.parseDouble(validationSize);
		int sizeValidationSample = (int) (doubleValidationSize*numberOfFiles); 
		
		File folder = new File(vehicleFolder);		
		List<File> vehicleFiles = FileUtils.sampleFiles(folder, sizeValidationSample, FileUtils.getFileFilter(".xml.gz"));
				
		return vehicleFiles;
		
	}
	
	
	/**
	 * This method writes all sampled files to the specified folder.
	 * @param validationList a {@link List} of {@link File}s sampled
	 * @param outputFolder
	 */
	public static void writeValidationSample(List<File> validationList, String outputFolder) {
		/* Check if the output folder exists, and delete if it does. */
		File validation_sample = new File(outputFolder);
		if(validation_sample.exists()){
			LOG.warn("The output folder exists and will be deleted.");
			LOG.warn("  --> " + validation_sample.getAbsolutePath());
			FileUtils.delete(validation_sample);
		}
		validation_sample.mkdirs();
		
		/* Write all files in the validation sample to the outputFolder 
		 * (files are actually copied from one location to another, but the 
		 * result is the same). */
		if(!validationList.isEmpty()){
			for (File file : validationList) {
				String fileName  = file.getName();
				File newFile = new File(outputFolder + "\\" + fileName);
				IOUtils.copyFile(file, newFile);
			}
		}else{
			LOG.error("There are no files in the sampled list to be written.");
		}
				
		
	}
	
	/**
	 * This method writes the remaining files (those that are not part of 
	 * the validation sample) to a separate directory.
	 * @param outputRemainingFolder
	 * @param validationList a {@link List} of {@link File}s sampled
	 * @param originalList a {@link List} of the input {@link File}s
	 * @return remainingFileList a {@link List} of the remaining {@link File}s
	 */
	public static List<File> writeRemainingFiles(String outputRemainingFolder, List<File> validationList, String inputFolder) {
		
		File input = new File(inputFolder);
		File[] fileArr = input.listFiles(FileUtils.getFileFilter(".xml.gz"));
		List<File> remainingFileList = new ArrayList<File>();
		List<File> originalList = new ArrayList<File>(fileArr.length);
		for (File file : fileArr) {
			originalList.add(file);
		}
		
		File validation_remaining = new File(outputRemainingFolder);
		if(validation_remaining.exists()){
			LOG.warn("The output folder exists and will be deleted.");
			LOG.warn("  --> " + validation_remaining.getAbsolutePath());
			FileUtils.delete(validation_remaining);
		}
		validation_remaining.mkdirs();
		
		/* Create an empty ArrayList and add all original files to it. */
//		List<File> remainingFileList = new ArrayList<File>();
//		for (File file : originalList) {
//			remainingFileList.add(file);
//		}
		
		for (File file : originalList) {
			if (!validationList.contains(file)) {
				remainingFileList.add(file);				
			}
		}
		
//		for (File validationFile : validationList) {
//			for (File originalFile : originalList) {
//				if(validationFile.equals(originalFile)){
//					remainingFileList.remove(originalFile);
//				}
//			}
//		}
		
		/*
		 * Write all remaining files to the outputRemainingFolder.
		 */
		
		for (File file : remainingFileList) {
			String fileName  = file.getName();
			File newFile = new File(outputRemainingFolder + "\\" + fileName);
			IOUtils.copyFile(file, newFile);
				
		}
		return remainingFileList;
		
	}

}

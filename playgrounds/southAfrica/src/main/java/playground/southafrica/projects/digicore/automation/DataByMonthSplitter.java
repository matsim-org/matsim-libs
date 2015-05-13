/**
 * 
 */
package playground.southafrica.projects.digicore.automation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.projects.digicore.DigicoreUtils;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 * Class to read through the raw Digicore accelerometer data, and split it by
 * month.
 * 
 * @author jwjoubert
 */
public class DataByMonthSplitter {
	final private Logger log = Logger.getLogger(getClass());
	final private String outputFolder;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(DataByMonthSplitter.class.toString(), args);
		
		String inputFile = args[0];
		String outputFolder = args[1];
		
		DataByMonthSplitter ds = new DataByMonthSplitter(outputFolder);
		
		try {
			ds.splitDataByMonth(inputFile);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not split file");
		}

		Header.printFooter();
	}
	
	
	public DataByMonthSplitter(final String outputFolder){
		this.outputFolder = outputFolder;
		
		/* Ensure that the output folder is empty. */
		File folder = new File(outputFolder);
		if(folder.isDirectory() && folder.listFiles().length > 0){
			/* Wanted to throw an exception, but will, for now only give a 
			 * warning as this forms part of an automated script on ie-susie. */
//			throw new IOException("May not overwrite output folder: " + outputFolder);
			
			/* Also, for the purpose of incremental analysis I will also not
			 * delete the folder if it exists. Only it create it if it does
			 * NOT exist. */
//			log.warn("The output folder exists and will be overwritten");
//			FileUtils.delete(folder);
		} else{
			folder.mkdirs();
		}
	}
	
	public void splitDataByMonth(String filename) throws IOException{
		log.info("Splitting file by month...");
		Counter counter = new Counter("  line # ");
		
		List<String> dateList = new ArrayList<String>();
		String thisFilename = null;
		String previousFilename = null;
		
		
		BufferedReader br = IOUtils.getBufferedReader(filename);
		BufferedWriter bw = null;
		try{
			String line = null;
			while((line=br.readLine()) != null){
				String[] sa1 = line.split(",");
				long date1996 = Long.parseLong(sa1[2]);
				String date = DigicoreUtils.getDateSince1996(date1996);
				String[] sa2 = date.split(" ");
				String[] sa3 = sa2[0].split("/");
				String yearMonth = sa3[0] + sa3[1];
				String yearMonthDay = sa3[0] + sa3[1] + sa3[2];
				
				thisFilename = yearMonth + ".csv";
				
				if(!dateList.contains(yearMonthDay)){
					log.info("---------------------> new date: " + yearMonthDay);
					dateList.add(yearMonthDay);
				}
				
				if(!thisFilename.equalsIgnoreCase(previousFilename)) {
					if(bw != null){
						bw.close();
					}
					bw = IOUtils.getAppendingBufferedWriter(outputFolder + (outputFolder.endsWith("/") ? "" : "/") + thisFilename);
				}

				bw.write(line);
				bw.newLine();
				previousFilename = thisFilename;
				
				counter.incCounter();
			}
		} finally{
			br.close();
		}
		counter.printCounter();
	}

}

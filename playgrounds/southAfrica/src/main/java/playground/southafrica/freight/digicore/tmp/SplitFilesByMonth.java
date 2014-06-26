package playground.southafrica.freight.digicore.tmp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

public class SplitFilesByMonth {
	final private static Logger LOG = Logger.getLogger(SplitFilesByMonth.class);

	public static void main(String[] args) {
		Header.printHeader(SplitFilesByMonth.class.toString(), args);
		
		String foldername = args[0];
		File folder = new File(foldername);
		if(!folder.exists() || !folder.isDirectory() || !folder.canRead()){
			throw new RuntimeException("Cannot read from folder " + foldername);
		}
		
		File[] files = folder.listFiles(FileUtils.getFileFilter(".csv.gz"));
		for(File file : files){
			LOG.info("Working through file " + file.getAbsolutePath());
			/* Check if a folder with the same name as the file exists, throw 
			 * an error if it does, or create it if it doesn't. */
			String outputFolderName = foldername + (foldername.endsWith("/") ? "" : "/") + file.getName().substring(0, file.getName().indexOf("."));
			File outputFolder = new File(outputFolderName);
			if(outputFolder.exists()){
				throw new RuntimeException("First delete output folder: " + outputFolderName);
			}
			outputFolder.mkdirs();			
			
			Counter counter = new Counter("   lines # ");
			BufferedReader br = IOUtils.getBufferedReader(file.getAbsolutePath());
			try{
				/* Read header line. */
				String line = br.readLine();
				counter.incCounter();
				
				while((line = br.readLine()) != null){
					String[] sa = line.split(",");
					long date = Long.parseLong(sa[0]);
					GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("GMT+2"));
					gc.setTimeInMillis(date*1000);
					
					int year = gc.get(Calendar.YEAR);
					int month = gc.get(Calendar.MONTH)+1;
					
					String outputFile = String.format("%s/%04d%02d.csv", outputFolderName, year, month);
					
					BufferedWriter bw = IOUtils.getAppendingBufferedWriter(outputFile);
					try{
						bw.write(line);
						bw.newLine();
					} finally{
						bw.close();
					}
					counter.incCounter();
				}
				
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot read from " + file.getAbsolutePath());
			} finally{
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Cannot close " + file.getAbsolutePath().toString());
				}
			}
			counter.printCounter();
		}

		Header.printFooter();
	}

}

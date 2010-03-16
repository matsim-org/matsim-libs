package playground.mmoyo.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

/**compress a file in format gz*/
public class FileCompressor {

	public void run(String inputFile, String outputFile) throws IOException{
		System.out.println("Compressing file " + inputFile);
		GZIPOutputStream gZIPOutputStream = new GZIPOutputStream(new FileOutputStream(outputFile));
		FileInputStream fileInputStream = new FileInputStream(inputFile);
		byte[] buffer = new byte[1024];
		int length;
		while ((length = fileInputStream.read(buffer)) > 0) {
			gZIPOutputStream.write(buffer, 0, length);
		}
		fileInputStream.close();
		gZIPOutputStream.finish();
		gZIPOutputStream.close();
		System.out.println("done");
	}
	
	//stores in the same directory and deletes original file 
	public void run(String inputFile){
		File file = new File(inputFile);
		String output = file.getParent()  + "/" + file.getName() + ".gz";
		try {
			run (inputFile, output);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (!file.delete()); 
		//wait some time
		System.out.println( file + " could not be deleted");
	}
	
	public static void main(String[] args) {
		String inputFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/parameters_analysis with increasing time priority.xls";
		new FileCompressor().run(inputFile);
	}

}

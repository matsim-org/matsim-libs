package playground.southafrica.utilities;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class FileUtils {
	private final static Logger LOG = Logger.getLogger(FileUtils.class);

	/**
	 * Copies a file from one location to another.
	 * @param sourceFile
	 * @param destFile
	 * @throws IOException
	 */
	public static void copyFile(File sourceFile, File destFile) throws IOException {
	    if(!destFile.exists()) {
	        destFile.createNewFile();
	    }

	    FileChannel source = null;
	    FileChannel destination = null;

	    try {
	        source = new FileInputStream(sourceFile).getChannel();
	        destination = new FileOutputStream(destFile).getChannel();
	        destination.transferFrom(source, 0, source.size());
	    }
	    finally {
	        if(source != null) {
	            source.close();
	        }
	        if(destination != null) {
	            destination.close();
	        }
	    }
	}
	
	
	/**
	 * Cleans a given file. If the file is a directory, it first cleans all
	 * its contained files (or folders).
	 * @param folder
	 */
	public static void delete(File folder){
		if(folder.isDirectory()){
			File[] contents = folder.listFiles();
			for(File file : contents){
				FileUtils.delete(file);
			}
			folder.delete();
		} else{
			folder.delete();
		}
	}
	
	
	/**
	 * Creates a file filter based on the filename extension.
	 * @param extension
	 * @return
	 */
	public static FileFilter getFileFilter(final String extension){
		FileFilter filter = new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				if(pathname.getName().endsWith(extension)){
					return true;
				} else{
					return false;
				}
			}
		};
		return filter;
	}

	
	/**
	 * The method samples from a filtered file list. 
	 * @param the folder (type {@link File}) from where files should be sampled.
	 * @param number the number of files that must be sampled. 
	 * @param filter the {@link FilenameFilter} filter that will be	used to 
	 *        filter the files from the source folder. 
	 * @return a {@link List} of {@link File}s sampled.
	 * @author jwjoubert
	 */
	public static List<File> sampleFiles(File folder, int number, FileFilter filter){
		List<File> result = null;
		if(!folder.exists() || !folder.isDirectory() || !folder.canRead()){
			LOG.error("Could not read from " + folder.getAbsolutePath());
			return null;
		}
		
		File[] fileList = folder.listFiles(filter);
		if(fileList.length > 0){
			result = new ArrayList<File>();
			if(fileList.length < number){
				LOG.warn("Although " + number + " files were requested, only " + fileList.length + " are available");
			}
			
			for(File f : fileList){
				result.add(f);
			}
		} else{
			LOG.warn("The folder contains no relevant files. A null list is returned!");
		}
		LOG.info("File sampling complete.");
		return result;
	}


}

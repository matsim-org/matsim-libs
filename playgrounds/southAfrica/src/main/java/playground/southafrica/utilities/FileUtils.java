package playground.southafrica.utilities;

import java.io.BufferedReader;
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
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;

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
	 * @return a {@link List} of {@link File}s sampled, or null if there are no
	 * 		  files with the given extension.
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
			
			/* Generate a random permutation of integers. */
			int[] permutation = RandomPermutation.getRandomPermutation(fileList.length);
			int index = 0;
			while(result.size() < number && index < permutation.length){
				result.add(fileList[permutation[index++]-1]); /* Permutation is from 1... while index is from 0... */
			}
			LOG.info("File sampling complete, " + result.size() + " returned.");
			return result;
		} else{
			LOG.warn("The folder contains no relevant files. A null object is returned!");
			return null;
		}
	}
	
	
	/**
	 * Reads a given file and parses a {@link List} of (typed) {@link Id}s from 
	 * the file. It is assumed that the file contains <i>NO HEADER</i> row, and 
	 * each row contains a single Id. No validation is done on the {@link Id}s, 
	 * <i><b>so make sure you read in the file you really want!</b></i>. The 
	 * file may be compressed as the parsing will uncompress on the fly. 
	 * @param <T>
	 * @param filename
	 * @return {@link List}<{@link Id}>.
	 */
	public static <T> List<Id<T>> readIds(String filename, Class<T> type){
		LOG.info("Reading Ids from file " + filename);
		List<Id<T>> list = new ArrayList<Id<T>>();
		
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = null;
			while((line = br.readLine()) != null){
				if(line.contains(" ")){
					LOG.error("The id `" + line + "' contains whitespaces and will result in a corrupted Id. Line will be ignored.");
				} else{
					list.add(Id.create(line, type));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not read from BufferedReader " + filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Could not close the BufferedReader " + filename);
			}
		}
		
		LOG.info("Read " + list.size() + " Ids.");
		return list;
	}


}

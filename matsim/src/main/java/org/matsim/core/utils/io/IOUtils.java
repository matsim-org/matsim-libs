/* *********************************************************************** *
 * project: org.matsim.*
 * IOUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.utils.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

/** A class with some static utility functions for file-I/O. */
public class IOUtils {

	private static final String GZ = ".gz";
	
	public static final Charset CHARSET_UTF8 = Charset.forName("UTF8");
	public static final Charset CHARSET_WINDOWS_ISO88591 = Charset.forName("ISO-8859-1");
	
	public static final String NATIVE_NEWLINE = System.getProperty("line.separator");

	private final static Logger log = Logger.getLogger(IOUtils.class);

	public static URL getUrlFromFileOrResource(String filename) {
		if (filename.startsWith("~" + File.separator)) {
		    filename = System.getProperty("user.home") + filename.substring(1);
		}
		
		File file = new File(filename);
		if (file.exists()) {
			try {
				return file.toURI().toURL();
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
		log.info("String " + filename + " does not exist as file on the file system; trying it as URL resource ..."); 
		URL resourceAsStream = IOUtils.class.getClassLoader().getResource(filename);
		if ( resourceAsStream==null ) {
			throw new RuntimeException("Filename |" + filename + "| not found." ) ;
		}
		return resourceAsStream;
	}

	/**
	 * Tries to open the specified file for reading and returns a BufferedReader for it.
	 * Supports gzip-compressed files, such files are automatically decompressed.
	 * If the file is not found, a gzip-compressed version of the file with the
	 * added ending ".gz" will be searched for and used if found. Assumes that the text
	 * in the file is stored in UTF-8 (without BOM).
	 *
	 * @param filename The file to read, may contain the ending ".gz" to force reading a compressed file.
	 * @return BufferedReader for the specified file.
	 * @throws UncheckedIOException
	 *
	 * @author mrieser
	 */
	public static BufferedReader getBufferedReader(final String filename) throws UncheckedIOException {
		return getBufferedReader(filename, Charset.forName("UTF8"));
	}

	/**
	 * Tries to open the specified file for reading and returns a BufferedReader for it.
	 * Supports gzip-compressed files, such files are automatically decompressed.
	 * If the file is not found, a gzip-compressed version of the file with the
	 * added ending ".gz" will be searched for and used if found.
	 *
	 * @param filename The file to read, may contain the ending ".gz" to force reading a compressed file.
	 * @param charset the Charset of the file to read
	 * @return BufferedReader for the specified file.
	 * @throws UncheckedIOException
	 *
	 * @author mrieser
	 */
	public static BufferedReader getBufferedReader(final String filename, final Charset charset) throws UncheckedIOException {
		BufferedReader infile = null;
		if (filename == null) {
			throw new UncheckedIOException(new FileNotFoundException("No filename given (filename == null)"));
		}
		try {
			if (new File(filename).exists()) {
				if (filename.endsWith(GZ)) {
					infile = new BufferedReader(new InputStreamReader(new UnicodeInputStream(new GZIPInputStream(new FileInputStream(filename))), charset));
				} else {
					infile = new BufferedReader(new InputStreamReader(new UnicodeInputStream(new FileInputStream(filename)), charset));
				}
			} else if (new File(filename + GZ).exists()) {
				infile = new BufferedReader(new InputStreamReader(new UnicodeInputStream(new GZIPInputStream(new FileInputStream(filename  + GZ))), charset));
			} else {
				InputStream stream = IOUtils.class.getClassLoader().getResourceAsStream(filename);
				if (stream != null) {
					if (filename.endsWith(GZ)) {
						infile = new BufferedReader(new InputStreamReader(new UnicodeInputStream(new GZIPInputStream(stream)), charset));
						log.info("loading file from classpath: " + filename);
					} else {
						infile = new BufferedReader(new InputStreamReader(new UnicodeInputStream(stream), charset));
						log.info("loading file from classpath: " + filename);
					}
				} else {
					stream = IOUtils.class.getClassLoader().getResourceAsStream(filename + GZ);
					if (stream != null) {
						infile = new BufferedReader(new InputStreamReader(new UnicodeInputStream(new GZIPInputStream(stream)), charset));
						log.info("loading file from classpath: " + filename + GZ);
					}
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		if (infile == null) {
			throw new UncheckedIOException(new FileNotFoundException(filename));
		}
		return infile;
	}


	/**
	 * Tries to open the specified file for writing and returns a BufferedWriter for it.
	 * Supports gzip-compression of the written data. The filename may contain the
	 * ending ".gz". If no compression is to be used, the ending will be removed
	 * from the filename. If compression is to be used and the filename does not yet
	 * have the ending ".gz", the ending will be added to it.
	 *
	 * @param filename The filename where to write the data.
	 * @param useCompression whether the file should be gzip-compressed or not.
	 * @return BufferedWriter for the specified file.
	 * @throws UncheckedIOException
	 */
	public static BufferedWriter getBufferedWriter(final String filename, final boolean useCompression) throws UncheckedIOException {
		if (filename == null) {
			throw new UncheckedIOException(new FileNotFoundException("No filename given (filename == null)"));
		}
		if (useCompression && !filename.endsWith(GZ)) {
			return getBufferedWriter(filename + GZ);
		} else if (!useCompression && filename.endsWith(GZ)) {
			return getBufferedWriter(filename.substring(0, filename.length() - 3));
		} else {
			return getBufferedWriter(filename);
		}
	}


	/**
	 * Tries to open the specified file for writing and returns a BufferedWriter for it.
	 * If the filename ends with ".gz", data will be automatically gzip-compressed.
	 * The data written will be encoded as UTF-8 (only relevant if you use Umlauts or
	 * other characters not used in plain English).
	 *
	 * @param filename The filename where to write the data.
	 * @return BufferedWriter for the specified file.
	 * @throws UncheckedIOException
	 */
	public static BufferedWriter getBufferedWriter(final String filename) throws UncheckedIOException {
		return getBufferedWriter(filename, Charset.forName("UTF8"));
	}


	/**
	 * Tries to open the specified file for writing and returns a BufferedWriter for it.
	 * If the filename ends with ".gz", data will be automatically gzip-compressed.
	 * The data written will be encoded as UTF-8 (only relevant if you use Umlauts or
	 * other characters not used in plain English). If the file already exists, content
	 * will not be overwritten, but new content be appended to the file.
	 *
	 * @param filename The filename where to write the data.
	 * @return BufferedWriter for the specified file.
	 * @throws UncheckedIOException
	 */
	public static BufferedWriter getAppendingBufferedWriter(final String filename) throws UncheckedIOException {
		return getBufferedWriter(filename, Charset.forName("UTF8"), true);
	}


	/**
	 * Tries to open the specified file for writing and returns a BufferedWriter for it.
	 * If the filename ends with ".gz", data will be automatically gzip-compressed.
	 *
	 * @param filename The filename where to write the data.
	 * @param charset the encoding to use to write the file.
	 * @return BufferedWriter for the specified file.
	 * @throws UncheckedIOException
	 */
	public static BufferedWriter getBufferedWriter(final String filename, final Charset charset) throws UncheckedIOException {
		return getBufferedWriter(filename, charset, false);
	}


	/**
	 * Tries to open the specified file for writing and returns a BufferedWriter for it.
	 * If the filename ends with ".gz", data will be automatically gzip-compressed. If
	 * the file already exists, content will not be overwritten, but new content be
	 * appended to the file.
	 *
	 * @param filename The filename where to write the data.
	 * @param charset the encoding to use to write the file.
	 * @return BufferedWriter for the specified file.
	 * @throws UncheckedIOException
	 */
	public static BufferedWriter getAppendingBufferedWriter(final String filename, final Charset charset) throws UncheckedIOException {
		return getBufferedWriter(filename, charset, true);
	}


	/**
	 * Tries to open the specified file for writing and returns a BufferedWriter for it.
	 * If the filename ends with ".gz", data will be automatically gzip-compressed.
	 *
	 * @param filename The filename where to write the data.
	 * @param charset the encoding to use to write the file.
	 * @param append <code>true</code> if the file should be opened for appending, instead of overwriting
	 * @return BufferedWriter for the specified file.
	 * @throws UncheckedIOException
	 */
	public static BufferedWriter getBufferedWriter(final String filename, final Charset charset, final boolean append) throws UncheckedIOException {
		if (filename == null) {
			throw new UncheckedIOException(new FileNotFoundException("No filename given (filename == null)"));
		}
		try {
			if (filename.toLowerCase(Locale.ROOT).endsWith(GZ)) {
				File f = new File(filename);
				if (append && f.exists() && (f.length() > 0)) {
					throw new IllegalArgumentException("Appending to an existing gzip-compressed file is not supported.");
				}
				return new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(filename, append)), charset));
			}
			return new BufferedWriter(new OutputStreamWriter(new FileOutputStream (filename, append), charset));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}


	/**
	 * Attempts to rename a file. The built-in method File.renameTo() often fails
	 * for different reasons (e.g. if the file should be moved across different
	 * file systems). This method first tries the method File.renameTo(),
	 * but tries other possibilities if the first one fails.
	 *
	 * @param fromFilename The file name of an existing file
	 * @param toFilename The new file name
	 * @return <code>true</code> if the file was successfully renamed, <code>false</code> otherwise
	 *
	 * @author mrieser
	 */
	public static boolean renameFile(final String fromFilename, final String toFilename) {
		return renameFile(new File(fromFilename), new File(toFilename));
	}

	/**
	 * Attempts to rename a file. The built-in method File.renameTo() often fails
	 * for different reasons (e.g. if the file should be moved across different
	 * file systems). This method first tries the method File.renameTo(),
	 * but tries other possibilities if the first one fails.
	 *
	 * @param fromFile The existing file.
	 * @param toFile A file object pointing to the new location of the file.
	 * @return <code>true</code> if the file was successfully renamed, <code>false</code> otherwise
	 *
	 * @author mrieser
	 */
	public static boolean renameFile(final File fromFile, final File toFile) {
		File toFile2 = toFile;

		// the first, obvious approach
		if (fromFile.renameTo(toFile)) {
			return true;
		}

		// if it failed, make some tests to decide if we should try any other approach
		if (!fromFile.exists()) {
			// there's no use renaming an inexistant file
			return false;
		}

		if (!fromFile.canRead()) {
			// there's no use to proceed further if we cannot read the file
			return false;
		}

		if (toFile.isDirectory()) {
			toFile2 = new File(toFile, fromFile.getName());
		}

		if (toFile2.exists()) {
			// we do not overwrite existing files
			return false;
		}

		String parent = toFile2.getParent();
		if (parent == null)
			parent = System.getProperty("user.dir");
		File dir = new File(parent);

		if (!dir.exists()) {
			// we cannot move a file to an inexistent directory
			return false;
		}

		if (!dir.canWrite()) {
			// we cannot write into the directory
			return false;
		}

		try {
			copyFile(fromFile, toFile2);
		} catch (UncheckedIOException e) {
			if (toFile2.exists()) toFile2.delete();
			return false;
		}

		// okay, at this place we can assume that we successfully copied the data, so remove the old file
		fromFile.delete();

		return true;
	}

	/**
	 * Copies the file content from one file to the other file.
	 *
	 * @param fromFile The file containing the data to be copied
	 * @param toFile The file the data should be written to
	 * @throws UncheckedIOException
	 *
	 * @author mrieser
	 */
	public static void copyFile(final File fromFile, final File toFile) throws UncheckedIOException {
		InputStream from = null;
		OutputStream to = null;
		try {
			from = new FileInputStream(fromFile);
			to = new FileOutputStream(toFile);
			copyStream(from, to);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			if (from != null) {
				try {
					from.close();
				} catch (IOException ignored) {
					ignored.printStackTrace();
				}
			}
			if (to != null) {
				try {
					to.close();
				} catch (IOException ignored) {
					ignored.printStackTrace();
				}
			}
		}
	}

	/**
	 * Copies the content from one stream to another stream.
	 *
	 * @param fromStream The stream containing the data to be copied
	 * @param toStream The stream the data should be written to
	 * @throws IOException
	 *
	 * @author mrieser
	 */
	public static void copyStream(final InputStream fromStream, final OutputStream toStream) throws IOException {
		byte[] buffer = new byte[4096];
		int bytesRead;
		while ((bytesRead = fromStream.read(buffer)) != -1) {
			toStream.write(buffer, 0, bytesRead);
		}
	}

	/**
	 * Method to create a directory.  Taken from OutputDirectoryHierarchy.  I am no expert on this so this may be wrong or incomplete.
	 * 
	 * @param dirName
	 * 
	 * @author kai nagel
	 */
	public static File createDirectory(final String dirName) {
		File tmpDir = new File(dirName);
		if ( !tmpDir.mkdir() && !tmpDir.exists() ) {
			throw new RuntimeException("The tmp directory " + dirName + " could not be created.");
		}
		return tmpDir ;
	}
	
	/**
	 * shortcut for deleteDirectory( dir, true ) 
	 */
	public static void deleteDirectory(final File dir ) {
		deleteDirectory( dir, true ) ;
	}

		/**
	 * This method recursively deletes the directory and all its contents. This
	 * method will not follow symbolic links, i.e. if the directory or one of the
	 * sub-directories contains a symbolic link, it will not delete this link. Thus
	 * the directory can then not be deleted, too.
	 *
	 * @param dir
	 *          File which must represent a directory; files or symbolic links are
	 *          not permitted as arguments
	 * @param checkForLinks
	 *          Do not check if dir is a link.  This is helpful in cases when ./xyz or ../xyz is considered a link.
	 *
	 * @author dgrether
	 */
	public static void deleteDirectory(final File dir, boolean checkForLinks ) {
		if (dir.isFile()) {
			throw new IllegalArgumentException("Directory " + dir.getName()
					+ " must not be a file!");
		}
		else if ( checkForLinks && isLink(dir) ) {
			// according to documentation, isLink has problems with pathnames of type ./xyz or ../xyz. kai, nov'14
			throw new IllegalArgumentException("Directory " + dir.getName()
					+ " doesn't exist or is a symbolic link or has a path name of type ./xyz or ../xyz !");
		}
		if (dir.exists()) {
			IOUtils.deleteDir(dir, checkForLinks);
		}
		else {
			throw new IllegalArgumentException("Directory " + dir.getName()
					+ " doesn't exist!");
		}
	}

	/**
	 * Recursive helper for {@link #deleteDirectory(File)}. Deletes a directory
	 * recursive. If the directory or one of its sub-directories is a symbolic
	 * neither the link nor its parent directories are deleted.
	 *
	 * @param dir The directory to be recursively deleted.
	 * @param checkForLinks TODO
	 *
	 * @author dgrether
	 */
	private static void deleteDir(final File dir, boolean checkForLinks) {
		File[] outDirContents = dir.listFiles();
		for (File outDirContent : outDirContents) {
			if (checkForLinks && isLink(outDirContent)) {
				continue;
			}
			if (outDirContent.isDirectory()) {
				deleteDir(outDirContent, checkForLinks);
			}
			if (!outDirContent.delete() && outDirContent.exists()) {
				// some file systems do not immediately delete directories (because of caches or whatever)
				// so we do not trust the return value of "delete()" alone, but make additional checks before issuing a warning
				log.error("Could not delete " + outDirContent.getAbsolutePath());
			}
		}
		if (!dir.delete()) {
			log.error("Could not delete " + dir.getAbsolutePath());
		}
	}

	/**
	 * Checks if the given File Object may be a symbolic link.<br />
	 *
	 * For a link that actually points to something (either a file or a
	 * directory), the absolute path is the path through the link, whereas the
	 * canonical path is the path the link references.<br />
	 *
	 * Dangling links appear as files of size zero, and generate a
	 * FileNotFoundException when you try to open them. Unfortunately non-existent
	 * files have the same behavior (size zero - why did't file.length() throw an
	 * exception if the file doesn't exist, rather than returning length zero?).<br />
	 *
	 * The routine below appears to detect Unix/Linux symbolic links, but it also
	 * returns true for non-existent files. Note that if a directory is a link,
	 * all the contents of this directory will appear as symbolic links as well.<br />
	 *
	 * Note that this method has problems if the file is specified with a relative
	 * path like "./" or "../".<br />
	 *
	 * @see <a href="http://www.idiom.com/~zilla/Xfiles/javasymlinks.html">Javasymlinks on www.idiom.com</a>
	 *
	 * @param file
	 * @return true if the file is a symbolic link or does not even exist. false if not
	 *
	 * @author dgrether
	 */
	public static boolean isLink(final File file) {
		try {
			if (!file.exists()) {
				return true;
			}
			String cnnpath = file.getCanonicalPath();
			String abspath = file.getAbsolutePath();
			return !abspath.equals(cnnpath);
		} catch (IOException ex) {
			System.err.println(ex);
			return true;
		}
	}

	 /**
   * Tries to open the specified file for reading and returns an InputStream for it.
   * Supports gzip-compressed files, such files are automatically decompressed.
   * If the file is not found, a gzip-compressed version of the file with the
   * added ending ".gz" will be searched for and used if found.
   *
   * @param filename The file to read, may contain the ending ".gz" to force reading a compressed file.
   * @return InputStream for the specified file.
   * @throws UncheckedIOException
   *
   * @author dgrether
   */
	public static InputStream getInputStream(final String filename) throws UncheckedIOException {
		InputStream inputStream = null;
		if (filename == null) {
			throw new UncheckedIOException(new FileNotFoundException("No filename given (filename == null)"));
		}
		try {
			// search in file system
			if (new File(filename).exists()) {
				if (filename.endsWith(GZ)) {
					inputStream = new GZIPInputStream(new FileInputStream(filename));
				} else {
					inputStream = new FileInputStream(filename);
				}
			} else if (new File(filename + GZ).exists()) {
				inputStream = new GZIPInputStream(new FileInputStream(filename));
			} else {
				// search in classpath
				InputStream stream = IOUtils.class.getClassLoader().getResourceAsStream(filename);
				if (stream != null) {
					if (filename.endsWith(GZ)) {
						inputStream = new GZIPInputStream(stream);
					}
					else {
						inputStream = stream;
					}
				} else {
					stream = IOUtils.class.getClassLoader().getResourceAsStream(filename + GZ);
					if (stream != null) {
						inputStream = new GZIPInputStream(stream);
					}
				}
				if (inputStream != null) {
					log.info("streaming file from classpath: " + filename);
				}
			}
			if (inputStream == null) {
				throw new FileNotFoundException(filename);
			}
			return new BufferedInputStream(new UnicodeInputStream(inputStream));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static InputStream getInputStream(URL url) throws UncheckedIOException {
		try {
			if (url.getFile().endsWith(".gz")) {
				return new GZIPInputStream(url.openStream());
			} else {
				return url.openStream();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Returns a buffered and optionally gzip-compressed output stream to the specified file.
	 * If the given filename ends with ".gz", the written file content will be automatically 
	 * compressed with the gzip-algorithm.
	 * 
	 * @throws UncheckedIOException if the file cannot be created.
	 * 
	 * @author mrieser
	 */
	public static OutputStream getOutputStream(final String filename)  {
		if (filename == null) {
			throw new UncheckedIOException(new FileNotFoundException("No filename given (filename == null)"));
		}
		try {
			if (filename.toLowerCase(Locale.ROOT).endsWith(GZ)) {
				return new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(filename)));
			} else {
				return new BufferedOutputStream(new FileOutputStream (filename));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// Compares two InputStreams.
	// Interestingly, StackOverflow claims that this naive way would be slow,
	// but for me, it is OK and the fast alternative which is proposed there is 
	// much slower, so I'm just doing it like this for now.
	
	// http://stackoverflow.com/questions/4245863/fast-way-to-compare-inputstreams
	public static boolean isEqual(InputStream i1, InputStream i2)
			throws IOException {
		try {
			while (true) {
				int fr = i1.read();
	            int tr = i2.read();

	            if (fr != tr) {
	                return false;
	            }
	            if (fr == -1) {
	                return true; // EOF on both sides
	            }
			}
		} finally {
			if (i1 != null) i1.close();
			if (i2 != null) i2.close();
		}
	}

	public static URL newUrl(URL context, String spec) {
		try {
			return new URL(context, spec);
		} catch (MalformedURLException e) {
			try {
				return new File(spec).toURI().toURL();
			} catch (MalformedURLException e1) {
				throw new RuntimeException(e1);
			}
		}
	}
}

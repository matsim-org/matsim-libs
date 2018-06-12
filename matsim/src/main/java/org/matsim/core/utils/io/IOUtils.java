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
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;

/** A class with some static utility functions for file-I/O. */
public class IOUtils {

	private static final String GZ = ".gz";
	private static final String LZ4 = ".lz4";

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
		log.info("String " + filename + " does not exist as file on the file system; trying it as a resource ...");
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
	 * <br> author mrieser
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
	 * <br> author mrieser
	 */
	public static BufferedReader getBufferedReader(final String filename, final Charset charset) throws UncheckedIOException {
		BufferedReader infile = null;
		if (filename == null) {
			throw new UncheckedIOException(new FileNotFoundException("No filename given (filename == null)"));
		}
		try {
//			if (new File(filename).exists()) {
//				if (filename.endsWith(GZ)) {
//					infile = new BufferedReader(new InputStreamReader(new UnicodeInputStream(new GZIPInputStream(new FileInputStream(filename))), charset));
//				} else {
//					infile = new BufferedReader(new InputStreamReader(new UnicodeInputStream(new FileInputStream(filename)), charset));
//				}
//			} else if (new File(filename + GZ).exists()) {
//				infile = new BufferedReader(new InputStreamReader(new UnicodeInputStream(new GZIPInputStream(new FileInputStream(filename  + GZ))), charset));
//			} else {
//				InputStream stream = IOUtils.class.getClassLoader().getResourceAsStream(filename);
//				if (stream != null) {
//					if (filename.endsWith(GZ)) {
//						infile = new BufferedReader(new InputStreamReader(new UnicodeInputStream(new GZIPInputStream(stream)), charset));
//						log.info("loading file from classpath: " + filename);
//					} else {
//						infile = new BufferedReader(new InputStreamReader(new UnicodeInputStream(stream), charset));
//						log.info("loading file from classpath: " + filename);
//					}
//				} else {
//					stream = IOUtils.class.getClassLoader().getResourceAsStream(filename + GZ);
//					if (stream != null) {
//						infile = new BufferedReader(new InputStreamReader(new UnicodeInputStream(new GZIPInputStream(stream)), charset));
//						log.info("loading file from classpath: " + filename + GZ);
//					}
//				}
//			}
			infile = new BufferedReader(new InputStreamReader(getInputStream(filename), charset));
		} catch (UncheckedIOException e) {
			log.fatal("encountered IOException.  This will most probably be fatal.  Note that for relative path names, the root is no longer the Java root, but the directory where the config file resides.");
			throw new UncheckedIOException(e);
		}

//		if (infile == null) {
//			throw new UncheckedIOException(new FileNotFoundException(filename));
//		}
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
//			if (filename.toLowerCase(Locale.ROOT).endsWith(GZ)) {
//				File f = new File(filename);
//				if (append && f.exists() && (f.length() > 0)) {
//					throw new IllegalArgumentException("Appending to an existing gzip-compressed file is not supported.");
//				}
//				return new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(filename, append)), charset));
//			}
//			return new BufferedWriter(new OutputStreamWriter(new FileOutputStream (filename, append), charset));
			return new BufferedWriter(new OutputStreamWriter(getOutputStream(filename, append), charset));
		} catch (UncheckedIOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Copies the content from one stream to another stream.
	 *
	 * @param fromStream The stream containing the data to be copied
	 * @param toStream The stream the data should be written to
	 * @throws IOException
	 *
	 * <br> author mrieser
	 */
	public static void copyStream(final InputStream fromStream, final OutputStream toStream) throws IOException {
		byte[] buffer = new byte[4096];
		int bytesRead;
		while ((bytesRead = fromStream.read(buffer)) != -1) {
			toStream.write(buffer, 0, bytesRead);
		}
	}

	/**
	 *
	 * Deletes a directory tree recursively. Should behave like rm -rf, i.e. there should not be
	 * any accidents like following symbolic links.
	 *
	 * @param path The directoy to be deleted
	 */
	public static void deleteDirectoryRecursively(Path path) throws UncheckedIOException {
		try {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			throw new UncheckedIOException(e.getMessage(), e);
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
	 * <br> author dgrether
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
				}else if (filename.endsWith(LZ4)) {
					inputStream = new UnicodeInputStream(new LZ4BlockInputStream(new FileInputStream(filename)));
				} else {
					inputStream = new FileInputStream(filename);
				}
			} else if (new File(filename + GZ).exists()) {
				inputStream = new GZIPInputStream(new FileInputStream(filename + GZ));
			}  else {
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
	
	public static OutputStream getOutputStream(final String filename) throws UncheckedIOException {
		return getOutputStream(filename, false);
	}

	/**
	 * Returns a buffered and optionally gzip-compressed output stream to the specified file.
	 * If the given filename ends with ".gz", the written file content will be automatically 
	 * compressed with the gzip-algorithm.
	 * 
	 * @throws UncheckedIOException if the file cannot be created.
	 * 
	 * <br> author mrieser
	 */
	public static OutputStream getOutputStream(final String filename, boolean append) throws UncheckedIOException {
		if (filename == null) {
			throw new UncheckedIOException(new FileNotFoundException("No filename given (filename == null)"));
		}
		try {
			if (filename.toLowerCase(Locale.ROOT).endsWith(GZ)) {
				File f = new File(filename);
				if (append && f.exists() && (f.length() > 0)) {
					throw new IllegalArgumentException("Appending to an existing gzip-compressed file is not supported.");
				}
				return new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(filename, append)));
			} else if (filename.toLowerCase(Locale.ROOT).endsWith(LZ4)) {
				File f = new File(filename);
				if (append && f.exists() && (f.length() > 0)) {
					throw new IllegalArgumentException("Appending to an existing lz4-compressed file is not supported.");
				}
				return new BufferedOutputStream(new LZ4BlockOutputStream(new FileOutputStream(filename)));
			}else {
				return new BufferedOutputStream(new FileOutputStream (filename, append));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Copy of getOutputStream and then changed to correspond to the PrintStream signature.  Device to hopefully reduce FindBugs warnings.  kai, may'17
	 * 
	 * @param filename
	 * @return
	 */
	public static PrintStream getPrintStream( final String filename ) {
		if (filename == null) {
			throw new UncheckedIOException(new FileNotFoundException("No filename given (filename == null)"));
		}
		try {
			if (filename.toLowerCase(Locale.ROOT).endsWith(GZ)) {
				return new PrintStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(filename))));
			} else {
				return new PrintStream(new BufferedOutputStream(new FileOutputStream (filename))) ;
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

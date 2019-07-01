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

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.log4j.Logger;

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
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/** A class with some static utility functions for file-I/O. */
public class IOUtils {
	private static final Map<String, String> COMPRESSIONS = new HashMap<>();
	
	static {
		COMPRESSIONS.put("gz", CompressorStreamFactory.GZIP);
		COMPRESSIONS.put("bz2", CompressorStreamFactory.BZIP2);
		COMPRESSIONS.put("lz4", CompressorStreamFactory.LZ4_BLOCK);
	}

	public static final Charset CHARSET_UTF8 = StandardCharsets.UTF_8;
	public static final Charset CHARSET_WINDOWS_ISO88591 = StandardCharsets.ISO_8859_1;

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
		return getBufferedReader(filename, StandardCharsets.UTF_8);
	}

	public static BufferedReader getBufferedReader(final URL url) throws UncheckedIOException {
		return getBufferedReader(url, StandardCharsets.UTF_8);
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
			infile = new BufferedReader(new InputStreamReader(getInputStream(filename), charset));
		} catch (UncheckedIOException e) {
			log.fatal("encountered IOException.  This will most probably be fatal.  Note that for relative path names, the root is no longer the Java root, but the directory where the config file resides.");
			throw new UncheckedIOException(e);
		}

		return infile;
	}

	public static BufferedReader getBufferedReader(final URL url, final Charset charset) throws UncheckedIOException {
		BufferedReader infile = null;
		if (url == null) {
			throw new UncheckedIOException(new FileNotFoundException("No url given (url == null)"));
		}
		try {
			infile = new BufferedReader(new InputStreamReader(getInputStream(url), charset));
		} catch (UncheckedIOException e) {
			log.fatal("encountered IOException.  This will most probably be fatal.  Note that for relative path names, the root is no longer the Java root, but the directory where the config file resides.");
			throw new UncheckedIOException(e);
		}

		return infile;
	}

	private static String getCompressionExtension(String filename) {
		for (String compression : COMPRESSIONS.keySet()) {
			if (filename.endsWith("." + compression)) {
				return compression;
			}
		}
		
		return null;
	}

	/**
	 * Tries to open the specified file for writing and returns a BufferedWriter for it.
	 * Supports gzip-compression of the written data. The filename may contain a
	 * compression ending (*.gz, *.bz2, *.lz4). If no compression is to be used, the ending will be removed
	 * from the filename. If compression is to be used and the filename does not yet
	 * have a compression ending, the .gz ending will be added to it.
	 *
	 * @param filename The filename where to write the data.
	 * @param useCompression whether the file should be compressed or not.
	 * @return BufferedWriter for the specified file.
	 * @throws UncheckedIOException
	 */
	public static BufferedWriter getBufferedWriter(String filename, final boolean useCompression) throws UncheckedIOException {
		if (filename == null) {
			throw new UncheckedIOException(new FileNotFoundException("No filename given (filename == null)"));
		}
		
		String compressionExtension = getCompressionExtension(filename);
		
		if (useCompression && compressionExtension == null) {
			filename += ".gz";
		} else if (!useCompression && compressionExtension != null) {
			filename = filename.substring(0, filename.length() - compressionExtension.length() - 1);
		}
		
		return getBufferedWriter(filename);
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
		if (filename == null) {;
			throw new UncheckedIOException(new FileNotFoundException("No filename given (filename == null)"));
		}
		try {
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
	 * 
	 * <ul>
	 * <li>If the file is not found in the file system, we try to find it in the class path.</li>
	 * <li>Depending on the file extension, we uncompress the file, for: *.gz, *.bz2, *.lz4</li>
	 * </ul>
	 * 
	 * @param filename The file to read
	 * @return InputStream for the specified file.
	 * @throws UncheckedIOException
	 */
	public static InputStream getInputStream(final String filename) throws UncheckedIOException {
		if (filename == null) {
			throw new UncheckedIOException(new FileNotFoundException("No filename given (filename == null)"));
		}

		try {
			InputStream inputStream = null;

			if (new File(filename).exists()) {
				inputStream = new FileInputStream(filename);
				log.info("Streaming file from file system: " + filename);
			} else {
				inputStream = IOUtils.class.getClassLoader().getResourceAsStream(filename);

				if (inputStream != null) {
					log.info("Streaming file from classpath: " + filename);
				} else {
					throw new FileNotFoundException(filename);
				}
			}
			
			String compressionExtension = getCompressionExtension(filename);
			
			if (compressionExtension != null) {
				CompressorStreamFactory factory = new CompressorStreamFactory();
				inputStream = factory.createCompressorInputStream(COMPRESSIONS.get(compressionExtension), inputStream);
			}

			return new BufferedInputStream(inputStream);
		} catch (FileNotFoundException | CompressorException e) {
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
	 * Returns a buffered and optionally compressed output stream to the specified file.
	 * If the given filename ends with a compression extension (*.gz, *.bz2, *.lz4), the written file content will be automatically 
	 * compressed with the respective algorithm.
	 * 
	 * @throws UncheckedIOException if the file cannot be created.
	 */
	public static OutputStream getOutputStream(final String filename, boolean append) throws UncheckedIOException {
		if (filename == null) {
			throw new UncheckedIOException(new FileNotFoundException("No filename given (filename == null)"));
		}
		
		try {
			String compressionExtension = getCompressionExtension(filename);
			
			if (compressionExtension != null && new File(filename).exists() && append) {
				throw new IllegalArgumentException("Appending to a compressed file is not supported.");
			}
			
			OutputStream outputStream = new FileOutputStream(new File(filename), append);
			
			if (compressionExtension != null) {
				CompressorStreamFactory factory = new CompressorStreamFactory();
				outputStream = factory.createCompressorOutputStream(COMPRESSIONS.get(compressionExtension), outputStream);
			}
			
			return new BufferedOutputStream(outputStream);
		} catch (IOException | CompressorException e) {
			
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
		
		return new PrintStream(getOutputStream(filename));
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

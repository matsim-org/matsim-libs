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
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.log4j.Logger;

/**
 * This class provides helper methods for input/output in MATSim.
 */
final public class IOUtils {
	/**
	 * This is only a static helper class.
	 */
	private IOUtils() {
	}

	// Define compressions that can be used.
	private static final Map<String, String> COMPRESSION_EXTENSIONS = new TreeMap<>();

	static {
		COMPRESSION_EXTENSIONS.put("gz", CompressorStreamFactory.GZIP);
		COMPRESSION_EXTENSIONS.put("lz4", CompressorStreamFactory.LZ4_BLOCK);
		COMPRESSION_EXTENSIONS.put("bz2", CompressorStreamFactory.BZIP2);
	}

	// Define a number of charsets that are / have been used.
	public static final Charset CHARSET_UTF8 = StandardCharsets.UTF_8;
	public static final Charset CHARSET_WINDOWS_ISO88591 = StandardCharsets.ISO_8859_1;

	// Define new line character depending on system
	public static final String NATIVE_NEWLINE = System.getProperty("line.separator");

	// Logger
	private final static Logger log = Logger.getLogger(IOUtils.class);

	/**
	 * This function takes a path and tries to find the file in the file system or
	 * in the resource path. The order of resolution is as follows:
	 * 
	 * <ol>
	 * <li>Find path in file system</li>
	 * <li>Find path in file system with compression extension (e.g. *.gz)</li>
	 * <li>Find path in class path as resource</li>
	 * <li>Find path in class path with compression extension</li>
	 * </ol>
	 * 
	 * @throws MalformedURLException 
	 * @throws FileNotFoundException 
	 */
	public static URL resolveFileOrResource(String filename) throws MalformedURLException, FileNotFoundException {
		// I) Replace home identifier
		if (filename.startsWith("~" + File.separator)) {
			filename = System.getProperty("user.home") + filename.substring(1);
		}

		// II.1) First, try to find the file in the file system
		File file = new File(filename);

		if (file.exists()) {
			return file.toURI().toURL();
		}

		// II.2) Try to find file with an additional postfix for compression
		for (String postfix : COMPRESSION_EXTENSIONS.keySet()) {
			file = new File(filename + "." + postfix);

			if (file.exists()) {
				return file.toURI().toURL();
			}
		}

		// III.1) First, try to find the file in the class path
		URL resource = IOUtils.class.getClassLoader().getResource(filename);

		if (resource != null) {
			return resource;
		}

		// III.2) Second, try to find the resource with a compression extension
		for (String postfix : COMPRESSION_EXTENSIONS.keySet()) {
			resource = IOUtils.class.getClassLoader().getResource(filename);

			if (resource != null) {
				return resource;
			}
		}

		throw new FileNotFoundException(filename);
	}

	/**
	 * Gets the compression of a certain URL by file extension. May return null if
	 * not compression is assumed.
	 */
	private static String getCompression(URL url) {
		String[] segments = url.getPath().split("\\.");
		String lastExtension = segments[segments.length - 1];
		return COMPRESSION_EXTENSIONS.get(lastExtension);
	}

	/**
	 * Opens an input stream for a given URL. If the URL has a compression
	 * extension, the method will try to open the compressed file using the proper
	 * decompression algorithm.
	 * 
	 * @throws IOException 
	 * @throws CompressorException 
	 */
	public static InputStream getInputStream(URL url) throws IOException, CompressorException {
		InputStream inputStream = url.openStream();

		String compression = getCompression(url);
		if (compression != null) {
			CompressorStreamFactory compressorStreamFactory = new CompressorStreamFactory();
			inputStream = compressorStreamFactory.createCompressorInputStream(inputStream);
		}

		return new UnicodeInputStream(new BufferedInputStream(inputStream));
	}

	/**
	 * Creates a reader for an input URL. If the URL has a compression extension,
	 * the method will try to open the compressed file using the proper
	 * decompression algorithm. A given character set is used for the reader.
	 * 
	 * @throws CompressorException 
	 * @throws IOException 
	 */
	public static BufferedReader getBufferedReader(URL url, Charset charset) throws IOException, CompressorException {
		InputStream inputStream = getInputStream(url);
		return new BufferedReader(new InputStreamReader(inputStream, charset));
	}

	/**
	 * See {@link #getBufferedReader(String, Charset)}. UTF-8 is assumed as the
	 * character set.
	 * 
	 * @throws CompressorException 
	 * @throws IOException 
	 */
	public static BufferedReader getBufferedReader(URL url) throws IOException, CompressorException {
		return getBufferedReader(url, CHARSET_UTF8);
	}

	/**
	 * Convenience wrapper which works like
	 * {@link #getBufferedReader(String, Charset)}, but receives a String file name
	 * that is resolved to a URL in the background according to
	 * {@link #resolveFileOrResource(String)}. UTF-8 is assumed as the encoding. For
	 * more flexibility, please combine the two aforementioned methods.
	 * 
	 * @throws CompressorException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws MalformedURLException 
	 */
	public static BufferedReader getBufferedReader(String filename) throws MalformedURLException, FileNotFoundException, IOException, CompressorException {
		return getBufferedReader(resolveFileOrResource(filename));
	}

	/**
	 * Opens an output stream for a given URL. If the URL has a compression
	 * extension, the method will try to open the compressed file using the proper
	 * decompression algorithm. Note that compressed files cannot be appended and
	 * that it is only possible to write to the file system (i.e. file:// protocol).
	 * 
	 * @throws IOException 
	 * @throws CompressorException 
	 */
	@SuppressWarnings("resource")
	public static OutputStream getOutputStream(URL url, boolean append) throws IOException, CompressorException {
		if (!url.getProtocol().equals("file")) {
			throw new IOException("Can only write to file:// protocol URLs");
		}

		File file = new File(url.getPath());
		String compression = getCompression(url);

		if (compression != null && append && file.exists()) {
			throw new IOException("Cannot append to compressed files.");
		}

		OutputStream outputStream = new FileOutputStream(file, append);

		if (compression != null) {
			CompressorStreamFactory compressorStreamFactory = new CompressorStreamFactory();
			outputStream = compressorStreamFactory.createCompressorOutputStream(compression, outputStream);
		}

		return new BufferedOutputStream(outputStream);
	}

	/**
	 * Creates a writer for an output URL. If the URL has a compression extension,
	 * the method will try to open the compressed file using the proper
	 * decompression algorithm. Note that compressed files cannot be appended and
	 * that it is only possible to write to the file system (i.e. file:// protocol).
	 * 
	 * @throws CompressorException 
	 * @throws IOException 
	 */
	public static BufferedWriter getBufferedWriter(URL url, Charset charset, boolean append) throws IOException, CompressorException {
		OutputStream outputStream = getOutputStream(url, append);
		return new BufferedWriter(new OutputStreamWriter(outputStream, charset));
	}

	/**
	 * See {@link #getBufferedWriter(String, Charset, bool)}. UTF-8 is assumed as
	 * the character set and non-appending mode is used.
	 * 
	 * @throws CompressorException 
	 * @throws IOException 
	 */
	public static BufferedWriter getBufferedWriter(URL url) throws IOException, CompressorException {
		return getBufferedWriter(url, CHARSET_UTF8, false);
	}

	/**
	 * Convenience wrapper which works like
	 * {@link #getBufferedWriter(String, Charset, bool)}, but receives a String file
	 * name that is resolved to a URL in the background according to
	 * {@link #resolveFileOrResource(String)}. UTF-8 is assumed as the encoding and
	 * non-appending mode is used. For more flexibility, please combine the two
	 * aforementioned methods.
	 * 
	 * @throws CompressorException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws MalformedURLException 
	 */
	public static BufferedWriter getBufferedWriter(String filename) throws MalformedURLException, FileNotFoundException, IOException, CompressorException {
		return getBufferedWriter(resolveFileOrResource(filename));
	}

	/**
	 * Wrapper function for {@link #getBufferedWriter(URL)} that creates a
	 * PrintStream.
	 * 
	 * @throws CompressorException 
	 * @throws IOException 
	 */
	public static PrintStream getPrintStream(URL url) throws IOException, CompressorException {
		return new PrintStream(getOutputStream(url, false));
	}

	/**
	 * Copies the content from one stream to another stream.
	 *
	 * @param fromStream The stream containing the data to be copied
	 * @param toStream   The stream the data should be written to
	 * @throws IOException
	 */
	public static void copyStream(final InputStream fromStream, final OutputStream toStream) throws IOException {
		byte[] buffer = new byte[4096];
		int bytesRead;

		while ((bytesRead = fromStream.read(buffer)) != -1) {
			toStream.write(buffer, 0, bytesRead);
		}
	}

	/**
	 * Deletes a directory tree recursively. Should behave like rm -rf, i.e. there
	 * should not be any accidents like following symbolic links.
	 *
	 * @param path The directory to be deleted
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
	 * Compares two InputStreams.
	 * 
	 * Source:
	 * http://stackoverflow.com/questions/4245863/fast-way-to-compare-inputstreams
	 */
	public static boolean isEqual(InputStream first, InputStream second) throws IOException {
		try {
			while (true) {
				int fr = first.read();
				int tr = second.read();

				if (fr != tr) {
					return false;
				}
				if (fr == -1) {
					return true; // EOF on both sides
				}
			}
		} finally {
			if (first != null)
				first.close();
			if (second != null)
				second.close();
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

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
 * 
 * The whole I/O infrastructure is based on URLs, which allows more flexibility
 * than String-based paths or URLs. The structure follows three levels: Stream
 * level, writer/reader level, and convenience methods.
 * 
 * <h2>Stream level</h2>
 * 
 * The two main methods on the stream level are {@link #getInputStream(URL)} and
 * {@link #getOutputStream(URL, boolean)}. Their use is rather obvious, the
 * boolean argument of the output stream is whether it is an appending output
 * stream. Depending on the extension of the reference file of the URL,
 * compression will be detected automatically. See below for a list of active
 * compression algorithms.
 * 
 * <h2>Reader/Writer level</h2>
 * 
 * Use {@link #getBufferedWriter(URL, Charset, boolean)} and its simplified
 * versions to obtained a BufferedWriter object. Use
 * {@link #getBufferedReader(URL)} to obtain a BufferedReader. These functions
 * should be used preferredly, because they allow for future movements of files
 * to servers etc.
 * 
 * <h2>Convenience methods</h2>
 * 
 * Two convenience methods exist: {@link #getBufferedReader(String)} and
 * {@link #getBufferedReader(String)}, which take a String-based path as input.
 * They intentionally do not allow for much flexibility (e.g. choosing the
 * character set of the files). If this is needed, please use the reader/writer
 * level methods and construct the URL via the helper functions that are
 * documented below.
 * 
 * <h2>URL handling</h2>
 * 
 * To con
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
		COMPRESSION_EXTENSIONS.put("lz4", CompressorStreamFactory.LZ4_FRAMED);
		COMPRESSION_EXTENSIONS.put("bz2", CompressorStreamFactory.BZIP2);
	}

	// Define a number of charsets that are / have been used.
	public static final Charset CHARSET_UTF8 = StandardCharsets.UTF_8;
	public static final Charset CHARSET_WINDOWS_ISO88591 = StandardCharsets.ISO_8859_1;

	// Define new line character depending on system
	public static final String NATIVE_NEWLINE = System.getProperty("line.separator");

	// Logger
	private final static Logger logger = Logger.getLogger(IOUtils.class);

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
	 * @throws UncheckedIOException
	 */
	public static URL resolveFileOrResource(String filename) throws UncheckedIOException {
		try {
			// I) Replace home identifier
			if (filename.startsWith("~" + File.separator)) {
				filename = System.getProperty("user.home") + filename.substring(1);
			}

			// II.1) First, try to find the file in the file system
			File file = new File(filename);

			if (file.exists()) {
				logger.info(String.format("Resolved %s to %s", filename, file));
				return file.toURI().toURL();
			}

			// II.2) Try to find file with an additional postfix for compression
			for (String postfix : COMPRESSION_EXTENSIONS.keySet()) {
				file = new File(filename + "." + postfix);

				if (file.exists()) {
					logger.info(String.format("Resolved %s to %s", filename, file));
					return file.toURI().toURL();
				}
			}

			// III.1) First, try to find the file in the class path
			URL resource = IOUtils.class.getClassLoader().getResource(filename);

			if (resource != null) {
				logger.info(String.format("Resolved %s to %s", filename, resource));
				return resource;
			}

			// III.2) Second, try to find the resource with a compression extension
			for (String postfix : COMPRESSION_EXTENSIONS.keySet()) {
				resource = IOUtils.class.getClassLoader().getResource(filename + "." + postfix);

				if (resource != null) {
					logger.info(String.format("Resolved %s to %s", filename, resource));
					return resource;
				}
			}

			throw new FileNotFoundException(filename);
		} catch (FileNotFoundException | MalformedURLException e) {
			throw new UncheckedIOException(e);
		}
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
	 * @throws UncheckedIOException
	 */
	public static InputStream getInputStream(URL url) throws UncheckedIOException {
		try {
			InputStream inputStream = url.openStream();

			String compression = getCompression(url);
			if (compression != null) {
				CompressorStreamFactory compressorStreamFactory = new CompressorStreamFactory();
				inputStream = compressorStreamFactory.createCompressorInputStream(compression, inputStream);
				logger.info(String.format("Using %s compression for %s", compression, url));
			} else {
				logger.info(String.format("Using no compression for %s", url));
			}

			return new UnicodeInputStream(new BufferedInputStream(inputStream));
		} catch (IOException | CompressorException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Creates a reader for an input URL. If the URL has a compression extension,
	 * the method will try to open the compressed file using the proper
	 * decompression algorithm. A given character set is used for the reader.
	 * 
	 * @throws UncheckedIOException
	 */
	public static BufferedReader getBufferedReader(URL url, Charset charset) throws UncheckedIOException {
		InputStream inputStream = getInputStream(url);
		return new BufferedReader(new InputStreamReader(inputStream, charset));
	}

	/**
	 * See {@link #getBufferedReader(String, Charset)}. UTF-8 is assumed as the
	 * character set.
	 * 
	 * @throws UncheckedIOException
	 */
	public static BufferedReader getBufferedReader(URL url) throws UncheckedIOException {
		return getBufferedReader(url, CHARSET_UTF8);
	}

	/**
	 * Opens an output stream for a given URL. If the URL has a compression
	 * extension, the method will try to open the compressed file using the proper
	 * decompression algorithm. Note that compressed files cannot be appended and
	 * that it is only possible to write to the file system (i.e. file:// protocol).
	 * 
	 * @throws UncheckedIOException
	 */
	@SuppressWarnings("resource")
	public static OutputStream getOutputStream(URL url, boolean append) throws UncheckedIOException {
		try {
			if (!url.getProtocol().equals("file")) {
				throw new UncheckedIOException("Can only write to file:// protocol URLs");
			}

			File file = new File(url.getPath());
			String compression = getCompression(url);

			if (compression != null && append && file.exists()) {
				throw new UncheckedIOException("Cannot append to compressed files.");
			}

			OutputStream outputStream = new FileOutputStream(file, append);

			if (compression != null) {
				CompressorStreamFactory compressorStreamFactory = new CompressorStreamFactory();
				outputStream = compressorStreamFactory.createCompressorOutputStream(compression, outputStream);
				logger.info(String.format("Using %s compression for %s", compression, url));
			} else {
				logger.info(String.format("Using no compression for %s", url));
			}

			return new BufferedOutputStream(outputStream);
		} catch (IOException | CompressorException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Creates a writer for an output URL. If the URL has a compression extension,
	 * the method will try to open the compressed file using the proper
	 * decompression algorithm. Note that compressed files cannot be appended and
	 * that it is only possible to write to the file system (i.e. file:// protocol).
	 * 
	 * @throws UncheckedIOException
	 */
	public static BufferedWriter getBufferedWriter(URL url, Charset charset, boolean append)
			throws UncheckedIOException {
		OutputStream outputStream = getOutputStream(url, append);
		return new BufferedWriter(new OutputStreamWriter(outputStream, charset));
	}

	/**
	 * See {@link #getBufferedWriter(String, Charset, bool)}. UTF-8 is assumed as
	 * the character set and non-appending mode is used.
	 * 
	 * @throws UncheckedIOException
	 */
	public static BufferedWriter getBufferedWriter(URL url) throws UncheckedIOException {
		return getBufferedWriter(url, CHARSET_UTF8, false);
	}

	/**
	 * Wrapper function for {@link #getBufferedWriter(URL)} that creates a
	 * PrintStream.
	 * 
	 * @throws UncheckedIOException
	 */
	public static PrintStream getPrintStream(URL url) throws UncheckedIOException {
		return new PrintStream(getOutputStream(url, false));
	}

	/**
	 * Copies the content from one stream to another stream.
	 *
	 * @param fromStream The stream containing the data to be copied
	 * @param toStream   The stream the data should be written to
	 * 
	 * @throws UncheckedIOException
	 */
	public static void copyStream(final InputStream fromStream, final OutputStream toStream)
			throws UncheckedIOException {
		try {
			byte[] buffer = new byte[4096];
			int bytesRead;

			while ((bytesRead = fromStream.read(buffer)) != -1) {
				toStream.write(buffer, 0, bytesRead);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Deletes a directory tree recursively. Should behave like rm -rf, i.e. there
	 * should not be any accidents like following symbolic links.
	 *
	 * @param path The directory to be deleted
	 * 
	 * @throws UncheckedIOException
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
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Compares two InputStreams.
	 * 
	 * Source:
	 * http://stackoverflow.com/questions/4245863/fast-way-to-compare-inputstreams
	 * 
	 * @throws UncheckedIOException
	 */
	public static boolean isEqual(InputStream first, InputStream second) throws UncheckedIOException {
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
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Returns a URL for a (not necessarily existing) file path.
	 * 
	 * @param filename File name.
	 * 
	 * @throws UncheckedIOException
	 */
	public static URL getFileUrl(String filename) throws UncheckedIOException {
		try {
			return new File(filename).toURI().toURL();
		} catch (MalformedURLException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Given a base URL, returns the extended URL.
	 * 
	 * @param context   Base URL, e.g. from the Config object.
	 * @param extension Extended path specification.
	 * 
	 * @throws UncheckedIOException
	 */
	public static URL extendUrl(URL context, String extension) throws UncheckedIOException {
		if (context == null) {
			throw new IllegalArgumentException("Please use IOUtils.getFileUrl");
		}

		try {
			return new URL(context, extension);
		} catch (MalformedURLException e) {
			// We cannot construct a URL for some reason (see respective unit test)
			return getFileUrl(extension);
		}
	}

	/**
	 * Convenience wrapper, see {@link #getBufferedReader(URL, Charset)}.
	 * 
	 * Note, that in general you should rather use URLs and the respective
	 * {@link #getBufferedReader(URL)} function. You can obtain URLs for your file
	 * paths either using {@link #resolveFileOrResource(String)} for an existing
	 * identifier for which it is not sure a priori whether it is a local file or a
	 * URL.
	 */
	public static BufferedReader getBufferedReader(String filename) {
		return getBufferedReader(resolveFileOrResource(filename));
	}

	/**
	 * Convenience wrapper, see {@link #getBufferedWriter(URL, Charset, boolean)}.
	 */
	public static BufferedWriter getBufferedWriter(String filename) {
		return getBufferedWriter(getFileUrl(filename));
	}

	/**
	 * Convenience wrapper, see {@link #getBufferedWriter(URL, Charset, boolean)}.
	 */
	public static BufferedWriter getAppendingBufferedWriter(String filename) {
		return getBufferedWriter(getFileUrl(filename), CHARSET_UTF8, true);
	}
}

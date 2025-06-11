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

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import net.jpountz.lz4.LZ4FrameInputStream;
import net.jpountz.lz4.LZ4FrameOutputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
 * To convert a file name to a URL, use {@link #getFileUrl(String)}. This is
 * mostly useful to determine the URL for an output file. If you are working
 * with input files, the best is to make use of
 * {@link #resolveFileOrResource(String)}, which will first try to find a
 * certain file in the file system and then in the class path (i.e. in the Java
 * resources). This makes it easy to write versatile code that can work with
 * local files and resources at the same time.
 *
 * <h2>Compression</h2>
 *
 * Compressed files are automatically assumed if certain file types are
 * encountered. Currently, the following patterns match certain compression
 * algorithms:
 *
 * <ul>
 * <li><code>*.gz</code>: GZIP compression</li>
 * <li><code>*.lz4</code>: LZ4 compression</li>
 * <li><code>*.bz2</code>: Bzip2 compression</li>
 * <li><code>*.zst</code>: ZStandard compression</li>
 * </ul>
 *
 * <h2>Encryption</h2>
 *
 * Files ending with {@code .enc} are assumed to be encrypted and will be handled with {@link CipherUtils}.
 *
 */
final public class IOUtils {

/*
The following says something about URLs vs Strings as file"names" (by Marcel):

Generell sollten File-Zugriffe entweder per URL oder per String erfolgen. Früher waren alles Strings, neu gibt es mit dem Config-Context auch URLs. Man sollte aber, wenn
man URLs verwendet, möglichst die ganze Aufruf-Kette als URL durchziehen, da es ansonsten Probleme geben kann (und eben nicht versuchen, die URL in einen String
umzuwandeln, wie ich in meiner Commit-Message geschrieben hatte).

Da wird eine URL erzeugt und in einen String umgewandelt, was definitiv zu Problemen führen wird, wenn der Pfad Leerzeichen enthält.

Zudem wird durch die Umwandlung in einen String dann im Code ein anderer Pfad aufgerufen (nämlich der für String-Pfade), welcher dann mit der verschachtelten URL für das File innerhalb eines Jars Probleme hat. In kurz:

		String urlString = "jar:file:/Users/kainagel/.m2/repository/org/matsim/matsim-examples/12.0-SNAPSHOT/matsim-examples-12.0-SNAPSHOT.jar!/test/scenarios/equil/config.xml”;
		URL url = new URL(urlString);
		InputStream is = new FileInputStream(new File(url.toURI()); // produces an exception, as it cannot access the file inside a Jar.
		InputStream is = url.openStream(); // this works
		InputStream is = new FileInputStream(new File(“file:/Users/kainagel/.m2/repository/org/matsim/matsim-examples/12.0-SNAPSHOT/matsim-examples-12.0-SNAPSHOT.jar"); // this works, as it is an actual file on the file system

In ConfigUtils.loadConfig( String[] ) wird nun IOUtils.resolveFileOrResource(String) aufgerufen, das unter der Annahme, dass weil das Argument ein String ist, es sich um ein File im Filesystem handeln muss, versucht mittels new File() darauf zuzugreifen. Da es sich aber eigentlich um eine URL handelt, die dazu noch ein File innerhalb eines Jar spezifiziert, schlägt das fehl.

Ich habe nun IOUtils.resolveFileOrResource(String) so angepasst, dass es versucht zu erkennen, ob das String-Argument eine URL ist, und in diesem Falle dies einfach als URL
zurückgibt und nicht noch all die anderen Tests durchführt, welche die Methode sonst macht.

Damit scheint dann auch dein Test zu funktionieren.

PR ist hier: https://github.com/matsim-org/matsim/pull/646
*/

	/**
	 * This is only a static helper class.
	 */
	private IOUtils() {
	}

	private enum CompressionType { GZIP, LZ4, BZIP2, ZSTD }

	// Define compressions that can be used.
	private static final Map<String, CompressionType> COMPRESSION_EXTENSIONS = new TreeMap<>();

	static {
		COMPRESSION_EXTENSIONS.put("gz", CompressionType.GZIP);
		COMPRESSION_EXTENSIONS.put("lz4", CompressionType.LZ4);
		COMPRESSION_EXTENSIONS.put("bz2", CompressionType.BZIP2);
		COMPRESSION_EXTENSIONS.put("zst", CompressionType.ZSTD);
	}

	private static int zstdCompressionLevel = 6;

	public static void setZstdCompressionLevel(int level) {
		if (level >= 1) {
			zstdCompressionLevel = level;
		} else {
			logger.error("Invalid ZSTD compression level.");
		}
	}

	// Define a number of charsets that are / have been used.
	public static final Charset CHARSET_UTF8 = StandardCharsets.UTF_8;
	public static final Charset CHARSET_WINDOWS_ISO88591 = StandardCharsets.ISO_8859_1;

	// We niw use Unix line endings everywhere.
	public static final String NATIVE_NEWLINE = "\n";

	// Logger
	private final static Logger logger = LogManager.getLogger(IOUtils.class);

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
	 * In case the filename is a URL (i.e. starting with "file:" or "jar:file:"),
	 * then no resolution is done but the provided filename returned as URL.
	 *
	 * @throws UncheckedIOException
	 */
	public static URL resolveFileOrResource(String filename) throws UncheckedIOException {
		try {
			// I) do not handle URLs
			if (filename.startsWith("jar:file:") || filename.startsWith("file:") || filename.startsWith( "https:" ) || filename.startsWith( "http:" )) {
				// looks like an URI
				return new URL(filename);
			}

			// II) Replace home identifier
			if (filename.startsWith("~" + File.separator)) {
				filename = System.getProperty("user.home") + filename.substring(1);
			}

			// III.1) First, try to find the file in the file system
			File file = new File(filename);

			if (file.exists()) {
				logger.info(String.format("Resolved %s to %s", filename, file));
				return file.toURI().toURL();
			}

			// III.2) Try to find file with an additional postfix for compression
			for (String postfix : COMPRESSION_EXTENSIONS.keySet()) {
				file = new File(filename + "." + postfix);

				if (file.exists()) {
					logger.info(String.format("Resolved %s to %s", filename, file));
					return file.toURI().toURL();
				}
			}

			// IV.1) First, try to find the file in the class path
			URL resource = IOUtils.class.getClassLoader().getResource(filename);

			if (resource != null) {
				logger.info(String.format("Resolved %s to %s", filename, resource));
				return resource;
			}

			// IV.2) Second, try to find the resource with a compression extension
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
	private static CompressionType getCompression(URL url) {

		// .enc extension is ignored
		String[] segments = url.getPath().replace(".enc", "").split("\\.");
		String lastExtension = segments[segments.length - 1];
		return COMPRESSION_EXTENSIONS.get(lastExtension.toLowerCase(Locale.ROOT));
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

			if (url.getPath().endsWith(".enc"))
				inputStream = CipherUtils.getDecryptedInput(inputStream);

			CompressionType compression = getCompression(url);
			if (compression != null) {
				switch (compression) {
					case GZIP:
						inputStream = new GZIPInputStream(inputStream);
						break;
					case LZ4:
						inputStream = new LZ4FrameInputStream(inputStream);
						break;
					case BZIP2:
						inputStream = new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.BZIP2, inputStream);
						break;
					case ZSTD:
						inputStream = new ZstdInputStream(inputStream);
						break;
				}
			}

			return new UnicodeInputStream(new BufferedInputStream(inputStream));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (CompressorException | GeneralSecurityException e) {
			throw new UncheckedIOException(new IOException(e));
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
	 * See {@link #getBufferedReader(URL, Charset)}. UTF-8 is assumed as the
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
	public static OutputStream getOutputStream(URL url, boolean append) throws UncheckedIOException {
		try {
			if (!url.getProtocol().equals("file")) {
				throw new UncheckedIOException(new IOException("Can only write to file:// protocol URLs"));
			}

			File file = new File(url.toURI());
			CompressionType compression = getCompression(url);

			if ((compression != null && compression != CompressionType.ZSTD) && append && file.exists()) {
				throw new UncheckedIOException(new IOException("Cannot append to compressed files."));
			}

			OutputStream outputStream = new FileOutputStream(file, append);

			if (compression != null) {
				switch (compression) {
					case GZIP:
						outputStream = new GZIPOutputStream(outputStream);
						break;
					case LZ4:
						outputStream = new LZ4FrameOutputStream(outputStream);
						break;
					case BZIP2:
						outputStream = new CompressorStreamFactory().createCompressorOutputStream(CompressorStreamFactory.BZIP2, outputStream);
						break;
					case ZSTD:
						outputStream = new ZstdOutputStream(outputStream, zstdCompressionLevel);
						break;
				}
			}

			return new BufferedOutputStream(outputStream);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (CompressorException | URISyntaxException e) {
			throw new UncheckedIOException(new IOException(e));
		}
	}

	/**
	 * Creates a writer for an output URL. If the URL has a compression extension,
	 * the method will try to open the compressed file using the proper
	 * decompression algorithm. Note that compressed files cannot be appended and
	 * that it is only possible to write to the file system (i.e. file:// protocol).
	 */
	public static BufferedWriter getBufferedWriter(URL url, Charset charset, boolean append)
			throws UncheckedIOException {
		OutputStream outputStream = getOutputStream(url, append);
		return new BufferedWriter(new OutputStreamWriter(outputStream, charset));
	}

	/**
	 * See {@link #getBufferedWriter(URL, Charset, boolean)}. UTF-8 is assumed as
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
		byte[] buf1 = new byte[64 * 1024];
		byte[] buf2 = new byte[64 * 1024];
		try (first; second) {
			DataInputStream d2 = new DataInputStream(second);
			int len;
			while ((len = first.read(buf1)) > 0) {
				d2.readFully(buf2,0, len);
				if (!Arrays.equals(buf1, 0, len, buf2, 0, len)) {
					return false;
				}
			}
			return d2.read() < 0; // is the end of the second file also.
		} catch(EOFException ioe) {
			return false;
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

	/**
	 * Takes an inputFilename and copies it to the outputFilename without any further modification of the file paths.
	 * @param inputFilename
	 * @param outputFilename
	 */
	public static void copyFile(String inputFilename, String outputFilename) throws IOException {
		File fromFile = new File(inputFilename);
		File toFile = new File(outputFilename);
		Files.copy(fromFile.toPath(), toFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
	}
}

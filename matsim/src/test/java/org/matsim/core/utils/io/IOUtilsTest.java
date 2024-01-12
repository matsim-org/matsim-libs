/* *********************************************************************** *
 * project: org.matsim.*
 * IOUtilsTest
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

/**
 * @author mrieser
 * @author dgrether
 */
public class IOUtilsTest {

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testInitOutputDirLogging() throws IOException {
		System.out.println(utils.getOutputDirectory());
		String outDir = utils.getOutputDirectory();
		OutputDirectoryLogging.initLoggingWithOutputDirectory(outDir);

		File l = new File(outDir + OutputDirectoryLogging.LOGFILE);
		File errorLog = new File(outDir + OutputDirectoryLogging.WARNLOGFILE);
		Assertions.assertTrue(l.exists());
		Assertions.assertTrue(errorLog.exists());
	}

	/**
	 * @author mrieser
	 */
	@Test
	void testDeleteDir() throws IOException {
		String outputDir = utils.getOutputDirectory();
		String testDir = outputDir + "a";
		String someFilename = testDir + "/a.txt";
		File dir = new File(testDir);
		Assertions.assertTrue(dir.mkdir());
		File someFile = new File(someFilename);
		Assertions.assertTrue(someFile.createNewFile());

		IOUtils.deleteDirectoryRecursively(dir.toPath());

		Assertions.assertFalse(someFile.exists());
		Assertions.assertFalse(dir.exists());
	}

	/**
	 * @author mrieser
	 */
	@Test
	void testDeleteDir_InexistentDir() {
		assertThrows(UncheckedIOException.class, () -> {
			String outputDir = utils.getOutputDirectory();
			String testDir = outputDir + "a";
			File dir = new File(testDir);
			IOUtils.deleteDirectoryRecursively(dir.toPath());
			Assertions.assertFalse(dir.exists());
		});
	}

	@Test
	void testGetBufferedReader_encodingMacRoman() throws IOException {
		URL url = IOUtils.resolveFileOrResource(this.utils.getClassInputDirectory() + "textsample_MacRoman.txt");
		BufferedReader reader = IOUtils.getBufferedReader(url, Charset.forName("MacRoman"));
		String line = reader.readLine();
		Assertions.assertNotNull(line);
		Assertions.assertEquals("äöüÉç", line);
	}

	@Test
	void testGetBufferedReader_encodingIsoLatin1() throws IOException {
		URL url = IOUtils.resolveFileOrResource(this.utils.getClassInputDirectory() + "textsample_IsoLatin1.txt");
		BufferedReader reader = IOUtils.getBufferedReader(url, StandardCharsets.ISO_8859_1);
		String line = reader.readLine();
		Assertions.assertNotNull(line);
		Assertions.assertEquals("äöüÉç", line);
	}

	@Test
	void testGetBufferedReader_encodingUTF8() throws IOException {
		URL url = IOUtils.resolveFileOrResource(this.utils.getClassInputDirectory() + "textsample_UTF8.txt");
		BufferedReader reader = IOUtils.getBufferedReader(url);
		String line = reader.readLine();
		Assertions.assertNotNull(line);
		Assertions.assertEquals("äöüÉç", line);
	}

	@Test
	void testGetBufferedWriter_encodingMacRoman() throws IOException {
		String filename = this.utils.getOutputDirectory() + "textsample_MacRoman.txt";
		URL url = IOUtils.getFileUrl(filename);
		BufferedWriter writer = IOUtils.getBufferedWriter(url, Charset.forName("MacRoman"), false);
		writer.write("äöüÉç");
		writer.close();
		long crc1 = CRCChecksum.getCRCFromFile(this.utils.getClassInputDirectory() + "textsample_MacRoman.txt");
		long crc2 = CRCChecksum.getCRCFromFile(filename);
		Assertions.assertEquals(crc1, crc2, "File was not written with encoding MacRoman.");
	}

	@Test
	void testGetBufferedWriter_encodingIsoLatin1() throws IOException {
		String filename = this.utils.getOutputDirectory() + "textsample_IsoLatin1.txt";
		URL url = IOUtils.getFileUrl(filename);
		BufferedWriter writer = IOUtils.getBufferedWriter(url, StandardCharsets.ISO_8859_1, false);
		writer.write("äöüÉç");
		writer.close();
		long crc1 = CRCChecksum.getCRCFromFile(this.utils.getClassInputDirectory() + "textsample_IsoLatin1.txt");
		long crc2 = CRCChecksum.getCRCFromFile(filename);
		Assertions.assertEquals(crc1, crc2, "File was not written with encoding IsoLatin1.");
	}

	@Test
	void testGetBufferedWriter_encodingUTF8() throws IOException {
		String filename = this.utils.getOutputDirectory() + "textsample_UTF8.txt";
		URL url = IOUtils.getFileUrl(filename);
		BufferedWriter writer = IOUtils.getBufferedWriter(url);
		writer.write("äöüÉç");
		writer.close();
		long crc1 = CRCChecksum.getCRCFromFile(this.utils.getClassInputDirectory() + "textsample_UTF8.txt");
		long crc2 = CRCChecksum.getCRCFromFile(filename);
		Assertions.assertEquals(crc1, crc2, "File was not written with encoding UTF8.");
	}

	@Test
	void testGetBufferedWriter_overwrite() throws IOException {
		String filename = this.utils.getOutputDirectory() + "test.txt";
		URL url = IOUtils.getFileUrl(filename);
		BufferedWriter writer = IOUtils.getBufferedWriter(url);
		writer.write("aaa");
		writer.close();
		BufferedWriter writer2 = IOUtils.getBufferedWriter(url);
		writer2.write("bbb");
		writer2.close();
		BufferedReader reader = IOUtils.getBufferedReader(url);
		String line = reader.readLine();
		Assertions.assertEquals("bbb", line);
	}

	@Test
	void testGetBufferedWriter_append() throws IOException {
		String filename = this.utils.getOutputDirectory() + "test.txt";
		URL url = IOUtils.getFileUrl(filename);
		BufferedWriter writer = IOUtils.getBufferedWriter(url, IOUtils.CHARSET_UTF8, true);
		writer.write("aaa");
		writer.close();
		BufferedWriter writer2 = IOUtils.getBufferedWriter(url, IOUtils.CHARSET_UTF8, true);
		writer2.write("bbb");
		writer2.close();
		BufferedReader reader = IOUtils.getBufferedReader(url);
		String line = reader.readLine();
		Assertions.assertEquals("aaabbb", line);
	}

	@Test
	void testGetBufferedWriter_overwrite_gzipped() throws IOException {
		String filename = this.utils.getOutputDirectory() + "test.txt.gz";
		URL url = IOUtils.getFileUrl(filename);
		BufferedWriter writer = IOUtils.getBufferedWriter(url);
		writer.write("aaa");
		writer.close();
		BufferedWriter writer2 = IOUtils.getBufferedWriter(url);
		writer2.write("bbb");
		writer2.close();
		BufferedReader reader = IOUtils.getBufferedReader(url);
		String line = reader.readLine();
		Assertions.assertEquals("bbb", line);
	}

	@Test
	void testGetBufferedWriter_append_gzipped() throws IOException {
		assertThrows(UncheckedIOException.class, () -> {
			String filename = this.utils.getOutputDirectory() + "test.txt.gz";
			URL url = IOUtils.getFileUrl(filename);
			BufferedWriter writer = IOUtils.getBufferedWriter(url, IOUtils.CHARSET_UTF8, true);
			writer.write("aaa");
			writer.close();
			IOUtils.getBufferedWriter(url, IOUtils.CHARSET_UTF8, true);
		});
	}

	@Test
	void testGetBufferedWriter_gzipped() throws IOException {
		String filename = this.utils.getOutputDirectory() + "test.txt.gz";
		URL url = IOUtils.getFileUrl(filename);
		BufferedWriter writer = IOUtils.getBufferedWriter(url);
		writer.write("12345678901234567890123456789012345678901234567890");
		writer.close();
		File file = new File(filename);
		Assertions.assertTrue(file.length() < 50, "compressed file should be less than 50 bytes, but is " + file.length());
	}

	@Test
	void testGetBufferedWriter_append_lz4() throws IOException {
		assertThrows(UncheckedIOException.class, () -> {
			String filename = this.utils.getOutputDirectory() + "test.txt.lz4";
			URL url = IOUtils.getFileUrl(filename);
			BufferedWriter writer = IOUtils.getBufferedWriter(url, IOUtils.CHARSET_UTF8, true);
			writer.write("aaa");
			writer.close();
			IOUtils.getBufferedWriter(url, IOUtils.CHARSET_UTF8, true);
		});
	}

	@Test
	void testGetBufferedWriter_lz4() throws IOException {
		String filename = this.utils.getOutputDirectory() + "test.txt.lz4";
		URL url = IOUtils.getFileUrl(filename);
		BufferedWriter writer = IOUtils.getBufferedWriter(url);
		writer.write("12345678901234567890123456789012345678901234567890");
		writer.close();
		File file = new File(filename);
		Assertions.assertEquals(35, file.length(), "compressed file should be equal 35 bytes, but is " + file.length());

		String content = IOUtils.getBufferedReader(filename).readLine();
		Assertions.assertEquals("12345678901234567890123456789012345678901234567890", content);
	}

	@Test
	void testGetBufferedWriter_append_bz2() throws IOException {
		assertThrows(UncheckedIOException.class, () -> {
			String filename = this.utils.getOutputDirectory() + "test.txt.bz2";
			URL url = IOUtils.getFileUrl(filename);
			BufferedWriter writer = IOUtils.getBufferedWriter(url, IOUtils.CHARSET_UTF8, true);
			writer.write("aaa");
			writer.close();
			IOUtils.getBufferedWriter(url, IOUtils.CHARSET_UTF8, true);
		});
	}

	@Test
	void testGetBufferedWriter_bz2() throws IOException {
		String filename = this.utils.getOutputDirectory() + "test.txt.bz2";
		URL url = IOUtils.getFileUrl(filename);
		BufferedWriter writer = IOUtils.getBufferedWriter(url);
		writer.write("12345678901234567890123456789012345678901234567890");
		writer.close();
		File file = new File(filename);
		Assertions.assertTrue(file.length() == 51, "compressed file should be equal 51 bytes, but is " + file.length());
	}

	@Test
	void testGetBufferedWriter_append_zst() throws IOException {
		String filename = this.utils.getOutputDirectory() + "test.txt.zst";
		URL url = IOUtils.getFileUrl(filename);
		BufferedWriter writer = IOUtils.getBufferedWriter(url, IOUtils.CHARSET_UTF8, true);
		writer.write("aaa");
		writer.close();
		writer = IOUtils.getBufferedWriter(url, IOUtils.CHARSET_UTF8, true);
		writer.write("bbb");
		writer.close();
		BufferedReader reader = IOUtils.getBufferedReader(url);
		String content = reader.readLine();
		Assertions.assertEquals("aaabbb", content);
	}

	@Test
	void testGetBufferedWriter_zst() throws IOException {
		String filename = this.utils.getOutputDirectory() + "test.txt.zst";
		URL url = IOUtils.getFileUrl(filename);
		BufferedWriter writer = IOUtils.getBufferedWriter(url);
		writer.write("12345678901234567890123456789012345678901234567890");
		writer.close();
		File file = new File(filename);
		Assertions.assertEquals(28, file.length(), "compressed file should be equal 28 bytes, but is " + file.length());
	}

	@Test
	void testGetInputStream_UTFwithoutBOM() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt";
		FileOutputStream out = new FileOutputStream(filename);
		out.write("ABCdef".getBytes());
		out.close();

		InputStream in = IOUtils.getInputStream(IOUtils.resolveFileOrResource(filename));
		Assertions.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
		in.close();
	}

	@Test
	void testGetInputStream_UTFwithBOM() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt";
		FileOutputStream out = new FileOutputStream(filename);
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		out.write("ABCdef".getBytes());
		out.close();

		InputStream in = IOUtils.getInputStream(IOUtils.resolveFileOrResource(filename));
		Assertions.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
		in.close();
	}

	@Test
	void testGetInputStream_UTFwithBOM_Compressed() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt.gz";
		OutputStream out = IOUtils.getOutputStream(IOUtils.getFileUrl(filename), false);
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		out.write("ABCdef".getBytes());
		out.close();

		InputStream in = IOUtils.getInputStream(IOUtils.resolveFileOrResource(filename));
		Assertions.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
		in.close();
	}

	@Test
	void testGetInputStream_UTFwithBOM_Lz4() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt.lz4";
		OutputStream out = IOUtils.getOutputStream(IOUtils.getFileUrl(filename), false);
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		out.write("ABCdef".getBytes());
		out.close();

		InputStream in = IOUtils.getInputStream(IOUtils.resolveFileOrResource(filename));
		Assertions.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
		in.close();
	}

	@Test
	void testGetInputStream_UTFwithBOM_bz2() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt.bz2";
		OutputStream out = IOUtils.getOutputStream(IOUtils.getFileUrl(filename), false);
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		out.write("ABCdef".getBytes());
		out.close();

		InputStream in = IOUtils.getInputStream(IOUtils.resolveFileOrResource(filename));
		Assertions.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
		in.close();
	}

	@Test
	void testGetInputStream_UTFwithBOM_zst() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt.zst";
		OutputStream out = IOUtils.getOutputStream(IOUtils.getFileUrl(filename), false);
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		out.write("ABCdef".getBytes());
		out.close();

		InputStream in = IOUtils.getInputStream(IOUtils.resolveFileOrResource(filename));
		Assertions.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
		in.close();
	}

	@Test
	void testGetBufferedReader_UTFwithoutBOM() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt";
		FileOutputStream out = new FileOutputStream(filename);
		out.write("ABCdef".getBytes());
		out.close();

		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename));
			Assertions.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename), IOUtils.CHARSET_UTF8);
			Assertions.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename), IOUtils.CHARSET_WINDOWS_ISO88591);
			Assertions.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
	}

	@Test
	void testGetBufferedReader_UTFwithBOM() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt";
		FileOutputStream out = new FileOutputStream(filename);
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		out.write("ABCdef".getBytes());
		out.close();

		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename));
			Assertions.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename), IOUtils.CHARSET_UTF8);
			Assertions.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename), IOUtils.CHARSET_WINDOWS_ISO88591);
			Assertions.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
	}

	@Test
	void testGetBufferedReader_UTFwithBOM_Compressed() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt.gz";
		OutputStream out = IOUtils.getOutputStream(IOUtils.getFileUrl(filename), false);
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		out.write("ABCdef".getBytes());
		out.close();

		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename));
			Assertions.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename.substring(0, filename.length() - 3))); // without the .gz extension
			Assertions.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename), IOUtils.CHARSET_UTF8);
			Assertions.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename), IOUtils.CHARSET_WINDOWS_ISO88591);
			Assertions.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
	}

	@Test
	void testGetBufferedReader_UTFwithBOM_lz4() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt.lz4";
		OutputStream out = IOUtils.getOutputStream(IOUtils.getFileUrl(filename), false);
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		out.write("ABCdef".getBytes());
		out.close();

		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename));
			Assertions.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename), IOUtils.CHARSET_UTF8);
			Assertions.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename), IOUtils.CHARSET_WINDOWS_ISO88591);
			Assertions.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
	}

	@Test
	void testGetBufferedReader_UTFwithBOM_bz2() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt.bz2";
		OutputStream out = IOUtils.getOutputStream(IOUtils.getFileUrl(filename), false);
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		out.write("ABCdef".getBytes());
		out.close();

		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename));
			Assertions.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename), IOUtils.CHARSET_UTF8);
			Assertions.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename), IOUtils.CHARSET_WINDOWS_ISO88591);
			Assertions.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
	}

	@Test
	void testGetBufferedReader_UTFwithBOM_zst() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt.zst";
		OutputStream out = IOUtils.getOutputStream(IOUtils.getFileUrl(filename), false);
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		out.write("ABCdef".getBytes());
		out.close();

		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename));
			Assertions.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename), IOUtils.CHARSET_UTF8);
			Assertions.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename), IOUtils.CHARSET_WINDOWS_ISO88591);
			Assertions.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
	}

	/**
	 * Based on a report from a user on the mailing list that he has problems creating files with '+' in the filename.
	 *
	 * @author mrieser
	 * @throws IOException
	 */
	@Test
	void testGetBufferedWriter_withPlusInFilename() throws IOException {
		String filename = this.utils.getOutputDirectory() + "test+test.txt";
		BufferedWriter writer = IOUtils.getBufferedWriter(IOUtils.getFileUrl(filename));
		writer.write("hello world!");
		writer.close();

		File file = new File(filename);
		Assertions.assertTrue(file.exists());
		Assertions.assertEquals("test+test.txt", file.getCanonicalFile().getName());
	}

	@Test
	void testNewUrl() throws MalformedURLException {
		URL context = Paths.get("").toUri().toURL();
		System.out.println(context.toString());
		URL url = IOUtils.extendUrl(context, "C:\\windows\\directory\\filename.txt");
		System.out.println(url.toString());
	}

	@Test
	void testResolveFileOrResource() throws URISyntaxException, IOException {

		File jarFile = new File("test/input/org/matsim/core/utils/io/IOUtils/testfile.jar");
		String jarUrlString = "file:" + jarFile.getAbsolutePath(); // URLs require absolute paths
		String fileUrlString = "jar:" + jarUrlString + "!/the_file.txt";

		URL url = IOUtils.resolveFileOrResource(fileUrlString);
		try (InputStream is = url.openStream()) {
			byte[] data = new byte[4096];
			int size = is.read(data);
			Assertions.assertEquals(9, size);
			Assertions.assertEquals("Success!\n", new String(data, 0, size));
		}
	}

	@Test
	void testResolveFileOrResource_withWhitespace() throws URISyntaxException, IOException {

		File jarFile = new File("test/input/org/matsim/core/utils/io/IOUtils/test directory/testfile.jar");
		String fileUrlString = "jar:" + jarFile.toURI().toString() + "!/the_file.txt";
		Assertions.assertTrue(fileUrlString.contains("test%20directory")); // just make sure the space is correctly URL-encoded

		URL url = IOUtils.resolveFileOrResource(fileUrlString);
		try (InputStream is = url.openStream()) {
			byte[] data = new byte[4096];
			int size = is.read(data);
			Assertions.assertEquals(9, size);
			Assertions.assertEquals("Success!\n", new String(data, 0, size));
		}
	}

	@Test
	void testEncryptedFile() throws IOException {

		System.setProperty(CipherUtils.ENVIRONMENT_VARIABLE, "abc123");

		String input = IOUtils.getBufferedReader(new File("test/input/org/matsim/core/utils/io/IOUtils/some.secret").toURL()).readLine();

		// openssl enc -aes256 -md sha512 -pbkdf2 -iter 10000 -in some.secret -out some.secret.enc
		BufferedReader decrypted = IOUtils.getBufferedReader(new File("test/input/org/matsim/core/utils/io/IOUtils/some.secret.enc").toURL());
		Assertions.assertEquals(input, decrypted.readLine());

		BufferedReader gziped = IOUtils.getBufferedReader(new File("test/input/org/matsim/core/utils/io/IOUtils/some.secret.gz.enc").toURL());
		Assertions.assertEquals(input, gziped.readLine());
	}

}

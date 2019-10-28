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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Paths;

/**
 * @author mrieser
 * @author dgrether
 */
public class IOUtilsTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testInitOutputDirLogging() throws IOException {
		System.out.println(utils.getOutputDirectory());
		String outDir = utils.getOutputDirectory();
		OutputDirectoryLogging.initLoggingWithOutputDirectory(outDir);

		File l = new File(outDir + OutputDirectoryLogging.LOGFILE);
		File errorLog = new File(outDir + OutputDirectoryLogging.WARNLOGFILE);
		Assert.assertTrue(l.exists());
		Assert.assertTrue(errorLog.exists());
	}

	/**
	 * @author mrieser
	 */
	@Test
	public void testDeleteDir() throws IOException {
		String outputDir = utils.getOutputDirectory();
		String testDir = outputDir + "a";
		String someFilename = testDir + "/a.txt";
		File dir = new File(testDir);
		Assert.assertTrue(dir.mkdir());
		File someFile = new File(someFilename);
		Assert.assertTrue(someFile.createNewFile());

		IOUtils.deleteDirectoryRecursively(dir.toPath());

		Assert.assertFalse(someFile.exists());
		Assert.assertFalse(dir.exists());
	}

	/**
	 * @author mrieser
	 */
	@Test(expected = UncheckedIOException.class)
	public void testDeleteDir_InexistentDir() {
		String outputDir = utils.getOutputDirectory();
		String testDir = outputDir + "a";
		File dir = new File(testDir);
		IOUtils.deleteDirectoryRecursively(dir.toPath());
		Assert.assertFalse(dir.exists());
	}

	@Test
	public void testGetBufferedReader_encodingMacRoman() throws IOException {
		URL url = IOUtils.resolveFileOrResource(this.utils.getClassInputDirectory() + "textsample_MacRoman.txt");
		BufferedReader reader = IOUtils.getBufferedReader(url, Charset.forName("MacRoman"));
		String line = reader.readLine();
		Assert.assertNotNull(line);
		Assert.assertEquals("äöüÉç", line);
	}

	@Test
	public void testGetBufferedReader_encodingIsoLatin1() throws IOException {
		URL url = IOUtils.resolveFileOrResource(this.utils.getClassInputDirectory() + "textsample_IsoLatin1.txt");
		BufferedReader reader = IOUtils.getBufferedReader(url, Charset.forName("ISO-8859-1"));
		String line = reader.readLine();
		Assert.assertNotNull(line);
		Assert.assertEquals("äöüÉç", line);
	}

	@Test
	public void testGetBufferedReader_encodingUTF8() throws IOException {
		URL url = IOUtils.resolveFileOrResource(this.utils.getClassInputDirectory() + "textsample_UTF8.txt");
		BufferedReader reader = IOUtils.getBufferedReader(url);
		String line = reader.readLine();
		Assert.assertNotNull(line);
		Assert.assertEquals("äöüÉç", line);
	}

	@Test
	public void testGetBufferedWriter_encodingMacRoman() throws IOException {
		String filename = this.utils.getOutputDirectory() + "textsample_MacRoman.txt";
		URL url = IOUtils.getFileUrl(filename);
		BufferedWriter writer = IOUtils.getBufferedWriter(url, Charset.forName("MacRoman"), false);
		writer.write("äöüÉç");
		writer.close();
		long crc1 = CRCChecksum.getCRCFromFile(this.utils.getClassInputDirectory() + "textsample_MacRoman.txt");
		long crc2 = CRCChecksum.getCRCFromFile(filename);
		Assert.assertEquals("File was not written with encoding MacRoman.", crc1, crc2);
	}

	@Test
	public void testGetBufferedWriter_encodingIsoLatin1() throws IOException {
		String filename = this.utils.getOutputDirectory() + "textsample_IsoLatin1.txt";
		URL url = IOUtils.getFileUrl(filename);
		BufferedWriter writer = IOUtils.getBufferedWriter(url, Charset.forName("ISO-8859-1"), false);
		writer.write("äöüÉç");
		writer.close();
		long crc1 = CRCChecksum.getCRCFromFile(this.utils.getClassInputDirectory() + "textsample_IsoLatin1.txt");
		long crc2 = CRCChecksum.getCRCFromFile(filename);
		Assert.assertEquals("File was not written with encoding IsoLatin1.", crc1, crc2);
	}

	@Test
	public void testGetBufferedWriter_encodingUTF8() throws IOException {
		String filename = this.utils.getOutputDirectory() + "textsample_UTF8.txt";
		URL url = IOUtils.getFileUrl(filename);
		BufferedWriter writer = IOUtils.getBufferedWriter(url);
		writer.write("äöüÉç");
		writer.close();
		long crc1 = CRCChecksum.getCRCFromFile(this.utils.getClassInputDirectory() + "textsample_UTF8.txt");
		long crc2 = CRCChecksum.getCRCFromFile(filename);
		Assert.assertEquals("File was not written with encoding UTF8.", crc1, crc2);
	}

	@Test
	public void testGetBufferedWriter_overwrite() throws IOException {
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
		Assert.assertEquals("bbb", line);
	}

	@Test
	public void testGetBufferedWriter_append() throws IOException {
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
		Assert.assertEquals("aaabbb", line);
	}

	@Test
	public void testGetBufferedWriter_overwrite_gzipped() throws IOException {
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
		Assert.assertEquals("bbb", line);
	}

	@Test(expected = UncheckedIOException.class)
	public void testGetBufferedWriter_append_gzipped() throws IOException {
		String filename = this.utils.getOutputDirectory() + "test.txt.gz";
		URL url = IOUtils.getFileUrl(filename);
		BufferedWriter writer = IOUtils.getBufferedWriter(url, IOUtils.CHARSET_UTF8, true);
		writer.write("aaa");
		writer.close();
		IOUtils.getBufferedWriter(url, IOUtils.CHARSET_UTF8, true);
	}

	@Test
	public void testGetBufferedWriter_gzipped() throws IOException {
		String filename = this.utils.getOutputDirectory() + "test.txt.gz";
		URL url = IOUtils.getFileUrl(filename);
		BufferedWriter writer = IOUtils.getBufferedWriter(url);
		writer.write("12345678901234567890123456789012345678901234567890");
		writer.close();
		File file = new File(filename);
		Assert.assertTrue("compressed file should be less than 50 bytes, but is " + file.length(), file.length() < 50);
	}
	
	@Test(expected = UncheckedIOException.class)
	public void testGetBufferedWriter_append_lz4() throws IOException {
		String filename = this.utils.getOutputDirectory() + "test.txt.lz4";
		URL url = IOUtils.getFileUrl(filename);
		BufferedWriter writer = IOUtils.getBufferedWriter(url, IOUtils.CHARSET_UTF8, true);
		writer.write("aaa");
		writer.close();
		IOUtils.getBufferedWriter(url, IOUtils.CHARSET_UTF8, true);
	}

	@Test
	public void testGetBufferedWriter_lz4() throws IOException {
		String filename = this.utils.getOutputDirectory() + "test.txt.lz4";
		URL url = IOUtils.getFileUrl(filename);
		BufferedWriter writer = IOUtils.getBufferedWriter(url);
		writer.write("12345678901234567890123456789012345678901234567890");
		writer.close();
		File file = new File(filename);
		Assert.assertEquals("compressed file should be equal 62 bytes, but is " + file.length(), 62, file.length());
	}
	
	@Test(expected = UncheckedIOException.class)
	public void testGetBufferedWriter_append_bz2() throws IOException {
		String filename = this.utils.getOutputDirectory() + "test.txt.bz2";
		URL url = IOUtils.getFileUrl(filename);
		BufferedWriter writer = IOUtils.getBufferedWriter(url, IOUtils.CHARSET_UTF8, true);
		writer.write("aaa");
		writer.close();
		IOUtils.getBufferedWriter(url, IOUtils.CHARSET_UTF8, true);
	}

	@Test
	public void testGetBufferedWriter_bz2() throws IOException {
		String filename = this.utils.getOutputDirectory() + "test.txt.bz2";
		URL url = IOUtils.getFileUrl(filename);
		BufferedWriter writer = IOUtils.getBufferedWriter(url);
		writer.write("12345678901234567890123456789012345678901234567890");
		writer.close();
		File file = new File(filename);
		Assert.assertTrue("compressed file should be equal 51 bytes, but is " + file.length(), file.length() == 51);
	}

	@Test
	public void testGetBufferedWriter_append_zst() throws IOException {
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
		Assert.assertEquals("aaabbb", content);
	}

	@Test
	public void testGetBufferedWriter_zst() throws IOException {
		String filename = this.utils.getOutputDirectory() + "test.txt.zst";
		URL url = IOUtils.getFileUrl(filename);
		BufferedWriter writer = IOUtils.getBufferedWriter(url);
		writer.write("12345678901234567890123456789012345678901234567890");
		writer.close();
		File file = new File(filename);
		Assert.assertEquals("compressed file should be equal 50 bytes, but is " + file.length(), 50, file.length());
	}

	@Test
	public void testGetInputStream_UTFwithoutBOM() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt";
		FileOutputStream out = new FileOutputStream(filename);
		out.write("ABCdef".getBytes());
		out.close();
		
		InputStream in = IOUtils.getInputStream(IOUtils.resolveFileOrResource(filename));
		Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
		in.close();
	}

	@Test
	public void testGetInputStream_UTFwithBOM() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt";
		FileOutputStream out = new FileOutputStream(filename);
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		out.write("ABCdef".getBytes());
		out.close();
		
		InputStream in = IOUtils.getInputStream(IOUtils.resolveFileOrResource(filename));
		Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
		in.close();
	}
	
	@Test
	public void testGetInputStream_UTFwithBOM_Compressed() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt.gz";
		OutputStream out = IOUtils.getOutputStream(IOUtils.getFileUrl(filename), false);
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		out.write("ABCdef".getBytes());
		out.close();

		InputStream in = IOUtils.getInputStream(IOUtils.resolveFileOrResource(filename));
		Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
		in.close();
	}
	
	@Test
	public void testGetInputStream_UTFwithBOM_Lz4() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt.lz4";
		OutputStream out = IOUtils.getOutputStream(IOUtils.getFileUrl(filename), false);
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		out.write("ABCdef".getBytes());
		out.close();

		InputStream in = IOUtils.getInputStream(IOUtils.resolveFileOrResource(filename));
		Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
		in.close();
	}
	
	@Test
	public void testGetInputStream_UTFwithBOM_bz2() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt.bz2";
		OutputStream out = IOUtils.getOutputStream(IOUtils.getFileUrl(filename), false);
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		out.write("ABCdef".getBytes());
		out.close();

		InputStream in = IOUtils.getInputStream(IOUtils.resolveFileOrResource(filename));
		Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
		in.close();
	}
	
	@Test
	public void testGetInputStream_UTFwithBOM_zst() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt.zst";
		OutputStream out = IOUtils.getOutputStream(IOUtils.getFileUrl(filename), false);
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		out.write("ABCdef".getBytes());
		out.close();

		InputStream in = IOUtils.getInputStream(IOUtils.resolveFileOrResource(filename));
		Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
		in.close();
	}

	@Test
	public void testGetBufferedReader_UTFwithoutBOM() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt";
		FileOutputStream out = new FileOutputStream(filename);
		out.write("ABCdef".getBytes());
		out.close();
		
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename));
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename), IOUtils.CHARSET_UTF8);
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename), IOUtils.CHARSET_WINDOWS_ISO88591);
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
	}
	
	@Test
	public void testGetBufferedReader_UTFwithBOM() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt";
		FileOutputStream out = new FileOutputStream(filename);
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		out.write("ABCdef".getBytes());
		out.close();
		
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename));
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename), IOUtils.CHARSET_UTF8);
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename), IOUtils.CHARSET_WINDOWS_ISO88591);
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
	}

	@Test
	public void testGetBufferedReader_UTFwithBOM_Compressed() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt.gz";
		OutputStream out = IOUtils.getOutputStream(IOUtils.getFileUrl(filename), false);
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		out.write("ABCdef".getBytes());
		out.close();
		
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename));
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename.substring(0, filename.length() - 3))); // without the .gz extension
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename), IOUtils.CHARSET_UTF8);
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename), IOUtils.CHARSET_WINDOWS_ISO88591);
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
	}
	
	@Test
	public void testGetBufferedReader_UTFwithBOM_lz4() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt.lz4";
		OutputStream out = IOUtils.getOutputStream(IOUtils.getFileUrl(filename), false);
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		out.write("ABCdef".getBytes());
		out.close();
		
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename));
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename), IOUtils.CHARSET_UTF8);
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename), IOUtils.CHARSET_WINDOWS_ISO88591);
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
	}
	
	@Test
	public void testGetBufferedReader_UTFwithBOM_bz2() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt.bz2";
		OutputStream out = IOUtils.getOutputStream(IOUtils.getFileUrl(filename), false);
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		out.write("ABCdef".getBytes());
		out.close();
		
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename));
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename), IOUtils.CHARSET_UTF8);
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename), IOUtils.CHARSET_WINDOWS_ISO88591);
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
	}

	@Test
	public void testGetBufferedReader_UTFwithBOM_zst() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt.zst";
		OutputStream out = IOUtils.getOutputStream(IOUtils.getFileUrl(filename), false);
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		out.write("ABCdef".getBytes());
		out.close();

		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename));
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename), IOUtils.CHARSET_UTF8);
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(IOUtils.resolveFileOrResource(filename), IOUtils.CHARSET_WINDOWS_ISO88591);
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
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
	public void testGetBufferedWriter_withPlusInFilename() throws IOException {
		String filename = this.utils.getOutputDirectory() + "test+test.txt";
		BufferedWriter writer = IOUtils.getBufferedWriter(IOUtils.getFileUrl(filename));
		writer.write("hello world!");
		writer.close();
		
		File file = new File(filename);
		Assert.assertTrue(file.exists());
		Assert.assertEquals("test+test.txt", file.getCanonicalFile().getName());
	}

	@Test
	public void testNewUrl() throws MalformedURLException {
		URL context = Paths.get("").toUri().toURL();
		System.out.println(context.toString());
		URL url = IOUtils.extendUrl(context, "C:\\windows\\directory\\filename.txt");
		System.out.println(url.toString());
	}

	@Test
	public void testResolveFileOrResource() throws URISyntaxException, IOException {

		File jarFile = new File("test/input/org/matsim/core/utils/io/IOUtils/testfile.jar");
		String jarUrlString = "file:" + jarFile.getAbsolutePath(); // URLs require absolute paths
		String fileUrlString = "jar:" + jarUrlString + "!/the_file.txt";

		URL url = IOUtils.resolveFileOrResource(fileUrlString);
		try (InputStream is = url.openStream()) {
			byte[] data = new byte[4096];
			int size = is.read(data);
			Assert.assertEquals(9, size);
			Assert.assertEquals("Success!\n", new String(data, 0, size));
		}
	}

	@Test
	public void testResolveFileOrResource_withWhitespace() throws URISyntaxException, IOException {

		File jarFile = new File("test/input/org/matsim/core/utils/io/IOUtils/test directory/testfile.jar");
		String fileUrlString = "jar:" + jarFile.toURI().toString() + "!/the_file.txt";
		Assert.assertTrue(fileUrlString.contains("test%20directory")); // just make sure the space is correctly URL-encoded

		URL url = IOUtils.resolveFileOrResource(fileUrlString);
		try (InputStream is = url.openStream()) {
			byte[] data = new byte[4096];
			int size = is.read(data);
			Assert.assertEquals(9, size);
			Assert.assertEquals("Success!\n", new String(data, 0, size));
		}
	}

}

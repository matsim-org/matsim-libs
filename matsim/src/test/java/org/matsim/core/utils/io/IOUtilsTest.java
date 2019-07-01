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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Paths;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 * @author dgrether
 * @author sebhoerl
 */
public class IOUtilsTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	private final static Logger log = Logger.getLogger(IOUtilsTest.class);

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
		String filename = this.utils.getClassInputDirectory() + "textsample_MacRoman.txt";
		BufferedReader reader = IOUtils.getBufferedReader(filename, Charset.forName("MacRoman"));
		String line = reader.readLine();
		Assert.assertNotNull(line);
		Assert.assertEquals("äöüÉç", line);
	}

	@Test
	public void testGetBufferedReader_encodingIsoLatin1() throws IOException {
		String filename = this.utils.getClassInputDirectory() + "textsample_IsoLatin1.txt";
		BufferedReader reader = IOUtils.getBufferedReader(filename, Charset.forName("ISO-8859-1"));
		String line = reader.readLine();
		Assert.assertNotNull(line);
		Assert.assertEquals("äöüÉç", line);
	}

	@Test
	public void testGetBufferedReader_encodingUTF8() throws IOException {
		String filename = this.utils.getClassInputDirectory() + "textsample_UTF8.txt";
		BufferedReader reader = IOUtils.getBufferedReader(filename);
		String line = reader.readLine();
		Assert.assertNotNull(line);
		Assert.assertEquals("äöüÉç", line);
	}

	@Test
	public void testGetBufferedWriter_encodingMacRoman() throws IOException {
		String filename = this.utils.getOutputDirectory() + "textsample_MacRoman.txt";
		BufferedWriter writer = IOUtils.getBufferedWriter(filename, Charset.forName("MacRoman"));
		writer.write("äöüÉç");
		writer.close();
		long crc1 = CRCChecksum.getCRCFromFile(this.utils.getClassInputDirectory() + "textsample_MacRoman.txt");
		long crc2 = CRCChecksum.getCRCFromFile(filename);
		Assert.assertEquals("File was not written with encoding MacRoman.", crc1, crc2);
	}

	@Test
	public void testGetBufferedWriter_encodingIsoLatin1() throws IOException {
		String filename = this.utils.getOutputDirectory() + "textsample_IsoLatin1.txt";
		BufferedWriter writer = IOUtils.getBufferedWriter(filename, Charset.forName("ISO-8859-1"));
		writer.write("äöüÉç");
		writer.close();
		long crc1 = CRCChecksum.getCRCFromFile(this.utils.getClassInputDirectory() + "textsample_IsoLatin1.txt");
		long crc2 = CRCChecksum.getCRCFromFile(filename);
		Assert.assertEquals("File was not written with encoding IsoLatin1.", crc1, crc2);
	}

	@Test
	public void testGetBufferedWriter_encodingUTF8() throws IOException {
		String filename = this.utils.getOutputDirectory() + "textsample_UTF8.txt";
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
		writer.write("äöüÉç");
		writer.close();
		long crc1 = CRCChecksum.getCRCFromFile(this.utils.getClassInputDirectory() + "textsample_UTF8.txt");
		long crc2 = CRCChecksum.getCRCFromFile(filename);
		Assert.assertEquals("File was not written with encoding UTF8.", crc1, crc2);
	}

	@Test
	public void testGetBufferedWriter_overwrite() throws IOException {
		String filename = this.utils.getOutputDirectory() + "test.txt";
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
		writer.write("aaa");
		writer.close();
		BufferedWriter writer2 = IOUtils.getBufferedWriter(filename);
		writer2.write("bbb");
		writer2.close();
		BufferedReader reader = IOUtils.getBufferedReader(filename);
		String line = reader.readLine();
		Assert.assertEquals("bbb", line);
	}

	@Test
	public void testGetBufferedWriter_append() throws IOException {
		String filename = this.utils.getOutputDirectory() + "test.txt";
		BufferedWriter writer = IOUtils.getAppendingBufferedWriter(filename);
		writer.write("aaa");
		writer.close();
		BufferedWriter writer2 = IOUtils.getAppendingBufferedWriter(filename);
		writer2.write("bbb");
		writer2.close();
		BufferedReader reader = IOUtils.getBufferedReader(filename);
		String line = reader.readLine();
		Assert.assertEquals("aaabbb", line);
	}

	@Test
	public void testGetBufferedWriter_overwrite_gzipped() throws IOException {
		String filename = this.utils.getOutputDirectory() + "test.txt.gz";
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
		writer.write("aaa");
		writer.close();
		BufferedWriter writer2 = IOUtils.getBufferedWriter(filename);
		writer2.write("bbb");
		writer2.close();
		BufferedReader reader = IOUtils.getBufferedReader(filename);
		String line = reader.readLine();
		Assert.assertEquals("bbb", line);
	}

	@Test
	public void testGetBufferedWriter_append_gzipped() throws IOException {
		String filename = this.utils.getOutputDirectory() + "test.txt.gz";
		BufferedWriter writer = IOUtils.getAppendingBufferedWriter(filename);
		writer.write("aaa");
		writer.close();
		try {
			IOUtils.getAppendingBufferedWriter(filename);
			Assert.fail("expected exception.");
		} catch (IllegalArgumentException e) {
			log.info("Catched expected exception.", e);
		}
	}

	@Test
	public void testGetBufferedWriter_gzipped() throws IOException {
		String filename = this.utils.getOutputDirectory() + "test.txt.gz";
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
		writer.write("12345678901234567890123456789012345678901234567890");
		writer.close();
		File file = new File(filename);
		Assert.assertTrue("compressed file should be less than 50 bytes, but is " + file.length(), file.length() < 50);
	}
	
	@Test
	public void testGetBufferedWriter_append_lz4() throws IOException {
		String filename = this.utils.getOutputDirectory() + "test.txt.lz4";
		BufferedWriter writer = IOUtils.getAppendingBufferedWriter(filename);
		writer.write("aaa");
		writer.close();
		try {
			IOUtils.getAppendingBufferedWriter(filename);
			Assert.fail("expected exception.");
		} catch (IllegalArgumentException e) {
			log.info("Catched expected exception.", e);
		}
	}

	@Test
	public void testGetBufferedWriter_lz4() throws IOException {
		String filename = this.utils.getOutputDirectory() + "test.txt.lz4";
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
		writer.write("12345678901234567890123456789012345678901234567890");
		writer.close();
		File file = new File(filename);
		Assert.assertTrue("compressed file should be equal 62 bytes, but is " + file.length(), file.length() == 62);
	}
	
	@Test
	public void testGetBufferedWriter_append_bz2() throws IOException {
		String filename = this.utils.getOutputDirectory() + "test.txt.bz2";
		BufferedWriter writer = IOUtils.getAppendingBufferedWriter(filename);
		writer.write("aaa");
		writer.close();
		try {
			IOUtils.getAppendingBufferedWriter(filename);
			Assert.fail("expected exception.");
		} catch (IllegalArgumentException e) {
			log.info("Catched expected exception.", e);
		}
	}

	@Test
	public void testGetBufferedWriter_bz2() throws IOException {
		String filename = this.utils.getOutputDirectory() + "test.txt.bz2";
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
		writer.write("12345678901234567890123456789012345678901234567890");
		writer.close();
		File file = new File(filename);
		Assert.assertTrue("compressed file should be equal 62 bytes, but is " + file.length(), file.length() == 62);
	}

	@Test
	public void testGetInputStream_UTFwithoutBOM() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt";
		FileOutputStream out = new FileOutputStream(filename);
		out.write("ABCdef".getBytes());
		out.close();
		
		InputStream in = IOUtils.getInputStream(filename);
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
		
		InputStream in = IOUtils.getInputStream(filename);
		Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
		in.close();
	}
	
	@Test
	public void testGetInputStream_UTFwithBOM_Compressed() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt.gz";
		OutputStream out = IOUtils.getOutputStream(filename);
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		out.write("ABCdef".getBytes());
		out.close();

		InputStream in = IOUtils.getInputStream(filename);
		Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
		in.close();
	}
	
	@Test
	public void testGetInputStream_UTFwithBOM_Lz4() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt.lz4";
		OutputStream out = IOUtils.getOutputStream(filename);
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		out.write("ABCdef".getBytes());
		out.close();

		InputStream in = IOUtils.getInputStream(filename);
		Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
		in.close();
	}
	
	@Test
	public void testGetInputStream_UTFwithBOM_bz2() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt.bz2";
		OutputStream out = IOUtils.getOutputStream(filename);
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		out.write("ABCdef".getBytes());
		out.close();

		InputStream in = IOUtils.getInputStream(filename);
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
			BufferedReader in = IOUtils.getBufferedReader(filename);
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(filename, IOUtils.CHARSET_UTF8);
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(filename, IOUtils.CHARSET_WINDOWS_ISO88591);
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
			BufferedReader in = IOUtils.getBufferedReader(filename);
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(filename, IOUtils.CHARSET_UTF8);
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(filename, IOUtils.CHARSET_WINDOWS_ISO88591);
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
	}

	@Test
	public void testGetBufferedReader_UTFwithBOM_Compressed() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt.gz";
		OutputStream out = IOUtils.getOutputStream(filename);
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		out.write("ABCdef".getBytes());
		out.close();
		
		{
			BufferedReader in = IOUtils.getBufferedReader(filename);
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(filename.substring(0, filename.length() - 3)); // without the .gz extension
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(filename, IOUtils.CHARSET_UTF8);
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(filename, IOUtils.CHARSET_WINDOWS_ISO88591);
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
	}
	
	@Test
	public void testGetBufferedReader_UTFwithBOM_lz4() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt.lz4";
		OutputStream out = IOUtils.getOutputStream(filename);
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		out.write("ABCdef".getBytes());
		out.close();
		
		{
			BufferedReader in = IOUtils.getBufferedReader(filename);
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(filename, IOUtils.CHARSET_UTF8);
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(filename, IOUtils.CHARSET_WINDOWS_ISO88591);
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
	}
	
	@Test
	public void testGetBufferedReader_UTFwithBOM_bz2() throws IOException {
		String filename = utils.getOutputDirectory() + "test.txt.bz2";
		OutputStream out = IOUtils.getOutputStream(filename);
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		out.write("ABCdef".getBytes());
		out.close();
		
		{
			BufferedReader in = IOUtils.getBufferedReader(filename);
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(filename, IOUtils.CHARSET_UTF8);
			Assert.assertEquals("ABCdef", new String(new byte[] { (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read(), (byte) in.read() }));
			in.close();
		}
		{
			BufferedReader in = IOUtils.getBufferedReader(filename, IOUtils.CHARSET_WINDOWS_ISO88591);
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
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
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
		URL url = IOUtils.newUrl(context, "C:\\windows\\directory\\filename.txt");
		System.out.println(url.toString());
	}

}

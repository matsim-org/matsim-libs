package org.matsim.core.controler;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.BitSet;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;

class OutputDirectoryLoggingTest {
	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();
	
	private final static Logger log = LogManager.getLogger(OutputDirectoryLoggingTest.class);
	
	@Test
	void testParallelLogEntryCatching() throws IOException {
		final int numEntries = 10;
		OutputDirectoryLogging.catchLogEntries();
		
		IntStream.range(0, numEntries).parallel().forEach(i -> log.info("Log message " + i));
		
		OutputDirectoryLogging.initLoggingWithOutputDirectory(utils.getOutputDirectory());
		
		BitSet writtenNumbers = new BitSet(numEntries);
		try(BufferedReader reader = IOUtils.getBufferedReader(utils.getOutputDirectory() + OutputDirectoryLogging.LOGFILE)) {
			for (int i = 0; i < numEntries; i ++) {
				String line = reader.readLine();
				int lastSpaceIndex = line.lastIndexOf(" ");
				int number = Integer.parseInt(line.substring(lastSpaceIndex).trim());
				assertFalse(writtenNumbers.get(number), "Encountered log message " + number + " twice");
				writtenNumbers.set(number);
			}
		}
		for (int i = 0; i < numEntries; i ++) {
			assertTrue(writtenNumbers.get(i), "Missing log message " + i);
		}
	}

}

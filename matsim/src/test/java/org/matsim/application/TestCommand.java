package org.matsim.application;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test command that writes content to file.
 */
public class TestCommand implements MATSimAppCommand {

	private final Path out;
	private final String content;

	public TestCommand(Path out, String content) {
		this.out = out;
		this.content = content;
	}


	@Override
	public Integer call() throws Exception {

		Files.writeString(out, content);

		return 0;
	}
}

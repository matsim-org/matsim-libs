package org.matsim.core.events.algorithms;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.utils.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * @author mrieser / Simunto GmbH
 * Code contributed by Data Foundry, LLC.
 */
public class EventWriterJson implements EventWriter, BasicEventHandler {
	private OutputStream out = null;
	private JsonGenerator jsonGenerator = null;

	public EventWriterJson(File outfile) {
		try {
			this.out = IOUtils.getOutputStream(outfile.toURI().toURL(), false);
			this.jsonGenerator = new JsonFactory().createGenerator(this.out);
			this.jsonGenerator.setPrettyPrinter(new MinimalPrettyPrinter("\n"));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public EventWriterJson(OutputStream stream) {
		try {
			this.out = stream;
			this.jsonGenerator = new JsonFactory().createGenerator(this.out);
			this.jsonGenerator.setPrettyPrinter(new MinimalPrettyPrinter("\n"));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void closeFile() {
		try {
			this.jsonGenerator.close();
			this.out.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void handleEvent(final Event event) {
		try {
			this.jsonGenerator.writeStartObject();
			Map<String, String> attr = event.getAttributes();
			for(Map.Entry<String, String> entry : attr.entrySet()) {
				this.jsonGenerator.writeStringField(entry.getKey(), entry.getValue());
			}
			this.jsonGenerator.writeEndObject();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void reset(final int iter) {
	}

}

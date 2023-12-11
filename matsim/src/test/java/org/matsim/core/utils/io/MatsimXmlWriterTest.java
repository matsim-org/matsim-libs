package org.matsim.core.utils.io;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * @author mrieser / Simunto
 */
public class MatsimXmlWriterTest {


	/**
	 * According to the type specificiation for the data type xs:double in XSD,
	 * special values like infinity and not-a-number must be written as "INF", "-INF" and "NaN".
	 */
	@Test
	void testWriteInfiniteValuesInAttributes() {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DummyXmlWriter writer = new DummyXmlWriter();

		writer.openOutputStream(baos);
		writer.writeData();
		writer.close();

		String xml = baos.toString();
//		System.out.println(xml);

		assertTrue(xml.contains("double=\"1.234\""));
		assertTrue(xml.contains("negative=\"-INF\""));
		assertTrue(xml.contains("positive=\"INF\""));
		assertTrue(xml.contains("notanumber=\"NaN\""));
	}

	private static class DummyXmlWriter extends MatsimXmlWriter {
		public void writeData() {
			this.writeXmlHead();
			this.writeStartTag("root",
				List.of(
					createTuple("double", 1.234),
					createTuple("negative", Double.NEGATIVE_INFINITY),
					createTuple("positive", Double.POSITIVE_INFINITY),
					createTuple("notanumber", Double.NaN)
					));
			this.writeEndTag("root");
		}
	}

}

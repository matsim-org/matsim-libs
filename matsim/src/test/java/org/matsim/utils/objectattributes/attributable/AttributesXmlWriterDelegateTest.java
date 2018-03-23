/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package org.matsim.utils.objectattributes.attributable;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.core.utils.io.MatsimXmlParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Stack;

/**
 * @author mrieser / SBB
 */
public class AttributesXmlWriterDelegateTest {

	@Test
	public void testWrite_specialCharacters() throws IOException {
	    Attributes attributes = new Attributes();

        attributes.putAttribute("normal", "one");
        attributes.putAttribute("special", "two, three & four % five");
        attributes.putAttribute("special&2", "six # seven");
        attributes.putAttribute("special<3>\"'", "eight > nine < ten\"'");

        AttributesXmlWriterDelegate outDelegate = new AttributesXmlWriterDelegate();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(outStream);
        outDelegate.writeAttributes("   ", writer, attributes);

        writer.flush();

        String rawXml = new String(outStream.toByteArray());
//        System.out.println(rawXml);

        Attributes attributes2 = new Attributes();
        DelegatingParser testParser = new DelegatingParser(attributes2);
        ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());
        testParser.parse(inStream);

        Assert.assertEquals("one", attributes2.getAttribute("normal"));
        Assert.assertEquals("two, three & four % five", attributes2.getAttribute("special"));
        Assert.assertEquals("six # seven", attributes2.getAttribute("special&2"));
        Assert.assertEquals("eight > nine < ten\"'", attributes2.getAttribute("special<3>\"'"));

        String expected = "   <attributes>\n" +
                "   \t<attribute name=\"normal\" class=\"java.lang.String\" >one</attribute>\n" +
                "   \t<attribute name=\"special\" class=\"java.lang.String\" >two, three &amp; four % five</attribute>\n" +
                "   \t<attribute name=\"special&amp;2\" class=\"java.lang.String\" >six # seven</attribute>\n" +
                "   \t<attribute name=\"special&lt;3&gt;&quot;'\" class=\"java.lang.String\" >eight &gt; nine &lt; ten\"'</attribute>\n" +
                "   </attributes>\n";

        Assert.assertEquals(expected, rawXml);
    }

    private static class DelegatingParser extends MatsimXmlParser {
	    AttributesXmlReaderDelegate delegate = new AttributesXmlReaderDelegate();
	    Attributes attributes;

        public DelegatingParser(Attributes attributes) {
            this.attributes = attributes;
            this.setValidating(false);
        }

        @Override
        public void startTag(String name, org.xml.sax.Attributes atts, Stack<String> context) {
            this.delegate.startTag(name, atts, context, this.attributes);
        }

        @Override
        public void endTag(String name, String content, Stack<String> context) {
            this.delegate.endTag(name, content, context);
        }
    }

}
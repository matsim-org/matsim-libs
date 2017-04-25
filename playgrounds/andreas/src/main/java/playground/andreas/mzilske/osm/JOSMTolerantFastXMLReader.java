// This software is released into the Public Domain.  See copying.txt for details.
package playground.andreas.mzilske.osm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.xml.common.CompressionActivator;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;


/**
 * An OSM data source reading from an xml file. The entire contents of the file
 * are read.
 * 
 * @author Jiri Clement
 * @author Brett Henderson
 */
public class JOSMTolerantFastXMLReader implements RunnableSource {
		
		private static Logger log = Logger.getLogger(JOSMTolerantFastXMLReader.class.getName());
		
		private Sink sink;
		private final File file;
		private final boolean enableDateParsing;
		private final CompressionMethod compressionMethod;
		
		
		/**
		 * Creates a new instance.
		 * 
		 * @param file
		 *            The file to read.
		 * @param enableDateParsing
		 *            If true, dates will be parsed from xml data, else the current
		 *            date will be used thus saving parsing time.
		 * @param compressionMethod
		 *            Specifies the compression method to employ.
		 */
		public JOSMTolerantFastXMLReader(File file, boolean enableDateParsing, CompressionMethod compressionMethod) {
			this.file = file;
			this.enableDateParsing = enableDateParsing;
			this.compressionMethod = compressionMethod;
		}
		
		
		/**
		 * {@inheritDoc}
		 */
		public void setSink(Sink sink) {
			this.sink = sink;
		}
		
				
		
		/**
		 * Reads all data from the file and send it to the sink.
		 */
		public void run() {
			InputStream inputStream = null;
			JOSMTolerantFastXMLParser parser = null;
			
			try {
				
				// make "-" an alias for /dev/stdin
				if(file.getName().equals("-")) {
					inputStream = System.in;
				} else {
					inputStream = new FileInputStream(file);
				}
				
				
				inputStream =
					new CompressionActivator(compressionMethod).
						createCompressionInputStream(inputStream);
				
		        XMLInputFactory factory = XMLInputFactory.newInstance();
		        factory.setProperty(XMLInputFactory.IS_COALESCING, false);
		        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
		        factory.setProperty(XMLInputFactory.IS_VALIDATING, false);
		        XMLStreamReader xpp = factory.createXMLStreamReader(inputStream);
				
				parser = new JOSMTolerantFastXMLParser(sink, xpp, enableDateParsing);
				parser.readOsm();
				
				sink.complete();
				
			} catch (Exception e) {
				throw new OsmosisRuntimeException("Unable to read XML file " + file + ".", e);
			} finally {
				sink.release();
				
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (IOException e) {
						log.log(Level.SEVERE, "Unable to close input stream.", e);
					}
					inputStream = null;
				}
			}
		}
}

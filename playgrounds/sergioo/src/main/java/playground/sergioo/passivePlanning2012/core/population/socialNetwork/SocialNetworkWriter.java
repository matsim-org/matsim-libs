package playground.sergioo.passivePlanning2012.core.population.socialNetwork;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.io.UncheckedIOException;

public class SocialNetworkWriter extends MatsimXmlWriter implements MatsimWriter {

	private static final Logger log = Logger.getLogger(SocialNetworkWriter.class);
	
	private final SocialNetwork socialNetwork;

	public SocialNetworkWriter(final SocialNetwork socialNetwork) {
		super();
		this.socialNetwork = socialNetwork;
	}

	@Override
	public void write(final String filename) {
		log.info("Writing network to file: " + filename  + "...");
		// always write out in newest version, currently v1
		writeFileV1(filename);
		log.info("done.");
	}

	public void writeFileV1(final String filename) {
		String dtd = "http://www.matsim.org/files/dtd/social_network.dtd";
		try {
			openFile(filename);
			writeXmlHead();
			writeDoctype("social_network", dtd);
			startSocialNetwork(socialNetwork, this.writer);
			for(Entry<Id<Person>, Map<Id<Person>, Set<String>>> entry: socialNetwork.getNetwork().entrySet()) {
				Id<Person> egoId = entry.getKey();
				for(Entry<Id<Person>, Set<String>> types:entry.getValue().entrySet()) {
					Id<Person> alterId = types.getKey();
					for(String type:types.getValue()) {
						startRelation(egoId, alterId, type, this.writer);
						endRelation(this.writer);
					}
				}
			}
			endRelation(this.writer);
			endSocialNetwork(this.writer);
			this.writer.close();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	public void startSocialNetwork(final SocialNetwork socialNetwork, final BufferedWriter out) throws IOException {
		out.write("<social_network");
		if (socialNetwork.getDescription() != null) {
			out.write(" desc=\"" + socialNetwork.getDescription() + "\"");
		}
		out.write(">\n\n");
	}

	public void endSocialNetwork(final BufferedWriter out) throws IOException {
		out.write("</social_network>\n");
	}

	//////////////////////////////////////////////////////////////////////
	// <relation ... > ... </relation>
	//////////////////////////////////////////////////////////////////////

	public void startRelation(final Id<Person> egoId, final Id<Person> alterId, final String type, final BufferedWriter out) throws IOException {
		out.write("\t<relation");
		out.write(" id_ego=\"" + egoId + "\"");
		out.write(" id_alter=\"" + alterId + "\"");
		if (type != null)
			out.write(" type=\"" + type + "\"");
		out.write(" />\n");
	}

	public void endRelation(final BufferedWriter out) throws IOException {
	}

	//////////////////////////////////////////////////////////////////////
	// <!-- ============ ... ========== -->
	//////////////////////////////////////////////////////////////////////

	public void writeSeparator(final BufferedWriter out) throws IOException {
		out.write("<!-- =================================================" +
							"===================== -->\n\n");
	}
}

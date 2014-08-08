package playground.mzilske.ant2014;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


public class ExperimentResource {

	private final String wd;

	public ExperimentResource(String wd) {
		this.wd = wd;
	}

	public Collection<String> getRegimes() {
		final Set<String> REGIMES = new HashSet<String>();
		REGIMES.add("uncongested");
		REGIMES.add("congested");
		return REGIMES;
	}

	public RegimeResource getRegime(String regime) {
		return new RegimeResource(wd + "regimes/" + regime, regime);
	}

	public void personKilometers() {	
		FileIO.writeToFile(wd + "person-kilometers.txt", new StreamingOutput() {
			@Override
			public void write(final PrintWriter pw) throws IOException {
				boolean first = true;
				for (final String regime : getRegimes()) {
					final boolean first2 = first;
					FileIO.readFromResponse(getRegime(regime).getMultiRateRun("regular").getPersonKilometers(), new Reading() {
						@Override
						public void read(BufferedReader br) throws IOException {
							String header = br.readLine();
							if (first2) {
								pw.println(header);
							}
							String line = br.readLine();
							while (line != null) {
								pw.println(line);
								line = br.readLine();
							}
						}
					});
					first = false;
				}			
			}
		});
	}
}

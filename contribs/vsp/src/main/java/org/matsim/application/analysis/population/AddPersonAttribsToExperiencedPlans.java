package org.matsim.application.analysis.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

@CommandLine.Command(name = "run-vtts-analysis", description = "")
public class AddPersonAttribsToExperiencedPlans implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger( AddPersonAttribsToExperiencedPlans.class );

	@CommandLine.Option(names = "--path", description = "Path to output folder", required = true)
	private Path path;

	@CommandLine.Option(names = "--runId", description = "Run id (i.e. prefixes of files)")
	private String runId;

//	@CommandLine.Option(names = "--prefix", description = "Prefix for filtered events output file, optional." )
//	private String prefix;
//
//	@CommandLine.Option(names = "--threads", description = "Number of threads to use for processing events", defaultValue = "1")
//	private int numberOfThreads = 1;

	public static void main(String[] args) {
		new AddPersonAttribsToExperiencedPlans().execute(args );
	}

	@Override
	public Integer call() throws Exception {
		String runPrefix = Objects.nonNull(runId ) ? runId + "." : "";

		Path outputPopPath = path.resolve( runPrefix + "output_" + Controler.DefaultFiles.population.getFilename() + ".gz");
		Path expPlansPath =  path.resolve( runPrefix + "output_" + Controler.DefaultFiles.experiencedPlans.getFilename() + ".gz");

		Path postprocExpPlansPath = path.resolve( runPrefix + "postproc_" + Controler.DefaultFiles.experiencedPlans.getFilename() + ".gz");

		// ---

		Population outputPop = PopulationUtils.readPopulation( outputPopPath.toString() );
		Population experiencedPlans = PopulationUtils.readPopulation( expPlansPath.toString() );

		for( Person person : experiencedPlans.getPersons().values() ){
			Person personFromOutputPlans = outputPop.getPersons().get( person.getId() );
			for( Map.Entry<String, Object> entry2 : personFromOutputPlans.getAttributes().getAsMap().entrySet() ){
				person.getAttributes().putAttribute( entry2.getKey(),entry2.getValue() );
				// note that this is not a completely deep copy.  Should not be a problem since we only write to file, but in the
				// end we never know.  kai, oct'25
			}
		}

		PopulationUtils.writePopulation( experiencedPlans, postprocExpPlansPath.toString() );

		return 0;
	}
}

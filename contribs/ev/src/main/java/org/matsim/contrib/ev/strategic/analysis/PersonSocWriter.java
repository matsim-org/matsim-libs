package org.matsim.contrib.ev.strategic.analysis;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.ev.strategic.scoring.ChargingPlanScoring;
import org.matsim.core.config.groups.ControllerConfigGroup.CompressionType;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;

public class PersonSocWriter implements IterationEndsListener {
    static public final String OUTPUT_FILE = "sevc_person_socs.csv";

    private final CompressionType compressionType;
    private final OutputDirectoryHierarchy outputDirectoryHierarchy;
    private final Population population;

    private boolean written = false;

    public PersonSocWriter(Population population, OutputDirectoryHierarchy outputDirectoryHierarchy,
            CompressionType compressionType) {
        this.population = population;
        this.outputDirectoryHierarchy = outputDirectoryHierarchy;
        this.compressionType = compressionType;
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (!written) {
            written = true; // write after first iteration

            String outputPath = outputDirectoryHierarchy.getOutputFilename(OUTPUT_FILE, compressionType);

            try {
                BufferedWriter writer = IOUtils.getBufferedWriter(outputPath);

                writer.write(String.join(";", new String[] {
                        "person_id", "minimum_soc", "minimum_end_soc", "target_soc"
                }) + "\n");

                for (Person person : population.getPersons().values()) {
                    Double minimumSoc = ChargingPlanScoring.getMinimumSoc(person);
                    Double minimumEndSoc = ChargingPlanScoring.getMinimumEndSoc(person);
                    Double targetSoc = ChargingPlanScoring.getTargetSoc(person);

                    writer.write(String.join(";", new String[] {
                            person.getId().toString(), String.valueOf(minimumSoc), String.valueOf(minimumEndSoc),
                            String.valueOf(targetSoc)
                    }) + "\n");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

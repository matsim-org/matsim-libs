package org.matsim.application.prepare.freight.dataProcessing;

import org.matsim.application.MATSimAppCommand;
import picocli.CommandLine;

import java.nio.file.Path;

public class GenerateLookupTable implements MATSimAppCommand {

    @CommandLine.Option(names = "--input", description = "input region list (Verkehrszellen)", required = true)
    private Path input;

    @CommandLine.Option(names = "--germany", description = "german lookup table", required = true)
    private Path germanTable;

    @CommandLine.Option(names = "--international", description = "international lookup table", required = true)
    private Path internationalTable;

    @CommandLine.Option(names = "--output", description = "output lookup table", required = true)
    private Path output;

    public static void main(String[] args) {



    }

    @Override
    public Integer call() throws Exception {

        return 0;
    }
}

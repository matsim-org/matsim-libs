/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,       *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.johannes.studies.matrix2014.demo;

import org.apache.log4j.Logger;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.InterpolatingDiscretizer;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.common.util.XORShiftRandom;
import playground.johannes.gsv.popsim.AgeIncomeCorrelation;
import playground.johannes.gsv.popsim.AnalyzerListener;
import playground.johannes.gsv.popsim.CollectionUtils;
import playground.johannes.gsv.popsim.analysis.*;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PersonUtils;
import playground.johannes.synpop.data.PlainFactory;
import playground.johannes.synpop.data.io.PopulationIO;
import playground.johannes.synpop.processing.PersonTask;
import playground.johannes.synpop.processing.TaskRunner;
import playground.johannes.synpop.sim.*;

import java.util.Collection;
import java.util.Random;
import java.util.Set;

/**
 * @author jillenberger
 */
public class AgeIncomeDemo {

    private static final Logger logger = Logger.getLogger(AgeIncomeDemo.class);

    public static void main(String args[]) {
        String refPopFile = "/home/johannes/gsv/matrix2014/popgen/demo/mid2008.midtrips.validated.xml";
        int simPopSize = 100000;
        long iterations = (long)1e7;
        int logInterval = (int)1e6;
        int dumpInterval = (int)2e6;
        String outputRoot = "/home/johannes/gsv/matrix2014/popgen/demo/output/";

        Random random = new XORShiftRandom(4711);

        logger.info("Loading reference population...");
        Set<? extends Person> refPersons = PopulationIO.loadFromXML(refPopFile, new PlainFactory());

        logger.info("Generating simulation population...");
        Set<? extends Person> simPersons = PersonUtils.weightedCopy(refPersons, new PlainFactory(), simPopSize, random);

        logger.info("Initializing simulation population...");
        final RandomIntGenerator ageGenerator = new RandomIntGenerator(random, 0, 100);
        TaskRunner.run(new PersonTask() {
            @Override
            public void apply(Person person) {
                Double val = (Double)ageGenerator.newValue(null);
                person.setAttribute(CommonKeys.PERSON_AGE, val.toString());
            }
        }, simPersons);

        final RandomIntGenerator incomeGenerator = new RandomIntGenerator(random, 500, 8000);
        TaskRunner.run(new PersonTask() {
            @Override
            public void apply(Person person) {
                Double val = (Double)incomeGenerator.newValue(null);
                person.setAttribute(CommonKeys.HH_INCOME, val.toString());
            }
        }, simPersons);
        /*
        Setup analyzer...
         */
        logger.info("Setting up analyzer...");
        FileIOContext ioContext = new FileIOContext(outputRoot);

        NumericAnalyzer ageAnalyzer = new NumericAnalyzer(
                new PersonCollector<>(new NumericAttributeProvider<Person>(CommonKeys.PERSON_AGE)), CommonKeys.PERSON_AGE);
        NumericAnalyzer incomeAnalyzer = new NumericAnalyzer(
                new PersonCollector<>(new NumericAttributeProvider<Person>(CommonKeys.HH_INCOME)), CommonKeys.HH_INCOME);

        ageAnalyzer.setIoContext(ioContext);
        incomeAnalyzer.setIoContext(ioContext);

        ageAnalyzer.addDiscretizer(new PassThroughDiscretizerBuilder(new LinearDiscretizer(1)), "linear");

        PersonCollector<Double> incomeCollector = new PersonCollector<>(new NumericAttributeProvider<Person>(CommonKeys.HH_INCOME));
        double[] incomeValues = CollectionUtils.toNativeArray(incomeCollector.collect(refPersons));
        Discretizer incomeDiscretizer = new InterpolatingDiscretizer(incomeValues);
//        incomeAnalyzer.addDiscretizer(new LinearDiscretizer(500), "linear");
        incomeAnalyzer.addDiscretizer(new PassThroughDiscretizerBuilder(incomeDiscretizer), "linear");

        AgeIncomeCorrelation ageIncomeCorrelation = new AgeIncomeCorrelation();
        ageIncomeCorrelation.setIoContext(ioContext);

        ConcurrentAnalyzerTask<Collection<? extends Person>> task = new ConcurrentAnalyzerTask<>();
        task.addComponent(ageAnalyzer);
        task.addComponent(incomeAnalyzer);
        task.addComponent(ageIncomeCorrelation);

        logger.info("Analyzing reference population...");
        ioContext.append("ref");
        AnalyzerTaskRunner.run(refPersons, task, ioContext);
        /*
        Setup hamiltonian...
         */
        logger.info("Setting up hamiltonian...");
        Discretizer ageDiscretizer = new LinearDiscretizer(1);
        UnivariatFrequency ageTerm = new UnivariatFrequency(refPersons, simPersons, CommonKeys.PERSON_AGE, ageDiscretizer);


//        Discretizer incomeDiscretizer = new LinearDiscretizer(500);
        UnivariatFrequency incomeTerm = new UnivariatFrequency(refPersons, simPersons, CommonKeys.HH_INCOME, incomeDiscretizer);

        BivariatMean ageIncomeTerm = new BivariatMean(refPersons, simPersons, CommonKeys.PERSON_AGE, CommonKeys.HH_INCOME, ageDiscretizer);

        HamiltonianComposite hamiltonian = new HamiltonianComposite();
        hamiltonian.addComponent(ageTerm, 10);
        hamiltonian.addComponent(incomeTerm, 1e4);
        hamiltonian.addComponent(ageIncomeTerm, 0.6);
        /*
        Setup mutators...
         */
        logger.info("Setting up mutators...");
        AttributeChangeListenerComposite changeListerners = new AttributeChangeListenerComposite();
        changeListerners.addComponent(ageTerm);
        changeListerners.addComponent(incomeTerm);
        changeListerners.addComponent(ageIncomeTerm);

        AgeMutatorBuilder ageBuilder = new AgeMutatorBuilder(changeListerners, random);
        IncomeMutatorBuilder incomeBuilder = new IncomeMutatorBuilder(changeListerners, random);

        MutatorComposite mutators = new MutatorComposite(random);
        mutators.addMutator(ageBuilder.build());
        mutators.addMutator(incomeBuilder.build());
        /*
        Setup engine listeners...
         */
        logger.info("Setting up engine listeners...");
        MarkovEngineListenerComposite listeners = new MarkovEngineListenerComposite();

        listeners.addComponent(new AnalyzerListener(task, ioContext, dumpInterval));
        listeners.addComponent(new HamiltonianLogger(hamiltonian, logInterval, "SystemTemperature"));
        listeners.addComponent(new HamiltonianLogger(ageTerm, logInterval, "AgeDistribution"));
        listeners.addComponent(new HamiltonianLogger(incomeTerm, logInterval, "IncomeDistribution"));
        listeners.addComponent(new HamiltonianLogger(ageIncomeTerm, logInterval, "AgeMeanIncome"));
        /*
        Setup markov engine...
         */
        logger.info("Starting sampling...");
        MarkovEngine engine = new MarkovEngine(simPersons, hamiltonian, mutators, random);
        engine.setListener(listeners);

        engine.run(iterations+1);

        Executor.shutdown();
        logger.info("Done.");
    }
}

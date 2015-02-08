package playground.pieter.distributed.listeners.controler;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import playground.pieter.distributed.plans.PlanGenome;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by fouriep on 1/30/15.
 */
public class GenomeAnalysis implements IterationEndsListener {
    private final boolean writePlanScores;
    private final boolean writeFullGenomeStats;

    public GenomeAnalysis(boolean writePlanScores, boolean writeFullGenomeStats) {
        this.writePlanScores = writePlanScores;
        this.writeFullGenomeStats = writeFullGenomeStats;
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        Map<String, Integer> fullGeneCount = new HashMap<>();
        Map<String, Double> fullGeneScore = new HashMap<>();
        Map<String, Double> fullGeneAltScore = new HashMap<>();
        Map<Id<Person>, ? extends Person> persons = event.getControler().getScenario().getPopulation().getPersons();
        boolean append = event.getIteration() != event.getControler().getConfig().controler().getFirstIteration();
        try {
            PrintWriter writer = null;
            if (writePlanScores) {
                writer = new PrintWriter(new BufferedWriter(new FileWriter(event.getControler().getControlerIO().getOutputPath() + "/planS" +
                        "cores.csv", append)));
                if (!append)
                    writer.println(String.format("iter\tpersonid\tgenome\tscore\taltscore"));
            }
            for (Person person : persons.values()) {
                List<? extends Plan> plans = person.getPlans();
                for (Plan plan : plans) {
                    PlanGenome planGenome = (PlanGenome) plan;
                    if (writePlanScores) {
                        writer.println(String.format("%d\t%s\t%s\t%.3f\t%.3f", event.getIteration(), person.getId(), planGenome.getGenome(), planGenome.getScore(), planGenome.getpSimScore()));
                    }
                    try {
                        int count = fullGeneCount.get(planGenome.getGenome());
                        fullGeneCount.put(planGenome.getGenome(), count + 1);
                        double meanScore = fullGeneScore.get(planGenome.getGenome());
                        double dcount = (double) count;
                        meanScore = (meanScore * dcount + planGenome.getScore()) / (dcount + 1);
                        fullGeneScore.put(planGenome.getGenome(), meanScore);
                        meanScore = fullGeneAltScore.get(planGenome.getGenome());
                        meanScore = (meanScore * dcount + planGenome.getpSimScore()) / (dcount + 1);
                        fullGeneAltScore.put(planGenome.getGenome(), meanScore);
                    } catch (NullPointerException e) {
                        fullGeneCount.put(planGenome.getGenome(), 1);
                        fullGeneScore.put(planGenome.getGenome(), planGenome.getScore());
                        fullGeneAltScore.put(planGenome.getGenome(), planGenome.getpSimScore());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Logger logger = Logger.getLogger(this.getClass());
        List<String> keys = new ArrayList<>();
        keys.addAll(fullGeneCount.keySet());
        Collections.sort(keys);
        if (writeFullGenomeStats) {
            logger.info("Printing genome stats");
            try {
                PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(event.getControler().getControlerIO().getOutputPath() + "/planEvo.csv", append)));
                if (!append)
                    writer.println(String.format("iter\tgenome\tcount\tscore\taltscore"));
                for (String key : keys) {
                    writer.println(String.format("%d\t%s\t%d\t%.3f\t%.3f", event.getIteration(), key, fullGeneCount.get(key), fullGeneScore.get(key), fullGeneAltScore.get(key)));
                }
                writer.flush();
                writer.close();
//            Set<SimpleLink> edges = new HashSet<>();
//            for(String key:keys){
//                edges.add(new SimpleLink(key, fullGeneCount.get(key),fullGeneScore.get(key),fullGeneAltScore.get(key)));
//            }
//            writer = new PrintWriter(new BufferedWriter(new FileWriter(event.getControler().getControlerIO().getOutputPath() + "/planEvoEdges.csv", false)));
//                writer.println(String.format("Id\tSource\tTarget\tType\tLabel\tCount\tScore\taltscore"));
//            for (SimpleLink edge : edges) {
//                writer.println(String.format("%s\t%s\t%s\tUndirected\t%s\t%d\t%.3f\t%.3f", edge.to,edge.from,edge.to,edge.type, edge.count,edge.score,edge.altscore));
//            }
//            writer.flush();
//            writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Map<String, Integer> strategyMutationCount = new HashMap<>();
        Map<String, Integer> simpleGeneCount = new HashMap<>();
        Map<String, Double> simpleGeneScore = new HashMap<>();
        Map<String, Double> simpleGeneAltScore = new HashMap<>();
        Map<Integer, Integer> geneLengths = new TreeMap<>();
        Map<Integer, Double> geneScoresbyLength = new TreeMap<>();

        for (String s : fullGeneCount.keySet()) {
            String[] charSplit = s.split("[0-9]*");
            String[] numSplit = s.split("[A-Z]+");
            int geneLength = charSplit.length / 2 + 1;
            if (geneLengths.get(geneLength) != null) {
                geneLengths.put(geneLength, geneLengths.get(geneLength) + fullGeneCount.get(s));
                geneScoresbyLength.put(geneLength,
                        (geneScoresbyLength.get(geneLength) * geneLengths.get(geneLength) +
                                fullGeneScore.get(s) * fullGeneCount.get(s)) / (geneLengths.get(geneLength) + fullGeneCount.get(s)));
            } else {
                geneLengths.put(geneLength, fullGeneCount.get(s));
                geneScoresbyLength.put(geneLength, fullGeneScore.get(s));
            }
            if (charSplit.length == 1) {
                String simpleGene = "I0000";
                strategyMutationCount.put("I0000", fullGeneCount.get(s));
                simpleGeneCount.put(simpleGene, fullGeneCount.get(s));
                simpleGeneScore.put(simpleGene, fullGeneScore.get(s));
                simpleGeneAltScore.put(simpleGene, fullGeneAltScore.get(s));
            } else {

                for (int i = 2; i < charSplit.length; i += 2) {
                    String simpleGene = charSplit[i] + numSplit[i / 2 + 1];
                    try {
                        int count = strategyMutationCount.get(charSplit[i]);
                        strategyMutationCount.put(charSplit[i], fullGeneCount.get(s) + count);
                        count = simpleGeneCount.get(simpleGene);
                        simpleGeneCount.put(simpleGene, count + fullGeneCount.get(s));
                        Double score = simpleGeneScore.get(simpleGene);
                        score = (score * (double) count + fullGeneScore.get(s) * (double) fullGeneCount.get(s)) / (count + fullGeneCount.get(s));
                        simpleGeneScore.put(simpleGene, score);
                        score = simpleGeneAltScore.get(simpleGene);
                        score = (score * (double) count + fullGeneAltScore.get(s) * (double) fullGeneCount.get(s)) / (count + fullGeneCount.get(s));
                        simpleGeneAltScore.put(simpleGene, score);
                    } catch (NullPointerException ne) {
                        strategyMutationCount.put(charSplit[i], fullGeneCount.get(s));
                        simpleGeneCount.put(simpleGene, fullGeneCount.get(s));
                        simpleGeneScore.put(simpleGene, fullGeneScore.get(s));
                        simpleGeneAltScore.put(simpleGene, fullGeneAltScore.get(s));
                    }
                }
            }
        }
        logger.warn("Printing mutation counts in surviving plans");
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(event.getControler().getControlerIO().getOutputPath() + "/survivorMutationCounts.csv", append)));
            if (!append)
                writer.println(String.format("iter\tmutator\tcount"));
            Iterator<Map.Entry<String, Integer>> iterator = strategyMutationCount.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Integer> next = iterator.next();
                writer.println(String.format("%d\t%s\t%d", event.getIteration(), next.getKey(), next.getValue()));
            }
            writer.flush();
            writer.close();
            logger.warn("Printing number of surviving plans by mutation and iteration");
            keys = new ArrayList<>();
            keys.addAll(simpleGeneCount.keySet());
            Collections.sort(keys);
            writer = new PrintWriter(new BufferedWriter(new FileWriter(event.getControler().getControlerIO().getOutputPath() + "/mutationAtIterSurvivorCounts.csv", append)));
            if (!append)
                writer.println(String.format("%s\t%s\t%s\t%s\t%s\t%s", "iter", "mutator", "mutationIter", "survivors", "avg.score", "avg.alt.score"));
            for (String key : keys) {
                writer.println(String.format("%s\t%s\t%d\t%d\t%.3f\t%.3f", event.getIteration(), key.substring(0, 1), Integer.parseInt(key.substring(1)), simpleGeneCount.get(key), simpleGeneScore.get(key), simpleGeneAltScore.get(key)));

            }
            writer.flush();
            writer.close();
            logger.warn("Gene lengths and average scores");
            writer = new PrintWriter(new BufferedWriter(new FileWriter(event.getControler().getControlerIO().getOutputPath() + "/geneLengths.csv", append)));
            if (!append)
                writer.println(String.format("%s\t%s\t%s\t%s", "iter", "len", "num", "avgscor"));
            for (int gl : geneLengths.keySet()) {
                writer.println(String.format("%d\t%d\t%d\t%.3f", event.getIteration(), gl, geneLengths.get(gl), geneScoresbyLength.get(gl)));
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.warn("Done");

    }
}

class SimpleLink {
    final int count;
    final double score;
    final double altscore;
    String from;
    String to;
    String type;

    public SimpleLink(String genome, int count, double score, double altscore) {
        to = genome;
        from = genome.substring(0, genome.length() - 5);
        type = genome.substring(genome.length() - 5, genome.length());
        this.count = count;
        this.score = score;
        this.altscore = altscore;
    }
}
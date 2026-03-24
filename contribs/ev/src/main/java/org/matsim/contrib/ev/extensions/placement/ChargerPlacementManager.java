package org.matsim.contrib.ev.extensions.placement;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.extensions.placement.ChargerPlacementConfigGroup.ChargerPlacementObjective;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.utils.io.IOUtils;

public class ChargerPlacementManager implements IterationStartsListener {
    private static final String OUTPUT_FILE = "ev_charger_placement.csv.gz";
    private static final String OBJECTIVE_OUTPUT_FILE = "ev_charger_placement_objectives.csv.gz";

    private final List<ChargerSpecification> chargers = new LinkedList<>();
    private final Map<Id<Charger>, Integer> blacklist = new HashMap<>();

    private final OutputDirectoryHierarchy outputDirectoryHierarchy;
    private final Network network;

    private final ChargerPlacementCollector collector;

    private final int interval;
    private final double removalQuantile;
    private final boolean removeUnused;
    private final ChargerPlacementObjective objective;

    public ChargerPlacementManager(ChargingInfrastructureSpecification infrastructure,
            OutputDirectoryHierarchy outputDirectoryHierarchy, ChargerPlacementCollector collector, Network network,
            int interval,
            double removalQuantile, boolean removeUnused, ChargerPlacementObjective objective) {
        this.outputDirectoryHierarchy = outputDirectoryHierarchy;
        this.network = network;
        this.collector = collector;
        this.interval = interval;
        this.removalQuantile = removalQuantile;
        this.removeUnused = removeUnused;
        this.objective = objective;

        for (ChargerSpecification charger : infrastructure.getChargerSpecifications().values()) {
            if (ChargerPlacement.isRemovable(charger)) {
                this.chargers.add(charger);
            }
        }
    }

    public boolean isBlacklisted(Id<Charger> chargerId) {
        return blacklist.containsKey(chargerId);
    }

    public Set<Id<Charger>> getBlacklist() {
        return blacklist.keySet();
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        writeObjectives(event.getIteration());

        if (event.getIteration() > 0 && event.getIteration() % interval == 0) {
            List<ChargerRecord> sorted = switch (objective) {
                case Revenue -> collect(collector::getRevenue);
                case Energy -> collect(collector::getEnergy);
            };

            if (removeUnused) {
                Iterator<ChargerRecord> iterator = sorted.iterator();

                while (iterator.hasNext()) {
                    ChargerRecord record = iterator.next();

                    if (!collector.isUsed(record.chargerId)) {
                        iterator.remove();
                        blacklist.put(record.chargerId, event.getIteration());
                    }
                }
            }

            if (sorted.size() > 0) {
                int remove = Math.max(1, (int) Math.floor(removalQuantile * sorted.size()));

                for (int k = 0; k < remove; k++) {
                    blacklist.put(sorted.get(k).chargerId(), event.getIteration());
                }
            }

            writeChargers(event.getIteration());
            collector.clear();
        }
    }

    private void writeChargers(int iteration) {
        try {
            String outputPath = outputDirectoryHierarchy.getOutputFilename(OUTPUT_FILE);

            BufferedWriter writer = IOUtils.getBufferedWriter(outputPath);

            writer.write(String.join(";", new String[] {
                    "charger_id", "link_id", "x", "y", "blacklisted", "iteration"
            }) + "\n");

            for (ChargerSpecification charger : chargers) {
                Link link = network.getLinks().get(charger.getLinkId());

                writer.write(String.join(";", new String[] {
                        charger.getId().toString(), //
                        link.getId().toString(), //
                        String.valueOf(link.getCoord().getX()), //
                        String.valueOf(link.getCoord().getY()), //
                        String.valueOf(blacklist.containsKey(charger.getId())), //
                        String.valueOf(blacklist.getOrDefault(charger.getId(), 0)) //
                }) + "\n");
            }

            writer.close();
        } catch (IOException e) {
        }
    }

    private void writeObjectives(int iteration) {
        try {
            String outputPath = outputDirectoryHierarchy.getIterationFilename(iteration, OBJECTIVE_OUTPUT_FILE);

            BufferedWriter writer = IOUtils.getBufferedWriter(outputPath);

            writer.write(String.join(";", new String[] {
                    "charger_id", "used", "revenue", "energy"
            }) + "\n");

            for (ChargerSpecification charger : chargers) {
                writer.write(String.join(";", new String[] {
                        charger.getId().toString(), //
                        String.valueOf(collector.isUsed(charger.getId())), //
                        String.valueOf(collector.getRevenue(charger.getId())), //
                        String.valueOf(collector.getEnergy(charger.getId())) //
                }) + "\n");
            }

            writer.close();
        } catch (IOException e) {
        }
    }

    private record ChargerRecord(Id<Charger> chargerId, double objective) {
    }

    private List<ChargerRecord> collect(Function<Id<Charger>, Double> objectiveFunction) {
        List<ChargerRecord> records = new LinkedList<>();

        for (ChargerSpecification charger : chargers) {
            if (!blacklist.containsKey(charger.getId())) {
                records.add(new ChargerRecord(charger.getId(), objectiveFunction.apply(charger.getId())));
            }
        }

        Collections.sort(records, Comparator.comparing(ChargerRecord::objective));

        return records;
    }

    public List<ChargerSpecification> getChargers() {
        return chargers;
    }
}

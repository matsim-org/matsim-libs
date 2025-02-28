package org.matsim.contrib.dvrp.load;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

/**
 * @author Tarek Chouaki (tkchouaki), IRT SystemX
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class IntegersLoadType implements DvrpLoadType {
    private final IntegersLoad emptyLoad;
    private final List<String> dimensions;

    public IntegersLoadType(String... dimensions) {
        Preconditions.checkArgument(dimensions.length > 0);
        this.dimensions = Arrays.asList(dimensions);
        this.emptyLoad = new IntegersLoad(new int[dimensions.length]);
    }

    public IntegersLoadType(List<String> dimensions) {
        Preconditions.checkArgument(dimensions.size() > 0);
        this.dimensions = dimensions;
        this.emptyLoad = new IntegersLoad(new int[dimensions.size()]);
    }

    @Override
    public DvrpLoad getEmptyLoad() {
        return emptyLoad;
    }

    @Override
    public List<String> getDimensions() {
        return dimensions;
    }

    @Override
    public int size() {
        return dimensions.size();
    }

    @Override
    public DvrpLoad fromMap(Map<String, Number> map) {
        Set<String> consumed = new HashSet<>(map.keySet());
        int[] values = new int[dimensions.size()];

        for (int i = 0; i < dimensions.size(); i++) {
            String slot = dimensions.get(i);
            values[i] = (int) map.getOrDefault(slot, 0);
            consumed.remove(slot);
        }

        Preconditions.checkState(consumed.isEmpty(),
                "Unknown slots provided: " + consumed.stream().collect(Collectors.joining(", ")));

        return new IntegersLoad(values);
    }

    @Override
    public String serialize(DvrpLoad load) {
        if (dimensions.size() == 1) {
            return String.valueOf(load.getElement(0));
        } else {
            Map<String, Integer> output = new LinkedHashMap<>();

            for (int i = 0; i < dimensions.size(); i++) {
                int value = (int) load.getElement(i);

                if (value > 0) {
                    output.put(dimensions.get(i), value);
                }
            }

            if (output.size() == 0) {
                return "0";
            } else {
                return output.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue())
                        .collect(Collectors.joining(","));
            }
        }
    }

    @Override
    public DvrpLoad deserialize(String representation) {
        if (representation.trim().equals("0")) {
            // empty
            return getEmptyLoad();
        } else if (!representation.contains("=")) {
            // list of numbers

            String[] components = representation.split(",");
            int[] values = new int[components.length];

            for (int k = 0; k < values.length; k++) {
                values[k] = Integer.parseInt(components[k].trim());
            }

            return new IntegersLoad(values);
        } else {
            // map to dimensions

            String[] components = representation.split(",");
            Map<String, Number> values = new HashMap<>();

            for (String component : components) {
                String[] pair = component.split("=");

                if (pair.length != 2) {
                    throw new IllegalStateException("Invalid load format: " + representation);
                }

                if (!dimensions.contains(pair[0].trim())) {
                    throw new IllegalStateException(
                            "Dimension '" + pair[0].trim() + "' does not exist in load: " + representation);
                }

                values.put(pair[0].trim(), Integer.parseInt(pair[1].trim()));
            }

            return fromMap(values);
        }
    }

    public IntegersLoad fromArray(int... values) {
        Preconditions.checkArgument(values.length == dimensions.size());
        return new IntegersLoad(values);
    }
}

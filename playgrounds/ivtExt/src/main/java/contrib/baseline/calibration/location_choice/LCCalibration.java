package contrib.baseline.calibration.location_choice;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class LCCalibration {
    static class Iteration {
        public double candidate;
        public double response;
    }

    static class State {
        final double K = 0.1;
        final String[] purposes = { "remote_work", "leisure", "shop", "escort_kids", "escort_other" };
        final double[] initialCandidates = { 60.00, 20.00, 7.00, 12.00, 16.00 };
        final double[] reference = { 29.054722, 13.128054, 6.889934, 5.991882, 12.379748 };

        public int iteration = 0;
        public ArrayList<Stack<Iteration>> history = new ArrayList<>(purposes.length);

        public double[] bestCandidates = new double[purposes.length];
        public double[] bestResponses = new double[purposes.length];

        public State() {
            for (int i = 0; i < purposes.length; i++) {
                bestCandidates[i] = Double.POSITIVE_INFINITY;
                bestResponses[i] = Double.POSITIVE_INFINITY;
                history.add(new Stack<>());
            }
        }
    }

    void run(String name, int percentage, int cores) throws IOException {
        File statePath = new File("state_" + name + ".json");
        LCScenarioRunner scenarioRunner = new LCScenarioRunner();

        while (true) {
            ObjectMapper objectMapper = new ObjectMapper();

            State state = new State();

            try {
                state = objectMapper.readValue(statePath, State.class);
            } catch (FileNotFoundException e) {}

            List<Double> newCandidates = new ArrayList<>(state.purposes.length);
            double newResponses[] = new double[state.purposes.length];

            if (state.iteration == 0) {
                for (int i = 0; i < state.purposes.length; i++) newCandidates.add(state.initialCandidates[i]);
            } else {
                double[] currentCandidates = state.history.stream().mapToDouble(s -> s.peek().candidate).toArray();
                double[] currentResponses = state.history.stream().mapToDouble(s -> s.peek().response).toArray();

                for (int i = 0; i < state.purposes.length; i++) {
                    newCandidates.add(currentCandidates[i] + state.K * (state.reference[i] - currentResponses[i]));
                }
            }

            Map<String, Double> mappedResponses = scenarioRunner.runScenario(name, state.iteration, percentage, cores, state.purposes, newCandidates);
            for (int i = 0; i < state.purposes.length; i++) newResponses[i] = mappedResponses.get(state.purposes[i]);

            for (int i = 0; i < state.purposes.length; i++) {
                Iteration iteration = new Iteration();
                iteration.candidate = newCandidates.get(i);
                iteration.response = newResponses[i];

                state.history.get(i).push(iteration);
            }

            state.iteration += 1;
            objectMapper.writeValue(statePath, state);
        }
    }

    public static void main(String[] args) throws IOException {
        LCCalibration calibration = new LCCalibration();
        calibration.run(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]));
    }
}

package playground.sebhoerl.mexec_opdyts;

import floetteroed.opdyts.SimulatorState;
import floetteroed.utilities.math.Vector;
import org.apache.commons.lang3.NotImplementedException;

public class Iteration implements SimulatorState {
    final private SimulationTracker tracker;

    final private double objectiveValue;

    public Iteration(SimulationTracker tracker, double objectiveValue) {
        this.tracker = tracker;
        this.objectiveValue = objectiveValue;
    }

    public double getObjectiveValue() {
        return objectiveValue;
    }

    @Override
    public Vector getReferenceToVectorRepresentation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void implementInSimulation() {
        tracker.implementIteration(this);
    }
}

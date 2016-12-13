package playground.sebhoerl.plcpc;

import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.vehicles.Vehicle;

public class ParallelLeastCostPathCalculatorTask {
    final public Node fromNode;
    final public Node toNode;
    final public double time;
    final public Person person;
    final public Vehicle vehicle;

    public LeastCostPathCalculator.Path result = null;

    public ParallelLeastCostPathCalculatorTask(Node fromNode, Node toNode, double time, Person person, Vehicle vehicle) {
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.time = time;
        this.person = person;
        this.vehicle = vehicle;
    }
}

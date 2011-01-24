package playground.mzilske.freight.vrp;

import java.util.ArrayList;
import java.util.Iterator;

public interface VehicleTour {

	public ArrayList<Node> getNodes();
	
	public Iterator<Node> tourIterator();

	public double getCostValue();

	public void setCost(double cost);

	public Node getFirst();

	public Node getLast();

	public boolean nodeIsAtEnd(Node node);

	public boolean nodeIsAtBeginning(Node node);

	public boolean nodeIsInterior(Node node);

	public String toString();

}
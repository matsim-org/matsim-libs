package playground.ciarif.retailers.data;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;

public class LinkRetailersImpl extends LinkImpl
{
  private static final long serialVersionUID = 1L;
  protected Node from = null;
  protected Node to = null;

  protected double length = (0.0D / 0.0D);
  protected double freespeed = (0.0D / 0.0D);
  protected double capacity = (0.0D / 0.0D);
  protected double nofLanes = (0.0D / 0.0D);
  protected int maxFacOnLink = 0;
  private double potentialCustomers;
  private double potentialCompetitors;
  
  public LinkRetailersImpl(Link link, NetworkImpl network, Double potentialCustomers, Double potentialCompetitors)
  {
    super(link.getId(), link.getFromNode(), link.getToNode(), network, link.getLength(), link.getFreespeed(), link.getCapacity(), link.getNumberOfLanes());
    this.potentialCustomers = potentialCustomers.doubleValue();
    this.potentialCompetitors = potentialCompetitors.doubleValue(); }

  public void setMaxFacOnLink(int max_number_facilities) {
    this.maxFacOnLink = max_number_facilities; }

  public void setPotentialCustomers(int potentialCustomers) {
    this.potentialCustomers = potentialCustomers; }

  public void setPotentialCustomers(double potentialCustomers) {
    this.potentialCustomers = potentialCustomers;
  }

  public void setPotentialCompetitors(int potentialCompetitors) {
    this.potentialCompetitors = potentialCompetitors;
  }

  public int getMaxFacOnLink() {
    return this.maxFacOnLink; }

  public double getPotentialCustomers() {
    return this.potentialCustomers; }

  public double getPotentialCompetitors() {
    return this.potentialCompetitors;
  }
}
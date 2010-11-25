package playground.ciarif.retailers.data;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

public class PersonPrimaryActivity
{
  private final Id id;
  private final Id personId;
  private String activityType;
  private IdImpl activityLink;

  public PersonPrimaryActivity(int id, IdImpl personId)
  {
    this.id = new IdImpl(id);
    this.personId = personId;
  }

  public PersonPrimaryActivity(String activityType, int id, Id personId, IdImpl activityLink)
  {
    this.id = new IdImpl(id);
    this.personId = personId;
    this.activityType = activityType;
    this.activityLink = activityLink;
  }

  public String getActivityType() {
    return this.activityType;
  }

  public void setActivityType(String activityType) {
    this.activityType = activityType;
  }

  public final Id getId() {
    return this.id; }

  public Id getPersonId() {
    return this.personId; }

  public IdImpl getActivityLinkId() {
    return this.activityLink;
  }
}

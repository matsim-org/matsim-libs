package playground.balac.retailers.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public class PersonPrimaryActivity
{
  private final Id<PersonPrimaryActivity> id;
  private final Id<Person> personId;
  private String activityType;
  private Id<Link> activityLink;

  public PersonPrimaryActivity(int id, Id<Person> personId)
  {
    this.id = Id.create(id, PersonPrimaryActivity.class);
    this.personId = personId;
  }

  public PersonPrimaryActivity(String activityType, int id, Id<Person> personId, Id<Link> activityLink)
  {
    this.id = Id.create(id, PersonPrimaryActivity.class);
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

  public final Id<PersonPrimaryActivity> getId() {
    return this.id; }

  public Id<Person> getPersonId() {
    return this.personId; }

  public Id<Link> getActivityLinkId() {
    return this.activityLink;
  }
}

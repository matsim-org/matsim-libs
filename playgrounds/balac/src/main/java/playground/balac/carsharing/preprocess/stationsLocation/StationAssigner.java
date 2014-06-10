package playground.balac.carsharing.preprocess.stationsLocation;

import org.apache.log4j.Logger;

import playground.balac.carsharing.preprocess.membership.MembershipAssigner;

public class StationAssigner
{
  private static final Logger log = Logger.getLogger(StationAssigner.class);
  private MembershipAssigner membershipAssigner;

  public StationAssigner(MembershipAssigner membershipAssigner)
  {
    this.membershipAssigner = membershipAssigner;
  }

  private void init()
  {
  }

  public void run() {
    init();
    modifyStations();
  }

  private void modifyStations()
  {
  }
}
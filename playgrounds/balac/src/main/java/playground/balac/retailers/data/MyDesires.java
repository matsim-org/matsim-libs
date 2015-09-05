package playground.balac.retailers.data;

import java.util.Map;

import org.apache.log4j.Logger;
import playground.ivt.utils.Desires;

public class MyDesires extends Desires
{
  private static final Logger log = Logger.getLogger(Desires.class);
  private String desc = null;
  private Map<String, Double> act_durs = null;

  public MyDesires(Desires desires) {
    super("");
    this.act_durs = desires.getActivityDurations();
  }
}

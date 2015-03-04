package playground.balac.retailers.data;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

public class RetailZones
{
  private static final Logger log = Logger.getLogger(RetailZones.class);
  private final Map<Id, RetailZone> retailZones = new LinkedHashMap();

  public final boolean addRetailZone(RetailZone retailZone) {
    if (retailZone == null) return false;
    if (this.retailZones.containsKey(retailZone.getId())) return false;
    this.retailZones.put(retailZone.getId(), retailZone);
    log.info("In the retail zone " + retailZone.getId() + " are living " + retailZone.getPersonsQuadTree().size() + " persons, and are based " + retailZone.getShopsQuadTree().size() + " shops");
    log.info("The retail zone " + retailZone.getId() + " has coordinates  minX = " + retailZone.getMinCoord().getX() + ", minY = " + retailZone.getMinCoord().getY() + ", maxX = " + retailZone.getMaxCoord().getX() + ", maxY = " + retailZone.getMaxCoord().getY());
    return true;
  }

  public Map<Id, RetailZone> getRetailZones() {
    return this.retailZones;
  }
}

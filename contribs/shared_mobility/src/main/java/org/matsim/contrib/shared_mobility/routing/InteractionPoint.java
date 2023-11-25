package org.matsim.contrib.shared_mobility.routing;

import java.util.Optional;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.shared_mobility.io.SharingStationSpecification;
import org.matsim.contrib.shared_mobility.service.SharingStation;

public class InteractionPoint {
  private final Id<Link> linkId;
  private final Optional<Id<SharingStation>> stationId;

  protected InteractionPoint(Id<Link> linkId, Optional<Id<SharingStation>> stationId) {
    this.stationId = stationId;
    this.linkId = linkId;
  }

  public boolean isStation() {
    return stationId.isPresent();
  }

  public Optional<Id<SharingStation>> getStationId() {
    return stationId;
  }

  public Id<Link> getLinkId() {
    return linkId;
  }

  public static InteractionPoint of(Link link) {
    return new InteractionPoint(link.getId(), Optional.empty());
  }

  public static InteractionPoint of(Id<Link> linkId) {
    return new InteractionPoint(linkId, Optional.empty());
  }

  public static InteractionPoint of(SharingStationSpecification station) {
    return new InteractionPoint(station.getLinkId(), Optional.of(station.getId()));
  }

  public static InteractionPoint of(SharingStation station) {
    return new InteractionPoint(station.getLink().getId(), Optional.of(station.getId()));
  }

  public boolean equals(InteractionPoint interactionPoint) {
    return interactionPoint.getLinkId().equals(this.getLinkId());
  }
}

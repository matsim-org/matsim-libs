package org.matsim.dsim.messages;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.matsim.api.core.v01.Message;

@Builder(setterPrefix = "set")
public record Teleportation(Message personMessage, double exitTime) {

}

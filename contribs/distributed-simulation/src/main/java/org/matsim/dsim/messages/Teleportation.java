package org.matsim.dsim.messages;

import lombok.Builder;
import lombok.Getter;

@Builder(setterPrefix = "set")
@Getter
public class Teleportation {

    private final PersonMsg person;
    private final double exitTime;

}

package playground.pieter.distributed.scoring;

import java.io.Serializable;

/**
 * Created by fouriep on 2/9/15.
 */
public enum ScoreComponentType implements Serializable {
    Activity, Leg, Fare, Money, Stuck, Event
}

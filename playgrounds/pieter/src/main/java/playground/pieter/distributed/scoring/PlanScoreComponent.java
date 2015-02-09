package playground.pieter.distributed.scoring;

import java.io.Serializable;

public class PlanScoreComponent implements Serializable{
    ScoreComponentType type;
    double score;

    public PlanScoreComponent(ScoreComponentType type, double score) {
        this.type = type;
        this.score = score;
    }
}
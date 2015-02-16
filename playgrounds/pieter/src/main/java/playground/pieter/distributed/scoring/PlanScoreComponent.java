package playground.pieter.distributed.scoring;

import java.io.Serializable;

public class PlanScoreComponent implements Serializable{
    private final String description;
    private ScoreComponentType type;

    public double getScore() {
        return score;
    }

    public ScoreComponentType getType() {
        return type;
    }

    private double score;

    @Override
    public String toString() {
        return String.format("%s\t%s\t%.3f",type,description,score);
    }

    public PlanScoreComponent(ScoreComponentType type, double score, String description) {
        this.type = type;
        this.score = score;
        this.description=description;
    }

    public String getDescription() {
        return description;
    }
}
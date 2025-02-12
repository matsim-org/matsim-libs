package org.matsim.profiling.aop;

public aspect ScoringListenerProfilingAspect extends AbstractProfilingEventAspect {

    pointcut eventPoints(Object o):
            target(o) &&
            call(void ScoringListener.notifyScoring(..));

}

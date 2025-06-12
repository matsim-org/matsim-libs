package org.matsim.contrib.profiling.aop.trace;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public abstract class TraceProfilingAspect {

	@Pointcut
	public abstract void traceTarget();

	@Around("traceTarget() && org.matsim.contrib.profiling.instrument.Trace.isEnabled() && !within(org.matsim.contrib.profiling..*)")
	public Object trace(ProceedingJoinPoint joinPoint) throws Throwable {
		var event = new TraceProfilingEvent();
		event.begin();
		Object returnValue;
		try {
			returnValue = joinPoint.proceed();
			if (!event.isEnabled()) {
				event.end(); // just to make sure no event timer is still running
				return returnValue;
			}
		} catch (Throwable e) {
			event.end();
			event.errorClass = e.getClass();
			event.errorMessage = e.getMessage();
			event.commit();
			throw e;
		}
		event.end();

		// to log
		// class + method reference
		event.joinPointId = joinPoint.getStaticPart().getId();
		event.joinPointKind = joinPoint.getStaticPart().getKind();
		event.sourceLocation = joinPoint.getStaticPart().getSourceLocation().getFileName() + ":" + joinPoint.getStaticPart().getSourceLocation().getLine();
		event.longSignature = joinPoint.getStaticPart().getSignature().toLongString();

		// these are objects, can be null, and require reflection
	  	//event.callerClass = Objects.nonNull(joinPoint.getThis()) ? joinPoint.getThis().getClass() : null;
	  	//event.targetClass = Objects.nonNull(joinPoint.getTarget()) ? joinPoint.getTarget().getClass() : null;
		// todo one of those can be retrieved from joinPoint.getStaticPart().getSourceLocation().getWithinType()

		// args
		//event.args = String.join(", ", Arrays.stream(joinPoint.getArgs()).map((o) -> Objects.toString(o, "null")).toList());
		// return value
		//event.returnValue = Objects.toString(returnValue, "null"); // todo possible avoid errors in toString and represent complex data structures differently

		event.commit();
		return returnValue;
	}


}

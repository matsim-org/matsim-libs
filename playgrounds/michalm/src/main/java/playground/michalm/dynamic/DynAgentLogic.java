package playground.michalm.dynamic;

public interface DynAgentLogic
{
    DynActivity init(DynAgent agent);


    DynAgent getDynAgent();


    void endActivityAndAssumeControl(DynActivity oldActivity, double now);


    void endLegAndAssumeControl(DynLeg oldLeg, double now);
}

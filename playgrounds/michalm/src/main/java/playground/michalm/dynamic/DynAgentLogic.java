package playground.michalm.dynamic;

public interface DynAgentLogic
{
    DynActivity init(DynAgent agent);


    void endActivityAndAssumeControl(DynActivity oldActivity, double now);


    void endLegAndAssumeControl(DynLeg oldLeg, double now);
}

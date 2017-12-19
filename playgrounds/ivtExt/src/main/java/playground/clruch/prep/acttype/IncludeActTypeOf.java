/**
 * 
 */
package playground.clruch.prep.acttype;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;

/**
 * @author Claudio Ruch
 *
 */
public enum IncludeActTypeOf {
    ;
    
    public static void Void(Config config){
        
    }
    

    public static void AstraZurich(Config config){
        config.planCalcScore().addActivityParams(new ActivityParams("home_6_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("home_8_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("education_3_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("education_1_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("remote_work_4_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("escort_kids_2_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("home_2_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("remote_work_6_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("escort_kids_4_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("home_4_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("remote_work_2_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("remote_home_1_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("work_2_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("leisure_3_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("leisure_1_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("leisure_5_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("escort_other_2_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("escort_other_4_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("work_6_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("work_4_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("shop_5_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("shop_3_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("shop_1_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("home_7_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("escort_kids_5_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("home_9_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("education_4_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("education_2_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("home_1_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("home_3_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("remote_work_5_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("remote_work_3_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("escort_kids_1_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("home_5_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("escort_kids_3_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("home_10_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("remote_home_2_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("remote_work_1_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("leisure_2_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("work_3_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("work_1_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("leisure_6_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("leisure_4_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("escort_other_1_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("escort_other_3_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("work_7_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("work_5_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("shop_4_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("shop_2_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("shop_6_1"));        
    }
    
    
    public static void BaselineCH(Config config){
        config.planCalcScore().addActivityParams(new ActivityParams("shop_4"));
        config.planCalcScore().addActivityParams(new ActivityParams("shop_3"));
        config.planCalcScore().addActivityParams(new ActivityParams("shop_2"));
        config.planCalcScore().addActivityParams(new ActivityParams("shop_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("shop_8"));
        config.planCalcScore().addActivityParams(new ActivityParams("shop_7"));
        config.planCalcScore().addActivityParams(new ActivityParams("shop_6"));
        config.planCalcScore().addActivityParams(new ActivityParams("shop_5"));
        config.planCalcScore().addActivityParams(new ActivityParams("leisure_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("home_7"));
        config.planCalcScore().addActivityParams(new ActivityParams("home_6"));
        config.planCalcScore().addActivityParams(new ActivityParams("home_5"));
        config.planCalcScore().addActivityParams(new ActivityParams("home_4"));
        config.planCalcScore().addActivityParams(new ActivityParams("home_3"));
        config.planCalcScore().addActivityParams(new ActivityParams("home_2"));
        config.planCalcScore().addActivityParams(new ActivityParams("home_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("leisure_6"));
        config.planCalcScore().addActivityParams(new ActivityParams("leisure_5"));
        config.planCalcScore().addActivityParams(new ActivityParams("leisure_4"));
        config.planCalcScore().addActivityParams(new ActivityParams("leisure_3"));
        config.planCalcScore().addActivityParams(new ActivityParams("home_9"));
        config.planCalcScore().addActivityParams(new ActivityParams("leisure_2"));
        config.planCalcScore().addActivityParams(new ActivityParams("home_8"));
        config.planCalcScore().addActivityParams(new ActivityParams("education_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("education_2"));
        config.planCalcScore().addActivityParams(new ActivityParams("education_3"));
        config.planCalcScore().addActivityParams(new ActivityParams("education_4"));
        config.planCalcScore().addActivityParams(new ActivityParams("remote_work_3"));
        config.planCalcScore().addActivityParams(new ActivityParams("escort_other_4"));
        config.planCalcScore().addActivityParams(new ActivityParams("remote_work_4"));
        config.planCalcScore().addActivityParams(new ActivityParams("remote_work_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("escort_other_2"));
        config.planCalcScore().addActivityParams(new ActivityParams("escort_other_3"));
        config.planCalcScore().addActivityParams(new ActivityParams("remote_work_2"));
        config.planCalcScore().addActivityParams(new ActivityParams("remote_home_2"));
        config.planCalcScore().addActivityParams(new ActivityParams("remote_home_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("home_10"));
        config.planCalcScore().addActivityParams(new ActivityParams("escort_kids_6"));
        config.planCalcScore().addActivityParams(new ActivityParams("work_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("escort_kids_5"));
        config.planCalcScore().addActivityParams(new ActivityParams("escort_kids_4"));
        config.planCalcScore().addActivityParams(new ActivityParams("escort_kids_3"));
        config.planCalcScore().addActivityParams(new ActivityParams("work_4"));
        config.planCalcScore().addActivityParams(new ActivityParams("escort_kids_2"));
        config.planCalcScore().addActivityParams(new ActivityParams("escort_kids_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("work_5"));
        config.planCalcScore().addActivityParams(new ActivityParams("work_2"));
        config.planCalcScore().addActivityParams(new ActivityParams("work_3"));
        config.planCalcScore().addActivityParams(new ActivityParams("work_6"));
        config.planCalcScore().addActivityParams(new ActivityParams("work_7"));
        config.planCalcScore().addActivityParams(new ActivityParams("escort_other_1"));
        config.planCalcScore().addActivityParams(new ActivityParams("remote_work_5"));
        config.planCalcScore().addActivityParams(new ActivityParams("remote_work_6"));
    }
    
    
}

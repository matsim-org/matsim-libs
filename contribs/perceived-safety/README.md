# The MATSim Perceived Safety Contrib

The Perceived Safety Module extends MATSim's typical scoring function such that perceived safety rates are considered. As can be seen in the Equation below, travel disutility now depends on time, distance (i.e., cost), and safety. The beta coefficient ($\beta_{psafe,m(q)}$ parameter) represents the overall weight of safety perceptions in the plan selection process, influencing mode or route choice. It is inspired by the MATSim Bicycle Contrib. 

This is an updated version of the [MATSim Psafe Module](https://github.com/panogjuras/Psafe) by Panagiotis Tzouras (@panogjuras). The update and integration into matsim-libs was done by Simon Meinhardt (@simei94).

**The MATSim Perceived Safety Contrib repository contains:**
- perceivedsafety package: includes all necessary classes to extend the MATSim scoring such that perceived safety is considered.
- PerceivedSafetyScoringTest: contains a test, which ensures that the updated scoring functionality delivers the same scores as the one from the [old approach](https://github.com/panogjuras/Psafe).

The Module can be utilized for research and education purposes. A Getting Started document will be prepared soon.

Overall, perceived safety is a network link attribute. The calculation of scores can be performed based on the tools uploaded in Perceived_Safety_Choices repository (a new version will be online soon).

To incorporate perceived safety, a threshold level is established, where safety ratings below the threshold decrease trip utility and vice versa. Perceived safety values are then multiplied by the ratio of the distance of each link to the distance threshold (${cd}_{m\left(q\right)}$ parameter). The parameter represents the level of unsafe distance a road user is willing to tolerate during a short trip. This is closely linked to the user’s experience, familiarity with, and tolerance for minor unsafe gaps that may arise along their route. A lower distance threshold indicates a higher contribution of perceived safety to the overall utility of a specific transport mode. The Psafe module also enables the use of a variable distance threshold rather than a fixed one. It assesses the impact of perceived safety by applying a distance-weighted average. In this approach, the distance threshold corresponds to the total length of each trip.

$S_{trav,m(q)} = \left[ C_{m(q)} + \beta_{trav,m(q)} \times t_{trav,m(q)} + \beta_{mon} \times \Delta m_q + \left(\frac{\beta_{d,m(q)}}{\gamma_{d,m(q)} + \beta_{mon}}\right) \times d_{trav,q} \right]$

$+ \beta_{psafe,m(q)} \times \sum_i \left[ \frac{(psafe_{i,m} - c_{psafe}) \times d_{trav,i}}{cd_{m(q)}} \right]$

where:

$S_{trav,m(q)}$: sum of all travel (dis)utilities of trip \(q) (e.g., travel time, cost, distance, etc.);  
$C_{m(q)}$: mode specific constant of mode \(m);  
$\beta_{trav,m(q)}$: the marginal utility of travel time of mode \(m);  
$t_{trav,m(q)}$: the travel time in hours of using mode \(m) in trip \(q);  
$\beta_{mon}$: the marginal utility of money;  
$\Delta m_q$: the change in the monetary budget in euros (equal to zero in this case);  
$\beta_{d,m(q)}$: the marginal utility of distance of mode \(m);  
$\gamma_{d,m(q)}$: the monetary distance rate of mode \(m);  
$d_{trav,q}$: the travel distance of trip \(q) in meters;  
$d_{trav,i}$: the travel distance in link \(i) in meters, by definition: $d_{trav,i} \le d_{trav,q}$;  
$\beta_{psafe,m(q)}$: the marginal utility of perceived safety of mode (m);  
$psafe_{i,m}$: the perceived safety level of link \( i \) of mode \(m) in Levels;  
$c_{psafe}$: the perceived safety threshold (Level 4 is recommended);  
$cd_{m(q)}$: the distance threshold of mode \(m).

All necessary dependencies are included in the [pom.xml](pom.xml) file.

The tools contained in this repository were developed within various research project of [Laboratory of Transportation Engineering](http://lte.survey.ntua.gr/main/en/) of National Technical University of Athens.

When referencing the contrib, please use the following papers:
> Tzouras, P.G., Mitropoulos, L., Karolemeas, C., Stravropoulou, E., Vlahogianni, E.I., Kepaptsoglou, K., 2024. Agent-based simulation model of micro-mobility trips in heterogeneous and perceived unsafe road environments. Journal of Cycling and Micromobility Research 2, 100042. [https://doi.org/10.1016/j.jcmr.2024.100042]


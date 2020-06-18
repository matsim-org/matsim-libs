
# commercialTrafficApplications
This contribution represents a collection of applications of the freight contrib in combination with one or more other contribs.
Applications are kept separated in packages.

##jointDemand
The jointDemand package provides functionality to link demand for freight with the demand for passenger transportation.
That means, that "normal" population agents can induce freight traffic by ordering 'commercial jobs' or 'services' of different types,
e.g. deliveries, maintenance or health care.
The order is placed by inserting a specific attribute at the corresponding activity, at which the service should be welcomed.
The score of such a customer is altered in order to model customer satisfaction. 
Currently, only the punctuality of a service has an impact on the score shift.
For each type of a 'commercial job', there can be multiple service providers.
When provided the ChangeCommercialJobOperator, customers can change the service provider at which the order is placed.

The user of this package can allow service providers to use drt for transportation.
Please see RunJointDemandCarExample and RunJointDemandUsingDRTExample.



  

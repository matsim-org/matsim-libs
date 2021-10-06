This sample scenario contains some sample tables with emissions factors.
It is also used by the tests in the main MATSim project. --> Please be careful in changing existing files.

The _Vehv**2** data shows the current way of defining the necessary attributes for the vehicles.

In HBEFA there are only cold emissions factors available for passenger cars (pass car; PC) and light commercial vehicles (LCV).
In order to have an clean approach, it was decided in Aug/Sep '21, that the old lookup behaviour for the other vehicle categories, 
e.g. heavy goods vehicles (HGV): Changing the type to pass.car or LCV  or and try to read the values from there 
(what fails, due to other engine specifications), or just return an 0.0, 
will not be supported any longer. Instead there should be an explicit lookup for the corresponding values.

In order to make this work, VSP has created an table (_coldTableExcept_LCV_PassCar_AllZero.csv_) with cold emissions factors for the missing vehicle types. 
All emissions factors in this table are set to 0.0. You can use this table to extend your existing cold emissions tables.


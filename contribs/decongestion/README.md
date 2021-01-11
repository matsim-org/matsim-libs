  # Decongestion
  
  This package provides some tools to compute delays per link and time interval and set tolls accordingly in order to reduce delays. 
  Tolls per link and time interval are adjusted from iteration to iteration or every specified number of iterations. 
  There are different implementations to compute the tolls: 
   *  using a proportional-integral-derivative controller which treats the average delay as the error term; requires tuning of the parameters K\_p, K\_i and K\_d 
   *  using a proportional controller which treats the average delay as the error term and where K\_p = value-of-travel-times-savings * number of delayed agents
   *  using a 'bang-bang' approach which either increases the toll or decreases the toll by a certain amount every specified number of iterations 


  

  
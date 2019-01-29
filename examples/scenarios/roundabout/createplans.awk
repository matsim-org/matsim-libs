# simple file to generate initial demand

# locations are in fact fake and are overridden by the Controler.  But
# they need to be in here so that the file is accepted by matsim. :-(

# run with "gawk -f createplans.awk" (or "awk -f createplans.awk")

############################################
############################################


BEGIN {

  num_agents = 300;

  # everybody leaves for work at this time
  end_time = "07:30";

  # the output file
  pfile = "plans.xml"

  print "<?xml version=\"1.0\" ?>" > pfile;
  print "<!DOCTYPE plans SYSTEM \"http://www.matsim.org/files/dtd/plans_v4.dtd\">" > pfile;
  print "<plans>" > pfile;

  { 
      { 
	  for(i=1;i<=num_agents;i++) {
 	      write_plan_one(4*i-3, pfile);
 	      write_plan_two(4*i-2, pfile);
  	      write_plan_thr(4*i-1, pfile);
  	      write_plan_fou(4*i  , pfile);
	  }
      }
  }

  print "</plans>" > pfile;

} # BEGIN

############################################
############################################

function write_plan_one(id, pfile) {
      print "<person id=\"" id "\">" > pfile;
      print "\t<plan>" > pfile;
      print "\t\t<act type=\"h\" link=\"21\" end_time=\"" end_time "\" />" > pfile;
      print "\t\t<leg mode=\"car\">" > pfile;
      print "\t\t\t<route> 1 2 4 6 8 </route>" > pfile;
      print "\t\t</leg>" > pfile;
      print "\t\t<act type=\"w\" link=\"87\" dur=\"08:00\" />" > pfile;
      print "\t\t<leg mode=\"car\">" > pfile;
      print "\t\t\t<route> 7 8 2  </route>" > pfile;
      print "\t\t</leg>" > pfile;
      print "\t\t<act type=\"h\" link=\"21\" />" > pfile;
      print "\t</plan>" > pfile;
      print "</person>\n" > pfile;
}

function write_plan_two(id, pfile) {
      print "<person id=\"" id "\">" > pfile;
      print "\t<plan>" > pfile;
      print "\t\t<act type=\"h\" link=\"43\" end_time=\"" end_time "\" />" > pfile;
      print "\t\t<leg mode=\"car\">" > pfile;
      print "\t\t\t<route> 3 4 6 8 2 </route>" > pfile;
      print "\t\t</leg>" > pfile;
      print "\t\t<act type=\"w\" link=\"21\" dur=\"08:00\" />" > pfile;
      print "\t\t<leg mode=\"car\">" > pfile;
      print "\t\t\t<route> 1 2 4 </route>" > pfile;
      print "\t\t</leg>" > pfile;
      print "\t\t<act type=\"h\" link=\"43\" />" > pfile;
      print "\t</plan>" > pfile;
      print "</person>\n" > pfile;
}

function write_plan_thr(id, pfile) {
      print "<person id=\"" id "\">" > pfile;
      print "\t<plan>" > pfile;
      print "\t\t<act type=\"h\" link=\"65\" end_time=\"" end_time "\" />" > pfile;
      print "\t\t<leg mode=\"car\">" > pfile;
      print "\t\t\t<route> 5 6 8 2 4 </route>" > pfile;
      print "\t\t</leg>" > pfile;
      print "\t\t<act type=\"w\" link=\"43\" dur=\"08:00\" />" > pfile;
      print "\t\t<leg mode=\"car\">" > pfile;
      print "\t\t\t<route> 3 4 6 </route>" > pfile;
      print "\t\t</leg>" > pfile;
      print "\t\t<act type=\"h\" link=\"65\" />" > pfile;
      print "\t</plan>" > pfile;
      print "</person>\n" > pfile;
}

function write_plan_fou(id, pfile) {
      print "<person id=\"" id "\">" > pfile;
      print "\t<plan>" > pfile;
      print "\t\t<act type=\"h\" link=\"87\" end_time=\"" end_time "\" />" > pfile;
      print "\t\t<leg mode=\"car\">" > pfile;
      print "\t\t\t<route> 7 8 2 4 6 </route>" > pfile;
      print "\t\t</leg>" > pfile;
      print "\t\t<act type=\"w\" link=\"65\" dur=\"08:00\" />" > pfile;
      print "\t\t<leg mode=\"car\">" > pfile;
      print "\t\t\t<route> 5 6 8 </route>" > pfile;
      print "\t\t</leg>" > pfile;
      print "\t\t<act type=\"h\" link=\"87\" />" > pfile;
      print "\t</plan>" > pfile;
      print "</person>\n" > pfile;
}
############################################
############################################

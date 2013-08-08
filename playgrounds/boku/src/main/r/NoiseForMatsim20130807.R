


#this file was developed from ..\MapBack20120710forFurtherCalculation.R, see MapBack20120623withPreparationFpktEW_withComputationAttenuationFactors.R for preparing the files and for computing the attenuation factors   


#goal: compute costs per receiver point and map it back to streets and vehicles, here in R, should be directly transferable to Java

#----------
#1. Compute noise emissions from traffic volumes, based on RLS90 and VBUS

#load an exemplary road network:
load("d:\\Synchro_Daten\\Laerm\\KopplungIMMI\\MappingCostsBack\\Lme_BaseCase.Rdata")
names(Lme_BaseCase) 
str(Lme_BaseCase)

#These variables are the variables you get from the noise software IMMI, relevant for noise calculation: 
#M_1, M_2, M_3: 
#"maßgebende" hourly traffic volumes day, evening, night, 
#in Kfz/h, including light and heavy duty vehicles, for one lane, 
#"bei mehrstreifigen Straßen ist M zu gleichen Teilen auf die beiden äußeren Fahrstreifen aufzuteilen"
#zu verwenden ist der Mittelwert über alle Tage des Jahres
#P_1, P_2, P_3:
#"maßgebender" share heavy duty vehicles (Lkw mit zulässigem Gesamtgewicht > 2,8 Tonnen)
#in %
#V_PKW_1, V_PKW_2, V_PKW_3: zulässige Höchstgeschwindigkeit Pkw (30-130 km/h)
#V_LKW_1, V_LKW_2, V_LKW_3: zulässige Höchstgeschwindigkeit Pkw (30-80 km/h)
#noise emissions should be written to: "LM_E_1","LM_E_2","LM_E_3"

#point 1 to be discussed: How to use hourly traffic volumes? See below.

#compute noise emissions, with equations from RLS, equation (8) on page 14
#Lpkw=27.7+10*log10(1+(0.02*vpkw)^3)
#Llkw=23.1+12.5*log10(vlkw) 
#number of cars per period: (1-(Lme_BaseCase$P_1/100))*Lme_BaseCase$M_1
#number of hdv per period: Lme_BaseCase$P_1/100*Lme_BaseCase$M_1
#energetic addition of several sound sources with similar noise levels: Lges = Li + 10*log10(n) gives the overall noise level for cars and hdv:
Lme_BaseCase$Lcard<-10*log10((1-(Lme_BaseCase$P_1/100))*Lme_BaseCase$M_1)+27.7+10*log10(1+(0.02*Lme_BaseCase$V_PKW_1)^3)
Lme_BaseCase$Lhdvd<-10*log10(Lme_BaseCase$P_1/100*Lme_BaseCase$M_1)+23.1+12.5*log10(Lme_BaseCase$V_LKW_1)
Lme_BaseCase$Lcare<-10*log10((1-(Lme_BaseCase$P_2/100))*Lme_BaseCase$M_2)+27.7+10*log10(1+(0.02*Lme_BaseCase$V_PKW_2)^3)
Lme_BaseCase$Lhdve<-10*log10(Lme_BaseCase$P_2/100*Lme_BaseCase$M_2)+23.1+12.5*log10(Lme_BaseCase$V_LKW_2)
Lme_BaseCase$Lcarn<-10*log10((1-(Lme_BaseCase$P_3/100))*Lme_BaseCase$M_3)+27.7+10*log10(1+(0.02*Lme_BaseCase$V_PKW_3)^3)
Lme_BaseCase$Lhdvn<-10*log10(Lme_BaseCase$P_3/100*Lme_BaseCase$M_3)+23.1+12.5*log10(Lme_BaseCase$V_LKW_3)

#answer to point 1: compute hourly noise levels with equations from above, compute day/evening/night level with energetic addition: Lges = 10 * log10 ( 10^(0.1*L1) + 10^(0.1*L2) + ... + 10^(0.1*Ln))

#compute Lden: (cost factors only exist for Lden)
Lme_BaseCase$Lden<-10*log10(1/24*(
  (12*(10^(Lme_BaseCase$Lcard/10)+10^(Lme_BaseCase$Lhdvd/10)))
   +(4*(10^((Lme_BaseCase$Lcare+5)/10)+10^((Lme_BaseCase$Lhdve+5)/10)))
   +(8*(10^((Lme_BaseCase$Lcarn+10)/10)+10^((Lme_BaseCase$Lhdvn+10)/10)))
  ))

#--####-------------
save(Lme_BaseCase,file="d:\\Synchro_Daten\\Laerm\\KopplungIMMI\\MappingCostsBack\\Lme_BaseCase.Rdata")

#----------
#2. compute contributions per link and receiver point 

#import attenuation factors (created in MapBack20120623withPreparationFpktEW_withComputationAttenuationFactors.R )
load("d:\\Synchro_Daten\\Laerm\\KopplungIMMI\\MappingCostsBack\\af_1_10000.Rdata ") 
names(af_1_10000)
#save(af_1_10000,file="d:\\Synchro_Daten\\Laerm\\KopplungIMMI\\MappingCostsBack\\af_1_10000.Rdata")

#point 2 to be discussed: Where do the differences in the attenuation factors day/evening/night come? 
#To be discussed with Wölfel, rechnen sie hier schon die Lden-Faktoren ein?
#for now, I take the specific attenuation factors for computing the contributions of the links per time period

#----------
#for receiver points 1 to 10000 (should be redone for each 10000 receiver points, not more):
names(Lme_BaseCase)
str(Lme_BaseCase[c(1,12,74:77)]) # 
names(af_1_10000) # 
str(af_1_10000) # 

table(duplicated(af_1_10000[,"IPkt"])) #F: 10000, T: 2668971
table(duplicated(af_1_10000[,"StrID"])) #F: 512, T: 2678459, dim(af_1_10000)[1]/512=5232.365 receiver points per street on average
table(duplicated(af_1_10000[,c("IPkt","StrID")])) #F: 2678971

#merge attenuation factors and Lme_BaseCase
ContrStreetIPKT1_10000<-merge(Lme_BaseCase[,c(1,12,74:77)],af_1_10000,by.x="StrID",by.y="StrID",all.x=F,all.y=F) #
names(ContrStreetIPKT1_10000)
summary(ContrStreetIPKT1_10000) #0 NAs
head(ContrStreetIPKT1_10000)
str(ContrStreetIPKT1_10000)

#compute contribution link emissions to receiver points:
#attenuation factors were computed with: af_lden=Lden-LdenIpktStr (emissions street minus contribution street to ipkt, for r-code doing these steps see MapBack20120623withPreparationFpktEW_withComputationAttenuationFactors.R )

#points 3 to be discussed: add DREFL?

#point 4 to be discussed: Should we use the attenuation factors day/evening/night 
#and compute Lipkt (noise level at each receiver point) from the individual contributions of each link per receiver point and time period, including the factors evening 5 and night 10 dba?

#LdenIpktStr=Lden-af_lden, same for day/evening/night
ContrStreetIPKT1_10000$LdenIpktStr<-(ContrStreetIPKT1_10000$Lden-ContrStreetIPKT1_10000$af_lden)
ContrStreetIPKT1_10000$LdayIpktStr<-(ContrStreetIPKT1_10000$Lday-ContrStreetIPKT1_10000$af_lday)
ContrStreetIPKT1_10000$LevIpktStr<-(ContrStreetIPKT1_10000$Lev-ContrStreetIPKT1_10000$af_lev)
ContrStreetIPKT1_10000$LnightIpktStr<-(ContrStreetIPKT1_10000$Lnight-ContrStreetIPKT1_10000$af_lnight)

#Compute squared sound power
p0<-2*(10^(-5)) #p_0=2*(10^(-5)) Pa (Schalldruck an der Hörschwelle bei 1 kHz), from Foundations_Calculation_Noise_20110225.docx 
ContrStreetIPKT1_10000$p2den_ipkt<-(p0^2)*(10^(0.1*ContrStreetIPKT1_10000$LdenIpktStr))
ContrStreetIPKT1_10000$p2day_ipkt<-(p0^2)*(10^(0.1*ContrStreetIPKT1_10000$LdayIpktStr))
ContrStreetIPKT1_10000$p2ev_ipkt<-(p0^2)*(10^(0.1*ContrStreetIPKT1_10000$LevIpktStr))
ContrStreetIPKT1_10000$p2night_ipkt<-(p0^2)*(10^(0.1*ContrStreetIPKT1_10000$LnightIpktStr))

#reorder the columns:
names(ContrStreetIPKT1_10000)
names(ContrStreetIPKT1_10000[c(7,1,2:6,11,8:10,13:20)])
ContrStreetIPKT1_10000<-ContrStreetIPKT1_10000[,c(7,1,2:6,11,8:10,13:20)]
ContrStreetIPKT1_10000[1:10,]
#order the obs:
ContrStreetIPKT1_10000<-ContrStreetIPKT1_10000[order(ContrStreetIPKT1_10000$IPkt,ContrStreetIPKT1_10000$LdenIpktStr,decreasing = T),]
ContrStreetIPKT1_10000[1:20,]

#--####-------------
save(ContrStreetIPKT1_10000,file="d:\\Synchro_Daten\\Laerm\\KopplungIMMI\\MappingCostsBack\\ContrStreetIPKT1_10000.Rdata")

#----------
#3. compute noise levels per receiver point 

#compute Lday/Lev/Lnight/Lden per receiver point from contributions per street and receiver point
names(ContrStreetIPKT1_10000)
head(ContrStreetIPKT1_10000)
#compute summands/addends that can be summed up per receiver point: (acutally only needed for lden?)
ContrStreetIPKT1_10000$log10lday<-(10^(0.1*ContrStreetIPKT1_10000$LdayIpktStr))
ContrStreetIPKT1_10000$log10lev<-(10^(0.1*ContrStreetIPKT1_10000$LevIpktStr))
ContrStreetIPKT1_10000$log10lnight<-(10^(0.1*ContrStreetIPKT1_10000$LnightIpktStr))
#ContrStreetIPKT1_10000$log10lden<-(10^(0.1*ContrStreetIPKT1_10000$LdenIpktStr))
head(ContrStreetIPKT1_10000)
str(ContrStreetIPKT1_10000)
summary(ContrStreetIPKT1_10000) #0 NAs
#create the vector for grouping
group<-ContrStreetIPKT1_10000$IPkt
IPKT_BaseCase1_10000<-as.data.frame(unique(ContrStreetIPKT1_10000$IPkt)) #10000 obs.
names(IPKT_BaseCase1_10000)<-"IPkt"
head(IPKT_BaseCase1_10000)

x<-10*log10(rowsum(ContrStreetIPKT1_10000$log10lday,group,na.rm=T))
x<-as.data.frame(x)
names(x)<-"IPKT_lday"
IPKT_BaseCase1_10000<-cbind(IPKT_BaseCase1_10000,x)
head(IPKT_BaseCase1_10000)
summary(IPKT_BaseCase1_10000)

x<-10*log10(rowsum(ContrStreetIPKT1_10000$log10lev,group,na.rm=T))
x<-as.data.frame(x)
names(x)<-"IPKT_lev"
IPKT_BaseCase1_10000<-cbind(IPKT_BaseCase1_10000,x)
head(IPKT_BaseCase1_10000)
summary(IPKT_BaseCase1_10000)

x<-10*log10(rowsum(ContrStreetIPKT1_10000$log10lnight,group,na.rm=T))
x<-as.data.frame(x)
names(x)<-"IPKT_lnight"
IPKT_BaseCase1_10000<-cbind(IPKT_BaseCase1_10000,x)
head(IPKT_BaseCase1_10000)
summary(IPKT_BaseCase1_10000)

#x<-10*log10(rowsum(ContrStreetIPKT1_10000$log10lden,group,na.rm=T))
#x<-as.data.frame(x)
#names(x)<-"IPKT_lden"
#IPKT_BaseCase1_10000<-cbind(IPKT_BaseCase1_10000,x)
#head(IPKT_BaseCase1_10000)
#summary(IPKT_BaseCase1_10000)

IPKT_BaseCase1_10000$IPKT_Lden<-10*log10(1/24*(
  (12*(10^(IPKT_BaseCase1_10000$IPKT_lday/10)))
  +(4*(10^((IPKT_BaseCase1_10000$IPKT_lev+5)/10)))
  +(8*(10^((IPKT_BaseCase1_10000$IPKT_lnight+10)/10)))
  ))
head(IPKT_BaseCase1_10000)
IPKT_BaseCase1_10000[1:100,5]

#----------
#4. compute costs per receiver point, with Lden
head(IPKT_BaseCase1_10000)
#merge with inhabitants per receiver point:
load("d:\\Synchro_Daten\\Laerm\\KopplungIMMI\\MappingCostsBack\\ipktew_forFurtherCalculation.Rdata")
head(ipktew_forFurtherCalculation)
IPKT_BaseCase1_10000<-merge(IPKT_BaseCase1_10000,ipktew_forFurtherCalculation,by.x="IPkt",by.y="Name",all.x=T,all.y=F)
head(IPKT_BaseCase1_10000)
summary(IPKT_BaseCase1_10000)

#cost factors from the handbook

#assign costs to ipkt, from the handbook, from file c:\Temp\NoiseCostFactors.txt, 2008_01_15_handbook_external_cost_en.pdf 
#page 67, table 20 and table 109 page 233, and HEATCO_D5.pdf, p. 111
#see page 238 for recommended unit values per vehicle kilometre (marginal costs)
#basis: HEATCO, del 5 (HEATCO_D5.pdf): page 105: these values "comprise the WTP for reducing annoyance based on stated 
#preference studies (see Working group on health and socio-economic aspects, 2003) and quantifiable costs of health effects.
#see also "HEATCO D5 Annex E (Fall-back values for noise impacts).pdf" table 3.4

IPKT_BaseCase1_10000<-transform(IPKT_BaseCase1_10000,cfactor_hb=0,costs_hb=0)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=51 & IPKT_BaseCase1_10000$IPKT_Lden<52,9)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=52 & IPKT_BaseCase1_10000$IPKT_Lden<53,18)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=53 & IPKT_BaseCase1_10000$IPKT_Lden<54,26)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=54 & IPKT_BaseCase1_10000$IPKT_Lden<55,35)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=55 & IPKT_BaseCase1_10000$IPKT_Lden<56,44)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=56 & IPKT_BaseCase1_10000$IPKT_Lden<57,53)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=57 & IPKT_BaseCase1_10000$IPKT_Lden<58,61)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=58 & IPKT_BaseCase1_10000$IPKT_Lden<59,70)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=59 & IPKT_BaseCase1_10000$IPKT_Lden<60,79)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=60 & IPKT_BaseCase1_10000$IPKT_Lden<61,88)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=61 & IPKT_BaseCase1_10000$IPKT_Lden<62,96)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=62 & IPKT_BaseCase1_10000$IPKT_Lden<63,105)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=63 & IPKT_BaseCase1_10000$IPKT_Lden<64,114)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=64 & IPKT_BaseCase1_10000$IPKT_Lden<65,123)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=65 & IPKT_BaseCase1_10000$IPKT_Lden<66,132)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=66 & IPKT_BaseCase1_10000$IPKT_Lden<67,140)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=67 & IPKT_BaseCase1_10000$IPKT_Lden<68,149)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=68 & IPKT_BaseCase1_10000$IPKT_Lden<69,158)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=69 & IPKT_BaseCase1_10000$IPKT_Lden<70,167)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=70 & IPKT_BaseCase1_10000$IPKT_Lden<71,175)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=71 & IPKT_BaseCase1_10000$IPKT_Lden<72,233)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=72 & IPKT_BaseCase1_10000$IPKT_Lden<73,247)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=73 & IPKT_BaseCase1_10000$IPKT_Lden<74,262)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=74 & IPKT_BaseCase1_10000$IPKT_Lden<75,277)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=75 & IPKT_BaseCase1_10000$IPKT_Lden<76,291)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=76 & IPKT_BaseCase1_10000$IPKT_Lden<77,306)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=77 & IPKT_BaseCase1_10000$IPKT_Lden<78,321)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=78 & IPKT_BaseCase1_10000$IPKT_Lden<79,335)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=79 & IPKT_BaseCase1_10000$IPKT_Lden<80,350)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=80 & IPKT_BaseCase1_10000$IPKT_Lden<81,365)
IPKT_BaseCase1_10000$cfactor_hb<-replace(IPKT_BaseCase1_10000$cfactor_hb,IPKT_BaseCase1_10000$IPKT_Lden>=81,379)

IPKT_BaseCase1_10000$costs_hb<-(IPKT_BaseCase1_10000$cfactor_hb*IPKT_BaseCase1_10000$PERSON_IP) #costs per day and ipkt, in Euro
head(IPKT_BaseCase1_10000)
IPKT_BaseCase1_10000[1:200,]

#cost factors from Thilo (for noise levels >=55):
IPKT_BaseCase1_10000$costs_tb<-((8944.6-427.238*IPKT_BaseCase1_10000$IPKT_Lden+
  6.15983*(IPKT_BaseCase1_10000$IPKT_Lden^2)-0.024537*(IPKT_BaseCase1_10000$IPKT_Lden^3))*IPKT_BaseCase1_10000$PERSON_IP)
IPKT_BaseCase1_10000$costs_tb<-replace(IPKT_BaseCase1_10000$costs_tb,IPKT_BaseCase1_10000$IPKT_Lden<55,0)
IPKT_BaseCase1_10000[1:200,]
summary(IPKT_BaseCase1_10000)

#--####-------------
save(IPKT_BaseCase1_10000,file="d:\\Synchro_Daten\\Laerm\\KopplungIMMI\\MappingCostsBack\\IPKT_BaseCase1_10000.Rdata")

#----------
#5. mapping the costs back to links and vehicle kilometers
#####################
#####################
#compute sound pressure for getting the cost shares
head(IPKT_BaseCase1_10000)
p0<-2*(10^(-5)) #p_0=2*(10^(-5)) Pa (Schalldruck an der Hörschwelle bei 1 kHz), see Foundations_Calculation_Noise_20110225.docx 
IPKT_BaseCase1_10000$p2ipkt<-(p0^2)*(10^(0.1*IPKT_BaseCase1_10000$IPKT_Lden))
head(IPKT_BaseCase1_10000) #IPKT IPKT_lday IPKT_lev IPKT_lnight IPKT_Lden  PERSON_IP cfactor_hb costs_hb costs_tb       p2ipkt
summary(IPKT_BaseCase1_10000)
head(IPKT_BaseCase1_10000[c(1,5,8:10)]) #IPKT IPKT_Lden costs_tb       p2ipkt

#merge ContrStreetIPKT1_10000 and IPKT_BaseCase1_10000 (costs_hb,costs_tb,p2ipkt)
head(IPKT_BaseCase1_10000)
names(IPKT_BaseCase1_10000[c(1,5,8:10)]) #IPKT IPKT_Lden costs_tb       p2ipkt  
names(ContrStreetIPKT1_10000)
#[1] "IPKT"          "StrID"         "LaengeElement" "Lday"          "Lev"           "Lnight"        "af_lday"       "af_lev"        "af_lnight"     "LdayIpktStr"   "LevIpktStr"    "LnightIpktStr"
#[13] "p2day_ipkt"    "p2ev_ipkt"     "p2night_ipkt"  "log10lday"     "log10lev"      "log10lnight"  
names(ContrStreetIPKT1_10000[c(1:3,13:15)]) #"IPKT"          "StrID"         "LaengeElement" "p2day_ipkt"    "p2ev_ipkt"     "p2night_ipkt"  "log10lday"     "log10lev"      "log10lnight"  
head(ContrStreetIPKT1_10000)
ContrStreetIPKT1_10000<-merge(ContrStreetIPKT1_10000[,c(1:3,13:15)],IPKT_BaseCase1_10000[,c(1,5,8:10)],by.x="IPKT",by.y="IPKT",all.x=T,all.y=T) #
names(ContrStreetIPKT1_10000)
#[1] "IPKT"  "StrID"  "LaengeElement" "p2day_ipkt"    "p2ev_ipkt"     "p2night_ipkt"  "IPKT_Lden"     "costs_hb"      "costs_tb"      "p2ipkt"       
head(ContrStreetIPKT1_10000)
str(ContrStreetIPKT1_10000)
#compute the costs per link and receiver point d/e/n, handbook:
ContrStreetIPKT1_10000$costs_hb_d<-ContrStreetIPKT1_10000$p2day_ipkt/ContrStreetIPKT1_10000$p2ipkt*ContrStreetIPKT1_10000$costs_hb
ContrStreetIPKT1_10000$costs_hb_e<-ContrStreetIPKT1_10000$p2ev_ipkt/ContrStreetIPKT1_10000$p2ipkt*ContrStreetIPKT1_10000$costs_hb
ContrStreetIPKT1_10000$costs_hb_n<-ContrStreetIPKT1_10000$p2night_ipkt/ContrStreetIPKT1_10000$p2ipkt*ContrStreetIPKT1_10000$costs_hb
#compute the costs per link and receiver point d/e/n, Thilo cost factors:
ContrStreetIPKT1_10000$costs_tb_d<-ContrStreetIPKT1_10000$p2day_ipkt/ContrStreetIPKT1_10000$p2ipkt*ContrStreetIPKT1_10000$costs_tb
ContrStreetIPKT1_10000$costs_tb_e<-ContrStreetIPKT1_10000$p2ev_ipkt/ContrStreetIPKT1_10000$p2ipkt*ContrStreetIPKT1_10000$costs_tb
ContrStreetIPKT1_10000$costs_tb_n<-ContrStreetIPKT1_10000$p2night_ipkt/ContrStreetIPKT1_10000$p2ipkt*ContrStreetIPKT1_10000$costs_tb
##compute the costs car/HDV d/e/n, handbook:
#ContrStreetIPKT1_10000$costs_hb_card<-ContrStreetIPKT1_10000$p2card_ipkt/ContrStreetIPKT1_10000$p2ipkt*ContrStreetIPKT1_10000$costs_hb
#ContrStreetIPKT1_10000$costs_hb_care<-ContrStreetIPKT1_10000$p2care_ipkt/ContrStreetIPKT1_10000$p2ipkt*ContrStreetIPKT1_10000$costs_hb
#ContrStreetIPKT1_10000$costs_hb_carn<-ContrStreetIPKT1_10000$p2carn_ipkt/ContrStreetIPKT1_10000$p2ipkt*ContrStreetIPKT1_10000$costs_hb
#ContrStreetIPKT1_10000$costs_hb_hdvd<-ContrStreetIPKT1_10000$p2hdvd_ipkt/ContrStreetIPKT1_10000$p2ipkt*ContrStreetIPKT1_10000$costs_hb
#ContrStreetIPKT1_10000$costs_hb_hdve<-ContrStreetIPKT1_10000$p2hdve_ipkt/ContrStreetIPKT1_10000$p2ipkt*ContrStreetIPKT1_10000$costs_hb
#ContrStreetIPKT1_10000$costs_hb_hdvn<-ContrStreetIPKT1_10000$p2hdvn_ipkt/ContrStreetIPKT1_10000$p2ipkt*ContrStreetIPKT1_10000$costs_hb
##compute the costs car/HDV d/e/n, Thilo cost factors:
#ContrStreetIPKT1_10000$costs_tb_card<-ContrStreetIPKT1_10000$p2card_ipkt/ContrStreetIPKT1_10000$p2ipkt*ContrStreetIPKT1_10000$costs_tb
#ContrStreetIPKT1_10000$costs_tb_care<-ContrStreetIPKT1_10000$p2care_ipkt/ContrStreetIPKT1_10000$p2ipkt*ContrStreetIPKT1_10000$costs_tb
#ContrStreetIPKT1_10000$costs_tb_carn<-ContrStreetIPKT1_10000$p2carn_ipkt/ContrStreetIPKT1_10000$p2ipkt*ContrStreetIPKT1_10000$costs_tb
#ContrStreetIPKT1_10000$costs_tb_hdvd<-ContrStreetIPKT1_10000$p2hdvd_ipkt/ContrStreetIPKT1_10000$p2ipkt*ContrStreetIPKT1_10000$costs_tb
#ContrStreetIPKT1_10000$costs_tb_hdve<-ContrStreetIPKT1_10000$p2hdve_ipkt/ContrStreetIPKT1_10000$p2ipkt*ContrStreetIPKT1_10000$costs_tb
#ContrStreetIPKT1_10000$costs_tb_hdvn<-ContrStreetIPKT1_10000$p2hdvn_ipkt/ContrStreetIPKT1_10000$p2ipkt*ContrStreetIPKT1_10000$costs_tb

#--####-------------
save(ContrStreetIPKT1_10000,file="KopplungIMMI\\MappingCostsBack\\ContrStreetIPKT1_10000.Rdata")

#----------
#sum up the costs per street over all receiver points the street contributes to total costs day/evening/night

#create the vector for grouping
names(ContrStreetIPKT1_10000)
head(ContrStreetIPKT1_10000) #StrID: STRb3594...., ID_Ipkt: IPkt00001...

group<-ContrStreetIPKT1_10000$StrID
LmeCostsInterim<-as.data.frame(unique(ContrStreetIPKT1_10000$StrID)) #512 obs.
names(LmeCostsInterim)<-"StrID"
head(LmeCostsInterim)

x<-rowsum(ContrStreetIPKT1_10000$costs_hb_d,group,na.rm=T)
x<-as.data.frame(x)
names(x)<-"costs_hb_d1" #the ..1 in the variable name stands for first part of receiver points (1:10000), the same values need to be computed for the other receiver points and need to be summed up at the end
LmeCostsInterim<-cbind(LmeCostsInterim,x)
LmeCostsInterim$costs_hb_d1<-round(LmeCostsInterim$costs_hb_d1,digits=2)
head(LmeCostsInterim)
summary(LmeCostsInterim)

x<-rowsum(ContrStreetIPKT1_10000$costs_hb_e,group,na.rm=T)
x<-as.data.frame(x)
names(x)<-"costs_hb_e1"
LmeCostsInterim<-cbind(LmeCostsInterim,x)
LmeCostsInterim$costs_hb_e1<-round(LmeCostsInterim$costs_hb_e1,digits=2)
head(LmeCostsInterim)
summary(LmeCostsInterim)

x<-rowsum(ContrStreetIPKT1_10000$costs_hb_n,group,na.rm=T)
x<-as.data.frame(x)
names(x)<-"costs_hb_n1"
LmeCostsInterim<-cbind(LmeCostsInterim,x)
LmeCostsInterim$costs_hb_n1<-round(LmeCostsInterim$costs_hb_n1,digits=2)
head(LmeCostsInterim)
summary(LmeCostsInterim)

x<-rowsum(ContrStreetIPKT1_10000$costs_tb_d,group,na.rm=T)
x<-as.data.frame(x)
names(x)<-"costs_tb_d1"
LmeCostsInterim<-cbind(LmeCostsInterim,x)
LmeCostsInterim$costs_tb_d1<-round(LmeCostsInterim$costs_tb_d1,digits=2)
head(LmeCostsInterim)
summary(LmeCostsInterim)

x<-rowsum(ContrStreetIPKT1_10000$costs_tb_e,group,na.rm=T)
x<-as.data.frame(x)
names(x)<-"costs_tb_e1"
LmeCostsInterim<-cbind(LmeCostsInterim,x)
LmeCostsInterim$costs_tb_e1<-round(LmeCostsInterim$costs_tb_e1,digits=2)
head(LmeCostsInterim)
summary(LmeCostsInterim)

x<-rowsum(ContrStreetIPKT1_10000$costs_tb_n,group,na.rm=T)
x<-as.data.frame(x)
names(x)<-"costs_tb_n1"
LmeCostsInterim<-cbind(LmeCostsInterim,x)
LmeCostsInterim$costs_tb_n1<-round(LmeCostsInterim$costs_tb_n1,digits=2)
head(LmeCostsInterim)
summary(LmeCostsInterim)

head(LmeCostsInterim)
summary(LmeCostsInterim) #there are huge variances in the cost per link
#and so are in the traffic volumes in Lme_BaseCase:
sd(Lme_BaseCase$M_1) #459.2394
sd(Lme_BaseCase$M_2) #311.0976
sd(Lme_BaseCase$M_3) #74.07087
sd(Lme_BaseCase$P_1) #0
sd(Lme_BaseCase$P_2) #0
sd(Lme_BaseCase$P_3) #0
summary(Lme_BaseCase[,c(48:53)])
#M_1              M_2              M_3              P_1          P_2          P_3    
#Min.   :   0.0   Min.   :   0.0   Min.   :  0.00   Min.   :20   Min.   :15   Min.   :10  
#1st Qu.: 241.2   1st Qu.: 163.4   1st Qu.: 38.90   1st Qu.:20   1st Qu.:15   1st Qu.:10  
#Median : 461.9   Median : 312.9   Median : 74.50   Median :20   Median :15   Median :10  
#Mean   : 572.6   Mean   : 387.9   Mean   : 92.36   Mean   :20   Mean   :15   Mean   :10  
#3rd Qu.: 827.7   3rd Qu.: 560.7   3rd Qu.:133.50   3rd Qu.:20   3rd Qu.:15   3rd Qu.:10  
#Max.   :3013.2   Max.   :2041.2   Max.   :486.00   Max.   :20   Max.   :15   Max.   :10  

#merge LmeCostsInterim with Lme_BaseCase
names(Lme_BaseCase)
#[45] "D_STRO_2"                      "D_STRO_3"                      "STR_OFL"                       "M_1"                          
#[49] "M_2"                           "M_3"                           "P_1"                           "P_2"                          
#[53] "P_3"                           "V_PKW_1"                       "V_PKW_2"                       "V_PKW_3"                      
head(Lme_BaseCase) #Name: STRb0001...
table(Lme_BaseCase$Gruppe)
#test<-Lme_BaseCase[Lme_BaseCase$Gruppe=="BaseCase",] #561 obs.

Lme_BaseCase<-merge(Lme_BaseCase,LmeCostsInterim,by.x="Name",by.y="StrID",all.x=T,all.y=T) #1287 obs. for 90 variables
#Lme_BaseCase<-Lme_BaseCase[Lme_BaseCase$Gruppe=="BaseCase",] #561 obs.
names(Lme_BaseCase) #
#[1] "Name"                          "Nr."                           "Bezeichnung"                   "Gruppe"                        "ABS_HOEHE"                     "REL_HOEHE"                    
#[7] "Knotenzahl"                    "Notiz"                         "Zeichenattribute"              "Automat. El.-Beschriftung"     "Kennzahl"                      "LaengeElement"                
#[13] "Fläche des Elements in qm"     "z ist absolut definiert."      "xmin / m"                      "xmax / m"                      "ymin / m"                      "ymax / m"                     
#[19] "zmin / m"                      "zmax / m"                      "zmin(rel)/ m"                  "zmax(rel)/ m"                  "zmin(abs)/ m"                  "zmax(abs)/ m"                 
#[25] "Wirkradius /m"                 "Spitzenpegel"                  "STG_AUS_Z"                     "Steigung in % direkt vorgeben" "STG_LIMIT"                     "Eingabetyp"                   
#[31] "QPTYP"                         "DSQ_L"                         "DSQ_R"                         "EMIFAK_LI"                     "EMIFAK_RE"                     "B_FAHRB_LI"                   
#[37] "B_FAHRB_RE"                    "B_MITTE_LI"                    "B_MITTE_RE"                    "DTV"                           "DTV_UND_P"                     "Straßengattung"               
#[43] "D_STRO"                        "D_STRO_1"                      "D_STRO_2"                      "D_STRO_3"                      "STR_OFL"                       "M_1"                          
#[49] "M_2"                           "M_3"                           "P_1"                           "P_2"                           "P_3"                           "V_PKW_1"                      
#[55] "V_PKW_2"                       "V_PKW_3"                       "V_PKW"                         "V_LKW_1"                       "V_LKW_2"                       "V_LKW_3"                      
#[61] "V_LKW"                         "Regelquerschnitt RQ"           "DSQ"                           "DREFL"                         "LM_E_1"                        "LM_E_2"                       
#[67] "LM_E_3"                        "LdenIMMI"                      "Lcard"                         "Lhdvd"                         "Lcare"                         "Lhdve"                        
#[73] "Lcarn"                         "Lhdvn"                         "Lden"                          "Lday"                          "Lev"                           "Lnight"                       
#[79] "costs_hb_d1"                   "costs_hb_e1"                   "costs_hb_n1"                   "costs_tb_d1"                   "costs_tb_e1"                   "costs_tb_n1"                  
head(Lme_BaseCase)
str(Lme_BaseCase)

#compute the average costs in ct per vehicle km from: 

#1. share of cars/hdv from total costs day/evening/night: p2 cars/hdv / p2 link
#2. costs car/hdv per vehicle kilometer: costs link day/ev/night / (no cars/hdv * length of element)
#3. average costs: 1. * 2.
#for costs handbook hb and Thilo-factors tb
#steps 1 to 3 can be done within one step

#comments on where the data comes from:
#M_1 comes from MATSimRGU_StatTrafficVol[,35]<-(MATSimRGU_StatTrafficVol[,124]*0.062) #124 BaseDTV (see above)
#BaseDTV comes from TotalVehicles, see StatisticsScenariosTrafficVolumes20120525.R, includes cars and HDV
#no cars: M_1-(P_1/100*M_1)=M_1*(1-P_1/100)
#no HDV: (P_1/100*M_1)
#length of the element (for getting vehicle km from vehicles): "Länge des Elements in m (3D)" (variable )

#costs are given in Euro, length is given in meter --> *100 for getting ct from Euro, *1000 for getting costs per km and not per meter
#divide by 365 for getting the costs per day from the costs IPKT per year
#IMPORTANT:
#before computing the costs per vehicle kilometer I should include here the costs from all receiver points and not only from point 1 to 10000 (as done at the moment)
p0<-2*(10^(-5)) #p_0=2*(10^(-5)) Pa (Schalldruck an der Hörschwelle bei 1 kHz), from Foundations_Calculation_Noise_20110225.docx 
Lme_BaseCase$p2day<-(p0^2)*(10^(0.1*Lme_BaseCase$Lday))
Lme_BaseCase$p2ev<-(p0^2)*(10^(0.1*Lme_BaseCase$Lev))
Lme_BaseCase$p2night<-(p0^2)*(10^(0.1*Lme_BaseCase$Lnight))
Lme_BaseCase$p2car_day<-(p0^2)*(10^(0.1*Lme_BaseCase$Lcard))
Lme_BaseCase$p2car_ev<-(p0^2)*(10^(0.1*Lme_BaseCase$Lcare))
Lme_BaseCase$p2car_night<-(p0^2)*(10^(0.1*Lme_BaseCase$Lcarn))
Lme_BaseCase$p2hdv_day<-(p0^2)*(10^(0.1*Lme_BaseCase$Lhdvd))
Lme_BaseCase$p2hdv_ev<-(p0^2)*(10^(0.1*Lme_BaseCase$Lhdve))
Lme_BaseCase$p2hdv_night<-(p0^2)*(10^(0.1*Lme_BaseCase$Lhdvn))
#costs handbook:
Lme_BaseCase$avCost_hb_card<-((Lme_BaseCase$p2car_day/Lme_BaseCase$p2day)/365*((Lme_BaseCase$costs_hb_d1*100*1000)/(Lme_BaseCase$M_1*(1-Lme_BaseCase$P_1/100)*Lme_BaseCase$LaengeElement)))
Lme_BaseCase$avCost_hb_care<-((Lme_BaseCase$p2car_day/Lme_BaseCase$p2ev)/365*((Lme_BaseCase$costs_hb_e1*100*1000)/(Lme_BaseCase$M_2*(1-Lme_BaseCase$P_2/100)*Lme_BaseCase$LaengeElement)))
Lme_BaseCase$avCost_hb_carn<-((Lme_BaseCase$p2car_day/Lme_BaseCase$p2night)/365*((Lme_BaseCase$costs_hb_n1*100*1000)/(Lme_BaseCase$M_3*(1-Lme_BaseCase$P_3/100)*Lme_BaseCase$LaengeElement)))
Lme_BaseCase$avCost_hb_hdvd<-((Lme_BaseCase$p2hdv_day/Lme_BaseCase$p2day)/365*((Lme_BaseCase$costs_hb_d1*100*1000)/(Lme_BaseCase$M_1*(1-Lme_BaseCase$P_1/100)*Lme_BaseCase$LaengeElement)))
Lme_BaseCase$avCost_hb_hdve<-((Lme_BaseCase$p2hdv_day/Lme_BaseCase$p2ev)/365*((Lme_BaseCase$costs_hb_e1*100*1000)/(Lme_BaseCase$M_2*(1-Lme_BaseCase$P_2/100)*Lme_BaseCase$LaengeElement)))
Lme_BaseCase$avCost_hb_hdvn<-((Lme_BaseCase$p2hdv_day/Lme_BaseCase$p2night)/365*((Lme_BaseCase$costs_hb_n1*100*1000)/(Lme_BaseCase$M_3*(1-Lme_BaseCase$P_3/100)*Lme_BaseCase$LaengeElement)))
#costs Thilo, ct/vkm:
Lme_BaseCase$avCost_tb_card<-((Lme_BaseCase$p2car_day/Lme_BaseCase$p2day)/365*((Lme_BaseCase$costs_tb_d1*100*1000)/(Lme_BaseCase$M_1*(1-Lme_BaseCase$P_1/100)*Lme_BaseCase$LaengeElement)))
Lme_BaseCase$avCost_tb_care<-((Lme_BaseCase$p2car_day/Lme_BaseCase$p2ev)/365*((Lme_BaseCase$costs_tb_e1*100*1000)/(Lme_BaseCase$M_2*(1-Lme_BaseCase$P_2/100)*Lme_BaseCase$LaengeElement)))
Lme_BaseCase$avCost_tb_carn<-((Lme_BaseCase$p2car_day/Lme_BaseCase$p2night)/365*((Lme_BaseCase$costs_tb_n1*100*1000)/(Lme_BaseCase$M_3*(1-Lme_BaseCase$P_3/100)*Lme_BaseCase$LaengeElement)))
Lme_BaseCase$avCost_tb_hdvd<-((Lme_BaseCase$p2hdv_day/Lme_BaseCase$p2day)/365*((Lme_BaseCase$costs_tb_d1*100*1000)/(Lme_BaseCase$M_1*(1-Lme_BaseCase$P_1/100)*Lme_BaseCase$LaengeElement)))
Lme_BaseCase$avCost_tb_hdve<-((Lme_BaseCase$p2hdv_day/Lme_BaseCase$p2ev)/365*((Lme_BaseCase$costs_tb_e1*100*1000)/(Lme_BaseCase$M_2*(1-Lme_BaseCase$P_2/100)*Lme_BaseCase$LaengeElement)))
Lme_BaseCase$avCost_tb_hdvn<-((Lme_BaseCase$p2hdv_day/Lme_BaseCase$p2night)/365*((Lme_BaseCase$costs_tb_n1*100*1000)/(Lme_BaseCase$M_3*(1-Lme_BaseCase$P_3/100)*Lme_BaseCase$LaengeElement)))




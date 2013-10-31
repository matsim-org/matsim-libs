\name{histAndBxp}
\alias{histAndBxp}
%- Also NEED an '\alias' for EACH other topic documented here.
\title{
	plot histogram with boxplot underneath
}
\description{
%%  ~~ A concise (1-5 lines) description of what the function does. ~~
}
\usage{
histAndBxp(x, width = 0.1, range = 1.5, border = NULL, ...)
}
%- maybe also 'usage' for other objects documented here.
\arguments{
  \item{x}{
%%     ~~Describe \code{x} here~~
}
  \item{width}{
%%     ~~Describe \code{width} here~~
}
  \item{range}{
%%     ~~Describe \code{range} here~~
}
  \item{border}{
%%     ~~Describe \code{border} here~~
}
  \item{\dots}{
%%     ~~Describe \code{\dots} here~~
}
}
\details{
%%  ~~ If necessary, more details than the description above ~~
}
\value{
%%  ~Describe the value returned
%%  If it is a LIST, use
%%  \item{comp1 }{Description of 'comp1'}
%%  \item{comp2 }{Description of 'comp2'}
%% ...
}
\references{
%% ~put references to the literature/web site here ~
}
\author{
%%  ~~who you are~~
}
\note{
%%  ~~further notes~~
}

%% ~Make other sections like Warning with \section{Warning }{....} ~

\seealso{
%% ~~objects to See Also as \code{\link{help}}, ~~~
}
\examples{
##---- Should be DIRECTLY executable !! ----
##-- ==>  Define data, use random,
##--	or do  help(data=index)  for the standard data sets.

## The function is currently defined as
function(
					   x, #the data
					   width=0.1, # the width of the bxp
					   range=1.5, #the whisker range
					   border=NULL, # the color
					   ... ) {
	h <- hist( x , col=0, border=0, ... )
	plot( h , border=border, ... )
	ymax <- par( "usr" )[4]
	ymin <- -width * ymax
	plot(h, ylim=c(ymin,ymax), border=border, ...)

	bxpData <- boxplot( x , range=range , plot=F )$stats
	##-- scale a range
	scale.r <- function(x1,x2, fact = 1.1)
		(x1+x2)/2 + c(-fact,fact) * (x2-x1)/2
	plotBxp(bxpData[1],
			bxpData[2],
			bxpData[3],
			bxpData[4],
			bxpData[5],
			scale.r(par("usr")[3], 0,
			 f = .8 - max(0, .15 - width)*(1+(par("mfg")[3] >= 3))),
			border)
	abline( h=0 );
  }
}
% Add one or more standard keywords, see file 'KEYWORDS' in the
% R documentation directory.
\keyword{ ~kwd1 }
\keyword{ ~kwd2 }% __ONLY ONE__ keyword per line

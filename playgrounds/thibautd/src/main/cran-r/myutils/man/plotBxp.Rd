\name{plotBxp}
\alias{plotBxp}
%- Also NEED an '\alias' for EACH other topic documented here.
\title{
	basic utility to create the box of a boxplot
}
\description{
%%  ~~ A concise (1-5 lines) description of what the function does. ~~
}
\usage{
plotBxp(mini, q1, med, q3, maxi, ys, border = NULL)
}
%- maybe also 'usage' for other objects documented here.
\arguments{
  \item{mini}{
%%     ~~Describe \code{mini} here~~
}
  \item{q1}{
%%     ~~Describe \code{q1} here~~
}
  \item{med}{
%%     ~~Describe \code{med} here~~
}
  \item{q3}{
%%     ~~Describe \code{q3} here~~
}
  \item{maxi}{
%%     ~~Describe \code{maxi} here~~
}
  \item{ys}{
%%     ~~Describe \code{ys} here~~
}
  \item{border}{
%%     ~~Describe \code{border} here~~
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
function(mini,q1,med,q3,maxi,ys,border=NULL) {
  if (is.null(border)) border="black"
  ymin <- ys[1]
  ymax <- ys[2]
  m <- (ymax + ymin)/2
  llhh <- c(ymin, ymin, ymax, ymax)
  ## drawing the box
  lines(c(q1,q1, q3, q3, q1), c(ymax,llhh), lty=1,col=border)
  ## Median
  lines(rep.int(med, 2), c(ymin, ymax), lwd = 2, lty = 1,col=border) #
  ## Whiskers
  lines(c(mini,q1, NA, q3, maxi), rep.int(m, 5), lty = 1,col=border) #
  }
}
% Add one or more standard keywords, see file 'KEYWORDS' in the
% R documentation directory.
\keyword{ ~kwd1 }
\keyword{ ~kwd2 }% __ONLY ONE__ keyword per line

.PHONY: pdf bib clean veryclean tags wc

NAME=user-guide

pdf:
	pdflatex ${NAME}

bib: clean
	-pdflatex -interaction nonstopmode ${NAME}
	-bibtex -min-crossrefs=99 ${NAME}
	-pdflatex -interaction nonstopmode ${NAME}
	-pdflatex -interaction nonstopmode ${NAME}

clean:
	-rm *.aux *.log

veryclean:
	-rm *.bbl *.blg

tags:
	-etags *.tex

wc:
	pdftotext ${NAME}.pdf
	wc ${NAME}.txt

localbib:
	bibexport.sh -a -o tub.bib ../../documents/inputs/bib/vsp ../../documents/inputs/bib/kai ../../documents/inputs/bib/ref
	./repair_bib.rb tub.bib

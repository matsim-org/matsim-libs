.PHONY: pdf pdf-full html bib clean veryclean tags wc

NAME=user-guide

pdf:
	pdflatex ${NAME}

# pdf-full is used by the build server to build the complete pdf from a clean checkout
pdf-full:
	pdflatex -interaction=nonstopmode ${NAME}
	-bibtex -min-crossrefs=99 ${NAME}
	pdflatex -interaction=nonstopmode ${NAME}
	pdflatex -interaction=nonstopmode ${NAME}


html:
	-mkdir html/
	htlatex ${NAME} 'html,2' "" -dhtml/ -interaction=nonstopmode 
	-cp -r figures html/

bib: clean
	-pdflatex -interaction nonstopmode ${NAME}
	-bibtex -min-crossrefs=99 ${NAME}
	-pdflatex -interaction nonstopmode ${NAME}
	-pdflatex -interaction nonstopmode ${NAME}

clean:
	-rm *.aux *.log *.out *.toc *.4ct *.4tc *.dvi *.idv *.lg *.tmp *.xref *.html *.css
	-rm chapters/*.aux

veryclean: clean
	-rm *.bbl *.blg *.pdf
	-rm -rf html

tags:
	-etags *.tex */*.tex

wc:
	pdftotext ${NAME}.pdf
	wc ${NAME}.txt

localbib:
	bibexport.sh -a -o tub.bib ../../documents/inputs/bib/vsp ../../documents/inputs/bib/kai ../../documents/inputs/bib/ref
	./repair_bib.rb tub.bib

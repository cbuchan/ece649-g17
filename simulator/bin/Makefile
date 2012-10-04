SOURCE=$(shell find jSimPack simulator -name '*.java')
CLASSES=$(subst .java,.class,$(SOURCE))

.PHONY: all code clean

all: code

code: $(CLASSES)

clean:
	find . -name '*'.class -exec rm -f {} ';'

$(CLASSES): %.class: %.java
	javac -source 1.5 -O -Xlint:all -cp $(PWD) $<

# ensure the next line is always the last line in this file.
# vi:noet

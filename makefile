JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
		  Vertex.java \
		  Contour.java \
		  EdgeDetection.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class

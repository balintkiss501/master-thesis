Information retrieval from Java archive format
==========================================================================================
**Computer Science master thesis by Bálint Kiss,**
**Eötvös Lóránd University, Budapest 2017**

This is my master thesis, which deals with the topic of reverse engineering Java
class structures from JAR files. The documentation contains technical background
information and methods on doing that, while describes implementation of a JAR
reader module for [CodeCompass](https://github.com/Ericsson/CodeCompass).

This reading is useful for anyone who wants to start out with reverse engineering
Java bytecode or anyone who wants an in-depth understanding on how the JVM executes
bytecode programs.

[Information retrieval from Java archive format (PDF)](https://github.com/balintkiss501/master-thesis/blob/master/balint_kiss_master_thesis.pdf)

## Abstract

During the course of my work, I contributed to CodeCompass, an open source code
comprehension tool made for making codebase of software projects written in C,
C++ and Java more understandable through navigation and visualization. I was
tasked with the development of a module for recovering code information of Java
classes in JAR files. This document details background concepts required for reverse-
engineering Java bytecode, creating a prototype JAR file reader and how this solu-
tion could be integrated to CodeCompass.

First, I studied the structure of JAR format and how class files are stored in it.
I looked into the Java Class file structure and how bytecode contained in class gets
interpreted by the Java Virtual Machine. I also looked at existing decompilers and
what bytecode libraries are.

I created a proof-of-concept prototype that reads compiled classes from JAR file
and extracts code information. I first showcased the use of Java Reflection API, then
the use of Apache Commons Byte Code Engineering Library, a third-party bytecode
library used for extracting and representing parts of Java class file as Java objects.
Finally, I examined how CodeCompass works, how part of the prototype could be
integrated into it and demonstrated the integration through parsing of a simple JAR
file.

## Contents of this repository

* Thesis in PDF format
* Source code of the JAR reader prototype in `jarreader-prototype/`
* API documentation for the JAR reader prototype in HTML form in `jarreader-prototype/doc/`
* Source code of example JAR files in `example-jars/`

## How to build the prototype and examples

### Requirements to build Java sources:

* [Java Development Kit (JDK)](http://www.oracle.com/technetwork/java/javase/downloads/index.html), at least version 1.8
* [Apache Maven](https://maven.apache.org/)

### How to build JAR reader prototype:

In the `jarreader-prototype` folder, issue Apache Maven with the following command:

```bash
mvn clean javadoc:javadoc package
```

The generated executable will be at "jarreader-prototype/target/jarreader-prototype-1.0.0.jar".

### How to build example JARs:

In the `example-jar` folder, issue Apache Maven with the following commands:

```bash
mvn -f ClassInJar/pom.xml clean package
mvn -f TestJar/pom.xml clean package
mvn -f InstanceMethodCalls/pom.xml clean package
mvn -f StaticMethodCalls/pom.xml clean package
```

## Copyright and License
Documentation and JAR reader prototype sources are Copyright 2017 by Balint Kiss.
JAR reader prototype sources are licensed with MIT license.

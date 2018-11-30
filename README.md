# Alloy 4 Fun

Web application for Alloy.

  

## About

  

Alloy is a language for describing structures and a tool for exploring them.

Alloy4Fun is a Web platform that supports edditing and interpreting Alloy models through your browser in real time.

The latest version is currently running at http://alloy4fun.di.uminho.pt.

  

Alloy4Fun is being developed using the Meteor framework which is a full-stack JavaScript platform for developing

modern web and mobile applications.

## Prerequisites

Install **meteor**. You cant get it here:
https://www.meteor.com/install

Install **Apache Tomcat**:
https://tomcat.apache.org

Install **Eclipse IDE for Java EE Developers**: (or any other IDE capable of creating a Dynamic web Service, although in this study case Ecplise is the one being used)
http://www.eclipse.org/downloads/packages/eclipse-ide-java-ee-developers/oxygen2

Add **axis2-1.7.7** Runtime location to Ecplise:
	- Window > Preferences > WebServices > Axis2 Preferences > [add axis2 folder location ]

Download these jar files:
-alloy4.2.jar 
-axis2-jaxws-1.7.7.jar
-javax.jws-3.1.2.jar
-javax.ws.rs-api-2.0.jar
-jaxws-rt.jar
-jaxws-tools.jar
-jstl-1.2.jar
-xmlschema-core-2.2.1.jar

## Running web application
installing **meteor** is pretty straight forward, just follow the steps in their website.

- in the folder  Alloy4FunMeteor just run:
```

>meteor run

```
## Running the web service:

  - create a Dynamic Web Project
	  - Project name: Alloy4Fun
	  - TargetRuntime: Apache TomCat v7.0
	  - Dynamic Web Module : 2.5
	  - Configuration > modify > select Axis2 Web Services
	  - Finish

- Alloy4Fun>JavaResources:
	- Create a new Package named **service**
	- Drag here the two java files in the **Alloy4FunWebService** folder

- Drag all the jar files mentioned above to this folder:
	- Alloy4Fun>WebContent>WEB-INF>lib


- create server
	- Windows > showView > servers 
		- new server > Tomcat V7.0 Server

- Right Bottom at Alloy4Fun > new > other > Web Service :
	- Service Implementation : service.AlloyService
	- WebService runtime :  Apache axis2
	- finish

- Open Alloy4Fun/WebContent/WEB-INF/services/AlloyService/META-INF/services.xml
	- delete all its content and paste the one in this repository in the Alloy4FunWebService folder.
	
- Right Bottom at Alloy4Fun > new > other > Web Service :
	- Service Implementation : service.AlloyService
	- WebService runtime :  Apache axis2
	- next > use existing services.xml > Alloy4Fun/WebContent/WEB-INF/services/AlloyService/META-INF/services.xml

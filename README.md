alfresco-webscript-manifold-connector
=====================================

Alfresco WebScripts Solr API Repository Connector for Apache ManifoldCF

Run Alfresco 4.2.c
---
```
cd alfresco-instance
mvn install -Pamp-to-war (advised MAVEN_OPTS="-Xms256m -Xmx2G -XX:PermSize=300m")
(will run on localhost:8080/alfresco-instance)
```

Install Manifold
---
```
curl http://www.apache.org/dist/manifoldcf/apache-manifoldcf-1.1.1-bin.zip > manifold-bin-1.1.1.zip
unzip manifold-bin-1.1.1.zip
```

Install the Alfresco WebScripts Connector
---
Prerequisite: you need to have installed Apache Maven 3.0.4

For building and packaging the connector run the following command from the root folder of the project:
```
mvn clean install
```


Deploy and configure the repository connector
---
```
cp mcf-alfresco-webscript-connector/target/mcf-alfresco-webscript-connector-1.1.1-jar-with-dependencies.jar $MANIFOLD_HOME/connector-lib
```

edit <code>$MANIFOLD_HOME/connectors.xml</code> and add this snippet:

```xml
<repositoryconnector
name="AlfrescoWebscript"
class="org.apache.manifoldcf.crawler.connectors.alfrescowebscripts.AlfrescoWebScriptsRepositoryConnector"/>
```

edit <code>$MANIFOLD_HOME/logging.ini</code> if you want to raise the default (WARN) Manifold log level

create a new <code>conf</code> folder in <code>$MANIFOLD_HOME/example/lib</code> 

copy in the new <code>conf</code> folder all these files related to the keystore configuration of your Alfresco instance:
```
-ssl-keystore-passwords.properties
-ssl-truststore-passwords.properties
-ssl.repo.client.keystore
-ssl.repo.client.truststore
```

Finally to start Apache ManifoldCF use the following command:
```
java -jar $MANIFOLD_HOME/example/start.jar
```

Remember to set the valid pointers to the Alfresco instance:
host: localhost
port: 8080
path: alfresco-instance
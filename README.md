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

Install and run Manifold 1.1.1
---
```
curl http://www.apache.org/dist/manifoldcf/apache-manifoldcf-1.1.1-bin.zip > manifold-bin-1.1.1.zip
unzip manifold-bin-1.1.1.zip
export MANIFOLD_HOME=$PWD/apache-manifoldcf-1.1.1
java -jar $MANIFOLD_HOME/example/start.jar
```


Install and run Solr 4.1.0
---
```
curl http://apache.rediris.es/lucene/solr/4.1.0/solr-4.1.0.zip > solr-4.1.0.zip
unzip solr-4.1.0.zip
export SOLR_HOME=$PWD/solr-4.1.0
java -jar $SOLR_HOME/example/start.jar
```

Install the Alfresco WebScripts Connector
---
Prerequisite: you need to have installed Apache Maven 3.0.4

For building and packaging the connector run the following command from the root folder of the project:
```
mvn clean install
```

Copy the conf folder into the manifold classpath
```
cp -Rf alfresco-webscript-manifold-connector/mcf-alfresco-webscript-connector/src/test/resources/conf $MANIFOLD_HOME/example
cp -Rf alfresco-webscript-manifold-connector/mcf-alfresco-webscript-connector/src/test/resources/alfrescoResources $MANIFOLD_HOME/example
```

Deploy and configure the Alfresco Webscript connector
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

Configure Manifold and start!
---
* Add Output Connection (List Output Connections)
  * Set name (free)
  * Set type to <code>Solr</code>
  * Save (all options by default)

* Add Repository Connection (List of Repository Connections)
  * Set name (free)
  * Set type to <code>AlfrescoWebscript</code>
  * Click on <code>Throttling</code> tab
  * Set <code>Max Connections</code> to <code>1</code>
  * Click on <code>AlfrescoWebscriptConnector.Server</code> tab
  * Set <code>Path</code> to <code>/alfresco-instance</code> (other options by default)
  * Save (all options by default)

* Add Job (List all Jobs)
  * Set name (free)
  * Click on <code>Connection</code> tab
  * Set <code>Output connection</code> to <code>your_output_connection</code>
  * Set <code>Repository connection</code> to <code>your_repository_connection</code>
  * Save (all options by default)

* Start the Job (Status and Job Management)
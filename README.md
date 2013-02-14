alfresco-webscript-manifold-connector
=====================================

Alfresco Solr API Repository Connector for Apache ManifoldCF


Install Manifold
---
```
curl http://www.apache.org/dist/manifoldcf/apache-manifoldcf-1.0.1-bin.zip > manifold-bin-1.0.1.zip
unzip manifold-bin-1.0.1.zip
```

Install the Alfresco Webscript Connector
---

```
mvn clean install
```

Deploy and configure the connector
---

```
cp target/mcf-alfresco-webscript-connector-1.1.1-jar-with-dependencies.jar $MANIFOLD_HOME/connector-lib
```

edit <code>$MANIFOLD_HOMEconnectors.xml</code> and add

```xml
<repositoryconnector
name="AlfrescoWebscript"
class="org.apache.manifoldcf.crawler.connectors.alfrescowebscripts.AlfrescoWebscriptsRepositoryConnector"/>
```

```
java -jar $MANIFOLD_HOME/example/start.jar
```
alfresco-webscript-manifold-connector
=====================================

Alfresco WebScripts Solr API Repository Connector for Apache ManifoldCF


Install Manifold
---
```
curl http://www.apache.org/dist/manifoldcf/apache-manifoldcf-1.1.1-bin.zip > manifold-bin-1.1.1.zip
unzip manifold-bin-1.1.1.zip
```

Install the Alfresco WebScripts Connector
---

```
mvn clean install
```

Deploy and configure the repository connector
---

```
cp target/mcf-alfresco-webscript-connector-1.1.1-jar-with-dependencies.jar $MANIFOLD_HOME/connector-lib
```

edit <code>$MANIFOLD_HOME/connectors.xml</code> and add

```xml
<repositoryconnector
name="AlfrescoWebscript"
class="org.apache.manifoldcf.crawler.connectors.alfrescowebscripts.AlfrescoWebScriptsRepositoryConnector"/>
```

```
create a new <code>conf</code> folder in <code>$MANIFOLD_HOME/example/lib</code> 
```

```
copy in the new <code>conf</code> folder all these files related to the keystore configuration of your Alfresco instance:
-ssl-keystore-passwords.properties
-ssl-truststore-passwords.properties
-ssl.repo.client.keystore
-ssl.repo.client.truststore
```

```
java -jar $MANIFOLD_HOME/example/start.jar
```
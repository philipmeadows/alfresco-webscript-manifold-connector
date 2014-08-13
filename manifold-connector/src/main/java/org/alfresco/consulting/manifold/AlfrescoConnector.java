package org.alfresco.consulting.manifold;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import org.alfresco.consulting.indexer.client.AlfrescoClient;
import org.alfresco.consulting.indexer.client.AlfrescoDownException;
import org.alfresco.consulting.indexer.client.AlfrescoResponse;
import org.alfresco.consulting.indexer.client.WebScriptsAlfrescoClient;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
import org.apache.manifoldcf.core.interfaces.ConfigParams;
import org.apache.manifoldcf.core.interfaces.IHTTPOutput;
import org.apache.manifoldcf.core.interfaces.IPostParameters;
import org.apache.manifoldcf.core.interfaces.IThreadContext;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.interfaces.Specification;
import org.apache.manifoldcf.crawler.connectors.BaseRepositoryConnector;
import org.apache.manifoldcf.crawler.interfaces.DocumentSpecification;
import org.apache.manifoldcf.crawler.interfaces.IProcessActivity;
import org.apache.manifoldcf.crawler.interfaces.ISeedingActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xml.messaging.saaj.util.ByteInputStream;

public class AlfrescoConnector extends BaseRepositoryConnector {
  private static final Logger logger = LoggerFactory.getLogger(AlfrescoConnector.class);
  private static final String ACTIVITY_FETCH = "fetch document";
  private static final String[] activitiesList = new String[]{ACTIVITY_FETCH};
  private AlfrescoClient alfrescoClient;
  private Boolean enableDocumentProcessing = Boolean.TRUE;
  
  private static final String CONTENT_URL_PROPERTY = "contentUrlPath";
  private static final String AUTHORITIES_PROPERTY = "readableAuthorities";
  
  // Static Fields
  private static final String FIELD_UUID = "uuid";
  private static final String FIELD_NODEREF = "nodeRef";
  private static final String FIELD_TYPE = "type";
  private static final String FIELD_NAME = "name";

  @Override
  public int getConnectorModel() {
    return MODEL_ALL; // We will always return all specified documents.
  }

  void setClient(AlfrescoClient client) {
    alfrescoClient = client;
  }

  @Override
  public void connect(ConfigParams config) {
    super.connect(config);

    String protocol = getConfig(config, "protocol", "http");
    String hostname = getConfig(config, "hostname", "localhost");
    String endpoint = getConfig(config, "endpoint", "/alfresco/service");
    String storeProtocol = getConfig(config, "storeprotocol", "workspace");
    String storeId = getConfig(config, "storeid", "SpacesStore");
    String username = getConfig(config, "username", null);
    String password = getConfig(config, "password", null);
    this.enableDocumentProcessing = new Boolean(getConfig(config, "enabledocumentprocessing", "false"));

    alfrescoClient = new WebScriptsAlfrescoClient(protocol, hostname, endpoint,
            storeProtocol, storeId, username, password);
  }

  private static String getConfig(ConfigParams config,
                                  String parameter,
                                  String defaultValue) {
    final String protocol = config.getParameter(parameter);
    if (protocol == null) {
      return defaultValue;
    }
    return protocol;
  }

  @Override
  public String check() throws ManifoldCFException {
    return super.check();
  }

  @Override
  public void disconnect() throws ManifoldCFException {
    super.disconnect();
  }

  @Override
  public String[] getActivitiesList() {
    return activitiesList;
  }

  @Override
  public int getMaxDocumentRequest() {
    return 20;
  }

  @Override
  public String addSeedDocuments(ISeedingActivity activities, Specification spec,
                                              String lastSeedVersion, long seedTime, int jobMode) throws ManifoldCFException, ServiceInterruption {
    try {
      long lastTransactionId = 0;
      long lastAclChangesetId = 0;
      
      if(lastSeedVersion != null && !lastSeedVersion.isEmpty()){
    	  StringTokenizer tokenizer = new StringTokenizer(lastSeedVersion,"|");

    	  if (tokenizer.countTokens() == 2) {
    		  lastTransactionId = new Long(tokenizer.nextToken());
    		  lastAclChangesetId = new Long(tokenizer.nextToken());
    	  }
      }
      
      logger.info("Starting from transaction id: {} and acl changeset id: {}", lastTransactionId, lastAclChangesetId);
      
      long transactionIdsProcessed;
      long aclChangesetsProcessed;
      do {
        final AlfrescoResponse response = alfrescoClient.
        		fetchNodes(lastTransactionId, 
        				lastAclChangesetId,
        				ConfigurationHandler.getFilters(spec));
        int count = 0;
        for (Map<String, Object> doc : response.getDocuments()) {
//          String json = gson.toJson(doc);
//          activities.addSeedDocument(json);
          String uuid = doc.get("uuid").toString();
          activities.addSeedDocument(uuid);
          count++;
        }
        logger.info("Fetched and added {} seed documents", count);

        transactionIdsProcessed = response.getLastTransactionId() - lastTransactionId;
        aclChangesetsProcessed = response.getLastAclChangesetId() - lastAclChangesetId;

        lastTransactionId = response.getLastTransactionId();
        lastAclChangesetId = response.getLastAclChangesetId();

        logger.info("transaction_id={}, acl_changeset_id={}", lastTransactionId, lastAclChangesetId);
      } while (transactionIdsProcessed > 0 || aclChangesetsProcessed > 0);

      logger.info("Recording {} as last transaction id and {} as last changeset id", lastTransactionId, lastAclChangesetId);
      return lastTransactionId + "|" + lastAclChangesetId;
    } catch (AlfrescoDownException e) {
      throw new ManifoldCFException(e);
    }
  }

@Override
  public void processDocuments(String[] documentIdentifiers, String[] versions,
                               IProcessActivity activities, DocumentSpecification spec,
                               boolean[] scanOnly, int jobMode) throws ManifoldCFException,
          ServiceInterruption {
	int i = 0;  
    for (String doc : documentIdentifiers) {
    
      String nextVersion = versions[i++];	
    	
      // Calling again Alfresco API because Document's actions are lost from seeding method
      AlfrescoResponse response = alfrescoClient.fetchNode(doc);
      if(response.getDocumentList().isEmpty()){ // Not found seeded document. Could reflect an error in Alfresco
    	  logger.error("Invalid Seeded Document from Alfresco with ID {}", doc);
    	  activities.noDocument(doc, nextVersion);
    	  continue;
      }
      Map<String, Object> map = response.getDocumentList().get(0); // Should be only one
      RepositoryDocument rd = new RepositoryDocument();
      String uuid = map.get(FIELD_UUID).toString();
      String nodeRef = map.get(FIELD_NODEREF).toString();
      rd.addField(FIELD_NODEREF, nodeRef);
      String type = map.get(FIELD_TYPE).toString();
      rd.addField(FIELD_TYPE, type);
      String name = map.get(FIELD_NAME).toString();
      rd.setFileName(name);

      if ((Boolean) map.get("deleted")) {
        activities.deleteDocument(uuid);
      } else {
        if (this.enableDocumentProcessing) {
          try{
          	processMetaData(rd,uuid);
          }catch(AlfrescoDownException e){
        	  logger.error("Invalid Document from Alfresco with ID {}", uuid, e);
        	  activities.noDocument(doc, nextVersion);
        	  continue; // No Metadata, No Content....skip document
          }
        }
        try {
        	if(rd.getBinaryStream() == null){
        		byte[] empty = new byte[0];
        		rd.setBinary(new ByteInputStream(empty, 0), 0L);
        	}
        	logger.info("Ingesting with id: {}, URI {} and rd {}", uuid, nodeRef, rd.getFileName());
			activities.ingestDocumentWithException(uuid, "", uuid, rd);
		} catch (IOException e) {
			throw new ManifoldCFException(
					"Error Ingesting Document with ID " + String.valueOf(uuid), e);
		}
      }
    }
  }
  
  @Override
  public String[] getDocumentVersions(String[] documentIdentifiers, DocumentSpecification spec)
		    throws ManifoldCFException, ServiceInterruption{
	  String[] versions = new String[documentIdentifiers.length];
	  for(int i = 0; i < documentIdentifiers.length; i++)
		  versions[i] = ConfigurationHandler.getSpecificationVersion(spec) + documentIdentifiers[i];
	  return versions;
  }

  private void processMetaData(RepositoryDocument rd,
		  String uuid) throws ManifoldCFException, AlfrescoDownException {
    Map<String,Object> properties = alfrescoClient.fetchMetadata(uuid);
    for(String property : properties.keySet()) {
      Object propertyValue = properties.get(property);
      rd.addField(property,propertyValue.toString());
    }
    
    // Document Binary Content
    String contentUrlPath = (String) properties.get(CONTENT_URL_PROPERTY);
    if(contentUrlPath != null && !contentUrlPath.isEmpty()){
    	InputStream binaryContent = alfrescoClient.fetchContent(contentUrlPath);
    	if(binaryContent != null) // Content-based Alfresco Document
    		rd.setBinary(binaryContent, 0L);
    }
    
    // Indexing Permissions
    @SuppressWarnings("unchecked")
	List<String> permissions = (List<String>) properties.remove(AUTHORITIES_PROPERTY);
    rd.setSecurityACL(RepositoryDocument.SECURITY_TYPE_DOCUMENT,
    		permissions.toArray(new String[permissions.size()]));
  }

  @Override
  public void outputConfigurationHeader(IThreadContext threadContext,
                                        IHTTPOutput out, Locale locale, ConfigParams parameters,
                                        List<String> tabsArray) throws ManifoldCFException, IOException {
    ConfigurationHandler.outputConfigurationHeader(threadContext, out, locale,
            parameters, tabsArray);
  }

  @Override
  public void outputConfigurationBody(IThreadContext threadContext,
                                      IHTTPOutput out, Locale locale, ConfigParams parameters, String tabName)
          throws ManifoldCFException, IOException {
    ConfigurationHandler.outputConfigurationBody(threadContext, out, locale,
            parameters, tabName);
  }

  @Override
  public String processConfigurationPost(IThreadContext threadContext,
                                         IPostParameters variableContext, Locale locale, ConfigParams parameters)
          throws ManifoldCFException {
    return ConfigurationHandler.processConfigurationPost(threadContext,
            variableContext, locale, parameters);
  }

  @Override
  public void viewConfiguration(IThreadContext threadContext, IHTTPOutput out,
                                Locale locale, ConfigParams parameters) throws ManifoldCFException,
          IOException {
    ConfigurationHandler.viewConfiguration(threadContext, out, locale,
            parameters);
  }
  
  
   @Override
   public void outputSpecificationHeader(IHTTPOutput out, Locale locale, Specification os,
     int connectionSequenceNumber, List<String> tabsArray)
     throws ManifoldCFException, IOException{
	   ConfigurationHandler.outputSpecificationHeader(out, locale, os, connectionSequenceNumber, tabsArray);
   }
   
   @Override
   public void outputSpecificationBody(IHTTPOutput out, Locale locale, Specification os,
		    int connectionSequenceNumber, int actualSequenceNumber, String tabName) throws ManifoldCFException, IOException{
	   ConfigurationHandler.outputSpecificationBody(out, locale, os, connectionSequenceNumber, actualSequenceNumber, tabName);
   }
   
   @Override
   public String processSpecificationPost(IPostParameters variableContext, Locale locale, Specification os,
			  int connectionSequenceNumber) throws ManifoldCFException{
	   return ConfigurationHandler.processSpecificationPost(variableContext, locale, os, connectionSequenceNumber);
   }
   
   @Override
   public void viewSpecification(IHTTPOutput out, Locale locale, Specification os,
			  int connectionSequenceNumber) throws ManifoldCFException, IOException{
	   ConfigurationHandler.viewSpecification(out, locale, os, connectionSequenceNumber);
   }
  
}
package org.apache.manifoldcf.crawler.connectors.alfrescowebscripts;

import org.alfresco.encryption.KeyStoreParameters;
import org.alfresco.encryption.ssl.SSLEncryptionParameters;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.httpclient.AlfrescoHttpClient;
import org.alfresco.httpclient.AuthenticationException;
import org.alfresco.httpclient.HttpClientFactory;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.cache.MemoryCache;
import org.alfresco.repo.dictionary.*;
import org.alfresco.repo.tenant.SingleTServiceImpl;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.service.cmr.dictionary.ModelDefinition;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.client.*;
import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.crawler.interfaces.DocumentSpecification;
import org.apache.manifoldcf.crawler.system.Logging;
import org.apache.solr.core.SolrResourceLoader;
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class AlfrescoIndexTracker {

  //@TODO - Static parameters that should be configurable
  public static final int RESULT_BATCH_SIZE = 10;
  private static final int ALFRESCO_SSL_PORT = 8043;
  private static final int MAX_TOTAL_CONNECTIONS = 40;
  private static final int MAX_HOST_CONNECTIONS = 40;
  private static final int SOCKET_TIMEOUT = 60000;
  private final static String SSL_KEY_STORE_TYPE = "JCEKS";
  private final static String SSL_KEY_STORE_PROVIDER = "";
  private final static String SSL_KEY_STORE_LOCATION = "ssl.repo.client.keystore";
  private final static String SSL_KEY_STORE_PASSWORD_FILE_LOCATION = "ssl-keystore-passwords.properties";
  private final static String SSL_TRUST_STORE_TYPE = "JCEKS";
  private final static String SSL_TRUST_STORE_PROVIDER = "";
  private final static String SSL_TRUST_STORE_LOCATION = "ssl.repo.client.truststore";
  private final static String SSL_TRUST_STORE_PASSWORD_FILE_LOCATION = "ssl-truststore-passwords.properties";
  private static final String CONFIG_ALFRESCO_RESOURCES_PATH = "alfrescoResources";

  private final String username;
  private final String password;
  private final String protocol;
  private final String server;
  private final String port;
  private final String path;
  private final String configPath;

  private SOLRAPIClient solrapiClient;

  /**
   * contains the Snapshot of indexing from the last thread being allocated *
   */
  private static volatile IndexingSnapshot lastStatus = new IndexingSnapshot(0,0);

  public AlfrescoIndexTracker(String configPath, String username, String password, String protocol, String server, String port, String path) {
    this.configPath = configPath;
    this.username = username;
    this.password = password;
    this.protocol = protocol;
    this.server = server;
    this.port = port;
    this.path = path;
  }

  public void init() {
    KeyStoreParameters keyStoreParameters = new KeyStoreParameters("SSL Key Store", SSL_KEY_STORE_TYPE, SSL_KEY_STORE_PROVIDER, SSL_KEY_STORE_PASSWORD_FILE_LOCATION, SSL_KEY_STORE_LOCATION);
    KeyStoreParameters trustStoreParameters = new KeyStoreParameters("SSL Trust Store", SSL_TRUST_STORE_TYPE, SSL_TRUST_STORE_PROVIDER, SSL_TRUST_STORE_PASSWORD_FILE_LOCATION, SSL_TRUST_STORE_LOCATION);
    SSLEncryptionParameters sslEncryptionParameters = new SSLEncryptionParameters(keyStoreParameters, trustStoreParameters);
    SolrKeyResourceLoader keyResourceLoader = new SolrKeyResourceLoader(new SolrResourceLoader(configPath));

    HttpClientFactory httpClientFactory = new HttpClientFactory(HttpClientFactory.SecureCommsType.NONE,
        sslEncryptionParameters, keyResourceLoader, null, null, server, new Integer(port), ALFRESCO_SSL_PORT, MAX_TOTAL_CONNECTIONS, MAX_HOST_CONNECTIONS, SOCKET_TIMEOUT);

    AlfrescoHttpClient repoClient = httpClientFactory.getRepoClient(server, ALFRESCO_SSL_PORT);
    repoClient.setBaseUrl(path);

    TenantService tenantService = new SingleTServiceImpl();

    AlfrescoSolrDataModel dataModel = AlfrescoSolrDataModel.getInstance(configPath);
    dataModel.setStoreAll(true);

    NamespaceDAOImpl namespaceDAO = new NamespaceDAOImpl();
    namespaceDAO.setTenantService(tenantService);
    namespaceDAO.setNamespaceRegistryCache(new MemoryCache<String, NamespaceDAOImpl.NamespaceRegistry>());

    DictionaryDAOImpl dictionaryDAO = new DictionaryDAOImpl(namespaceDAO);
    dictionaryDAO.setTenantService(tenantService);
    dictionaryDAO.setDictionaryRegistryCache(new MemoryCache<String, DictionaryDAOImpl.DictionaryRegistry>());
    dictionaryDAO.setDefaultAnalyserResourceBundleName("alfresco/model/dataTypeAnalyzers");
    dictionaryDAO.setResourceClassLoader(getResourceClassLoader());

    DictionaryComponent dictionaryComponent = new DictionaryComponent();
    dictionaryComponent.setDictionaryDAO(dictionaryDAO);
    //@TODO - cannot find StaticMessageLookup
    //dictionaryComponent.setMessageLookup(new StaticMessageLookup());

    this.solrapiClient = new SOLRAPIClient(repoClient, dictionaryComponent, namespaceDAO);
    trackModels(dataModel);
  }

  private void trackModels(AlfrescoSolrDataModel dataModel) {
    try {
      List<AlfrescoModelDiff> modelDiffs = this.solrapiClient.getModelsDiff(dataModel.getAlfrescoModels());
      HashMap<String, M2Model> modelMap = new HashMap<String, M2Model>();

      for (AlfrescoModelDiff modelDiff : modelDiffs) {
        switch (modelDiff.getType()) {
          case NEW:
            AlfrescoModel newModel = this.solrapiClient.getModel(modelDiff.getModelName());
            for (M2Namespace namespace : newModel.getModel().getNamespaces()) {
              modelMap.put(namespace.getUri(), newModel.getModel());
            }
            break;
        }
      }

      HashSet<String> loadedModels = new HashSet<String>();
      for (M2Model model : modelMap.values()) {
        loadModel(modelMap, loadedModels, model, dataModel);
      }
      if (modelDiffs.size() > 0) {
        dataModel.afterInitModels();
      }

      File alfrescoModelDir = new File(configPath, "alfrescoModels");
      if (!alfrescoModelDir.exists()) {
        alfrescoModelDir.mkdir();
      }
      for (AlfrescoModelDiff modelDiff : modelDiffs) {
        switch (modelDiff.getType()) {
          case NEW:
            M2Model newModel = dataModel.getM2Model(modelDiff.getModelName());
            // add on file
            File newFile = new File(alfrescoModelDir, getModelFileName(newModel));
            FileOutputStream nos = new FileOutputStream(newFile);
            newModel.toXML(nos);
            nos.flush();
            nos.close();
            break;
        }
      }
    } catch (IOException e) {

    } catch (AuthenticationException e) {
      Logging.connectors.error("Error on trackModels; indexStatus: "+lastStatus, e);
    } catch (JSONException e) {
      Logging.connectors.error("Error on trackModels; indexStatus: "+lastStatus, e);
    }
  }

  private String getModelFileName(M2Model model) {
    return model.getName().replace(":", ".") + "." + model.getChecksum(ModelDefinition.XMLBindingType.DEFAULT) + ".xml";
  }

  private void loadModel(Map<String, M2Model> modelMap, HashSet<String> loadedModels, M2Model model, AlfrescoSolrDataModel dataModel) {
    String modelName = model.getName();
    if (loadedModels.contains(modelName) == false) {
      for (M2Namespace importNamespace : model.getImports()) {
        M2Model importedModel = modelMap.get(importNamespace.getUri());
        if (importedModel != null) {
          // Ensure that the imported model is loaded first
          loadModel(modelMap, loadedModels, importedModel, dataModel);
        }
      }

      dataModel.putModel(model);
      loadedModels.add(modelName);
      Logging.connectors.info("Loading model " + model.getName());
    }
  }

  public ClassLoader getResourceClassLoader() {
    File f = new File(CONFIG_ALFRESCO_RESOURCES_PATH);
    if (f.canRead() && f.isDirectory()) {
      URL[] urls = new URL[1];
      try {
        URL url = f.toURI().normalize().toURL();
        urls[0] = url;
      } catch (MalformedURLException e) {
        throw new AlfrescoRuntimeException("Failed to add resources to classpath ", e);
      }
      return URLClassLoader.newInstance(urls, this.getClass().getClassLoader());
    } else {
      return this.getClass().getClassLoader();
    }
  }

  public Pair<RepositoryDocument, String> processMetaData(Long nodeId) throws ManifoldCFException {

    RepositoryDocument rd = new RepositoryDocument();

    NodeMetaDataParameters nmdp = new NodeMetaDataParameters();
    nmdp.setFromNodeId(nodeId);
    nmdp.setToNodeId(nodeId);
    nmdp.setIncludeAclId(true);
    nmdp.setIncludeAspects(true);
    nmdp.setIncludeChildAssociations(false);
    nmdp.setIncludeChildIds(true);
    nmdp.setIncludeNodeRef(true);
    nmdp.setIncludeOwner(true);
    nmdp.setIncludeParentAssociations(true);
    nmdp.setIncludePaths(true);
    nmdp.setIncludeProperties(false);
    nmdp.setIncludeType(true);
    nmdp.setIncludeTxnId(true);
    List<NodeMetaData> nodeMetaDatas = null;
    try {
      nodeMetaDatas = solrapiClient.getNodesMetaData(nmdp, 1);
    } catch (AuthenticationException e) {
      Logging.connectors.error("Error on getNodesMetaData fromId " + nmdp.getFromNodeId() + " toId " + nmdp.getToNodeId(), e);
    } catch (IOException e) {
      Logging.connectors.error("Error on getNodesMetaData fromId " + nmdp.getFromNodeId() + " toId " + nmdp.getToNodeId(), e);
    } catch (JSONException e) {
      Logging.connectors.error("Error on getNodesMetaData fromId " + nmdp.getFromNodeId() + " toId " + nmdp.getToNodeId(), e);
    } catch (Exception e){
      Logging.connectors.error("Error on getNodesMetaData fromId " + nmdp.getFromNodeId() + " toId " + nmdp.getToNodeId(), e);
    }

    if (nodeMetaDatas != null) {
      NodeMetaData metaData = nodeMetaDatas.get(0);
      Logging.connectors.info("Indexing metadata: " +
          "Type:" + metaData.getType() +
          ",ACL ID:" + metaData.getAclId() +
          ",Paths:" + metaData.getPaths() +
          ",Owner:" + metaData.getOwner() +
          ",Tenant:" + metaData.getTenantDomain() +
          ",Ancestors (number):" + metaData.getAncestors().size() +
          ",Child assocs (number):" + metaData.getChildAssocs().size() +
          ",Parent assocs (number):" + metaData.getParentAssocs().size() +
          ",Aspects:" + metaData.getAspects() +
          ",Properties:" + metaData.getProperties());

      Map<QName, PropertyValue> properties = metaData.getProperties();

      Iterator<Map.Entry<QName, PropertyValue>> propsIterator = properties.entrySet().iterator();
      while (propsIterator.hasNext()) {
        Map.Entry<QName, PropertyValue> entry = propsIterator.next();
        String prefix = entry.getKey().getPrefixString();
        String localname = entry.getKey().getLocalName();
        String fieldName = prefix + ":" + localname;
        String value = entry.getValue().toString();
        rd.addField(fieldName, value);
        Logging.connectors.debug("Indexing property: " + fieldName + " , value: " + value);
      }
      PropertyValue version = properties.get(ContentModel.PROP_VERSION_LABEL);
      String versionString = StringUtils.EMPTY;
      if (version != null) {
        versionString = version.toString();
      }
      return new Pair<RepositoryDocument, String>(rd, versionString);


      /**
       * TODO - for each seed document we will extract metadata
       * if the node is a folder then we need to get all the children and adding them as document references.
       * In this way the crawler will add all the children in the navigation
       *
       */

//      String uuid = NodeUtils.getUuidFromNodeReference(nodeReference);
//
//      if (Logging.connectors.isDebugEnabled())
//        Logging.connectors.debug("Alfresco: Processing document identifier '"
//            + nodeReference + "'");
//
//      Reference reference = new Reference();
//      reference.setStore(SearchUtils.STORE);
//      reference.setUuid(uuid);
//
//      Predicate predicate = new Predicate();
//      predicate.setStore(SearchUtils.STORE);
//      predicate.setNodes(new Reference[] { reference });
//
//      // getting properties
//      Node resultNode = NodeUtils.get(username, password, session, predicate);
//
//      String errorCode = "OK";
//      String errorDesc = StringUtils.EMPTY;
//
//      NamedValue[] properties = resultNode.getProperties();
//      boolean isDocument = ContentModelUtils.isDocument(properties);
//
//      boolean isFolder = ContentModelUtils.isFolder(username, password, session, reference);

      //a generic node in Alfresco could have child-associations
//      if (isFolder) {
//        // ingest all the children of the folder
//        QueryResult queryResult = SearchUtils.getChildren(username, password, session, reference);
//        ResultSet resultSet = queryResult.getResultSet();
//        ResultSetRow[] resultSetRows = resultSet.getRows();
//        for (ResultSetRow resultSetRow : resultSetRows) {
//          NamedValue[] childProperties = resultSetRow.getColumns();
//          String childNodeReference = PropertiesUtils.getNodeReference(childProperties);
//          activities.addDocumentReference(childNodeReference, nodeReference, RELATIONSHIP_CHILD);
//        }
//      }

      //a generic node in Alfresco could also have binaries content
//      if (isDocument) {
//        // this is a content to ingest
//        InputStream is = null;
//        long fileLength = 0;
//        try {
//          //properties ingestion
//          RepositoryDocument rd = new RepositoryDocument();
//          PropertiesUtils.ingestProperties(rd, properties);
//
//          // binaries ingestion - in Alfresco we could have more than one binary for each node (custom content models)
//          List<NamedValue> contentProperties = PropertiesUtils.getContentProperties(properties);
//          for (NamedValue contentProperty : contentProperties) {
//            //we are ingesting all the binaries defined as d:content property in the Alfresco content model
//            Content binary = ContentReader.read(username, password, session, predicate, contentProperty.getName());
//            fileLength = binary.getLength();
//            is = ContentReader.getBinary(binary, username, password, session);
//            rd.setBinary(is, fileLength);
//
//            //configPath is the node reference only if the node has an unique content stream
//            //For a node with a single d:content property: configPath = node reference
//            String configPath = PropertiesUtils.getNodeReference(properties);
//
//            //For a node with multiple d:content properties: configPath = node reference;QName
//            //The QName of a property of type d:content will be appended to the node reference
//            if(contentProperties.size()>1){
//              configPath = configPath + INGESTION_SEPARATOR_FOR_MULTI_BINARY + contentProperty.getName();
//            }
//
//            //version label
//            String version = PropertiesUtils.getVersionLabel(properties);
//
//            //the document uri is related to the specific d:content property available in the node
//            //we want to ingest each content stream that are nested in a single node
//            String documentURI = binary.getUrl();
//            activities.ingestDocument(configPath, version, documentURI, rd);
//          }
//
//        } finally {
//          try {
//            if(is!=null){
//              is.close();
//            }
//          } catch (InterruptedIOException e) {
//            errorCode = "Interrupted error";
//            errorDesc = e.getMessage();
//            throw new ManifoldCFException(e.getMessage(), e,
//                ManifoldCFException.INTERRUPTED);
//          } catch (IOException e) {
//            errorCode = "IO ERROR";
//            errorDesc = e.getMessage();
//            Logging.connectors.warn(
//                "Alfresco: IOException closing file input stream: "
//                    + e.getMessage(), e);
//          }
//
//          AuthenticationUtils.endSession();
//          session = null;
//
//          activities.recordActivity(new Long(startTime), ACTIVITY_READ,
//              fileLength, nodeReference, errorCode, errorDesc, null);
//        }

//     }
    }
    return null;
  }

  public List<String> getNodeIds() {
    Logging.connectors.info("Fetching transactions: " + lastStatus);

    List<String> ret = new ArrayList<String>();
    List<Node> nodes = null;
    ArrayList<Long> txs = new ArrayList<Long>();
    try {

      Transactions transactions = null;
      synchronized (this) {
        transactions = this.solrapiClient.getTransactions(
            lastStatus.getCommitTime(), //fromCommitTime
            lastStatus.getTxnId(), //minTxnId
            null, //toCommitTime
            null, //maxTxnId
            RESULT_BATCH_SIZE); //RESULT_BATCH_SIZE
        lastStatus = new IndexingSnapshot(new Date().getTime(), lastStatus.getTxnId() + RESULT_BATCH_SIZE);
      }

      Logging.connectors.info("Fetched " + transactions.getTransactions().size() + " transactions from solrApiClient");
      GetNodesParameters getNodeParameters = new GetNodesParameters();
      getNodeParameters.setTransactionIds(txs);
      getNodeParameters.setStoreProtocol(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol());
      getNodeParameters.setStoreIdentifier(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier());
      for (Transaction transaction : transactions.getTransactions()) {
        txs.add(transaction.getId());
      }
      nodes = solrapiClient.getNodes(getNodeParameters, RESULT_BATCH_SIZE);
      if (nodes != null) {
        for (Node node : nodes) {
          Logging.connectors.info("Indexing node: " +
              "nodeRef:" + node.getNodeRef() +
              ",TXN ID:" + node.getTxnId() +
              ",ID:" + node.getId() +
              ",status:" + node.getStatus());

          ret.add(String.valueOf(node.getId()));

        }
        return ret;
      }
    } catch (AuthenticationException e) {
      Logging.connectors.error("Error on getNodes of following TxnIds " + txs);
    } catch (IOException e) {
      Logging.connectors.error("Error on getNodes of following TxnIds " + txs);
    } catch (JSONException e) {
      Logging.connectors.error("Error on getNodes of following TxnIds " + txs);
    }
    return null;
  }

  public String[] getDocumentVersions(String[] documentIdentifiers, DocumentSpecification spec) {
    String[] rval = new String[documentIdentifiers.length];
    int i = 0;
    while (i < rval.length) {
      /**
       * TODO we should retrieve the version information for each node
       * ManifoldCF will process every node if rval[i] is empty, otherwise
       * will process nodes with a new version.
       * 
       * ManifoldCF will not process version nodes that have been previously processed
       * 
       * So this means that we should set a value related to each last version of the node that
       * we are processing
       * 
       */
      rval[i] = StringUtils.EMPTY;
      i++;
    }
    return rval;
  }
}

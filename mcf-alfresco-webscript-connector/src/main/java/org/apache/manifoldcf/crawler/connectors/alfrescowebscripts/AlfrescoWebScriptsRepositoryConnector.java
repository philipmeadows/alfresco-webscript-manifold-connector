/* $Id: AlfrescoRepositoryConnector.java 1299512 2012-03-12 00:58:38Z kwright $ */

/**

 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.manifoldcf.crawler.connectors.alfrescowebscripts;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.alfresco.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
import org.apache.manifoldcf.core.interfaces.ConfigParams;
import org.apache.manifoldcf.core.interfaces.IHTTPOutput;
import org.apache.manifoldcf.core.interfaces.IPostParameters;
import org.apache.manifoldcf.core.interfaces.IThreadContext;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.interfaces.SpecificationNode;
import org.apache.manifoldcf.crawler.connectors.BaseRepositoryConnector;
import org.apache.manifoldcf.crawler.interfaces.DocumentSpecification;
import org.apache.manifoldcf.crawler.interfaces.IProcessActivity;
import org.apache.manifoldcf.crawler.interfaces.ISeedingActivity;
import org.apache.manifoldcf.crawler.system.Logging;

public class AlfrescoWebScriptsRepositoryConnector extends BaseRepositoryConnector {

  /**
   * Fetch activity
   */
  public final static String ACTIVITY_FETCH = "fetch";

  /**
   * Username value for the Alfresco session
   */
  protected String username = null;

  /**
   * Password value for the Alfresco session
   */
  protected String password = null;

  /**
   * Endpoint protocol
   */
  protected String protocol = null;

  /**
   * Endpoint server name
   */
  protected String server = null;

  /**
   * Endpoint port
   */
  protected String port = null;

  /**
   * Endpoint context path of the Alfresco webapp
   */
  protected String path = null;

  protected static final String RELATIONSHIP_CHILD = "child";

  /**
   * Alfresco Server configuration tab name
   */
  private static final String ALFRESCO_SERVER_TAB_RESOURCE = "AlfrescoWebscriptConnector.Server";

  /**
   * Forward to the javascript to check the configuration parameters
   */
  private static final String EDIT_CONFIG_HEADER_FORWARD = "editConfiguration.js";

  /**
   * Forward to the HTML template to edit the configuration parameters
   */
  private static final String EDIT_CONFIG_FORWARD_SERVER = "editConfiguration_Server.html";

  /**
   * Forward to the javascript to check the specification parameters for the job
   */
  private static final String EDIT_SPEC_HEADER_FORWARD = "editSpecification.js";

  /**
   * Forward to the HTML template to view the configuration parameters
   */
  private static final String VIEW_CONFIG_FORWARD = "viewConfiguration.html";

  /**
   * The root node for the Alfresco connector configuration in ManifoldCF
   */
  private static final String JOB_STARTPOINT_NODE_TYPE = "startpoint";
  
  /** Read activity */
  protected final static String ACTIVITY_READ = "read document";

  private static AlfrescoIndexTracker alfrescoTracker;

  public AlfrescoWebScriptsRepositoryConnector() {
    super();
  }

  /**
   * Queue "seed" documents.  Seed documents are the starting places for crawling activity.  Documents
   * are seeded when this method calls appropriate methods in the passed in ISeedingActivity object.
   * <p/>
   * This method can choose to find repository changes that happen only during the specified time interval.
   * The seeds recorded by this method will be viewed by the framework based on what the
   * getConnectorModel() method returns.
   * <p/>
   * It is not a big problem if the connector chooses to create more seeds than are
   * strictly necessary; it is merely a question of overall work required.
   * <p/>
   * The times passed to this method may be interpreted for greatest efficiency.  The time ranges
   * any given job uses with this connector will not overlap, but will proceed starting at 0 and going
   * to the "current time", each time the job is run.  For continuous crawling jobs, this method will
   * be called once, when the job starts, and at various periodic intervals as the job executes.
   * <p/>
   * When a job's specification is changed, the framework automatically resets the seeding start time to 0.  The
   * seeding start time may also be set to 0 on each job run, depending on the connector model returned by
   * getConnectorModel().
   * <p/>
   * Note that it is always ok to send MORE documents rather than less to this method.
   *
   * @param activities is the interface this method should use to perform whatever framework actions are desired.
   * @param spec       is a document specification (that comes from the job).
   * @param startTime  is the beginning of the time range to consider, inclusive.
   * @param endTime    is the end of the time range to consider, exclusive.
   */
  @Override
  public void addSeedDocuments(ISeedingActivity activities,
                               DocumentSpecification spec, long startTime, long endTime)
      throws ManifoldCFException, ServiceInterruption {

    getSession();

    List<String> nodeIds = alfrescoTracker.getNodeIds();
    if (nodeIds != null) {
      for (String nodeId : nodeIds) {
        Logging.connectors.info("Indexing node: " + nodeId);
        activities.addSeedDocument(nodeId);
      }
    }
  }

  
  
  /**
   * Process a set of documents.
   * This is the method that should cause each document to be fetched, processed, and the results either added
   * to the queue of documents for the current job, and/or entered into the incremental ingestion manager.
   * The document specification allows this class to filter what is done based on the job.
   *
   * @param documentIdentifiers is the set of document identifiers to process.
   * @param versions            is the corresponding document versions to process, as returned by getDocumentVersions() above.
   *                            The implementation may choose to ignore this parameter and always process the current version.
   * @param activities          is the interface this method should use to queue up new document references
   *                            and ingest documents.
   * @param spec                is the document specification.
   * @param scanOnly            is an array corresponding to the document identifiers.  It is set to true to indicate when the processing
   *                            should only find other references, and should not actually call the ingestion methods.
   */
  @Override
  public void processDocuments(String[] documentIdentifiers, String[] versions,
                               IProcessActivity activities, DocumentSpecification spec,
                               boolean[] scanOnly) throws ManifoldCFException, ServiceInterruption {

    Logging.connectors.debug("Alfresco: Inside processDocuments");
    int i = 0;
    while (i < documentIdentifiers.length) {
      long startTime = System.currentTimeMillis();
      Long nodeId = Long.valueOf(documentIdentifiers[i]);
      Pair<RepositoryDocument, String> rd = alfrescoTracker.processMetaData(nodeId);
      String documentURI = StringUtils.EMPTY;
      if(rd!=null){
        documentURI = nodeId + "|" + rd.getSecond();
        activities.ingestDocument(String.valueOf(nodeId), rd.getSecond(), documentURI, rd.getFirst());
      }
      
//      activities.recordActivity(new Long(startTime), ACTIVITY_READ,
//          fileLength, nodeId, errorCode, errorDesc, null);
      
      i++;
    }
  }

  /**
   * The short version of getDocumentVersions.
   * Get document versions given an array of document identifiers.
   * This method is called for EVERY document that is considered. It is
   * therefore important to perform as little work as possible here.
   *
   * @param documentIdentifiers is the array of local document identifiers, as understood by this connector.
   * @param spec                is the current document specification for the current job.  If there is a dependency on this
   *                            specification, then the version string should include the pertinent data, so that reingestion will occur
   *                            when the specification changes.  This is primarily useful for metadata.
   * @return the corresponding version strings, with null in the places where the document no longer exists.
   *         Empty version strings indicate that there is no versioning ability for the corresponding document, and the document
   *         will always be processed.
   */
  @Override
  public String[] getDocumentVersions(String[] documentIdentifiers,
                                      DocumentSpecification spec) throws ManifoldCFException,
      ServiceInterruption {
    return alfrescoTracker.getDocumentVersions(documentIdentifiers, spec);
  }
  
  /**
   * Connect.
   *
   * @param configParams is the set of configuration parameters, which
   *                     in this case describe the target appliance, basic auth configuration, etc.  (This formerly came
   *                     out of the ini file.)
   */
  @Override
  public void connect(ConfigParams configParams) {
    super.connect(configParams);
    username = params.getParameter(AlfrescoConfig.USERNAME_PARAM);
    password = params.getParameter(AlfrescoConfig.PASSWORD_PARAM);
    protocol = params.getParameter(AlfrescoConfig.PROTOCOL_PARAM);
    server = params.getParameter(AlfrescoConfig.SERVER_PARAM);
    port = params.getParameter(AlfrescoConfig.PORT_PARAM);
    path = params.getParameter(AlfrescoConfig.PATH_PARAM);
    alfrescoTracker = new AlfrescoIndexTracker(".", username, password, protocol, server, port, path);
    
  }

  /**
   * Close the connection.  Call this before discarding the connection.
   */
  @Override
  public void disconnect() throws ManifoldCFException {
    username = null;
    password = null;
    protocol = null;
    server = null;
    port = null;
    path = null;
  }

  /**
   * Test the connection.  Returns a string describing the connection integrity.
   *
   * @return the connection's status as a displayable string.
   */
  @Override
  public String check() throws ManifoldCFException {
    try {
      getSession();
      return super.check();
    } catch (ManifoldCFException e) {
      return "Connection failed: " + e.getMessage();
    }
  }

  /**
   * Return the list of activities that this connector supports (i.e. writes into the log).
   *
   * @return the list.
   */
  @Override
  public String[] getActivitiesList() {
    return new String[]{ACTIVITY_FETCH};
  }

  /**
   * Get the bin name strings for a document identifier.  The bin name describes the queue to which the
   * document will be assigned for throttling purposes.  Throttling controls the rate at which items in a
   * given queue are fetched; it does not say anything about the overall fetch rate, which may operate on
   * multiple queues or bins.
   * For example, if you implement a web crawler, a good choice of bin name would be the server name, since
   * that is likely to correspond to a real resource that will need real throttle protection.
   *
   * @param documentIdentifier is the document identifier.
   * @return the set of bin names.  If an empty array is returned, it is equivalent to there being no request
   *         rate throttling available for this identifier.
   */
  @Override
  public String[] getBinNames(String documentIdentifier) {
    return new String[]{server};
  }

  /**
   * Set up a session
   */
  protected void getSession() throws ManifoldCFException {
    if (alfrescoTracker == null) {
      // Check for parameter validity

      if (StringUtils.isEmpty(username))
        throw new ManifoldCFException("Parameter " + AlfrescoConfig.USERNAME_PARAM
            + " required but not set");

      if (Logging.connectors.isDebugEnabled())
        Logging.connectors.debug("Alfresco: Username = '" + username + "'");

      if (StringUtils.isEmpty(password))
        throw new ManifoldCFException("Parameter " + AlfrescoConfig.PASSWORD_PARAM
            + " required but not set");

      Logging.connectors.debug("Alfresco: Password exists");

      if (StringUtils.isEmpty(protocol))
        throw new ManifoldCFException("Parameter " + AlfrescoConfig.PROTOCOL_PARAM
            + " required but not set");

      if (StringUtils.isEmpty(server))
        throw new ManifoldCFException("Parameter " + AlfrescoConfig.SERVER_PARAM
            + " required but not set");

      if (StringUtils.isEmpty(port))
        throw new ManifoldCFException("Parameter " + AlfrescoConfig.PORT_PARAM
            + " required but not set");

      if (StringUtils.isEmpty(path))
        throw new ManifoldCFException("Parameter " + AlfrescoConfig.PATH_PARAM
            + " required but not set");

    }
    
    alfrescoTracker.init();
    
    
  }

  /**
   * This method is periodically called for all connectors that are connected but not
   * in active use.
   */
  @Override
  public void poll() throws ManifoldCFException {
  }


  /**
   * Get the maximum number of documents to amalgamate together into one batch, for this connector.
   *
   * @return the maximum number. 0 indicates "unlimited".
   */
  @Override
  public int getMaxDocumentRequest() {
    return AlfrescoIndexTracker.RESULT_BATCH_SIZE;
  }

  /**
   * Return the list of relationship types that this connector recognizes.
   *
   * @return the list.
   */
  @Override
  public String[] getRelationshipTypes() {
    return new String[]{RELATIONSHIP_CHILD};
  }

  /**
   * Read the content of a resource, replace the variable ${PARAMNAME} with the
   * value and copy it to the out.
   *
   * @param resName
   * @param out
   * @throws ManifoldCFException
   */
  private static void outputResource(String resName, IHTTPOutput out,
                                     Locale locale, Map<String, String> paramMap) throws ManifoldCFException {
    Messages.outputResourceWithVelocity(out, locale, resName, paramMap, true);
  }

  /**
   * Fill in Velocity parameters for the Server tab.
   */
  private static void fillInServerParameters(Map<String, String> paramMap, ConfigParams parameters) {
    String username = parameters.getParameter(AlfrescoConfig.USERNAME_PARAM);
    if (username == null)
      username = AlfrescoConfig.USERNAME_DEFAULT_VALUE;
    paramMap.put(AlfrescoConfig.USERNAME_PARAM, username);

    String password = parameters.getParameter(AlfrescoConfig.PASSWORD_PARAM);
    if (password == null)
      password = AlfrescoConfig.PASSWORD_DEFAULT_VALUE;
    paramMap.put(AlfrescoConfig.PASSWORD_PARAM, password);

    String protocol = parameters.getParameter(AlfrescoConfig.PROTOCOL_PARAM);
    if (protocol == null)
      protocol = AlfrescoConfig.PROTOCOL_DEFAULT_VALUE;
    paramMap.put(AlfrescoConfig.PROTOCOL_PARAM, protocol);

    String server = parameters.getParameter(AlfrescoConfig.SERVER_PARAM);
    if (server == null)
      server = AlfrescoConfig.SERVER_DEFAULT_VALUE;
    paramMap.put(AlfrescoConfig.SERVER_PARAM, server);

    String port = parameters.getParameter(AlfrescoConfig.PORT_PARAM);
    if (port == null)
      port = AlfrescoConfig.PORT_DEFAULT_VALUE;
    paramMap.put(AlfrescoConfig.PORT_PARAM, port);

    String path = parameters.getParameter(AlfrescoConfig.PATH_PARAM);
    if (path == null)
      path = AlfrescoConfig.PATH_DEFAULT_VALUE;
    paramMap.put(AlfrescoConfig.PATH_PARAM, path);

    String tenantDomain = parameters.getParameter(AlfrescoConfig.TENANT_DOMAIN_PARAM);
    if (tenantDomain == null)
      tenantDomain = "";
    paramMap.put(AlfrescoConfig.TENANT_DOMAIN_PARAM, tenantDomain);
  }

  /**
   * View configuration. This method is called in the body section of the
   * connector's view configuration page. Its purpose is to present the
   * connection information to the user. The coder can presume that the HTML
   * that is output from this configuration will be within appropriate <html>
   * and <body> tags.
   *
   * @param threadContext is the local thread context.
   * @param out           is the output to which any HTML should be sent.
   * @param parameters    are the configuration parameters, as they currently exist, for
   *                      this connection being configured.
   */
  @Override
  public void viewConfiguration(IThreadContext threadContext, IHTTPOutput out,
                                Locale locale, ConfigParams parameters) throws ManifoldCFException, IOException {

    Map<String, String> paramMap = new HashMap<String, String>();

    // Server tab
    fillInServerParameters(paramMap, parameters);

    outputResource(VIEW_CONFIG_FORWARD, out, locale, paramMap);
  }

  /**
   * Output the configuration header section. This method is called in the head
   * section of the connector's configuration page. Its purpose is to add the
   * required tabs to the list, and to output any javascript methods that might
   * be needed by the configuration editing HTML.
   *
   * @param threadContext is the local thread context.
   * @param out           is the output to which any HTML should be sent.
   * @param parameters    are the configuration parameters, as they currently exist, for
   *                      this connection being configured.
   * @param tabsArray     is an array of tab names. Add to this array any tab names that are
   *                      specific to the connector.
   */
  @Override
  public void outputConfigurationHeader(IThreadContext threadContext,
                                        IHTTPOutput out, Locale locale, ConfigParams parameters, List<String> tabsArray)
      throws ManifoldCFException, IOException {
    // Add Server tab
    tabsArray.add(Messages.getString(locale, ALFRESCO_SERVER_TAB_RESOURCE));

    Map<String, String> paramMap = new HashMap<String, String>();

    // Fill in parameters for all tabs
    fillInServerParameters(paramMap, parameters);

    outputResource(EDIT_CONFIG_HEADER_FORWARD, out, locale, paramMap);
  }

  /**
   * Output the configuration body section.
   * This method is called in the body section of the connector's configuration page.  Its purpose is to present the required form elements for editing.
   * The coder can presume that the HTML that is output from this configuration will be within appropriate <html>, <body>, and <form> tags.  The name of the
   * form is "editconnection".
   *
   * @param threadContext is the local thread context.
   * @param out           is the output to which any HTML should be sent.
   * @param parameters    are the configuration parameters, as they currently exist, for this connection being configured.
   * @param tabName       is the current tab name.
   */
  @Override
  public void outputConfigurationBody(IThreadContext threadContext,
                                      IHTTPOutput out, Locale locale, ConfigParams parameters, String tabName)
      throws ManifoldCFException, IOException {

    // Do the Server tab
    Map<String, String> paramMap = new HashMap<String, String>();
    paramMap.put("TabName", tabName);
    fillInServerParameters(paramMap, parameters);
    outputResource(EDIT_CONFIG_FORWARD_SERVER, out, locale, paramMap);
  }

  /**
   * Process a configuration post. This method is called at the start of the
   * connector's configuration page, whenever there is a possibility that form
   * data for a connection has been posted. Its purpose is to gather form
   * information and modify the configuration parameters accordingly. The name
   * of the posted form is "editconnection".
   *
   * @param threadContext   is the local thread context.
   * @param variableContext is the set of variables available from the post, including binary
   *                        file post information.
   * @param parameters      are the configuration parameters, as they currently exist, for
   *                        this connection being configured.
   * @return null if all is well, or a string error message if there is an error
   *         that should prevent saving of the connection (and cause a
   *         redirection to an error page).
   */
  @Override
  public String processConfigurationPost(IThreadContext threadContext,
                                         IPostParameters variableContext, Locale locale, ConfigParams parameters)
      throws ManifoldCFException {

    String username = variableContext.getParameter(AlfrescoConfig.USERNAME_PARAM);
    if (username != null) {
      parameters.setParameter(AlfrescoConfig.USERNAME_PARAM, username);
    }

    String password = variableContext.getParameter(AlfrescoConfig.PASSWORD_PARAM);
    if (password != null) {
      parameters.setParameter(AlfrescoConfig.PASSWORD_PARAM, password);
    }

    String protocol = variableContext.getParameter(AlfrescoConfig.PROTOCOL_PARAM);
    if (protocol != null) {
      parameters.setParameter(AlfrescoConfig.PROTOCOL_PARAM, protocol);
    }

    String server = variableContext.getParameter(AlfrescoConfig.SERVER_PARAM);
    if (server != null) {
      parameters.setParameter(AlfrescoConfig.SERVER_PARAM, server);
    }

    String port = variableContext.getParameter(AlfrescoConfig.PORT_PARAM);
    if (port != null) {
      parameters.setParameter(AlfrescoConfig.PORT_PARAM, port);
    }

    String path = variableContext.getParameter(AlfrescoConfig.PATH_PARAM);
    if (path != null) {
      parameters.setParameter(AlfrescoConfig.PATH_PARAM, path);
    }

    String tenantDomain = variableContext.getParameter(AlfrescoConfig.TENANT_DOMAIN_PARAM);
    if (tenantDomain != null) {
      parameters.setParameter(AlfrescoConfig.TENANT_DOMAIN_PARAM, tenantDomain);
    }

    return null;
  }

  /**
   * Process a specification post. This method is called at the start of job's
   * edit or view page, whenever there is a possibility that form data for a
   * connection has been posted. Its purpose is to gather form information and
   * modify the document specification accordingly. The name of the posted form
   * is "editjob".
   *
   * @param variableContext contains the post data, including binary file-upload information.
   * @param ds              is the current document specification for this job.
   * @return null if all is well, or a string error message if there is an error
   *         that should prevent saving of the job (and cause a redirection to
   *         an error page).
   */
  @Override
  public String processSpecificationPost(IPostParameters variableContext,
                                         Locale locale, DocumentSpecification ds) throws ManifoldCFException {
    String luceneQuery = variableContext.getParameter(AlfrescoConfig.LUCENE_QUERY_PARAM);
    if (luceneQuery != null) {
      int i = 0;
      while (i < ds.getChildCount()) {
        SpecificationNode oldNode = ds.getChild(i);
        if (oldNode.getType().equals(JOB_STARTPOINT_NODE_TYPE)) {
          ds.removeChild(i);
        } else
          i++;
      }
      SpecificationNode node = new SpecificationNode(JOB_STARTPOINT_NODE_TYPE);
      node.setAttribute(AlfrescoConfig.LUCENE_QUERY_PARAM, luceneQuery);
      ds.addChild(ds.getChildCount(), node);
    }

    return null;
  }

  /**
   * Output the specification header section. This method is called in the head
   * section of a job page which has selected a repository connection of the
   * current type. Its purpose is to add the required tabs to the list, and to
   * output any javascript methods that might be needed by the job editing HTML.
   *
   * @param out       is the output to which any HTML should be sent.
   * @param ds        is the current document specification for this job.
   * @param tabsArray is an array of tab names. Add to this array any tab names that are
   *                  specific to the connector.
   */
  @Override
  public void outputSpecificationHeader(IHTTPOutput out,
                                        Locale locale, DocumentSpecification ds, List<String> tabsArray)
      throws ManifoldCFException, IOException {
    // Fill in parameters from all tabs
    Map<String, String> paramMap = new HashMap<String, String>();
    outputResource(EDIT_SPEC_HEADER_FORWARD, out, locale, paramMap);
  }
}

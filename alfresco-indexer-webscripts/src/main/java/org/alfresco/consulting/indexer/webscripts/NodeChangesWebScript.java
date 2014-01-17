package org.alfresco.consulting.indexer.webscripts;

import org.alfresco.consulting.indexer.dao.IndexingDaoImpl;
import org.alfresco.consulting.indexer.entities.NodeEntity;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.TemplateHashModel;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.domain.node.NodeDAO;
import org.alfresco.repo.domain.qname.QNameDAO;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.*;

import java.util.*;

/**
 * Renders out a list of nodes (UUIDs) that have been changed in Alfresco; the changes can affect:
 * - A node metadata
 * - Node content
 * - Node ACLs
 *
 * Please check src/main/amp/config/alfresco/extension/templates/webscripts/com/findwise/alfresco/changes.get.desc.xml
 * to know more about the RestFul interface to invoke the WebScript
 *
 * List of pending activities (or TODOs)
 * - Move private/static logic into the IndexingService
 * - Using JSON libraries (or StringBuffer), render out the payload without passing through FreeMarker template
 * - Wrap (or Proxy) IndexingDaoImpl into an IndexingService, which (optionally) performs any object manipulation
 */
public class NodeChangesWebScript extends DeclarativeWebScript {

  protected static final Log logger = LogFactory.getLog(NodeChangesWebScript.class);

  @Override
  protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {

    //Fetching request params
    Map<String, String> templateArgs = req.getServiceMatch().getTemplateVars();
    String storeId = templateArgs.get("storeId");
    String storeProtocol = templateArgs.get("storeProtocol");
    String lastTxnIdString = req.getParameter("lastTxnId");
    String lastAclChangesetIdString = req.getParameter("lastAclChangesetId");
    String maxTxnsString = req.getParameter("maxTxns");
    String maxAclChangesetsString = req.getParameter("maxAclChangesets");

    //Parsing parameters passed from the WebScript invocation
    Long lastTxnId = (lastTxnIdString == null ? null : Long.valueOf(lastTxnIdString));
    Long lastAclChangesetId = (lastAclChangesetIdString == null ? null : Long.valueOf(lastAclChangesetIdString));
    Integer maxTxns = (maxTxnsString == null ? maxNodesPerTxns : Integer.valueOf(maxTxnsString));
    Integer maxAclChangesets = (maxAclChangesetsString == null ? maxNodesPerAcl : Integer.valueOf(maxAclChangesetsString));

    logger.debug(String.format("Invoking Changes Webscript, using the following params\n" +
        "lastTxnId: %s\n" +
        "lastAclChangesetId: %s\n" +
        "storeId: %s\n" +
        "storeProtocol: %s\n", lastTxnId, lastAclChangesetId, storeId, storeProtocol));

    //Getting the Store ID on which the changes are requested
    Long storeLongId = nodeDao.getStore(new StoreRef(storeProtocol, storeId)).getFirst();

    Set<NodeEntity> nodes = new HashSet<NodeEntity>();
    //Updating the last IDs being processed
    //Depending on params passed to the request, results will be rendered out
    if (lastTxnId == null) {
      lastTxnId = new Long(0);
    }
    List<NodeEntity> nodesFromTxns = indexingService.getNodesByTransactionId(storeLongId, lastTxnId, maxTxns);
    if (nodesFromTxns != null && nodesFromTxns.size() > 0) {
      nodes.addAll(nodesFromTxns);
      lastTxnId = nodesFromTxns.get(nodesFromTxns.size()-1).getTransactionId();
    }

    if (lastAclChangesetId == null) {
      lastAclChangesetId = new Long(0);
    }
    List<NodeEntity> nodesFromAcls = indexingService.getNodesByAclChangesetId(storeLongId, lastAclChangesetId, maxAclChangesets);
    if (nodesFromAcls != null && nodesFromAcls.size() > 0) {
      nodes.addAll(nodesFromAcls);
      lastAclChangesetId = nodesFromAcls.get(nodesFromAcls.size()-1).getAclChangesetId();
    }

    //Render them out
    Map<String, Object> model = new HashMap<String, Object>(1, 1.0f);
    model.put("qnameDao", qnameDao);
    model.put("nsResolver", namespaceService);
    model.put("nodes", nodes);
    model.put("lastTxnId", lastTxnId);
    model.put("lastAclChangesetId", lastAclChangesetId);
    model.put("storeId", storeId);
    model.put("storeProtocol", storeProtocol);
    model.put("propertiesUrlTemplate", propertiesUrlTemplate);

    //This allows to call the static method QName.createQName from the FTL template
    try {
      BeansWrapper wrapper = BeansWrapper.getDefaultInstance();
      TemplateHashModel staticModels = wrapper.getStaticModels();
      TemplateHashModel qnameStatics = (TemplateHashModel) staticModels.get("org.alfresco.service.namespace.QName");
      model.put("QName",qnameStatics);
    } catch (Exception e) {
      throw new AlfrescoRuntimeException(
          "Cannot add BeansWrapper for static QName.createQName method to be used from a Freemarker template", e);
    }

    logger.debug(String.format("Attaching %s nodes to the WebScript template", nodes.size()));

    return model;
  }

  private NamespaceService namespaceService;
  private QNameDAO qnameDao;
  private IndexingDaoImpl indexingService;
  private NodeDAO nodeDao;

  private String propertiesUrlTemplate;
  private int maxNodesPerAcl = 1000;
  private int maxNodesPerTxns = 1000;


  public void setNamespaceService(NamespaceService namespaceService) {
    this.namespaceService = namespaceService;
  }
  public void setQnameDao(QNameDAO qnameDao) {
    this.qnameDao = qnameDao;
  }
  public void setIndexingService(IndexingDaoImpl indexingService) {
    this.indexingService = indexingService;
  }
  public void setNodeDao(NodeDAO nodeDao) {
    this.nodeDao = nodeDao;
  }

  public void setPropertiesUrlTemplate(String propertiesUrlTemplate) {
    this.propertiesUrlTemplate = propertiesUrlTemplate;
  }

  public void setMaxNodesPerAcl(int maxNodesPerAcl) {
    this.maxNodesPerAcl = maxNodesPerAcl;
  }

  public void setMaxNodesPerTxns(int maxNodesPerTxns) {
    this.maxNodesPerTxns = maxNodesPerTxns;
  }
}
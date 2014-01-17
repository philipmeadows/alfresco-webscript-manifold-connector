package org.alfresco.consulting.indexer.webscripts;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.web.scripts.BaseWebScriptTest;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceService;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.springframework.extensions.webscripts.TestWebScriptServer;
import org.springframework.extensions.webscripts.TestWebScriptServer.Response;

import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.util.ApplicationContextHelper;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

public class NodeWebScriptTest extends BaseWebScriptTest {

  private static final String STORE_PROTOCOL = "workspace";
  private static final String STORE_ID = "SpacesStore";

  private static Logger log = Logger.getLogger(NodeWebScriptTest.class);
  private static final String ADMIN_USER_NAME = "admin";

  protected static ApplicationContext applicationContext;
  protected static NodeService nodeService;
  protected static NamespaceService namespaceService;

  @BeforeClass
  public void setUp() throws Exception {
    ApplicationContextHelper.setUseLazyLoading(false);
    ApplicationContextHelper.setNoAutoStart(true);
    applicationContext = ApplicationContextHelper.getApplicationContext(new String[]{"classpath:alfresco/application-context.xml"});
    nodeService = (NodeService) applicationContext.getBean("nodeService");
    namespaceService = (NamespaceService) applicationContext.getBean("namespaceService");
    AuthenticationUtil.setFullyAuthenticatedUser(ADMIN_USER_NAME);
  }

  @Test
  public void testNodeChangesAndDetails() throws Exception {
    String changesUrl = String.format("/node/changes/%s/%s",
        STORE_PROTOCOL,
        STORE_ID);

    //Get (and assert) all node changes
    Response response = sendRequest(new TestWebScriptServer.GetRequest(changesUrl),200);
    JSONObject result = new JSONObject(response.getContentAsString());
    assertNodeChanges(result);

    //Find the uuid of a cm:content, not being deleted and that is part of an Alfresco Share site
    String uuid = null;
    JSONArray docs = result.getJSONArray("docs");
    for(int i=0; i < docs.length()-1; i++) {
      JSONObject doc = docs.getJSONObject(i);
      String type = doc.get("type").toString();
      String deleted = doc.get("deleted").toString();
      uuid = doc.get("uuid").toString();
      if (type.equals("cm:content") && deleted.equals("false")) {
        NodeRef nodeRef = new NodeRef(STORE_PROTOCOL,STORE_ID,uuid);
        if (nodeService.exists(nodeRef)) {
          String nodePath = nodeService.getPath(nodeRef).toPrefixString(namespaceService);
          if (nodePath.contains("sites")) {
            break;
          }
        }
      }
    }

    //Get (and assert) the uuid details
    String detailsUrl = String.format("/node/details/%s/%s/%s",
        STORE_PROTOCOL,
        STORE_ID,
        uuid);
    response = sendRequest(new TestWebScriptServer.GetRequest(detailsUrl),200);
    result = new JSONObject(response.getContentAsString());
    assertNodeDetails(result,uuid);
  }

  public void assertNodeChanges(JSONObject result) throws Exception {
    assertNotNull(result);

    String storeId = result.get("store_id").toString();
    assertEquals("SpacesStore",storeId);

    String storeProtocol = result.get("store_protocol").toString();
    assertEquals("workspace",storeProtocol);

    Integer lastTxn = new Integer(result.get("last_txn_id").toString());
    assertTrue(lastTxn > 0);

    Integer lastAcl = new Integer(result.get("last_acl_changeset_id").toString());
    assertTrue(lastAcl > 0);

    JSONArray docs = result.getJSONArray("docs");
    for(int i=0; i < docs.length()-1; i++) {
      JSONObject doc = docs.getJSONObject(i);
      String type = doc.get("type").toString();
      assertNotNull(type);
      String uuid = doc.get("uuid").toString();
      assertNotNull(uuid);
      String propertiesUrl = doc.get("propertiesUrl").toString();
      assertNotNull(propertiesUrl);
      assertTrue(propertiesUrl.contains(uuid));
      String deleted = doc.get("deleted").toString();
      assertNotNull(new Boolean(deleted));
    }
  }

  public void assertNodeDetails(JSONObject result, String uuid) throws Exception {
    assertNotNull(result);

    JSONArray authorities = result.getJSONArray("readableAuthorities");
    assertNotSame(authorities.length(), 0);
    String path = result.get("path").toString();
    assertNotNull(path);
    String shareUrlPath = result.get("shareUrlPath").toString();
    assertTrue(shareUrlPath.contains(uuid));
    assertTrue(shareUrlPath.contains("http"));
    String contentUrlPath = result.get("contentUrlPath").toString();
    assertTrue(contentUrlPath.contains(uuid));
    assertTrue(contentUrlPath.contains("http"));
    JSONArray aspects = result.getJSONArray("aspects");
    assertTrue(aspects.length() > 0);
    JSONArray properties = result.getJSONArray("properties");
    assertTrue(properties.length() > 0);
    for(int i=0; i < properties.length()-1; i++) {
      JSONObject prop = properties.getJSONObject(i);
      String name = prop.get("name").toString();
      assertNotNull(name);
      String type = prop.get("type").toString();
      assertNotNull(type);
      String value = prop.get("value").toString();
      assertNotNull(value);
    }
  }
}
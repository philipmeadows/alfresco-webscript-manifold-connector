package org.alfresco.consulting.indexer.webscripts;

import org.alfresco.repo.web.scripts.BaseWebScriptTest;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.util.ApplicationContextHelper;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.extensions.webscripts.TestWebScriptServer;
import org.springframework.extensions.webscripts.TestWebScriptServer.Response;

public class AuthorityResolverWebScriptTest extends BaseWebScriptTest {

  private static Logger log = Logger.getLogger(AuthorityResolverWebScriptTest.class);

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
  }

  @Test
  public void testAuth() throws Exception {
    setDefaultRunAs("admin");
    Response response = sendRequest(new TestWebScriptServer.GetRequest("/auth/resolve/admin"),200);
    JSONArray resultList = new JSONArray(response.getContentAsString());
    assertAdminAuthResolve(resultList);

    response = sendRequest(new TestWebScriptServer.GetRequest("/auth/resolve/"),200);
    resultList = new JSONArray(response.getContentAsString());
    assertAdminAuthResolve(resultList);
  }

  private void assertAdminAuthResolve(JSONArray resultList) throws Exception {
    for(int j=0; j < resultList.length()-1; j++) {
      JSONObject result = resultList.getJSONObject(j);
      String username = result.get("username").toString();
      assertNotNull(username);
      JSONArray auths = result.getJSONArray("authorities");
      assertTrue(auths.length() > 0);
      if (!username.equals("guest")) {
        boolean everyoneGroupFound = false;
        for(int i=0; i < auths.length()-1; i++) {
          String auth = auths.get(i).toString();
          if (auth.equals("GROUP_EVERYONE")) {
            everyoneGroupFound = true;
            break;
          }
        }
        assertTrue(everyoneGroupFound);
      }
    }
  }
}
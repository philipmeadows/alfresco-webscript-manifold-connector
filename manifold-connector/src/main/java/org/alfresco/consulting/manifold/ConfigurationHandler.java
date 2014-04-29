package org.alfresco.consulting.manifold;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.manifoldcf.core.i18n.Messages;
import org.apache.manifoldcf.core.interfaces.ConfigParams;
import org.apache.manifoldcf.core.interfaces.IHTTPOutput;
import org.apache.manifoldcf.core.interfaces.IPostParameters;
import org.apache.manifoldcf.core.interfaces.IThreadContext;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

public class ConfigurationHandler {
  private static final String PARAM_PROTOCOL = "protocol";
  private static final String PARAM_HOSTNAME = "hostname";
  private static final String PARAM_ENDPOINT = "endpoint";
  private static final String PARAM_STORE_PROTOCOL = "storeprotocol";
  private static final String PARAM_ENABLE_DOCUMENT_PROCESSING = "enabledocumentprocessing";
  private static final String PARAM_STORE_ID = "storeid";
  private static final String PARAM_USERNAME = "username";
  private static final String PARAM_PASSWORD = "password";

  private static final String EDIT_CONFIG_HEADER = "editConfiguration.js";
  private static final String EDIT_CONFIG_SERVER = "editConfiguration_Server.html";
  private static final String VIEW_CONFIG = "viewConfiguration.html";

  private static final Map<String, String> DEFAULT_CONFIGURATION_PARAMETERS = new HashMap<String, String>();
  static {
    DEFAULT_CONFIGURATION_PARAMETERS.put(PARAM_PROTOCOL, "http");
    DEFAULT_CONFIGURATION_PARAMETERS.put(PARAM_HOSTNAME, "localhost");
    DEFAULT_CONFIGURATION_PARAMETERS.put(PARAM_ENDPOINT, "/alfresco/service");
    DEFAULT_CONFIGURATION_PARAMETERS.put(PARAM_STORE_PROTOCOL, "workspace");
    DEFAULT_CONFIGURATION_PARAMETERS.put(PARAM_ENABLE_DOCUMENT_PROCESSING,"true");
    DEFAULT_CONFIGURATION_PARAMETERS.put(PARAM_STORE_ID, "SpacesStore");
    DEFAULT_CONFIGURATION_PARAMETERS.put(PARAM_USERNAME, "");
    DEFAULT_CONFIGURATION_PARAMETERS.put(PARAM_PASSWORD, "");
  }

  private ConfigurationHandler() {
  }

  public static void outputConfigurationHeader(IThreadContext threadContext,
      IHTTPOutput out, Locale locale, ConfigParams parameters,
      List<String> tabsArray) throws ManifoldCFException, IOException {
    tabsArray.add("Server");
    InputStream inputStream = ConfigurationHandler.class.getResourceAsStream("/org/alfresco/consulting/manifold/" + EDIT_CONFIG_HEADER);
    StringWriter writer = new StringWriter();
    IOUtils.copy(inputStream, writer, "UTF-8");
    inputStream.close();
    out.print(writer.toString());
  }

  private static void fillInParameters(Map<String, String> paramMap,
      ConfigParams parameters) {
    for (Map.Entry<String, String> parameter : DEFAULT_CONFIGURATION_PARAMETERS
        .entrySet()) {
      String paramValue = parameters.getParameter(parameter.getKey());
      if (paramValue == null) {
        paramValue = parameter.getValue();
      }
      paramMap.put(parameter.getKey(), paramValue);
    }
  }

  public static void outputConfigurationBody(IThreadContext threadContext,
      IHTTPOutput out, Locale locale, ConfigParams parameters, String tabName)
      throws ManifoldCFException, IOException {
    Map<String, String> paramMap = new HashMap<String, String>();
    paramMap.put("tabName", tabName);
    fillInParameters(paramMap, parameters);
    VelocityEngine velocityEngine = Messages.createVelocityEngine(ConfigurationHandler.class);
    VelocityContext context = createVelocityContext(paramMap);
    StringWriter w = new StringWriter();
    velocityEngine.mergeTemplate(EDIT_CONFIG_SERVER, "UTF-8", context, w);
    out.print(w.toString());
  }

  private static VelocityContext createVelocityContext(Map<String, String> paramMap) {
    VelocityContext context = new VelocityContext();
    for (Map.Entry<String, String> entry : paramMap.entrySet()) {
      context.put(entry.getKey(), entry.getValue());
    }
    return context;
  }

  public static String processConfigurationPost(IThreadContext threadContext,
      IPostParameters variableContext, Locale locale, ConfigParams parameters)
      throws ManifoldCFException {
    for (String paramName : DEFAULT_CONFIGURATION_PARAMETERS.keySet()) {
      String paramValue = variableContext.getParameter(paramName);
      if (paramValue != null) {
        parameters.setParameter(paramName, paramValue);
      }
    }
    return null;
  }

  public static void viewConfiguration(IThreadContext threadContext,
      IHTTPOutput out, Locale locale, ConfigParams parameters)
      throws ManifoldCFException, IOException {
    Map<String, String> paramMap = new HashMap<String, String>();
    fillInParameters(paramMap, parameters);
    VelocityEngine velocityEngine = Messages.createVelocityEngine(ConfigurationHandler.class);
    VelocityContext context = createVelocityContext(paramMap);
    StringWriter w = new StringWriter();
    velocityEngine.mergeTemplate(VIEW_CONFIG, "UTF-8", context, w);
    out.print(w.toString());
  }
}

package org.alfresco.consulting.manifold;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collection;
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
import org.apache.manifoldcf.core.interfaces.Specification;
import org.apache.manifoldcf.core.interfaces.SpecificationNode;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMultimap;

public class ConfigurationHandler {
  private static final String PARAM_PROTOCOL = "protocol";
  private static final String PARAM_HOSTNAME = "hostname";
  private static final String PARAM_ENDPOINT = "endpoint";
  private static final String PARAM_STORE_PROTOCOL = "storeprotocol";
  private static final String PARAM_ENABLE_DOCUMENT_PROCESSING = "enabledocumentprocessing";
  private static final String PARAM_STORE_ID = "storeid";
  private static final String PARAM_USERNAME = "username";
  private static final String PARAM_PASSWORD = "password";
  
  // Output Specification for Filtering
  /** Node describing a Site */
  public static final String NODE_SITE = "site";
  /** Attribute describing a site name */
  public static final String ATTRIBUTE_SITE = "site_name";
  
  /** Node describing a MimeType */
  public static final String NODE_MIMETYPE = "mimetype";
  /** Attribute describing a MimeType name */
  public static final String ATTRIBUTE_MIMETYPE = "mimetype_name";
  
  /** Node describing an Aspect */
  public static final String NODE_ASPECT = "aspect";
  /** Attribute describing an aspect name */
  public static final String ATTRIBUTE_ASPECT_SOURCE = "aspect_source";
  /** Attribute describing an aspect value */
  public static final String ATTRIBUTE_ASPECT_TARGET = "aspect_value";
  
  /** Node describing a Metadata */
  public static final String NODE_METADATA = "metadata";
  /** Attribute describing an aspect name */
  public static final String ATTRIBUTE_METADATA_SOURCE = "metadata_source";
  /** Attribute describing an aspect value */
  public static final String ATTRIBUTE_METADATA_TARGET = "metadata_value";
  
  public static final ImmutableMultimap<String, String> SPECIFICATION_MAP =
	        ImmutableMultimap.<String, String>builder().
	        put(NODE_SITE, ATTRIBUTE_SITE).
	        put(NODE_MIMETYPE, ATTRIBUTE_MIMETYPE).
	        put(NODE_ASPECT, ATTRIBUTE_ASPECT_SOURCE).
	        put(NODE_ASPECT, ATTRIBUTE_ASPECT_TARGET).
	        put(NODE_METADATA, ATTRIBUTE_METADATA_SOURCE).
	        put(NODE_METADATA, ATTRIBUTE_METADATA_TARGET).build();

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
  
  private static final Logger logger = LoggerFactory.getLogger(ConfigurationHandler.class);

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
  
  public static void outputSpecificationHeader(IHTTPOutput out, Locale locale, Specification os,
    int connectionSequenceNumber, List<String> tabsArray)
    throws ManifoldCFException, IOException
  {
    String seqPrefix = "s"+connectionSequenceNumber+"_";
    tabsArray.add("Alfresco Filtering Configuration");
    
    out.print(
    		"<script type=\"text/javascript\">\n"+
    		"<!--\n"+
    		"function "+seqPrefix+"checkSpecification()\n"+
    		"{\n"+
    		"  return true;\n"+
    		"}\n"+
    		"\n");
    
    for(String node:SPECIFICATION_MAP.keySet()){
    	out.print(
    			"function "+seqPrefix+"add"+node+"()\n"+
    			"{\n");
    	Collection<String> vars = SPECIFICATION_MAP.get(node);
    	for(String var:vars){
    		out.print(
    				"if (editjob."+seqPrefix+var+".value == \"\")\n"+
        			"  {\n"+
        			"    alert(\"Value of "+ node + "." + var + " can't be NULL" +"\");\n"+
        			"    editjob."+seqPrefix+var+".focus();\n"+
        			"    return;\n"+
        			"  }\n"
    				);
    	}
    			
    	out.print("editjob."+seqPrefix+node+"_op.value=\"Add\";\n"+
    			"  postFormSetAnchor(\""+seqPrefix+"+"+node+"\");\n"+
    			"}\n"+
    			"\n"+
    	"function "+seqPrefix+"delete"+node+"(i)\n"+
    			"{\n"+
    			"  // Set the operation\n"+
    			"  eval(\"editjob."+seqPrefix+node+"_\"+i+\"_op.value=\\\"Delete\\\"\");\n"+
    			"  // Submit\n"+
    			"  if (editjob."+seqPrefix+node+"_count.value==i)\n"+
    			"    postFormSetAnchor(\""+seqPrefix+node+"\");\n"+
    			"  else\n"+
    			"    postFormSetAnchor(\""+seqPrefix+node+"_\"+i)\n"+
    			"  // Undo, so we won't get two deletes next time\n"+
    			"  eval(\"editjob."+seqPrefix+node+"_\"+i+\"_op.value=\\\"Continue\\\"\");\n"+
    			"}\n"+
    			"\n");
    }
    		
    out.print("\n"+
    		"\n"+
    		"//-->\n"+
    		"</script>\n");		
  }

  
  public static void outputSpecificationBody(IHTTPOutput out, Locale locale, Specification os,
		  int connectionSequenceNumber, int actualSequenceNumber, String tabName)
				  throws ManifoldCFException, IOException
  {
	  String seqPrefix = "s"+connectionSequenceNumber+"_";
	  int i;
	  // Field Mapping tab
	  if (tabName.equals("Alfresco Filtering Configuration") && connectionSequenceNumber == actualSequenceNumber)
	  {
		  for(String node:SPECIFICATION_MAP.keySet()){
			  out.print(
					  "<table class=\"displaytable\">\n"+
							  "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
							  "  <tr>\n"+
							  "    <td class=\"description\"><nobr>" + node + " filtering:</nobr></td>\n"+
							  "    <td class=\"boxcell\">\n"+
							  "      <table class=\"formtable\">\n"+
							  "        <tr class=\"formheaderrow\">\n"+
							  "          <td class=\"formcolumnheader\"></td>\n");
			  Collection<String> vars = SPECIFICATION_MAP.get(node);
			  for(String var:vars){
				  out.print("<td class=\"formcolumnheader\"><nobr>" + var + "</nobr></td>\n");
			  }
			  out.print("</tr>\n");
			  
			  int fieldCounter = 0;
			  i = 0;
			  while (i < os.getChildCount()) {
				  SpecificationNode sn = os.getChild(i++);
				  if (sn.getType().equals(node)) {
					  String prefix = seqPrefix+node+"_" + Integer.toString(fieldCounter);
					  out.print(
							  "        <tr class=\""+(((fieldCounter % 2)==0)?"evenformrow":"oddformrow")+"\">\n"+
									  "          <td class=\"formcolumncell\">\n"+
									  "            <a name=\""+prefix+"\">\n"+
									  "              <input type=\"button\" value=\"Delete\" alt=\"Delete"+Integer.toString(fieldCounter+1)+"\" onclick='javascript:"+seqPrefix+"delete"+node+"("+Integer.toString(fieldCounter)+");'/>\n"+
									  "              <input type=\"hidden\" name=\""+prefix+"_op\" value=\"Continue\"/>\n");
					  for(String var:vars){
						  out.print("<input type=\"hidden\" name=\""+prefix+"_"+var+"\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(sn.getAttributeValue(var))+"\"/>\n");
					  }

					  out.print("            </a>\n"+
							  "          </td>\n");
					  for(String var:vars){
						  out.print(
								  "       <td class=\"formcolumncell\">\n"+
										  "            <nobr>"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(sn.getAttributeValue(var))+"</nobr>\n"+
								  "          </td>\n");
					  }
					  fieldCounter++;

				  }
			  }
			  
			  if (fieldCounter == 0)
		      {
		        out.print(
		        		"<tr class=\"formrow\"><td class=\"formmessage\" colspan=\"3\">No Filtering Configuration Specified</td></tr>\n");
		      }
			  
			  out.print(
						"        <tr class=\"formrow\"><td class=\"formseparator\" colspan=\"3\"><hr/></td></tr>\n"+
						"        <tr class=\"formrow\">\n"+
						"          <td class=\"formcolumncell\">\n"+
						"            <a name=\""+seqPrefix+node+"\">\n"+
						"              <input type=\"button\" value=\"Add\" alt=\"Add " + node + "\" onclick=\"javascript:"+seqPrefix+"add"+node+"();\"/>\n"+
						"            </a>\n"+
						"            <input type=\"hidden\" name=\""+seqPrefix+node+"_count\" value=\""+fieldCounter+"\"/>\n"+
						"            <input type=\"hidden\" name=\""+seqPrefix+node+"_op\" value=\"Continue\"/>\n"+
						"          </td>\n");
			  for(String var:vars){
				  out.print("          <td class=\"formcolumncell\">\n"+
						"            <nobr><input type=\"text\" size=\"15\" name=\""+seqPrefix+var+"\" value=\"\"/></nobr>\n"+
						"          </td>\n");
			  }

			 out.print("</tr>\n"+
						"      </table>\n"+
						"    </td>\n"+
						"  </tr>\n");
						

		  }
	  }
	  else{
	      for(String node:SPECIFICATION_MAP.keySet()){
	    	  i = 0;
		      int fieldCounter = 0;  
		      while (i < os.getChildCount()) {
		    	  SpecificationNode sn = os.getChild(i++);
		    	  if(sn.getType().equals(node)){
		    		String prefix = seqPrefix+node+"_" + Integer.toString(fieldCounter);  
		    		for(String var:SPECIFICATION_MAP.get(node)){
		    			out.print(
		    					"<input type=\"hidden\" name=\""+prefix+"_"+var+"\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(sn.getAttributeValue(var))+"\"/>\n");
		    		}
		    		fieldCounter++;
		    	  }
		      }
		      
		      out.print("<input type=\"hidden\" name=\""+seqPrefix+node+"_count\" value=\""+Integer.toString(fieldCounter)+"\"/>\n");
	      }
	    }
	  }
		  
  public static String processSpecificationPost(IPostParameters variableContext, Locale locale, Specification os,
		  int connectionSequenceNumber) throws ManifoldCFException {
	  // Remove old Nodes
	  int i;

	  String seqPrefix = "s"+connectionSequenceNumber+"_";
	 	 	  
	  for(String node:SPECIFICATION_MAP.keySet()){
		  
		  String x = variableContext.getParameter(seqPrefix+node+"_count");
		  if (x != null && x.length() > 0){
			  
			  i = 0;
			  while (i < os.getChildCount())
			  {
				  SpecificationNode specNode = os.getChild(i);
				  if (specNode.getType().equals(node))
					  os.removeChild(i);
				  else
					  i++;
			  }

			  Collection<String> vars = SPECIFICATION_MAP.get(node);

			  int count = Integer.parseInt(x);
			  i = 0;
			  while (i < count)
			  {
				  String prefix = seqPrefix+node+"_"+Integer.toString(i);
				  String op = variableContext.getParameter(prefix+"_op");
				  if (op == null || !op.equals("Delete"))
				  {
					  SpecificationNode specNode = new SpecificationNode(node);
					  for(String var:vars){
						  String value = variableContext.getParameter(prefix+"_"+var);
						  if(value == null)
							  value = "";
						  specNode.setAttribute(var, value);
					  }
					  os.addChild(os.getChildCount(), specNode);
				  }
				  i++;
			  }

			  String addop = variableContext.getParameter(seqPrefix+node+"_op");
			  if (addop != null && addop.equals("Add"))
			  {
				  SpecificationNode specNode = new SpecificationNode(node);
				  for(String var:vars){
					  String value = variableContext.getParameter(seqPrefix+var);
					  if(value == null)
						  value = "";
					  specNode.setAttribute(var, value);
				  }
				  os.addChild(os.getChildCount(), specNode);
			  }
		  }
	  }

	  return null;
  }
  
  public static void viewSpecification(IHTTPOutput out, Locale locale, Specification os,
		  int connectionSequenceNumber)
				  throws ManifoldCFException, IOException
  {
	  int i = 0;

	  for(String node:SPECIFICATION_MAP.keySet()){
		  Collection<String> vars = SPECIFICATION_MAP.get(node);
		  out.print(
				  "\n"+
						  "<table class=\"displaytable\">\n"+
						  "  <tr>\n"+
						  "    <td class=\"description\"><nobr>Alfresco "+ node + " Filtering Configuration</nobr></td>\n"+
						  "    <td class=\"boxcell\">\n"+
						  "      <table class=\"formtable\">\n"+
						  "        <tr class=\"formheaderrow\">\n");
		  for(String var:vars)
			  out.print(
					  "          <td class=\"formcolumnheader\"><nobr>" + var + "</nobr></td>\n");
						  
		 out.print("        </tr>\n");
		 
		 int fieldCounter = 0;
		  i = 0;
		 
		  while (i < os.getChildCount()) {
			  SpecificationNode sn = os.getChild(i++);
			  if (sn.getType().equals(node)) {
				  out.print(
						  "        <tr class=\""+(((fieldCounter % 2)==0)?"evenformrow":"oddformrow")+"\">\n");
				  for(String var:vars)
					  out.print(
								  "          <td class=\"formcolumncell\">\n"+
								  "            <nobr>"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(sn.getAttributeValue(var))+"</nobr>\n"+
								  "          </td>\n");
				  out.print(
								  "        </tr>\n");
				  fieldCounter++;
			  }
		  }

		  if (fieldCounter == 0)
		  {
			  out.print(
					  "        <tr class=\"formrow\"><td class=\"formmessage\" colspan=\"3\">No Filtering Configuration Specificied for " + node + "</td></tr>\n"
					  );
		  }
		  out.print(
				  "      </table>\n"+
						  "    </td>\n"+
						  "  </tr>\n"+
						  "  </tr>\n"+
						  "</table>\n");
	  }
  }
}

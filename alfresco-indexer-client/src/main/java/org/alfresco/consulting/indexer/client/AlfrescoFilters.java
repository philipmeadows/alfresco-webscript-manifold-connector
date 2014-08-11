package org.alfresco.consulting.indexer.client;

import java.util.Collection;
import java.util.Map;

import org.json.simple.JSONObject;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class AlfrescoFilters {

	private Collection<String> siteFilters;
	
	private Collection<String> typeFilters;
	
	private Collection<String> mimetypeFilters;
	
	private Collection<String> aspectFilters;
	
	private Map<String, String> metadataFilters;
	
	public AlfrescoFilters(){
		siteFilters = Sets.newHashSet();
		typeFilters = Sets.newHashSet();
		mimetypeFilters = Sets.newHashSet();
		aspectFilters = Sets.newHashSet();
		metadataFilters = Maps.newHashMap();
	}

	public Collection<String> getSiteFilters() {
		return siteFilters;
	}
	
	public Collection<String> getTypeFilters(){
	    return typeFilters;
	}

	public Collection<String> getMimetypeFilters() {
		return mimetypeFilters;
	}

	public Collection<String> getAspectFilters() {
		return aspectFilters;
	}

	public Map<String, String> getMetadataFilters() {
		return metadataFilters;
	}
	
	public void addSiteFilter(String site){
		siteFilters.add(site);
	}
	
	public void addTypeFilter(String type){
	    typeFilters.add(type);
	}
	
	public void addMimetypeFilter(String mimetype){
		mimetypeFilters.add(mimetype);
	}
	
	public void addAspectFilter(String aspect){
		aspectFilters.add(aspect);
	}
	
	public void addMetadataFilter(String metadata, String value){
		metadataFilters.put(metadata, value);
	}
	
	public  String toJSONString(){
	    
	    Gson gson= new GsonBuilder().create();
	    return gson.toJson(this);
	    
	}
}

package org.alfresco.consulting.indexer.client;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class AlfrescoFilters {

	private Collection<String> siteFilters;
	
	private Collection<String> mimetypeFilters;
	
	private Map<String, String> aspectFilters;
	
	private Map<String, String> metadataFilters;
	
	public AlfrescoFilters(){
		siteFilters = Sets.newHashSet();
		mimetypeFilters = Sets.newHashSet();
		aspectFilters = Maps.newHashMap();
		metadataFilters = Maps.newHashMap();
	}

	public Collection<String> getSiteFilters() {
		return siteFilters;
	}

	public Collection<String> getMimetypeFilters() {
		return mimetypeFilters;
	}

	public Map<String, String> getAspectFilters() {
		return aspectFilters;
	}

	public Map<String, String> getMetadataFilters() {
		return metadataFilters;
	}
	
	public void addSiteFilter(String site){
		siteFilters.add(site);
	}
	
	public void addMimetypeFilter(String mimetype){
		mimetypeFilters.add(mimetype);
	}
	
	public void addAspectFilter(String aspectName, String aspectValue){
		aspectFilters.put(aspectName, aspectValue);
	}
	
	public void addMetadataFilter(String metadata, String value){
		metadataFilters.put(metadata, value);
	}
}

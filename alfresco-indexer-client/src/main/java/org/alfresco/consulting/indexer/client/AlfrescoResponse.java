package org.alfresco.consulting.indexer.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AlfrescoResponse {
  private final long lastTransactionId;
  private final long lastAclChangesetId;
  private final String storeId;
  private final String storeProtocol;
  private final Iterable<Map<String, Object>> documents;

  public AlfrescoResponse(long lastTransactionId, long lastAclChangesetId, String storeId,
                          String storeProtocol, Iterable<Map<String, Object>> documents) {
    this.lastTransactionId = lastTransactionId;
    this.lastAclChangesetId = lastAclChangesetId;
    this.storeId = storeId;
    this.storeProtocol = storeProtocol;
    this.documents = documents;
  }

  public AlfrescoResponse(long lastTransactionId, long lastAclChangesetId) {
    this(lastTransactionId, lastAclChangesetId, "", "", Collections.<Map<String, Object>>emptyList());
  }

  public long getLastTransactionId() {
    return lastTransactionId;
  }

  public long getLastAclChangesetId() {
    return lastAclChangesetId;
  }

  public String getStoreId() {
    return storeId;
  }

  public String getStoreProtocol() {
    return storeProtocol;
  }

  public Iterable<Map<String,Object>> getDocuments() {
    return documents;
  }

  public List<Map<String, Object>> getDocumentList() {
    List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
    for (Map<String, Object> m : documents) {
      list.add(m);
    }
    return list;
  }
}
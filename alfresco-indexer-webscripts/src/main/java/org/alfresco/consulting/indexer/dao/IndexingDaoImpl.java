package org.alfresco.consulting.indexer.dao;

import org.alfresco.consulting.indexer.entities.NodeBatchLoadEntity;
import org.alfresco.consulting.indexer.entities.NodeEntity;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.session.RowBounds;
import org.mybatis.spring.SqlSessionTemplate;

import java.util.List;
import java.util.Set;

public class IndexingDaoImpl {

  private static final String SELECT_NODES_BY_ACLS = "alfresco.index.select_NodeIndexesByAclChangesetId";
  private static final String SELECT_NODES_BY_TXNS = "alfresco.index.select_NodeIndexesByTransactionId";

  protected static final Log logger = LogFactory.getLog(IndexingDaoImpl.class);

  public List<NodeEntity> getNodesByAclChangesetId(Pair<Long, StoreRef> store, Long lastAclChangesetId, int maxResults) {
    StoreRef storeRef = store.getSecond();
    if (maxResults <= 0 || maxResults == Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Maximum results must be a reasonable number.");
    }

    logger.debug("[getNodesByAclChangesetId] On Store "+storeRef.getProtocol()+"://"+storeRef.getIdentifier());

    NodeBatchLoadEntity nodeLoadEntity = new NodeBatchLoadEntity();
    nodeLoadEntity.setStoreId(store.getFirst());
    nodeLoadEntity.setStoreProtocol(storeRef.getProtocol());
    nodeLoadEntity.setStoreIdentifier(storeRef.getIdentifier());
    nodeLoadEntity.setMinId(lastAclChangesetId);
    nodeLoadEntity.setMaxId(lastAclChangesetId+maxResults);
    nodeLoadEntity.setAllowedTypes(this.allowedTypes);

    return (List<NodeEntity>) template.selectList(SELECT_NODES_BY_ACLS, nodeLoadEntity, new RowBounds(0, Integer.MAX_VALUE));
  }

  public List<NodeEntity> getNodesByTransactionId(Pair<Long, StoreRef> store, Long lastTransactionId, int maxResults) {
    StoreRef storeRef = store.getSecond();
    if (maxResults <= 0 || maxResults == Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Maximum results must be a reasonable number.");
    }

    logger.debug("[getNodesByTransactionId] On Store "+storeRef.getProtocol()+"://"+storeRef.getIdentifier());

    NodeBatchLoadEntity nodeLoadEntity = new NodeBatchLoadEntity();
    nodeLoadEntity.setStoreId(store.getFirst());
    nodeLoadEntity.setStoreProtocol(storeRef.getProtocol());
    nodeLoadEntity.setStoreIdentifier(storeRef.getIdentifier());
    nodeLoadEntity.setMinId(lastTransactionId);
    nodeLoadEntity.setMaxId(lastTransactionId+maxResults);
    nodeLoadEntity.setAllowedTypes(this.allowedTypes);

    return (List<NodeEntity>) template.selectList(SELECT_NODES_BY_TXNS, nodeLoadEntity, new RowBounds(0, Integer.MAX_VALUE));
  }

  private SqlSessionTemplate template;
  private Set<String> allowedTypes;
  public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
    this.template = sqlSessionTemplate;
  }
  public void setAllowedTypes(Set<String> allowedTypes) {
    this.allowedTypes = allowedTypes;
  }
}

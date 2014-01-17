package org.alfresco.consulting.indexer.dao;

import org.alfresco.consulting.indexer.entities.NodeBatchLoadEntity;
import org.alfresco.consulting.indexer.entities.NodeEntity;
import org.apache.ibatis.session.RowBounds;
import org.mybatis.spring.SqlSessionTemplate;

import java.util.List;
import java.util.Set;

public class IndexingDaoImpl {

  private static final String SELECT_NODES_BY_ACLS = "alfresco.index.select_NodeIndexesByAclChangesetId";
  private static final String SELECT_NODES_BY_TXNS = "alfresco.index.select_NodeIndexesByTransactionId";

  public List<NodeEntity> getNodesByAclChangesetId(Long storeId, Long lastAclChangesetId, int maxResults) {
    if (maxResults <= 0 || maxResults == Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Maximum results must be a reasonable number.");
    }

    NodeBatchLoadEntity nodeLoadEntity = new NodeBatchLoadEntity();
    nodeLoadEntity.setStoreId(storeId);
    nodeLoadEntity.setMinId(lastAclChangesetId);
    nodeLoadEntity.setMaxId(lastAclChangesetId+maxResults);
    nodeLoadEntity.setAllowedTypes(this.allowedTypes);

    return (List<NodeEntity>) template.selectList(SELECT_NODES_BY_ACLS, nodeLoadEntity, new RowBounds(0, Integer.MAX_VALUE));
  }

  public List<NodeEntity> getNodesByTransactionId(Long storeId, Long lastTransactionId, int maxResults) {
    if (maxResults <= 0 || maxResults == Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Maximum results must be a reasonable number.");
    }

    NodeBatchLoadEntity nodeLoadEntity = new NodeBatchLoadEntity();
    nodeLoadEntity.setStoreId(storeId);
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

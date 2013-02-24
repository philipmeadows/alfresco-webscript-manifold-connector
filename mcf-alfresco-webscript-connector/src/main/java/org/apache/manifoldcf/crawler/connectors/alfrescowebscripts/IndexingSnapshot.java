package org.apache.manifoldcf.crawler.connectors.alfrescowebscripts;

public final class IndexingSnapshot {

  private final long txnId;
  private final long commitTime;

  IndexingSnapshot(final long txnId,final long commitTime) {
    this.txnId = txnId;
    this.commitTime = commitTime;
  }

  public long getTxnId() {
    return txnId;
  }

  public long getCommitTime() {
    return commitTime;
  }

  public String toString() {
    return "IndexingSnapshot("+this.getTxnId()+","+this.getCommitTime()+")";
  }
}

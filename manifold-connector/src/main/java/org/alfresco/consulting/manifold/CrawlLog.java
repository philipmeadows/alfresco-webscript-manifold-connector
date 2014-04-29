package org.alfresco.consulting.manifold;

import org.apache.manifoldcf.core.interfaces.TimeMarker;

class CrawlLog {
  public final TimeMarker time;
  public final Long jobid;
  public final Long last_tx_id;
  public final Long last_acl_id;

  CrawlLog(TimeMarker time, Long jobid, Long last_tx_id, Long last_acl_id) {
    this.time = time;
    this.jobid = jobid;
    this.last_tx_id = last_tx_id;
    this.last_acl_id = last_acl_id;
  }

  @Override
  public String toString() {
    return "CrawlLog{" +
            "time='" + time + '\'' +
            ", jobid='" + jobid + '\'' +
            ", last_tx_id=" + last_tx_id +
            ", last_acl_id=" + last_acl_id +
            '}';
  }
}
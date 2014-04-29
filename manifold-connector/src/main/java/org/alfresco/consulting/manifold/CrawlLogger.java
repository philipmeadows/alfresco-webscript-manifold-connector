package org.alfresco.consulting.manifold;

import org.apache.manifoldcf.core.database.BaseTable;
import org.apache.manifoldcf.core.interfaces.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

class CrawlLogger extends BaseTable {
  public CrawlLogger(IDBInterface dbInterface, String tableName) {
    super(dbInterface, tableName);
  }

  public void install() throws ManifoldCFException {
    if (!tableExists()) {
      Map<String, ColumnDescription> table = new HashMap<String, ColumnDescription>();
      table.put("time", new ColumnDescription("timestamp", true, false, null, null, false));
      table.put("jobid", new ColumnDescription("bigint", false, false, null, null, false));
      table.put("last_tx_id", new ColumnDescription("bigint", false, false, null, null, false));
      table.put("last_acl_id", new ColumnDescription("bigint", false, false, null, null, false));
      performCreate(table, null);
    }
  }

  public CrawlLog getLatestLog(Long jobid) throws ManifoldCFException {
    if (dbInterface == null)
      return null;

    IResultSet iResultSet = performQuery("select * from " + tableName + " where jobid=" + jobid + " order by time desc limit 1",
            new ArrayList(), null, null);

    if (iResultSet.getRowCount() == 0)
      return null;

    IResultRow row = iResultSet.getRow(0);
    TimeMarker time = (TimeMarker) row.getValue("time");
    Long jobid1 = (Long) row.getValue("jobid");
    assert (jobid.equals(jobid1));
    Long last_tx_id = (Long) row.getValue("last_tx_id");
    Long last_acl_id = (Long) row.getValue("last_acl_id");
    return new CrawlLog(time, jobid1, last_tx_id, last_acl_id);
  }

  public void log(long jobid, long last_tx_id, long last_acl_id) throws ManifoldCFException {
    if (dbInterface == null)
      return;

    Map<String, Object> map = new HashMap<String, Object>();
    map.put("time", new TimeMarker(new Date().getTime()));
    map.put("jobid", jobid);
    map.put("last_tx_id", last_tx_id);
    map.put("last_acl_id", last_acl_id);
    performInsert(map, null);
  }

  public void uninstall() throws ManifoldCFException {
    if (tableExists()) {
      performDrop(null);
    }
  }

  private boolean tableExists() throws ManifoldCFException {
    return getTableSchema(null, null) != null;
  }
}

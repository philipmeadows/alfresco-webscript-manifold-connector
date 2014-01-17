package org.apache.manifoldcf.crawler.system;

import org.apache.manifoldcf.crawler.interfaces.ISeedingActivity;

/**
 * This is a hack to get the jobId given a {@link org.apache.manifoldcf.crawler.system.SeedingActivity}.
 * TODO: If a way to get the job id from within a connector in ManifoldCF is found, delete this.
 */
public class JobIdStealer {
  public static long stealId(ISeedingActivity activity) {
    if (!(activity instanceof SeedingActivity)) {
      throw new RuntimeException("Activity is not a " + SeedingActivity.class.getName() + "!");
    }
    return stealId((SeedingActivity) activity);
  }

  public static long stealId(SeedingActivity activity) {
    return activity.jobID == null ? 0 : activity.jobID;
  }
}
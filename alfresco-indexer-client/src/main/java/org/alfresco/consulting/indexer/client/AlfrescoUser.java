package org.alfresco.consulting.indexer.client;

import java.util.List;

public class AlfrescoUser {
  private final String username;
  private final List<String> authorities;

  public AlfrescoUser(String username, List<String> authorities) {
    this.username = username;
    this.authorities = authorities;
  }

  public String getUsername() {
    return username;
  }

  public List<String> getAuthorities() {
    return authorities;
  }
}

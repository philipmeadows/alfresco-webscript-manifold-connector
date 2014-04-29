package org.alfresco.consulting.indexer.client;

public class AlfrescoDownException extends RuntimeException {
  public AlfrescoDownException() {
    super();
  }

  public AlfrescoDownException(String s) {
    super(s);
  }

  public AlfrescoDownException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public AlfrescoDownException(Throwable throwable) {
    super(throwable);
  }
}

package org.alfresco.consulting.indexer.client;

public class AlfrescoParseException extends RuntimeException {
  public AlfrescoParseException() {
    super();
  }

  public AlfrescoParseException(String s) {
    super(s);
  }

  public AlfrescoParseException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public AlfrescoParseException(Throwable throwable) {
    super(throwable);
  }
}

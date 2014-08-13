package org.alfresco.consulting.indexer.utils;

import java.util.Iterator;

import org.alfresco.service.cmr.repository.Path;

public class Utils
{
    
    public static String getSiteName(Path path) {
        //Fetching Path and preparing for rendering
        Iterator<Path.Element> pathIter = path.iterator();

        //Scan the Path to find the Alfresco Site name
        boolean siteFound = false;
        while(pathIter.hasNext()) {
          String pathElement = pathIter.next().getElementString();
          //Stripping out namespace from PathElement
          int firstChar = pathElement.lastIndexOf('}');
          if (firstChar > 0) {
            pathElement = pathElement.substring(firstChar+1);
          }
          if (pathElement.equals("sites")) {
            siteFound = true;
          } else if (siteFound) {
            return pathElement;
          }
        }
        return null;
      }

}

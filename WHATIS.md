alfresco-webscript-manifold-connector
=====================================

What is it?
---
Alfresco WebScripts Manifold Connector is a connector for Apache ManifoldCF; it aims to deliver a different (custom) way to index content hosted in Alfresco.

It mimics the same behaviour of the built-in Solr API Webscripts (and Alfresco Solr CoreTracker), with one fundamental difference: this implementation delivers one single endpoint that returns one single list of nodeRefs, joining nodes that have been altered by transactions OR by ACL changesets; for each nodeRef returned, node properties, aspects and ACLs are indexed into one single (index) document

- Pro: Simplified Search Index structure, it improves integration of Alfresco indexing with existing Search engines and index data structures
- Pro: The authorization checks are implemented by query parsers by adding security constraints to a given query; there is no post-processing or data-joining activity involved during a query execution
- Cons: If an ACL changes on a node, also all other nodes that inherit from it will be re-indexed, including node properties and content
- Cons: Alfresco query parsers (delivering CMISQL, FTS and any other Alfresco custom search feature) is currently not implemented, therefore it cannot work as an Alfresco Search Subsystem (i.e. cannot work with Alfresco Share, without some customisation)

Project Structure
---

- Alfresco Indexer Webscripts - An Alfresco Module Package (AMP) that exposes the set of Webscripts on Alfresco Repository (similar to Solr API Webscripts)
- Alfresco Indexer Client - A Java API that wraps HTTP invocations to Alfresco Indexer Webscripts (similar to Alfresco Solr CoreTracker, the Alfresco API deployed into Apache Solr that invokes Alfresco Solr API against the repo and commits documents into Solr)
- Manifold Connector - The Manifold Connector that registers Alfresco as a Manifold source; it depends on Alfresco Indexer Client

Next
---
- Index aspect with manifold-connector (adding boolean to enable/disable feature)
- Index readable authorities with manifold-connector (adding boolean to enable/disable feature)
- Index TXT transformation of content with manifold-connector (adding boolean to enable/disable feature); this feature still needs to be implemented at Webscript and Client level
- Define configurable restrictions on NodeChangesWebScript: node-type, path, owner, modifier
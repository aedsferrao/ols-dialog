
---


**Changes in OLS Dialog v3.4.1 - (December 4. 2013):**
  * BUG FIX: Fixed a bug in the use of terms selected via the "Browse Ontology" tab.


---


**Changes in OLS Dialog v3.4.0 - (September 12. 2012):**
  * FEATURE IMPROVEMENT: Improved the way the auto search option in the term name search works. It now waits until the user finishes/pauses typing before starting the search. (Instead of simple starting the search whenever the user inputs a character.)
  * FEATURE IMPROVEMENT: Lots of minor GUI improvements.
  * FEATURE IMPROVEMENT: Made the code compatible with Maven 3.
  * LIBRARY UPDATE: Added jsparklines as a dependency, to support html links in the tables.
  * LIBRARY UPDATE: Removed swingx as a dependency.
  * LIBRARY UPDATE: Updated the ols dependency (now renamed to ols-client) to version 1.18.

Download Count: 76


---


**Changes in OLS Dialog v3.3.2 - (March 30. 2012):**
  * NEW FEATURE: New and optional functionality to filter the list of Ontologies from OLS. This functionality includes the option to browse ontology terms from a specific parent term.
  * NEW FEATURE: The look and feel is no longer set inside the OLS Dialog, but rather the look and feel from the parent is used.
  * FEATURE IMPROVEMENT: The Term Name Search is now only performed if at least three characters are inserted.
  * FEATURE IMPROVEMENT: Updated ols-dialog interface to return metadata of selected term to calling object.
  * FEATURE IMPROVEMENT: Minor GUI fixes related to using different look and feels.

Download Count: 23


---


**Changes in OLS Dialog v3.2.3 - (July 5. 2010):**
  * FEATURE IMPROVEMENT: Lots of minor updates to the GUI to make it look better on Windows 7.

Download Count: 86 (50 + 36 of snapshot 3.3.1)


---


**Changes in OLS Dialog v3.2.2 - (Oct. 08. 2009):**
  * FEATURE IMPROVEMENT: Minor updates to parts of the GUI.
  * FEATURE IMPROVEMENT: Short ontology names, e.g., GO or PSI, are now supported when setting the initial ontology.
  * BUG FIX: Fixed a couple of minor GUI bugs.

Download Count: 34


---


**Changes in OLS Dialog v3.2.1 - (Oct. 02. 2009):**
  * BUG FIX: Fixed a bug where the NEWT species tips link was shown when mass search was used to start the dialog.
  * BUG FIX: Fixed a Java 1.5 issue where the wrong GUI package was used.
  * BUG FIX: Fixed some errors in the Common NEWT Species list.

Download Count: 0


---


**Changes in OLS Dialog v3.2 - (Oct. 02. 2009):**
  * NEW FEATURE: Added a `Search In All Ontologies` option.
  * NEW FEATURE: Disabled the ontology browsing for NEWT (it did not work). Instead a NEWT Species Tip dialog has been added where the most common species can be easily selected.
  * FEATURE IMPROVEMENT: Updated the help files.
  * FEATURE IMPROVEMENT: Ontologies without root terms are now detected and a message is shown when trying to browse such an ontology.
  * BUG FIX: Fixed a problem with using the 'Enter' key to close some of the dialogs (the dialogs reappeared).
  * BUG FIX: Fixed a bug where the term hierarchy link was not disabled when a new Term Name or Term ID search was performed.
  * BUG FIX: Disabled column reordering in Term ID search columns.
  * BUG FIX: Made sure that only one row can be selected at once in the search result tables.

Download Count: 0


---


**Changes in OLS Dialog v3.1 - (Oct. 01. 2009):**
  * NEW FEATURE: Added a term hierarchy graph viewer.
  * NEW FEATURE: Added a simply term id search.
  * NEW FEATURE: Nodes in the tree browser that have children are now represented using folder icons. Previously these icons where added only after such a node had been opened, and there was no way of separating the different types of nodes from each other.
  * BUG FIX: Fixed a bug in the multiple line tool tips.

Download Count: 1


---


**Changes in OLS Dialog v3.0 - (Sep. 29. 2009):**
  * NEW FEATURE: Browse ontology feature added.
  * FEATURE IMPROVEMENT: Multiple line tool tips added to the meta data tables.
  * FEATURE IMPROVEMENT: xrefs are now added to the meta data tables.
  * FEATURE IMPROVEMENT: Help texts extended.
  * BUG FIX: Fixed an incorrect default subject in the mailto links.

Download Count: 0


---


**Changes in OLS Dialog v2.0.1 - (Sep. 23. 2009):**
  * FEATURE IMPROVEMENT: Clicking 'Enter' in the modification mass and mass error fields now starts the search (if the search button is enabled).
  * FEATURE IMPROVEMENT: Text and mass search is now performed by default if valid search values are provided.
  * FEATURE IMPROVEMENT: Changed the default mass search accuracy to 0.1.
  * BUG FIX: Added a dependency to be fully Java 1.5 compatible.

Download Count: 0


---


**Changes in OLS Dialog v2.0 - (Sep. 21. 2009):**
  * NEW FEATURE: Support for modification mass search has been added.
  * FEATURE IMPROVEMENT: The search result tables can now be sorted by clicking the table headers.
  * BUG FIX: Fixed a bug in the tool tips for the tables.

Download Count: 0


---


**Changes in OLS Dialog v1.0.2 - (Sep. 11. 2009):**
  * BUG FIX: Fixed some issues with the (Maven) dependencies not being set correctly.

Download Count: 1


---


**Changes in OLS Dialog v1.0.1 - (Aug. 19. 2009):**
  * Added support for handling CV terms where the key is set to 'null'.
  * Terms without a definition are now detected and an appropriate message is provided. (Previously 'null' was shown as the definition.)
  * Minor updates to the help files.

Download Count: 3


---


**Changes in OLS Dialog v1.0 - (July. 23. 2009):**
  * (initial version)

Download Count: 0


---

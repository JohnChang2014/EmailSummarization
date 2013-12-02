/*
 *  BWPGazetteer.java
 *
 *
 * Copyright (c) Bruno Woltzenlogel Paleo.
 *
 * This file is the source code of a plugin for GATE (see http://gate.ac.uk/), and is free
 * software, licenced under the GNU Library General Public License,
 * Version 2, June1991.
 *
 * A copy of this licence is included in the distribution of GATE in the file
 * licence.html, and is also available at http://gate.ac.uk/gate/licence.html.
 *
 *  Bruno Woltzenlogel Paleo (bruno.wp@gmail.com ; http://bruno-wp.blogspot.com/ ; http://planeta.terra.com.br/arte/lua/), 4/7/2006
 *
 *  $Id: BWPGazetteer.jav,v 1.4 2007/08/22 08:03:43 oana Exp $
 */

package bwp.gate.gazetteer;

import java.util.*;

import gate.*;
import gate.creole.*;
import gate.creole.gazetteer.*;
import gate.util.*;


/** 
 * This class is the implementation of the resource LEVENSHTEINGAZETEER.
 */
public class BWPGazetteer extends AbstractGazetteer {

  /** a map of nodes vs gaz lists */
  protected Map listsByNode;

  /** the maximum normalized distance considered to be a match*/
  private Double normalizedDistanceThreshold = new Double(0.15);
  
  /** a flag indicating whether overlapping annotations shouldn't be produced.
   *  if it is true, the algorithm annotates only the best match among
   *  the overlaping ones.*/
  private Boolean avoidOverlapingAnnotations = new Boolean(true);
  
  /** The separator used for gazetteer entry features */
  protected String gazetteerFeatureSeparator;
  
  /** the character that is used to separate words.*/
  private char wordSeparator= ' ';
  
  /** the costs associated with each string edition operation*/
  private double deleteCost = 1;
  private double insertCost = 1;
  private double substituteCost = 1;
  
  private Vector lookupVector;
  private Vector stringVector;
 
  public void setAvoidOverlapingAnnotations(Boolean avoid) {
      avoidOverlapingAnnotations = avoid;
  }
  public java.lang.Boolean getAvoidOverlapingAnnotations() {
      return avoidOverlapingAnnotations;
  }
  
  public void setNormalizedDistanceThreshold(Double threshold) {
      normalizedDistanceThreshold = threshold;
  }  
  public java.lang.Double getNormalizedDistanceThreshold() {
      return normalizedDistanceThreshold;
  }
  
 /** Does the actual loading and parsing of the lists. This method must be
   * called before the gazetteer can be used
   */
  public Resource init() throws ResourceInstantiationException{
      
    lookupVector = new Vector();
    stringVector = new Vector();
    
    if(listsURL == null){
      throw new ResourceInstantiationException (
            "No URL provided for gazetteer creation!");
    }
    definition = new LinearDefinition();
    definition.setSeparator(Strings.unescape(gazetteerFeatureSeparator));   
    definition.setURL(listsURL);
    definition.load();
    int linesCnt = definition.size();
    listsByNode = definition.loadLists();
    Iterator inodes = definition.iterator();

    int nodeIdx = 0;
    LinearNode node;
    while (inodes.hasNext()) {
      node = (LinearNode) inodes.next();
      fireStatusChanged("Reading " + node.toString());
      fireProgressChanged(++nodeIdx * 100 / linesCnt);
      readList(node,true);
    } // while iline
    fireProcessFinished();
    return this;
  }

    /** Reads one lists (one file) of phrases
   *
   * @param node the node
   * @param add if <b>true</b> will add the phrases found in the list to the ones
   *     recognised by this gazetter, if <b>false</b> the phrases found in the
   *     list will be removed from the list of phrases recognised by this
   *     gazetteer.
   */
   protected void readList(LinearNode node, boolean add) 
       throws ResourceInstantiationException{
    String listName, majorType, minorType, languages;
    if ( null == node ) {
      throw new ResourceInstantiationException(" LinearNode node is null ");
    }

    listName = node.getList();
    majorType = node.getMajorType();
    minorType = node.getMinorType();
    languages = node.getLanguage();
    GazetteerList gazList = (GazetteerList)listsByNode.get(node);
    if (null == gazList) {
      throw new ResourceInstantiationException("gazetteer list not found by node");
    }

    Iterator iline = gazList.iterator();

    // create default lookup for entries with no arbitary features
    Lookup defaultLookup = new Lookup(listName,majorType, minorType, languages);
    defaultLookup.list = node.getList();
    if ( null != mappingDefinition){
      MappingNode mnode = mappingDefinition.getNodeByList(defaultLookup.list);
      if (null!=mnode){
        defaultLookup.oClass = mnode.getClassID();
        defaultLookup.ontology = mnode.getOntologyID();
      }
    }//if mapping def

    Lookup lookup;
    String entry;
    while(iline.hasNext()){
      GazetteerNode gazNode = (GazetteerNode)iline.next();
      entry = gazNode.getEntry();
      
      Map features = gazNode.getFeatureMap();
      if (features == null) {
        lookup = defaultLookup;
      } else {
        // create a new Lookup object with features
        lookup = new Lookup(listName, majorType, minorType, languages);
        lookup.list = node.getList();
        if(null != mappingDefinition) {
          MappingNode mnode = mappingDefinition.getNodeByList(lookup.list);
          if(null != mnode) {
            lookup.oClass = mnode.getClassID();
            lookup.ontology = mnode.getOntologyID();
          }
        }// if mapping def
        lookup.features = features;
      } 
      
      if(add)addLookup(entry, lookup);
      else removeLookup(entry, lookup);
    }    
  } // void readList(String listDesc)

   /**
    * @return the gazetteerFeatureSeparator
    */
   public String getGazetteerFeatureSeparator() {
     return gazetteerFeatureSeparator;
   }

   /**
    * @param gazetteerFeatureSeparator the gazetteerFeatureSeparator to set
    */
   public void setGazetteerFeatureSeparator(String gazetteerFeatureSeparator) {
     this.gazetteerFeatureSeparator = gazetteerFeatureSeparator;
   }
   
  /**
   * Adds one phrase to the list of phrases recognized by this gazetteer
   *
   * @param text the phrase to be added
   * @param lookup the description of the annotation associated to this phrase
   */ 
  public void addLookup(String text, Lookup lookup) {
    lookupVector.add(lookup);
    stringVector.add(text);
  } // addLookup

 /** Removes one phrase to the list of phrases recognised by this gazetteer
   *
   * @param text the phrase to be removed
   * @param lookup the description of the annotation associated to this phrase
   */
  public void removeLookup(String text, Lookup lookup) {
     int index = lookupVector.indexOf(lookup);
     lookupVector.remove(index);
     stringVector.remove(index);
  } // removeLookup

   
  /**
   * This method runs the gazetteer. It assumes that all the needed parameters
   * are set. If they are not, an exception will be fired.
   */
  public void execute() throws ExecutionException{
    
    if (wholeWordsOnly && !avoidOverlapingAnnotations) {
            throw new ExecutionException("BWP Gazetteer should not be used with " +
                                    "'wholeWordsOnly == true' and  'avoidOverlapingAnnotations == false'. \n" +
                                    "Please reconfigure the parameters of BWPGazetteer to:\n " +
                                    "'wholeWordsOnly == true' and  'avoidOverlapingAnnotations == true', \n" +
                                    "and the results will be the same as expected with \n" +
                                    "'wholeWordsOnly == true' and  'avoidOverlapingAnnotations == false'");                            
    }    
      
    interrupted = false;
    AnnotationSet annotationSet;
    //check the input
    if(document == null) {
      throw new ExecutionException(
        "No document to process!"
      );
    }
    String content = document.getContent().toString();
    //System.out.println(content);
    if (!caseSensitive) content = content.toLowerCase();

    if(annotationSetName == null ||
       annotationSetName.equals("")) annotationSet = document.getAnnotations();
    else annotationSet = document.getAnnotations(annotationSetName);

    fireStatusChanged("Doing lookup in " +
                           document.getName() + "...");
      
    String text = content + " "; // to avoid out of boundaries problems
    char[] textArray = text.toCharArray();

    for (int i=0;i<stringVector.size();i++) {
    	String string = (String) stringVector.elementAt(i);
    	if (!caseSensitive) string = string.toLowerCase();
    	if (string != "") {
    		annotate(textArray,annotationSet,string,(Lookup) lookupVector.elementAt(i));
    	}
    }
              
    fireProcessFinished();
    fireStatusChanged("Lookup complete!");
  } // execute

  /**lookup <br>
   * @param singleItem a single string to be looked up by the gazetteer
   * @return set of the Lookups associated with the parameter*/
  public Set lookup(String singleItem) {
    char currentChar;
    Set set = new HashSet();
    
    
    return set;
  }

  public boolean remove(String singleItem) {
    int index = stringVector.indexOf(singleItem);
    lookupVector.remove(index);
    stringVector.remove(index);
    return true;
  }

  public boolean add(String singleItem, Lookup lookup) {
    addLookup(singleItem,lookup);
    return true;
  } 
    
    /** Does the actual search for a given word in the text and annotates the ocurrences found.
     *
     * @param text the text where the search should be performed
     * @param annotationSet the current set of annotations associated with the text
     * @param string the word that has to be found
     * @param lookup the type of annotation that will be associated with ocurrences of the word in the text
     */
    private void annotate(char[] content, AnnotationSet annotationSet, String string, Lookup lookup) {
        //System.out.println("-------------------------------------: " + string);
        //System.out.println(text.length());
                
        double [] distancesPrevCol  = new double [string.length()+1];
        int [] insertionsPrevCol  = new int [string.length()+1];
        int [] deletionsPrevCol  = new int [string.length()+1];
        double [] distancesCol  = new double [string.length()+1];
        int [] insertionsCol  = new int [string.length()+1];
        int [] deletionsCol  = new int [string.length()+1];
        double distanceThreshold;
        
        if (string.length()!=0) {
            distanceThreshold = (normalizedDistanceThreshold * string.length());
        } else distanceThreshold = -1.0; //We do not annotate empty strings.
               
        
        for (int i=0;i<string.length()+1;i++){
            distancesPrevCol[i] = i;
            deletionsPrevCol[i] = i;
        }
        
        double prevPrevDistance = Double.MAX_VALUE; // stores the distance 2 columns before. this is important to find the minimum distance
        for (int j=0; j<content.length;j++) {
            // distancesCol[0] = 0; //Unnecessary because Java initialize arrays with 0 automatically.
            for (int i=1;i<distancesCol.length;i++) {
                if (string.charAt(i - 1) == content[j]) {
                    distancesCol[i] = distancesPrevCol[i - 1];
                    deletionsCol[i] = deletionsPrevCol[i - 1];
                    insertionsCol[i] = insertionsPrevCol[i - 1];
                }
                else {
                    double delete = distancesCol[i-1] + deleteCost;
                    double insert = distancesPrevCol[i] + insertCost;
                    double substitute = distancesPrevCol[i-1] + substituteCost;
                    if (delete<=insert && delete<=substitute) {
                        distancesCol[i] = delete;
                        deletionsCol[i] = deletionsCol[i-1] + 1;
                        insertionsCol[i] = insertionsCol[i-1];
                    }
                    else if (insert<=substitute){
                        distancesCol[i] = insert;
                        deletionsCol[i] = deletionsPrevCol[i];
                        insertionsCol[i] = insertionsPrevCol[i]+1;
                    }
                    else {
                        distancesCol[i] = substitute;
                        deletionsCol[i] = deletionsPrevCol[i-1];
                        insertionsCol[i] = insertionsPrevCol[i-1];
                    }
                }
            }
            double distance = distancesCol[string.length()];
            double prevDistance = distancesPrevCol[string.length()];
            if (avoidOverlapingAnnotations) {
                if (distance >= prevDistance && prevDistance <= prevPrevDistance) { // a minimum was found
                    if (prevDistance <= distanceThreshold) {
                        int matchedRegionEnd = j;
                        int matchedRegionStart = (j) -(string.length()+insertionsPrevCol[string.length()]-deletionsPrevCol[string.length()]);

                        if (wholeWordsOnly) { //In this case, we must check if "matchedRegionEnd" and "matchedRegionStart" correspond to the end and beginning of a word
                            int startOffset = 0;
                            int endOffset = 0;
                            
                            if (matchedRegionEnd + endOffset < content.length) {
                                while (!Character.isWhitespace(content[matchedRegionEnd + endOffset])) {
                                    endOffset++;
                                    if (matchedRegionEnd + endOffset == content.length-1) break;
                                }
                            }    

                            if (matchedRegionStart + startOffset > 0) {
                                while (!Character.isWhitespace(content[matchedRegionStart + startOffset - 1])) {
                                   startOffset--;
                                   if (matchedRegionStart + startOffset == 0) break;
                                }
                            }
      
                            double currentDistance = prevDistance +(endOffset-startOffset)*insertCost;

                            if (currentDistance <= distanceThreshold) {
                                putAnnotation(annotationSet, lookup, string, 
                                            currentDistance, matchedRegionStart+startOffset, 
                                            matchedRegionEnd+endOffset);
                            }
                        }
                        else { //(!wholeWordsOnly), then we may simply add the annotation
                            putAnnotation(annotationSet, lookup, string, 
                                                prevDistance, matchedRegionStart, 
                                                matchedRegionEnd);     
                        }
                    }     
                } 
            } else {
                //System.out.println("Debug");
                if (!wholeWordsOnly) {
                    if (prevDistance <= distanceThreshold) {
                        int startOffset = 0;
                        if (prevDistance < distanceThreshold) startOffset -= (int)((distanceThreshold - prevDistance)/insertCost);
                        double currentDistance = prevDistance - startOffset*insertCost;
                        while (currentDistance <= distanceThreshold) { // Match detected. Create annotation.

                            int matchedRegionEnd = j;
                            int matchedRegionStart = startOffset + (j) -(string.length()+insertionsPrevCol[string.length()]-deletionsPrevCol[string.length()]);

                            // System.out.println(string + "- Offset: " + startOffset + " ; start: " + matchedRegionStart + "end: " + matchedRegionEnd + " ; distance: " + currentDistance);

                            putAnnotation(annotationSet, lookup, string, 
                                    currentDistance, matchedRegionStart, 
                                    matchedRegionEnd);
                        
                            startOffset++;
                            if (startOffset > 0) currentDistance += deleteCost;
                            else currentDistance -= insertCost;
                        }
                    }
                }
                else {
//                    System.err.println("BWP Gazetteer should not be used with" +
//                                    "'wholeWordsOnly == true' and  'avoidOverlapingAnnotations == false'. " +
//                                    "Please reconfigure the parameters of BWPGazetteer to " +
//                                    "'wholeWordsOnly == true' and  'avoidOverlapingAnnotations == true', and the results" +
//                                    "will be the same as expected with " +
//                                    "'wholeWordsOnly == true' and  'avoidOverlapingAnnotations == false'");                            
//                            try { 
//                                if (!Character.isWhitespace(text.charAt(matchedRegionStart)) 
//                                        && !Character.isWhitespace(text.charAt(matchedRegionEnd-1))
//                                        && ((matchedRegionStart == 0 && matchedRegionEnd == text.length())||
//                                            (matchedRegionStart == 0 && Character.isWhitespace(text.charAt(matchedRegionEnd)))||
//                                            (Character.isWhitespace(text.charAt(matchedRegionStart-1)) && matchedRegionEnd == text.length())||
//                                            (Character.isWhitespace(text.charAt(matchedRegionStart-1)) && Character.isWhitespace(text.charAt(matchedRegionEnd)))
//                                        )) {
//                                
//                                    putAnnotation(annotationSet, lookup, string, 
//                                                currentDistance, matchedRegionStart, 
//                                                matchedRegionEnd);                   
//                                }
//                            } catch (Exception e) {
//                                //System.out.println(e.toString());
//                                // throw new LuckyException(e.toString());
//                            } 
                }
                            
                            
                        
            }    
                    
            prevPrevDistance = distancesPrevCol[string.length()];
            for (int i=1; i<string.length()+1; i++) {
                distancesPrevCol[i] = distancesCol[i];
                insertionsPrevCol[i] = insertionsCol[i];
                deletionsPrevCol[i] = deletionsCol[i];
                
            }
        }
    }
    
    private void putAnnotation(AnnotationSet annotationSet, Lookup lookup, String matchedString, 
                                double distance, int matchedRegionStart, 
                                int matchedRegionEnd) {
        FeatureMap fm = Factory.newFeatureMap();
        fm.put(LOOKUP_MAJOR_TYPE_FEATURE_NAME, lookup.majorType);
        if (null!= lookup.oClass && null!=lookup.ontology){
            fm.put(LOOKUP_CLASS_FEATURE_NAME,lookup.oClass);
            fm.put(LOOKUP_ONTOLOGY_FEATURE_NAME,lookup.ontology);
        }
        if(null != lookup.minorType) {
            fm.put(LOOKUP_MINOR_TYPE_FEATURE_NAME, lookup.minorType);
            if(null != lookup.languages)
                fm.put("language", lookup.languages);
        }
        if(null != lookup.features) fm.putAll(lookup.features);
        fm.put("matchedString", matchedString);
        fm.put("distance", distance);
        fm.put("normalizedDistance", (distance/matchedString.length()));

        try {                    
            annotationSet.add(new Long(matchedRegionStart),
                            new Long(matchedRegionEnd),
                            LOOKUP_ANNOTATION_TYPE,
                            fm);
        } catch(InvalidOffsetException ioe) {
            //throw new LuckyException(ioe.toString());
        } 
    }

} // class BWPGazetteer

    
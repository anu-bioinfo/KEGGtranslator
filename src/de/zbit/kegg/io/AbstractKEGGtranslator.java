/*
 * $Id: AbstractKEGGtranslator.java 410 2015-09-13 04:49:20Z draeger $
 * $URL: https://rarepos.cs.uni-tuebingen.de/svn/KEGGconverter/trunk/src/de/zbit/kegg/io/AbstractKEGGtranslator.java $
 * ---------------------------------------------------------------------
 * This file is part of KEGGtranslator, a program to convert KGML files
 * from the KEGG database into various other formats, e.g., SBML, GML,
 * GraphML, and many more. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/KEGGtranslator> to
 * obtain the latest version of KEGGtranslator.
 *
 * Copyright (C) 2011-2015 by the University of Tuebingen, Germany.
 *
 * KEGGtranslator is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.kegg.io;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import de.zbit.kegg.KEGGtranslatorOptions;
import de.zbit.kegg.KeggTools;
import de.zbit.kegg.Translator;
import de.zbit.kegg.api.KeggInfos;
import de.zbit.kegg.api.cache.KeggInfoManagement;
import de.zbit.kegg.parser.KeggParser;
import de.zbit.kegg.parser.pathway.Entry;
import de.zbit.kegg.parser.pathway.EntryType;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.kegg.parser.pathway.Reaction;
import de.zbit.kegg.parser.pathway.ReactionComponent;
import de.zbit.util.ArrayUtils;
import de.zbit.util.EscapeChars;
import de.zbit.util.StringUtil;
import de.zbit.util.Utils;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.util.progressbar.AbstractProgressBar;
import de.zbit.util.progressbar.ProgressBar;


/**
 * Abstract superclass for Kegg translators. All non-ouput-format specific
 * stuff should be implemented into this class.
 * 
 * @author Clemens Wrzodek
 * @since 1.0
 * @version $Rev: 410 $
 */
public abstract class AbstractKEGGtranslator<OutputFormat> implements KEGGtranslator <OutputFormat> {
  
  /**
   * A {@link Logger} for this class.
   */
  public static final transient Logger log = Logger.getLogger(AbstractKEGGtranslator.class.getName());
  
  /**
   * SBPreferences object to store all preferences for this class.
   */
  protected SBPreferences prefs = SBPreferences.getPreferencesFor(KEGGtranslatorOptions.class);
  
  /**
   * Retrieve annotations from Kegg or use purely information available in the
   * document.
   */
  private boolean retrieveKeggAnnots = true;
  
  /**
   * Remove single, not linked nodes/species
   * Defauls: Graphical representation: true, functional: false.
   */
  private boolean removeOrphans = false;
  
  /**
   * If true, all nodes in white color (except for small molecules/ compounds)
   * will be removed from the graph. Kegg colors all nodes, which do NOT
   * occur in the current species in white. Removing these nodes is HEAVILY
   * recommended if you want to use the SBML document for simulations.
   * 
   * Set this node to false if you convert generic pathways (not species
   * specific), since they ONLY contain white nodes.
   * 
   * Defauls: Graphical representation: false, functional: true.
   */
  private boolean removeWhiteNodes = true;
  
  /**
   * If true, missing reactants and enzymes for reactions will be retrieved
   * from the KEGG-DB and added to the result file.
   * REQUIRES: {@link #retrieveKeggAnnots}
   */
  protected boolean autocompleteReactions=true;
  
  /**
   * Check the atom balance and write the result to
   * the notes, if reaction is unbalanced.
   */
  protected boolean checkAtomBalance = false;
  
  /**
   * If true, removes all entries that are referring to other pathways.
   */
  protected boolean removePathwayReferences=false;
  
  /**
   * Selector that allows to change the way how translated entries
   * should be labeled.
   * XXX: Implementing classes must implement this functionality!
   * You may use the {@link #getNameForEntry(Entry)}
   * function for that.
   */
  protected KEGGtranslatorOptions.NODE_NAMING nameToAssign = KEGGtranslatorOptions.NODE_NAMING.INTELLIGENT;
  
  /**
   * If true, show the chemical Formula (e.g. "C6H12OH") instead of
   * the actual name for all compounds.
   */
  protected boolean showFormulaForCompounds = false;
  
  /**
   * This manager uses a cache and retrieved informations from the KeggDB. By
   * using the cache, it is very fast in retrieving informations.
   */
  protected static KeggInfoManagement manager;
  
  /**
   * A flag, if the last sbml file that has been written by this class was
   * overwritten. This variable is used by the BatchConverter.
   */
  protected boolean lastFileWasOverwritten = false;
  
  /**
   * Remembers the last pathway that has been translated. Just remembers
   * the core {@link Pathway} object, no {@link Entry}s, Reactions,
   * Relations, etc. included.
   */
  protected Pathway lastTranslatedPathway = null;
  
  /**
   * Contains all ids already assigned to an element in, e.g., the sbml document.
   * Used for avoiding giving the same id to two or more different elements.
   */
  protected Set<String> SIds = new HashSet<String>();
  
  /**
   * ProgressBar for KEGG translation
   */
  protected AbstractProgressBar progress = null;
  
  
  /*===========================
   * CONSTRUCTORS
   * ===========================*/
  
  /**
   * @param manage
   */
  public AbstractKEGGtranslator(KeggInfoManagement manage) {
    if ((manager == null) && (manage == null)) {
      Translator.getManager();
    } else if (manage != null) {
      setKeggInfoManager(manage);
    }
    
    loadPreferences();
  }
  
  
  /*===========================
   * Getters and Setters
   * ===========================*/
  
  /**
   * See {@link #retrieveKeggAnnots}
   * @return
   */
  public boolean isRetrieveKeggAnnots() {
    return retrieveKeggAnnots;
  }
  /**
   * @param retrieveKeggAnnots - see {@link #retrieveKeggAnnots}.
   */
  public void setRetrieveKeggAnnots(boolean retrieveKeggAnnots) {
    this.retrieveKeggAnnots = retrieveKeggAnnots;
  }
  
  /**
   * See {@link #removeOrphans}
   * @return
   */
  public boolean isRemoveOrphans() {
    return removeOrphans;
  }
  /**
   * @param removeOrphans - see {@link #removeOrphans}.
   */
  public void setRemoveOrphans(boolean removeOrphans) {
    this.removeOrphans = removeOrphans;
  }
  
  /**
   * See {@link #removeWhiteNodes}
   * @return
   */
  public boolean isRemoveWhiteNodes() {
    return removeWhiteNodes;
  }
  /**
   * @param removeWhiteNodes - see {@link #removeWhiteNodes}.
   */
  public void setRemoveWhiteNodes(boolean removeWhiteNodes) {
    this.removeWhiteNodes = removeWhiteNodes;
  }
  
  /**
   * @param b
   */
  public void setShowFormulaForCompounds(boolean b) {
    showFormulaForCompounds = b;
  }
  
  /**
   * @param b
   */
  public void setRemovePathwayReferences(boolean b) {
    removePathwayReferences = b;
  }
  
  /**
   * See {@link #nameToAssign}
   * @param node_naming how to label translated entries.
   */
  public void setNameToAssign(KEGGtranslatorOptions.NODE_NAMING node_naming) {
    this.nameToAssign = node_naming;
  }
  
  /**
   * See {@link #autocompleteReactions}
   * @return
   */
  public boolean isAutocompleteReactions() {
    return autocompleteReactions;
  }
  
  /**
   * @param autocompleteReactions
   * @see #autocompleteReactions
   */
  public void setAutocompleteReactions(boolean autocompleteReactions) {
    this.autocompleteReactions = autocompleteReactions;
  }
  
  /**
   * See {@link #checkAtomBalance}
   * @param b
   */
  public void setCheckAtomBalance(boolean b) {
    checkAtomBalance = b;
  }
  
  /**
   * See {@link #manager}
   * @param manage
   */
  public static void setKeggInfoManager(KeggInfoManagement manage) {
    manager = manage;
  }
  /**
   * @return - see {@link #manager}.
   */
  public static KeggInfoManagement getKeggInfoManager() {
    return manager;
  }
  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isLastFileWasOverwritten() {
    return lastFileWasOverwritten;
  }
  
  /**
   * Returns the last pathway that has been translated. Just remembers
   * the core {@link Pathway} object, no {@link Entry}s, Reactions,
   * Relations, etc. included.
   * @return the lastTranslatedPathway
   */
  @Override
  public Pathway getLastTranslatedPathway() {
    return lastTranslatedPathway;
  }
  
  /**
   * Set a progressBar that should be used to display the
   * status of the conversion.
   * @param progressBar
   */
  public synchronized void setProgressBar(AbstractProgressBar progressBar) {
    if (progress!=null && progressBar!=null) {
      // It's very dirty and non-thread sage to exchange the progress-bar
      // in an ongoing translation! This is just a quick-hack trying to 
      // hand-over the state of the old bar to the new one. Remember, that
      // the state may change just in the moment the next line is executed!
      progressBar.setCallNr(progress.getCallNumber());
      progressBar.setNumberOfTotalCalls(progress.getNumberOfTotalCalls());
    }
    this.progress = progressBar;
  }
  
  
  /*===========================
   * FUNCTIONS
   * ===========================*/
  
  /** Load the default preferences from the SBPreferences object. */
  private void loadPreferences() {
    removeOrphans = KEGGtranslatorOptions.REMOVE_ORPHANS.getValue(prefs);
    retrieveKeggAnnots = !KEGGtranslatorOptions.OFFLINE_MODE.getValue(prefs);
    removeWhiteNodes = KEGGtranslatorOptions.REMOVE_WHITE_GENE_NODES.getValue(prefs);
    autocompleteReactions = KEGGtranslatorOptions.AUTOCOMPLETE_REACTIONS.getValue(prefs);
    checkAtomBalance = KEGGtranslatorOptions.CHECK_ATOM_BALANCE.getValue(prefs);
    nameToAssign = KEGGtranslatorOptions.GENE_NAMES.getValue(prefs);
    showFormulaForCompounds = KEGGtranslatorOptions.SHOW_FORMULA_FOR_COMPOUNDS.getValue(prefs);
    removePathwayReferences = KEGGtranslatorOptions.REMOVE_PATHWAY_REFERENCES.getValue(prefs);
  }
  
  
  /**
   * Preprocesses the given pathway, according to current settings/options.
   * Eventually
   * <ul><li>precaches all kegg ids</li>
   * <li>autocomplete reactions</li>
   * <li>remove orphans</li>
   * <li>remove white nodes</li>
   * <li>remove pathway-reference nodes</li>
   * </ul>
   * @param p {@link Pathway}
   */
  private void preProcessPathway(Pathway p) {
    boolean completeAndCacheReactions = considerReactions()&&autocompleteReactions;
    //TODO: or should auto... be set in the qual class?
    
    if (!retrieveKeggAnnots) {
      KeggInfoManagement.offlineMode = true;
    } else {
      KeggInfoManagement.offlineMode = false;
      
      // Remove pathway references
      if (removePathwayReferences) {
        KeggTools.removePathwayEntries(p);
      }
      
      // Prefetch kegg information (enormous speed improvement).
      log.info("Fetching information from KEGG online resources... ");
      KeggTools.preFetchInformation(p,manager,completeAndCacheReactions, progress);
      
      // Auto-complete the reaction by adding all substrates, products and enzymes.
      if (completeAndCacheReactions) {
        KeggTools.autocompleteReactions(p, manager, true);
        
        // Auto-completion requires API-infos and also adds new entries
        // => preFetch twice.
        KeggTools.preFetchInformation(p,manager,completeAndCacheReactions, progress);
      }
      
      // In KGML, the stoichiometry of reaction components is missing.
      // The following completes the stoichiometry from the equation.
      if (considerReactions()) {
        KeggTools.parseStoichiometryFromEquations(p,manager);
      }
      
      log.info("Information fetched. Translating pathway... ");
    }
    
    
    // Skip it, if it's white
    if (removeWhiteNodes) {
      KeggTools.removeWhiteNodes(p);
    }
    
    // Preprocess pathway (remove orphans after autocompletion and others)
    if (removeOrphans) {
      KeggTools.removeOrphans(p, considerRelations(),considerReactions());
    }
  }
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.io.KEGGtranslator#translate(de.zbit.kegg.parser.pathway.Pathway, java.lang.String)
   */
  @Override
  public boolean translate(Pathway p, String outFile) {
    OutputFormat doc = translate(p);
    
    if (new File(outFile).exists()) {
      // Remember that file was already there.
      lastFileWasOverwritten = true;
    }
    
    return writeToFile(doc, outFile);
  }
  
  
  /**
   * Translates the given pathway to the target document.
   * @param p - the Kegg Pathway.
   * @return OutputFormat
   */
  @Override
  public OutputFormat translate(Pathway p) {
    // REMARK: This class is and must be called by all other translate functions.
    preProcessPathway(p);
    
    // Reset lists and buffers.
    SIds = new HashSet<String>(); // Reset list of given SIDs. These are being remembered to avoid double ids.
    
    // Remember just the pathway object with core information for later information
    //lastTranslatedPathway = new Pathway(p.getName(), p.getOrg(), p.getNumber(), p.getTitle(), p.getImage(), p.getLink());
    // Since some methods really need the whole document, we now store it.
    lastTranslatedPathway = p;
    
    OutputFormat doc = null;
    try {
      doc = translateWithoutPreprocessing(p);
    } catch (Throwable t) {
      t.printStackTrace();
      log.log(Level.SEVERE, "Unhandled exception during translation!", t);
    }
    
    // Remember already queried objects
    // XXX: Disabled, because writing after every conversion is very time-consuming.
    // Should be considered by calling classes when to write the cache.
    //if (getKeggInfoManager().hasChanged()) {
    //KeggInfoManagement.saveToFilesystem(KEGGtranslator.cacheFileName, getKeggInfoManager());
    //}
    
    return doc;
  }
  
  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(AbstractKEGGtranslator.class.getName());
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.io.KEGGtranslator#translate(java.lang.String, java.lang.String)
   */
  @Override
  public void translate(String infile, String outfile) throws Exception {
    logger.fine("Reading kegg pathway...");
    Pathway p = KeggParser.parse(infile).get(0);
    translate(p, outfile);
  }
  
  /**
   * This method converts a given KGML file into the
   * specified {@code OutputFormat}.
   * 
   * @param f the input file.
   * @return the generated document in {@code OutputFormat}.
   * @throws IOException if the input file is not readable.
   */
  @Override
  public OutputFormat translate(File f) throws IOException {
    if (f.exists() && f.isFile() && f.canRead()) {
      List<Pathway> l;
      try {
        l = KeggParser.parse(f.getAbsolutePath());
      } catch (Exception e) {
        throw new IOException(MessageFormat.format("Cannot translate input file {0}.", f.getAbsolutePath()), e);
      }
      
      if (l.size() > 0) {
        Pathway p = l.get(0);
        OutputFormat doc = translate(p);
        return doc;
      } else {
        throw new IOException(MessageFormat.format("Empty or invalid input file {0}.", f.getAbsolutePath()));
      }
    }
    throw new IOException(MessageFormat.format("Invalid input file {0}.", f.getAbsolutePath()));
  }
  
  /**
   * Initializes the given, or a new progressBar with the number of
   * entries. Optionally, the number of relations or reactions can
   * be added.
   * @param p The KEGG Pathway to translate
   * @param addRelations if true, also adds the number of relations
   * to the number of total calls.
   * @param addReactions if true, also adds the number of reactions
   * to the number of total calls.
   */
  protected void initProgressBar(Pathway p, boolean addRelations, boolean addReactions) {
    // Initialize a progress bar.
    int totalCalls = p.getEntries().size(); // +p.getRelations().size(); // Relations are very fast.
    if (addRelations) {
      totalCalls += p.getRelations().size();
    }
    if (addReactions) {
      totalCalls += p.getReactions().size();
    }
    // if (!retrieveKeggAnnots) aufrufeGesamt+=p.getRelations().size();
    if (progress == null) {
      progress = new ProgressBar(totalCalls + 1);
      ((ProgressBar) progress).setPrintInOneLine(true);
    } else {
      progress.reset();
      progress.setNumberOfTotalCalls(totalCalls + 1);
    }
    progress.DisplayBar();
  }
  
  /**
   * Checks whether a given reaction has at least one product and substrate.
   * @param reaction
   * @param parentPathway
   * @return
   */
  public static boolean reactionHasAtLeastOneSubstrateAndProduct(Reaction reaction, Pathway parentPathway) {
    // Skip reaction if it has either no reactants or no products.
    boolean hasAtLeastOneReactantAndProduct = false;
    for (ReactionComponent rc : reaction.getSubstrates()) {
      Entry spec = parentPathway.getEntryForReactionComponent(rc);
      if (spec == null || spec.getCustom() == null) {
        continue;
      }
      hasAtLeastOneReactantAndProduct = true;
      break;
    }
    if (!hasAtLeastOneReactantAndProduct) {
      return false;
    }
    
    hasAtLeastOneReactantAndProduct = false;
    for (ReactionComponent rc : reaction.getProducts()) {
      Entry spec = parentPathway.getEntryForReactionComponent(rc);
      if (spec == null || spec.getCustom() == null) {
        continue;
      }
      hasAtLeastOneReactantAndProduct = true;
      break;
    }
    
    if (!hasAtLeastOneReactantAndProduct) {
      return false;
    }
    return true;
  }
  
  /**
   * Returns true if and only if the given entry refers to a group node.
   * @param e
   * @return
   */
  public static boolean isGroupNode(Entry e) {
    EntryType t = e.getType();
    return ((t.equals(EntryType.group) || e.getName().toLowerCase().trim().startsWith("group:")) && e.hasComponents());
  }
  
  /**
   * @param entry
   * @return {@link String} to use as label for the {@link Entry}.
   * @see #getNameForEntry(Entry, KeggInfos...)
   */
  protected String getNameForEntry(Entry entry) {
    return getNameForEntry(entry, (KeggInfos)null);
  }
  
  /**
   * Convenient method to be called by extending classes that
   * returns the name to assign for an entry, based on the
   * current user selection ({@link #nameToAssign}).
   * @param entry
   * @param infos already queried {@link KeggInfos}
   * @return {@link String} to use as label for the {@link Entry}.
   */
  protected String getNameForEntry(Entry entry, KeggInfos... infos) {
    
    // Query API
    if ((infos == null) || (infos.length == 0) ||
        ((infos.length == 1) && (infos[0] == null))) {
      List<KeggInfos> list = new LinkedList<KeggInfos>();
      for (String ko_id:entry.getName().split(" ")) {
        // Do not consider group nodes
        if (ko_id.trim().equalsIgnoreCase("undefined") || entry.hasComponents()) {
          continue;
        }
        
        list.add(KeggInfos.get(ko_id, manager));
      }
      infos = list.toArray(new KeggInfos[0]);
    }
    
    // Concatenate names and check for compound option
    StringBuilder name = new StringBuilder();
    for (int i=0; i<infos.length; i++) {
      if (infos[i]==null || !infos[i].queryWasSuccessfull()) {
        continue;
      }
      if (name.length()>0 && name.charAt(name.length()-1)!=';') {
        name.append(';'); // Add gene separator
      }
      
      if (showFormulaForCompounds && infos[i].getFormulaDirectOrFromSynonym(manager)!=null) {
        name.append(infos[i].getFormulaDirectOrFromSynonym(manager));
      } else if (nameToAssign.equals(KEGGtranslatorOptions.NODE_NAMING.INTELLIGENT_WITH_EC_NUMBERS) &&
          (!entry.getType().equals(EntryType.map)) && infos[i].isSetECcodes()){
        // If EC codes preferred, not is no PW-reference and has ECcodes, take them.
        name.append(StringUtil.implode(infos[i].getECcodes(), ","));
      } else if (infos[i].getKegg_ID().startsWith("br:")){
        name.append(infos[i].getDefinition().replace(";\n", ", ").replace(";", ","));
      } else if (infos[i].getNames()!=null){
        name.append(infos[i].getNames().replace(";\n", ", ").replace(";", ","));
      }
    }
    
    return getNameForEntry(entry, name.toString());
  }
  
  
  /**
   * Convenient method to be called by extending classes that
   * returns the name to assign for an entry, based on the
   * current user selection ({@link #nameToAssign}).
   * <p>Note: please use preferred method
   * {@link #getNameForEntry(Entry, KeggInfos...)}.
   * @param entry
   * @param names already API-queried names. Synonyms for same
   * gene are ", " separated and different genes contained in
   * same entry are ";" separated. Furthermore, requires to
   * check and react to the {@link #showFormulaForCompounds}
   * option in advance!
   * @return {@link String} to use as label for the {@link Entry}.
   * @see #getNameForEntry(Entry, KeggInfos...)
   */
  protected String getNameForEntry(Entry entry, String names) {
    // Please note further: kegg splits compound-synonyms by ";", not ",".
    if (nameToAssign.equals(KEGGtranslatorOptions.NODE_NAMING.FIRST_NAME_FROM_KGML)) {
      String name = entry.getName();
      if (entry.hasGraphics() && entry.getGraphics().getName()!=null &&
          entry.getGraphics().getName().length()>1) {
        name = entry.getGraphics().getName();
      }
      
      // SPECIAL CASES FOR MAPS AND BRITE
      if (entry.getType().equals(EntryType.map)) {
        // Pathway references are to be treated separately
        return trimSpeciesSuffix(name);
      } else if (entry.getName().startsWith("br:")) {
        // Kegg brite groups contain species suffixes
        names = trimSpeciesSuffix(names);
      }
      //--------
      return firstName(name);
      
    } else if ((names != null) && (names.length() > 0)) {
      
      // Pathway references are to be treated separately
      // SPECIAL CASES FOR MAPS AND BRITE
      if (entry.getType().equals(EntryType.map)) {
        return trimSpeciesSuffix(names);
      } else if (entry.getName().startsWith("br:")) {
        // Kegg brite groups contain species suffixes
        names = trimSpeciesSuffix(names);
      }
      //--------
      
      if (nameToAssign.equals(KEGGtranslatorOptions.NODE_NAMING.FIRST_NAME)) {
        return firstName(names);
      }
      
      // We need to split all genes for further naming options
      String[] multiNames = names.split(";"); // components are not trimmed!
      
      if (nameToAssign.equals(KEGGtranslatorOptions.NODE_NAMING.SHORTEST_NAME)) {
        return shortenName(ArrayUtils.implode(multiNames, ", "));
        
      } else if (nameToAssign.equals(KEGGtranslatorOptions.NODE_NAMING.ALL_FIRST_NAMES)) {
        Set<String> firstNames = new HashSet<String>();
        for (String name: multiNames) {
          firstNames.add(firstName(name));
        }
        
        // return separated by ';' to indicate different genes!
        return ArrayUtils.implode(firstNames, "; ");
        
      } else if (nameToAssign.equals(KEGGtranslatorOptions.NODE_NAMING.INTELLIGENT) ||
          nameToAssign.equals(KEGGtranslatorOptions.NODE_NAMING.INTELLIGENT_WITH_EC_NUMBERS) ) {
        // Shortest for compounds
        if (entry.getType().equals(EntryType.compound)) {
          return shortenName(ArrayUtils.implode(multiNames, ", "));
        }
        
        // Try to detect gene families
        Set<String> firstNames = new HashSet<String>();
        String veryFirst = null;
        for (String name: multiNames) {
          String first = firstName(name);
          if (veryFirst==null || veryFirst.length()<1) {
            veryFirst = first;
          }
          firstNames.add(first);
        }
        if (firstNames.size()>1) {
          String LCP = StringUtil.getLongestCommonPrefix(firstNames.toArray(new String[0]),true);
          // Require at least 3 chars for family identifiers
          if (LCP!=null && LCP.length()>2) {
            String removedPart = firstNames.iterator().next().substring(LCP.length());
            if (Utils.isNumber(removedPart, true)) {
              // ALG13 and ALG14 have the common prefix ALG1 => remove 1
              while (Character.isDigit(LCP.charAt(LCP.length()-1))) {
                LCP = LCP.substring(0, LCP.length()-1);
                if (LCP.length()<2) {
                  break;
                }
              }
              
            }
            
            if (LCP.length()>2) {
              return LCP;
            }
          }
        }
        
        // First for single genes or in doubt.
        if (veryFirst!=null && veryFirst.length()>0) {
          return firstNames.iterator().next();
        }
        
      }
    }
    
    // In doubt, return first nicest...
    if ((names != null) && (names.length() > 0)) {
      // From API
      return names;
    } else {
      // From KGML
      String name = entry.getName();
      if (entry.hasGraphics() && (entry.getGraphics().getName() != null) &&
          (entry.getGraphics().getName().length() > 0)) {
        name = entry.getGraphics().getName();
      }
      if (name.toLowerCase().startsWith("undefined")) {
        // Rename group-nodes to "Group".
        name = "Group";
      }
      return firstName(name);
    }
  }
  
  
  /**
   * @param name
   * @return
   */
  private static String trimSpeciesSuffix(String name) {
    //name is e.g. "Glycine, serine and threonine metabolism - Enterococcus faecalis"
    // => remove species and don't split at comma.
    int pos = name.lastIndexOf(" - ");
    if (pos > 0) {
      name = name.substring(0, pos).trim();
    }
    return name;
  }
  
  /**
   * Escapes all HTML-tags in the given string and
   * replaces new lines with a space.
   * @param text
   * @return
   */
  public static String formatTextForHTMLnotes(String text) {
    if (text == null) {
      return "";
    }
    return EscapeChars.forHTML(text.replace('\n', ' '));
  }
  
  
  /**
   * Shorten a given Entry full-name.
   * <p>Convert e.g. "PCK1, MGC22652, PEPCK-C, PEPCK1, PEPCKC..."
   * to "PCK1". Splits at ", " not at "," to preserve entries
   * like "Ins(1,4,5)P3".
   * <p>Returns the shortest resulting name.
   * @param name
   * @return short name.
   */
  protected static String shortenName(String name) {
    /*if (name.contains(",")) {
      return name.substring(0, name.indexOf(",")-1);
    }*/
    String[] names = name.split(", ");
    for (String name2: names) {
      name2 = name2 == null ? null : name2.trim();
      // E.g. 308800 has name "Tyr, C" and "C" is not that helpful
      // => At least 2 digits in name and shortest one.
      if (name2!=null && (name2.length() > 1) && name2.length()<name.length()) {
        name = name2;
      }
    }
    
    return name;
  }
  
  /**
   * Returns the first gene symbol from a (KEGG) list
   * of symbols.
   * @param name
   * @return first name
   */
  protected static String firstName(String name) {
    // Extract very first given name.
    name = name.trim();
    char[] names = name.toCharArray();
    int i=1;
    for (; i<name.length(); i++) {
      if (names[i]==';')
      {
        break; // Multiple genes in one node
      }
      // Multiple names for same gene, don not break, e.g. "Ins(1,4,5)P3".
      if (names[i]==',' && (i==(name.length()-1) || names[i+1]==' ')) {
        break;
      }
    }
    if (i>1) {
      return name.substring(0, i);
    } else {
      return name;
    }
  }
  
  
  /**
   * Generates a valid SId from a given name. If the name already is a valid
   * SId, the name is returned. If the SId already exists in this document,
   * "_&lt;number>" will be appended and the next free number is being assigned.
   * => See SBML L2V4 document for the Definition of SId. (Page 12/13)
   * 
   * @param name
   * @return SId
   */
  protected String NameToSId(String name) {
    /*
     * letter = a-z,A-Z; digit = 0-9; idChar = (letter | digit | _ );
     * SId = ( letter | _ ) idChar*
     */
    String ret;
    if (name == null || name.trim().length() == 0) {
      ret = incrementSIdSuffix("SId");
      SIds.add(ret);
    } else {
      name = name.trim();
      StringBuilder ret2 = new StringBuilder(name.length()+4);
      char c = name.charAt(0);
      
      // Must start with letter or '_'.
      if (!(isLetter(c) || (c == '_'))) {
        ret2.append("SId_");
      } else {
        ret2.append(c);
      }
      
      // May contain letters, digits or '_'
      for (int i = 1; i < name.length(); i++) {
        c = name.charAt(i);
        if (c==' ') {
          c='_'; // Replace spaces with "_"
        }
        
        if (isLetter(c) || Character.isDigit(c) || (c == '_')) {
          ret2.append(c);
        } // else: skip invalid characters
      }
      
      // Make unique
      ret = ret2.toString();
      if (SIds.contains(ret)) {
        ret = incrementSIdSuffix(ret);
      }
      SIds.add(ret);
    }
    
    return ret;
  }
  
  /**
   * Returns true if c is out of A-Z or a-z.
   * @param c
   * @return
   */
  private static boolean isLetter(char c) {
    // Unfortunately Character.isLetter also accepts symbols that SBML doesn't.
    // a-z or A-Z
    return (c>=97 && c<=122) || (c>=65 && c<=90);
  }
  
  /**
   * Appends "_<Number>" to a given String. <Number> is being set to the next
   * free number, so that this sID is unique in this sbml document. Should
   * only be called from "NameToSId".
   * 
   * @return
   */
  private String incrementSIdSuffix(String prefix) {
    int i = 1;
    String aktString = prefix + "_" + i;
    while (SIds.contains(aktString)) {
      aktString = prefix + "_" + (++i);
    }
    return aktString;
  }
  
  
  
  /**
   * If false, all relations in the document will be skipped. Just like most
   * of the other very-basic converters.
   * NOTE: Makes sense, e.g. in KEGG2SBML (or non-graphic-based-converters).
   * Kegg2yGraph by default only considers relations.
   */
  abstract protected boolean considerRelations();
  
  /**
   * If false, all reactions in the document will be skipped.
   * NOTE: Makes sense, e.g. in KEGG2SBMLqual.
   */
  abstract protected boolean considerReactions();
  
  /**
   * Write the translated document to the given file.
   * @param doc the translated document
   * @param outFile the file to write
   * @return true if and only if everything went fine.
   */
  @Override
  public abstract boolean writeToFile(OutputFormat doc, String outFile);
  
  /**
   * Translate the pathway to the new format - assumes that all preprocessing
   * (precaching of ids, autocomplete reactions, remove orphans, etc.) has
   * already been performed.
   * 
   * <p>This method should not be called directly. Use any other translate method,
   * e.g. {@link #translate(Pathway)}.
   * 
   * @param p Pathway to translate
   * @return Translated pathway
   * @throws XMLStreamException
   */
  protected abstract OutputFormat translateWithoutPreprocessing(Pathway p) throws XMLStreamException;
  
}

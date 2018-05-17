/*
 * $Id: KEGG2SBMLqual.java 412 2015-09-21 20:39:51Z draeger $
 * $URL: https://rarepos.cs.uni-tuebingen.de/svn/KEGGconverter/trunk/src/de/zbit/kegg/io/KEGG2SBMLqual.java $
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
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.AbstractSBase;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBO;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.TidySBMLWriter;
import org.sbml.jsbml.ext.SBasePlugin;
import org.sbml.jsbml.ext.groups.Group;
import org.sbml.jsbml.ext.qual.Input;
import org.sbml.jsbml.ext.qual.InputTransitionEffect;
import org.sbml.jsbml.ext.qual.Output;
import org.sbml.jsbml.ext.qual.OutputTransitionEffect;
import org.sbml.jsbml.ext.qual.QualConstants;
import org.sbml.jsbml.ext.qual.QualModelPlugin;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Sign;
import org.sbml.jsbml.ext.qual.Transition;
import org.sbml.jsbml.util.ValuePair;

import de.zbit.cache.InfoManagement;
import de.zbit.kegg.KEGGtranslatorOptions;
import de.zbit.kegg.Translator;
import de.zbit.kegg.api.KeggInfos;
import de.zbit.kegg.api.cache.KeggInfoManagement;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.kegg.parser.KeggParser;
import de.zbit.kegg.parser.pathway.Entry;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.kegg.parser.pathway.Relation;
import de.zbit.kegg.parser.pathway.SubType;
import de.zbit.sbml.util.AnnotationUtils;
import de.zbit.util.DatabaseIdentifierTools;
import de.zbit.util.DatabaseIdentifiers;
import de.zbit.util.DatabaseIdentifiers.IdentifierDatabases;
import de.zbit.util.Utils;

/**
 * KEGG2SBML with a qualitative model (SBML L3 V1, using the SBML Qual extension,
 * also KGML2JSBMLqual, KEGG2QUAL, KGML2QUAL).
 * 
 * @author Finja B&uuml;chel
 * @author Clemens Wrzodek
 * @version $Rev: 412 $
 */
public class KEGG2SBMLqual extends KEGG2jSBML {
  /**
   * This prefix is prepended to all entities in the qualitative model
   * (species, groups, etc).
   */
  private static final String QUAL_SPECIES_PREFIX = "qual_";
  
  /**
   * Qual Namespace definition URL.
   */
  public static final String QUAL_NS = QualConstants.namespaceURI;
  
  /**
   * Unique identifier to identify this Namespace/Extension.
   */
  public static final String QUAL_NS_NAME = QualConstants.shortLabel;
  
  /**
   * If false, the result will contain ONLY a qual model with
   * qual species and transitions.
   * if true, the resulting SBML will contain species and reactions,
   * as well as qualSpecies and transitions.
   */
  private boolean considerReactions = false;
  
  /**
   * All transitions that are added to the model.
   * Identified as "inputQualitativeSpecies ouptutQualitativeSpecies [SBOterm]",
   * used to check for duplicates.
   */
  private Set<String> containedTransitions = new HashSet<String>();
  
  
  /**
   * @param document
   * @return true if the given document has at least one {@link QualitativeSpecies}.
   */
  public static boolean hasQualSpecies(SBMLDocument document) {
    if (document==null || !document.isSetModel()) {
      return false;
    }
    SBasePlugin qm = document.getModel().getExtension(KEGG2SBMLqual.QUAL_NS);
    if ((qm != null) && (qm instanceof QualModelPlugin)) {
      QualModelPlugin q = (QualModelPlugin) qm;
      if (!q.isSetListOfQualitativeSpecies()) {
        return false;
      }
      return q.getListOfQualitativeSpecies().size()>0;
    }
    return false;
  }
  
  /*===========================
   * CONSTRUCTORS
   * ===========================*/
  
  /**
   * Initialize a new KEGG2SBMLqual object, using a new Cache and a new KeggAdaptor.
   */
  public KEGG2SBMLqual() {
    this(new KeggInfoManagement());
  }
  
  /**
   * Initialize a new Kegg2jSBML object, using the given cache.
   * @param manager
   */
  public KEGG2SBMLqual(KeggInfoManagement manager) {
    super(manager);
    // Important to manifest that we NEED the relations
    // see considerRealtions()
    
    loadPreferences();
  }
  
  
  /*===========================
   * FUNCTIONS
   * ===========================*/
  
  /** Load the default preferences from the SBPreferences object. */
  private void loadPreferences() {}
  
  /**
   * 
   * @return the level and version of the SBML core (2,4)
   */
  @Override
  protected ValuePair<Integer, Integer> getLevelAndVersion() {
    return new ValuePair<Integer, Integer>(Integer.valueOf(3), Integer.valueOf(1));
  }
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.io.KEGG2jSBML#translateWithoutPreprocessing(de.zbit.kegg.parser.pathway.Pathway)
   */
  @Override
  protected SBMLDocument translateWithoutPreprocessing(Pathway p) throws XMLStreamException {
    
    // Don't forget to clear all previous caches
    containedTransitions.clear();
    
    // Translate to normal SBML
    SBMLDocument doc = super.translateWithoutPreprocessing(p);
    
    // Create qualitative model
    Model model = doc.getModel();
    QualModelPlugin qualModel = new QualModelPlugin(model);
    
    // Determine if this is a combined model (core + qual) or a pure qual model.
    boolean isCombindedModel = considerReactions();
    
    // Add extension and namespace to model
    doc.addNamespace(KEGG2SBMLqual.QUAL_NS_NAME, "xmlns", KEGG2SBMLqual.QUAL_NS);
    doc.getSBMLDocumentAttributes().put(QUAL_NS_NAME + ":required", (isCombindedModel? "false" : "true"));
    model.addExtension(KEGG2SBMLqual.QUAL_NS, qualModel);
    
    /* Until (INCLUDING) Version 2.2, if there were no relations, no qual species
     * have been created. The comment was as follows:
     * 
     * // Qualitative species are just created if transitions are available
     * // Reason: all species are in the file as normal SBML species. Following the
     * // qualitative species are not necessary
     */
    
    // Create qual species for every species
    if (p.getRelations().size()<1 && KEGGtranslatorOptions.REMOVE_ORPHANS.getValue(prefs)) {
      // We have no relations and REMOVE_ORPHANS is true => do nothing.
    } else {
      // Create the species (independent of the relations.
      createQualSpecies(p, qualModel);
    }
    
    // Give a warning if we have no relations.
    if (p.getRelations().size()<1) {
      log.fine("File does not contain any relations. Graph will look quite boring...");
    } else {
      for (Relation r : p.getRelations()) {
        addKGMLRelation(r, p, qualModel);
      }
    }
    
    if(!considerReactions()) {
      model.unsetListOfSpecies();
    }
    
    // Update (UNSET OLD METABOLIC and create new, qual-species related) layout extension
    if (addLayoutExtension) {
      KEGG2SBMLLayoutExtension.addLayoutExtension(p, doc, model, false, !isCombindedModel);
    }
    
    return doc;
  }
  
  /**
   * Creates a qual species for every entry in the pathway
   * (as a side effect, also for every species in the model).
   * 
   * @param p
   * @param qualModel
   */
  private void createQualSpecies(Pathway p, QualModelPlugin qualModel) {
    for (Entry e : p.getEntries()) {
      Object s = e.getCustom();
      if ((s != null) && (s instanceof Species)) {
        QualitativeSpecies qs = createQualitativeSpeciesFromSpecies((Species) s, qualModel);
        e.setCustom(qs);
      } else if ((s != null) && (s instanceof Group)) {
        Group updatedReferences = createQualitativeGroupFromGroup((Group) s);
        e.setCustom(updatedReferences);
        // Sinmply KEEP non-species objects (e.g., groups when using the group extension)
        //      } else {
        //        e.setCustom(null);
      }
    }
  }
  
  /**
   * 
   * @param r
   * @param p
   * @param qualModel
   * @return the created {@link Transition} or null, if there is a missing
   * component or other conflicts occur.
   * @throws XMLStreamException
   */
  public Transition addKGMLRelation(Relation r, Pathway p, QualModelPlugin qualModel) throws XMLStreamException {
    // create transition and add it to the model
    
    Entry eOne = p.getEntryForId(r.getEntry1());
    Entry eTwo = p.getEntryForId(r.getEntry2());
    
    NamedSBase qOne = eOne==null?null:(NamedSBase) eOne.getCustom();
    NamedSBase qTwo = eTwo==null?null:(NamedSBase) eTwo.getCustom();
    
    if (qOne==null || qTwo==null) {
      // Happens, e.g. when remove_pw_references is true and there is a
      // relation to this (now removed) node.
      log.fine("Relation with unknown or removed entry: " + r);
      return null;
    }
    
    Transition t = qualModel.createTransition(NameToSId("tr"));
    
    // Input
    Input in = t.createInput(NameToSId("in"), qOne.getId(), InputTransitionEffect.none);
    in.setMetaId("meta_" + in.getId());
    
    // Output
    Output out = t.createOutput(NameToSId("out"), qTwo.getId(), OutputTransitionEffect.assignmentLevel);
    
    //XXX: "function term" is intentionally not set in KEGG2X (info not provided).
    
    
    // Determine sign variable, SBO Terms and add MIRIAM URNs
    Sign sign = Sign.unknown;
    Set<Integer> SBOs = new HashSet<Integer>();
    List<SubType> subTypes = r.getSubtypes();
    
    CVTerm cv = new CVTerm(CVTerm.Qualifier.BQB_IS);
    if ((subTypes != null) && (subTypes.size() > 0)) {
      Collection<String> subTypeNames = r.getSubtypesNames();
      
      // Parse activations/ inhibitions separately for the sign
      if (subTypeNames.contains(SubType.INHIBITION) || subTypeNames.contains(SubType.REPRESSION)) {
        if (subTypeNames.contains(SubType.ACTIVATION) || subTypeNames.contains(SubType.EXPRESSION)) {
          sign = Sign.dual;
          in.setSBOTerm(168); // control is parent of inhibition and activation
          SBOs.add(168);
          
        } else {
          sign = Sign.negative;
          setSBOTerm(in, SBOMapping.getSBOTerm(SubType.INHIBITION));
          
        }
        
      } else if (subTypeNames.contains(SubType.ACTIVATION) || subTypeNames.contains(SubType.EXPRESSION)) {
        sign = Sign.positive;
        setSBOTerm(in, SBOMapping.getSBOTerm(SubType.ACTIVATION));
        
      } else if(subTypeNames.contains(SubType.STATE_CHANGE)) {
        setSBOTerm(in, SBOMapping.getSBOTerm(SubType.STATE_CHANGE));
        
      }
      
      // Add all subtypes as MIRIAM annotation
      for (String subType: subTypeNames) {
        Integer subSBO = SBOMapping.getSBOTerm(subType);
        if ((subSBO != null) && (subSBO > 0)) {
          cv.addResource(String.format("http://identifiers.org/%s/%s",
            KeggInfos.miriam_urn_sbo.substring(11, KeggInfos.miriam_urn_sbo.length() - 1),
            SBO.intToString(subSBO)));
          SBOs.add(subSBO);
        }
        Integer subGO = SBOMapping.getGOTerm(subType);
        if ((subGO != null) && (subGO > 0)) {
          String go = DatabaseIdentifiers.getMiriamURI(IdentifierDatabases.GeneOntology, Integer.toString(subGO));
          if (go != null) {
            cv.addResource(go);
          }
        }
        de.zbit.util.objectwrapper.ValuePair<String, Integer> subMI = SBOMapping.getMITerm(subType);
        if ((subMI != null) && (subMI.getB() != null) && (subMI.getB() > 0)) {
          String mi = DatabaseIdentifiers.getMiriamURI(IdentifierDatabases.GeneOntology, Integer.toString(subMI.getB()));
          if (mi != null) {
            cv.addResource(mi);
          }
        }
      }
      
      in.setSign(sign);
    }
    
    // Set SBO term and miriam URNs on transition.
    if (SBOs.size() > 0 ) {
      // Remove unspecific ones, try to get the specific ones
      if (SBOs.size() > 1) {
        SBOs.remove(SBOMapping.getSBOTerm(SubType.MISSING_INTERACTION));
      }
      if (SBOs.size() > 1) {
        // Remark: If activation and inhibition is set, 168 is always added as third sbo.
        SBOs.remove(SBOMapping.getSBOTerm(SubType.ACTIVATION));
        SBOs.remove(SBOMapping.getSBOTerm(SubType.INHIBITION));
      }
      if (SBOs.size() > 1) {
        SBOs.remove(SBOMapping.getSBOTerm(SubType.STATE_CHANGE));
      }
      if (SBOs.size() > 1) {
        SBOs.remove(SBOMapping.getSBOTerm(SubType.BINDING_ASSOCIATION));
      }
      if (SBOs.size() > 1) {
        SBOs.remove(SBOMapping.getSBOTerm(SubType.INDIRECT_EFFECT));
      }
      t.setSBOTerm(SBOs.iterator().next());
    }
    
    if (cv.getResourceCount() > 0) {
      // Use always "IS", because "methylation" and "activation"
      // can both share the attribute IS and don't need
      // to be annotated as different versions ("HAS_VERSION").
      //setBiologicalQualifierISorHAS_VERSION(cv);
      t.addCVTerm(cv);
    }
    t.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS_DESCRIBED_BY, AnnotationUtils.convertURN2URI(KeggInfos.miriam_urn_eco + "ECO%3A0000313")));
    
    // Add additional miriam identifiers
    if (r.isSetDatabaseIdentifiers()) {
      List<CVTerm> cvTerms = DatabaseIdentifierTools.getCVTerms(r.getDatabaseIdentifiers(), null);
      if ((cvTerms != null) && (cvTerms.size() > 0)) {
        for (CVTerm cvTerm : cvTerms) {
          t.addCVTerm(cvTerm);
        }
      }
    }
    
    // Add the source of this transition, if it was NOT from kegg
    if (r.isSetSource()) {
      StringBuffer notes = new StringBuffer(KEGG2jSBML.notesStartString);
      notes.append(String.format("This transition is defined by '%s'.\n", r.getSource()));
      notes.append(KEGG2jSBML.notesEndString);
      try {
        t.setNotes(notes.toString());
      } catch (Throwable exc) {
        logger.warning(MessageFormat.format(
          "Cannot write notes for species ''{0}'' because of {1}: {2}.",
          t.getId(), exc.getClass().getName(), Utils.getMessage(exc)));
      }
    }
    
    // Don't add same relations twice
    String transitionIdentifier = in.getQualitativeSpecies() + " " + out.getQualitativeSpecies() + " " + (t.isSetSBOTerm() ? t.getSBOTermID():"");
    if (!containedTransitions.add(transitionIdentifier)) {
      qualModel.getListOfTransitions().remove(t);
      t=null;
    }
    
    return t;
  }
  
  /**
   * Checks if there is already a qual species, matching the given species
   * and returns it. If not, creates a new qual species for the given
   * species.
   * @param species
   * @param qualModel
   * @return
   */
  private QualitativeSpecies createQualitativeSpeciesFromSpecies(Species species, QualModelPlugin qualModel) {
    String id = QUAL_SPECIES_PREFIX + species.getId();
    QualitativeSpecies qs = qualModel.getQualitativeSpecies(id);
    if(qs == null){
      qs = qualModel.createQualitativeSpecies(id, "meta_" + id, species);
      // Martjin told me on 2012-04-13 that for the validator, constant must be set to false.
      // leaving this unset results in invalid sbml.
      qs.setConstant(false);
    }
    return qs;
  }
  
  /**
   * 
   * @param group
   * @return
   */
  private Group createQualitativeGroupFromGroup(Group group) {
    String id = QUAL_SPECIES_PREFIX + group.getId();
    // This will create a new separate group for qual and
    // will later result in two groups. Unfortunately, no
    // one knows which one to use for the quantiative and which
    // for the qualitative model => Better append qual compoents
    // to the exisint group!
    //return KEGG2SBMLGroupExtension.cloneGroup(id, group, QUAL_SPECIES_PREFIX);
    
    KEGG2SBMLGroupExtension.cloneGroupComponents(group, QUAL_SPECIES_PREFIX);
    return group;
  }
  
  
  /**
   * Accepts values smaller than or equal to zero to unset the SBO term.
   * Else, sets the SBO term to the given value.
   * @param sbase
   * @param term
   */
  private static void setSBOTerm(AbstractSBase sbase, int term) {
    if (term<=0) {
      sbase.unsetSBOTerm();
    } else {
      sbase.setSBOTerm(term);
    }
  }
  
  /**
   * Provides some direct access to KEGG2JSBML functionalities.
   * @param args
   * @throws Exception
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws XMLStreamException
   * @throws ClassNotFoundException
   */
  @SuppressWarnings({ "unchecked" })
  public static void main(String[] args) throws Exception {
    // Speedup Kegg2SBML by loading alredy queried objects. Reduces network
    // load and heavily reduces computation time.
    Format format = Format.SBML_QUAL;
    AbstractKEGGtranslator<SBMLDocument> k2s;
    KeggInfoManagement manager = null;
    if (new File(Translator.cacheFileName).exists()
        && new File(Translator.cacheFileName).length() > 1) {
      manager = (KeggInfoManagement) InfoManagement.loadFromFilesystem(Translator.cacheFileName);
    }
    k2s = (AbstractKEGGtranslator<SBMLDocument>) BatchKEGGtranslator.getTranslator(format, manager);
    // ---
    
    if (args != null && args.length > 0) {
      File f = new File(args[0]);
      if (f.isDirectory()) {
        // Directory mode. Convert all files in directory.
        BatchKEGGtranslator batch = new BatchKEGGtranslator();
        batch.setOrgOutdir(args[0]);
        if (args.length > 1) {
          batch.setChangeOutdirTo(args[1]);
        }
        batch.setTranslator(k2s);
        batch.setOutFormat(format);
        batch.parseDirAndSubDir();
        
      } else {
        // Single file mode.
        String outfile = args[0].substring(0,
          args[0].contains(".") ? args[0].lastIndexOf(".") : args[0].length())
          + ".sbml.xml";
        if (args.length > 1) {
          outfile = args[1];
        }
        
        Pathway p = KeggParser.parse(args[0]).get(0);
        try {
          k2s.translate(p, outfile);
        } catch (Throwable e) {
          e.printStackTrace();
        }
      }
      
      // Remember already queried objects (save cache)
      if (AbstractKEGGtranslator.getKeggInfoManager().hasChanged()) {
        KeggInfoManagement.saveToFilesystem(Translator.cacheFileName, AbstractKEGGtranslator.getKeggInfoManager());
      }
      
      return;
    }
    
    
    // Just a few test cases here.
    logger.info("Demo mode.");
    
    long start = System.currentTimeMillis();
    try {
      //k2s.translate("files/KGMLsamplefiles/hsa04010.xml", "files/KGMLsamplefiles/hsa04010.sbml.xml");
      //      k2s.translate("files/KGMLsamplefiles/hsa00010.xml", "files/KGMLsamplefiles/hsa00010.sbml.xml");
      
      SBMLDocument doc = k2s.translate(new File("files/KGMLsamplefiles/hsa04210.xml"));
      TidySBMLWriter.write(doc, "files/KGMLsamplefiles/hsa04210.sbml.xml", ' ', (short) 2);
      
      // Remember already queried objects
      if (AbstractKEGGtranslator.getKeggInfoManager().hasChanged()) {
        KeggInfoManagement.saveToFilesystem(Translator.cacheFileName, AbstractKEGGtranslator.getKeggInfoManager());
      }
      
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    logger.info("Conversion took " + Utils.getTimeString((System.currentTimeMillis() - start)));
  }
  
  /**
   * A {@link Logger} for this class.
   */
  private static transient final Logger logger = Logger.getLogger(KEGG2SBMLqual.class.getName());
  
  /**
   * See {@link #considerReactions}. Please stick to the default
   * (false) as this has a massive influence on the ouput of this class.
   * 
   * @param b
   */
  public void setConsiderReactions(boolean b) {
    considerReactions = b;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.io.KEGG2jSBML#considerRelations()
   */
  @Override
  protected boolean considerRelations() {
    return true;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.io.KEGG2jSBML#considerReactions()
   */
  @Override
  protected boolean considerReactions() {
    return considerReactions; // FALSE in doubt
  }
  
}

/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of KEGGtranslator, a program to convert KGML files
 * from the KEGG database into various other formats, e.g., SBML, GML,
 * GraphML, and many more. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/KEGGtranslator> to
 * obtain the latest version of KEGGtranslator.
 *
 * Copyright (C) 2011 by the University of Tuebingen, Germany.
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.SBasePlugin;
import org.sbml.jsbml.ext.qual.Input;
import org.sbml.jsbml.ext.qual.InputTransitionEffect;
import org.sbml.jsbml.ext.qual.OutputTransitionEffect;
import org.sbml.jsbml.ext.qual.QualitativeModel;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Sign;
import org.sbml.jsbml.ext.qual.Transition;
import org.sbml.jsbml.util.ValuePair;
import org.sbml.jsbml.xml.stax.SBMLWriter;

import de.zbit.kegg.Translator;
import de.zbit.kegg.api.KeggInfos;
import de.zbit.kegg.api.cache.KeggInfoManagement;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.kegg.parser.KeggParser;
import de.zbit.kegg.parser.pathway.Entry;
import de.zbit.kegg.parser.pathway.Graphics;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.kegg.parser.pathway.Relation;
import de.zbit.kegg.parser.pathway.SubType;
import de.zbit.util.ArrayUtils;
import de.zbit.util.StringUtil;
import de.zbit.util.Utils;

/**
 * KEGG2SBML with a qualitative model (SBML L3 V1, using the SBML Qual extension,
 * also KGML2JSBMLqual, KEGG2QUAL, KGML2QUAL).
 * 
 * @author Finja B&uuml;chel
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class KEGG2SBMLqual extends KEGG2jSBML {
  /**
   * Qual Namespace definition URL.
   */
  public static final String QUAL_NS = "http://www.sbml.org/sbml/level3/version1/qual/version1";
  
  /**
   * Unique identifier to identify this Namespace/Extension.
   */
  public static final String QUAL_NS_NAME = "qual";
  
  /**
   * A map to translate {@link SubType}s to SBO terms
   */
  public static Map<String, Integer> Subtype2SBO = new HashMap<String, Integer>();
  
  static {
    // Init subtype map
    Subtype2SBO.put(SubType.ACTIVATION, 170); // = stimulation
    Subtype2SBO.put(SubType.ASSOCIATION, 177); // = non-covalent binding
    Subtype2SBO.put(SubType.BINDING, 177);
    Subtype2SBO.put(SubType.BINDING_ASSOCIATION, 177);
    Subtype2SBO.put(SubType.DEPHOSPHORYLATION, 330); // dephosphorylation
    Subtype2SBO.put(SubType.DISSOCIATION, 177);
    Subtype2SBO.put(SubType.EXPRESSION, 170); 
    Subtype2SBO.put(SubType.GLYCOSYLATION, 217); // glycosylation
    Subtype2SBO.put(SubType.INDIRECT_EFFECT, 344); // molecular interaction
    Subtype2SBO.put(SubType.INHIBITION, 169);
    Subtype2SBO.put(SubType.METHYLATION, 214); // methylation
    Subtype2SBO.put(SubType.MISSING_INTERACTION, 396);  // uncertain process
    Subtype2SBO.put(SubType.PHOSPHORYLATION, 216); // phosphorylation
    Subtype2SBO.put(SubType.REPRESSION, 169);
    Subtype2SBO.put(SubType.STATE_CHANGE, 168); // control
    Subtype2SBO.put(SubType.UBIQUITINATION, 224); // ubiquitination
  }
  
  /**
   * @param document
   * @return true if the given document has at least one {@link QualitativeSpecies}.
   */
  public static boolean hasQualSpecies(SBMLDocument document) {
    if (document==null || !document.isSetModel()) return false;
    SBasePlugin qm = document.getModel().getExtension(KEGG2SBMLqual.QUAL_NS);
    if (qm!=null && qm instanceof QualitativeModel) {
      QualitativeModel q = (QualitativeModel) qm;
      if (!q.isSetListOfQualitativeSpecies()) return false;
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
  protected ValuePair<Integer, Integer> getLevelAndVersion() {
    return new ValuePair<Integer, Integer>(Integer.valueOf(3), Integer.valueOf(1));
  }
  
  @Override
  protected SBMLDocument translateWithoutPreprocessing(Pathway p) {
    // Translate to normal SBML
    SBMLDocument doc = super.translateWithoutPreprocessing(p);
    
    // Create qualitative model
    Model model = doc.getModel();
    QualitativeModel qualModel = new QualitativeModel(model);
   
    // Add extension and namespace to model
    doc.addNamespace(KEGG2SBMLqual.QUAL_NS_NAME, "xmlns", KEGG2SBMLqual.QUAL_NS);
    doc.getSBMLDocumentAttributes().put(QUAL_NS_NAME + ":required", "true");
    model.addExtension(KEGG2SBMLqual.QUAL_NS, qualModel);
    
    // Create qual species for every species
    createQualSpecies(p, qualModel);
    
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
      KEGG2SBMLLayoutExtension.addLayoutExtension(p, doc, model, false);
    }
    
    
    
    return doc;
  }
  
  /**
   * 
   * for writing a matching file in the form 
   * species_id x y width height
   * 
   * the default is false;
   * @param entry
   * @param s
   * @throws IOException 
   */
  public static void writeMatchingFile(String fileName, Pathway p)
      throws IOException {
    BufferedWriter matchWriter = new BufferedWriter(new FileWriter(fileName));
    List<String> output = new LinkedList<String>();

    for (Entry entry : p.getEntries()) {
      Object s = entry.getCustom();
      if (s != null && s instanceof Species) {
        if (entry.hasGraphics()) {
          Graphics g = entry.getGraphics();
          output = new LinkedList<String>();
          output.add(((Species) s).getId());
          if (!g.isSetCoords()) {
            output.add(g.getX() + "");
            output.add(g.getY() + "");
            output.add(g.getWidth() + "");
            output.add(g.getHeight() + "");
          } else {
            output.add("coords");
            for (int i = 0; i < g.getCoords().length; i++) {
              output.add(g.getCoords()[i] + "");
            }
          }

          matchWriter.append(ArrayUtils.implode(output, "\t"));
          matchWriter.append('\n');
        }
      }
    }

    matchWriter.close();

  }
  
  /**
   * Creates a qual species for every entry in the pathway
   * (as a side effect, also for every species in the model).
   * @param model
   * @param qualModel
   */
  private void createQualSpecies(Pathway p, QualitativeModel qualModel) {
    for (Entry e: p.getEntries()) {
      Object s = e.getCustom();
      if (s!=null && s instanceof Species) {
        QualitativeSpecies qs = createQualitativeSpeciesFromSpecies((Species) s, qualModel);
        e.setCustom(qs);
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
   */
  public Transition addKGMLRelation(Relation r, Pathway p, QualitativeModel qualModel) {
    // create transition and add it to the model
    
    Entry eOne = p.getEntryForId(r.getEntry1());
    Entry eTwo = p.getEntryForId(r.getEntry2());

    QualitativeSpecies qOne = eOne==null?null:(QualitativeSpecies) eOne.getCustom();
    QualitativeSpecies qTwo = eTwo==null?null:(QualitativeSpecies) eTwo.getCustom();

    if (qOne==null || qTwo==null) {
      // Happens, e.g. when remove_pw_references is true and there is a
      // relation to this (now removed) node.
      log.finer("Relation with unknown or removed entry: " + r);
      return null;
    }
    
    Transition t = qualModel.createTransition(NameToSId("tr"));
   
    // Input
    Input in = t.createInput(NameToSId("in"), qOne, InputTransitionEffect.none); //TODO: is this correct?
    in.setMetaId("meta_" + in.getId());

    // Output
    t.createOutput(NameToSId("out"), qTwo, OutputTransitionEffect.assignmentLevel); //TODO: is this correct?    
    
    //TODO: function term


    // Determine sign variable, SBO Terms and add MIRIAM URNs
    Sign sign = Sign.unknown;
    Set<Integer> SBOs = new HashSet<Integer>();
    List<SubType> subTypes = r.getSubtypes();
    CVTerm cv = new CVTerm(CVTerm.Qualifier.BQB_IS);
    if (subTypes != null && subTypes.size() > 0) {
      Collection<String> subTypeNames = r.getSubtypesNames();
      
      // Parse activations/ inhibitions separately for the sign
      if (subTypeNames.contains(SubType.INHIBITION) || subTypeNames.contains(SubType.REPRESSION)) {
        if (subTypeNames.contains(SubType.ACTIVATION) || subTypeNames.contains(SubType.EXPRESSION)) {
          sign=Sign.dual;
          in.setSBOTerm(168); // control is parent of inhibition and activation
          SBOs.add(168);
          
        } else {
          sign = Sign.negative;
          in.setSBOTerm(Subtype2SBO.get(SubType.INHIBITION));
        }
        
      } else if (subTypeNames.contains(SubType.ACTIVATION) || subTypeNames.contains(SubType.EXPRESSION)) {
        sign = Sign.positive;
        in.setSBOTerm(Subtype2SBO.get(SubType.ACTIVATION));
        
      } else if(subTypeNames.contains(SubType.STATE_CHANGE)) {
        in.setSBOTerm(Subtype2SBO.get(SubType.STATE_CHANGE));
        
      }
      
      // Add all subtypes as MIRIAM annotation
      for (String subType: subTypeNames) {
        Integer subSBO = Subtype2SBO.get(subType);
        if (subSBO!=null) {
          cv.addResource(KeggInfos.miriam_urn_sbo + formatSBO(subSBO));
          SBOs.add(subSBO);
        }
      }
      
      in.setSign(sign);
    }
    
    // Set SBO term and miriam URNs on transition.
    if (SBOs.size()>0 ) {
      // Remove unspecific ones, try to get the specific ones
      if (SBOs.size()>1) {
        SBOs.remove(Subtype2SBO.get(SubType.MISSING_INTERACTION));
      }
      if (SBOs.size()>1) {
        // Remark: If activation and inhibition is set, 168 is always added as third sbo.
        SBOs.remove(Subtype2SBO.get(SubType.ACTIVATION));
        SBOs.remove(Subtype2SBO.get(SubType.INHIBITION));
      }
      if (SBOs.size()>1) {
        SBOs.remove(Subtype2SBO.get(SubType.STATE_CHANGE));
      }
      if (SBOs.size()>1) {
        SBOs.remove(Subtype2SBO.get(SubType.BINDING_ASSOCIATION));
      }
      if (SBOs.size()>1) {
        SBOs.remove(Subtype2SBO.get(SubType.INDIRECT_EFFECT));
      }
      t.setSBOTerm(SBOs.iterator().next());
    }
    
    if (cv.getResourceCount()>0) {
      // Use always "IS", because "methylation" and "activation"
      // can both share the attribute IS and don't need
      // to be annotated as different versions ("HAS_VERSION").
      //setBiologicalQualifierISorHAS_VERSION(cv);
      t.addCVTerm(cv);
    }

    return t;
  }
  
  /**
   * Formats an SBO term. E.g. "177" to "SBO%3A0000177".
   * @param i
   * @return
   */
  private String formatSBO(int i) {
    StringBuilder b = new StringBuilder("SBO%3A");
    String iString = Integer.toString(i);
    b.append(StringUtil.replicateCharacter('0', 7-iString.length()));
    b.append(iString);
    return b.toString();
  }

  /**
   * Checks if there is already a qual species, matching the given species
   * and returns it. If not, creates a new qual species for the given
   * species.
   * @param species
   * @param qualModel
   * @return
   */
  private QualitativeSpecies createQualitativeSpeciesFromSpecies(Species species, QualitativeModel qualModel) {
    String id = "qual_" + species.getId();
    QualitativeSpecies qs = qualModel.getQualitativeSpecies(id);
    if(qs == null){
      qs = qualModel.createQualitativeSpecies(id, "meta_" + id, species);
    }
    return qs;  
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
  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws Exception {
    // Speedup Kegg2SBML by loading alredy queried objects. Reduces network
    // load and heavily reduces computation time.
    Format format = Format.SBML_QUAL;
    AbstractKEGGtranslator<SBMLDocument> k2s;
    KeggInfoManagement manager = null;
    if (new File(Translator.cacheFileName).exists()
        && new File(Translator.cacheFileName).length() > 1) {
      manager = (KeggInfoManagement) KeggInfoManagement.loadFromFilesystem(Translator.cacheFileName);
    }
    k2s = (AbstractKEGGtranslator<SBMLDocument>) BatchKEGGtranslator.getTranslator(format, manager);
    // ---
    
    if (args != null && args.length > 0) {
      File f = new File(args[0]);
      if (f.isDirectory()) {
        // Directory mode. Convert all files in directory.
        BatchKEGGtranslator batch = new BatchKEGGtranslator();
        batch.setOrgOutdir(args[0]);
        if (args.length > 1)
          batch.setChangeOutdirTo(args[1]);
        batch.setTranslator(k2s);
        batch.setOutFormat(format);
        batch.parseDirAndSubDir();
        
      } else {
        // Single file mode.
        String outfile = args[0].substring(0,
          args[0].contains(".") ? args[0].lastIndexOf(".") : args[0].length())
          + ".sbml.xml";
        if (args.length > 1) outfile = args[1];
        
        Pathway p = KeggParser.parse(args[0]).get(0);
        k2s.translate(p, outfile);
      }
      
      // Remember already queried objects (save cache)
      if (k2s.getKeggInfoManager().hasChanged()) {
        KeggInfoManagement.saveToFilesystem(Translator.cacheFileName, k2s.getKeggInfoManager());
      }
      
      return;
    }
    
    
    // Just a few test cases here.
    System.out.println("Demo mode.");
    
    long start = System.currentTimeMillis();
    try {
      //k2s.translate("files/KGMLsamplefiles/hsa04010.xml", "files/KGMLsamplefiles/hsa04010.sbml.xml");
//      k2s.translate("files/KGMLsamplefiles/hsa00010.xml", "files/KGMLsamplefiles/hsa00010.sbml.xml");
      
      SBMLDocument doc = k2s.translate(new File("files/KGMLsamplefiles/hsa04210.xml"));
      new SBMLWriter().write(doc, "files/KGMLsamplefiles/hsa04210.sbml.xml"); 
      
      // Remember already queried objects
      if (k2s.getKeggInfoManager().hasChanged()) {
        KeggInfoManagement.saveToFilesystem(Translator.cacheFileName, k2s.getKeggInfoManager());
      }
      
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    
    System.out.println("Conversion took "+Utils.getTimeString((System.currentTimeMillis() - start)));
  }
  
  
  @Override
  protected boolean considerRelations() {
    return true;
  }

  @Override
  protected boolean considerReactions() {    
    return false;
  }
}

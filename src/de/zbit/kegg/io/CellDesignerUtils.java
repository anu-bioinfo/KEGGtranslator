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
 * Copyright (C) 2011-2014 by the University of Tuebingen, Germany.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;

import de.zbit.kegg.KeggTools;
import de.zbit.kegg.parser.pathway.Entry;
import de.zbit.kegg.parser.pathway.EntryType;
import de.zbit.kegg.parser.pathway.Graphics;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.kegg.parser.pathway.Reaction;

/**
 * Contains functions to add CellDesigner annotations to the SBML
 * document, generated by {@link KEGG2jSBML}.
 * @author Clemens Wrzodek
 * @since 1.0
 * @version $Rev$
 */
public class CellDesignerUtils {
  
  /*
   * Temporary Stringbuffers, needed to write CellDesigner annotations. Clear
   * those before converting another document!
   */
  private StringBuffer CDloSpeciesAliases = new StringBuffer();
  private StringBuffer CDloComplexSpeciesAliases = new StringBuffer();
  private StringBuffer CDloProteins = new StringBuffer();
  
  /**
   * Initialize a new CellDesigner Annotation utility class.
   */
  public CellDesignerUtils() {
    super();
  }
  
  /**
   * Adds the cell designer annotaions the the model.
   * 
   * This HAS TO BE called after everyhing is converted. It closes all CellDesigner
   * tags and finalizes the annotaion.
   * 
   * @param p
   * @param model
   * @param defaultC
   * @throws XMLStreamException
   */
  public void addCellDesignerAnnotationToModel(Pathway p, Model model, Compartment defaultC) throws XMLStreamException {
    Annotation annot = model.getAnnotation();
    
    annot.appendNoRDFAnnotation("<celldesigner:extension>\n");
    annot.appendNoRDFAnnotation("<celldesigner:modelVersion>4.0</celldesigner:modelVersion>\n");
    int[] maxCoords = KeggTools.getMaxCoords(p);
    annot.appendNoRDFAnnotation("<celldesigner:modelDisplay sizeX=\""
        + (maxCoords[0] + 22) + "\" sizeY=\"" + (maxCoords[1] + 22) + "\"/>\n");
    annot.appendNoRDFAnnotation("<celldesigner:listOfCompartmentAliases>\n");
    
    annot.appendNoRDFAnnotation(String.format("<celldesigner:compartmentAlias id=\"cd_ca%s\" compartment=\"%s\">\n",
      defaultC.getId(), defaultC.getId()));
    annot.appendNoRDFAnnotation("<celldesigner:class>SQUARE</celldesigner:class>\n");
    annot.appendNoRDFAnnotation(String.format("<celldesigner:bounds x=\"10.0\" y=\"10.0\" w=\"%d\" h=\"%d\" />\n",
      (maxCoords[0] + 2), (maxCoords[1] + 2)));
    // <celldesigner:namePoint x="WIDTH HALBE - TEXT_WIDHT HALB"
    // y="COMPARTMENT_HEIGHT-25"/>
    annot.appendNoRDFAnnotation(String.format("<celldesigner:namePoint x=\"%d\" y=\"%d\"/>\n",
      ((maxCoords[0] + 22) / 2 - (3 * defaultC.getName().length())),maxCoords[1] - 22));
    annot.appendNoRDFAnnotation("<celldesigner:doubleLine thickness=\"10.0\" outerWidth=\"2.0\" innerWidth=\"1.0\"/>\n");
    annot.appendNoRDFAnnotation("<celldesigner:paint color=\"ffcccc00\" scheme=\"Color\" />\n");
    annot.appendNoRDFAnnotation("<celldesigner:info state=\"empty\" angle=\"0.0\"/>\n");
    annot.appendNoRDFAnnotation("</celldesigner:compartmentAlias>\n");
    
    annot.appendNoRDFAnnotation("</celldesigner:listOfCompartmentAliases>\n");
    
    if (CDloComplexSpeciesAliases.length() > 0) {
      annot.appendNoRDFAnnotation("<celldesigner:listOfComplexSpeciesAliases>\n");
      annot.appendNoRDFAnnotation(CDloComplexSpeciesAliases.toString());
      annot.appendNoRDFAnnotation("</celldesigner:listOfComplexSpeciesAliases>\n");
    } else {
      annot.appendNoRDFAnnotation("<celldesigner:listOfComplexSpeciesAliases/>\n");
    }
    if (CDloSpeciesAliases.length() > 0) {
      annot.appendNoRDFAnnotation("<celldesigner:listOfSpeciesAliases>\n");
      annot.appendNoRDFAnnotation(CDloSpeciesAliases.toString());
      annot.appendNoRDFAnnotation("</celldesigner:listOfSpeciesAliases>\n");
    } else {
      annot.appendNoRDFAnnotation("<celldesigner:listOfSpeciesAliases/>\n");
    }
    if (CDloProteins.length() > 0) {
      annot.appendNoRDFAnnotation("<celldesigner:listOfProteins>\n");
      annot.appendNoRDFAnnotation(CDloProteins.toString());
      annot.appendNoRDFAnnotation("</celldesigner:listOfProteins>\n");
    } else {
      annot.appendNoRDFAnnotation("<celldesigner:listOfProteins/>\n");
    }
    annot.appendNoRDFAnnotation("<celldesigner:listOfGroups/>\n");
    annot.appendNoRDFAnnotation("<celldesigner:listOfGenes/>\n");
    annot.appendNoRDFAnnotation("<celldesigner:listOfRNAs/>\n");
    annot.appendNoRDFAnnotation("<celldesigner:listOfAntisenseRNAs/>\n");
    annot.appendNoRDFAnnotation("<celldesigner:listOfLayers/>\n");
    annot.appendNoRDFAnnotation("<celldesigner:listOfBlockDiagrams/>\n");
    annot.appendNoRDFAnnotation("</celldesigner:extension>\n");
    
    closeOpenSpeciesTags(model);
  }
  
  /**
   * Adds "</celldesigner:extension>" to all species annotations.
   * @param model
   * @throws XMLStreamException
   */
  private void closeOpenSpeciesTags(Model model) throws XMLStreamException {
    // Close open species CD tags.
    for (Species s : model.getListOfSpecies()) {
      String a = s.getAnnotation().getNonRDFannotation().toXMLString();
      if (a != null && a.length() > 0 && a.contains("celldesigner")) {
        s.getAnnotation().appendNoRDFAnnotation("</celldesigner:extension>\n");
      }
    }
  }
  
  /**
   * Call me only on final/completely configured reactions!
   * 
   * @param sbReaction
   * @param r
   * @throws XMLStreamException
   */
  public void addCellDesignerAnnotationToReaction(
    org.sbml.jsbml.Reaction sbReaction, Reaction r) throws XMLStreamException {
    if (!containsCellDesignerNS(sbReaction)) {
      sbReaction.getAnnotation().addDeclaredNamespace("xmlns:celldesigner", "http://www.sbml.org/2001/ns/celldesigner");
      sbReaction.setNamespace("xmlns:celldesigner=http://www.sbml.org/2001/ns/celldesigner");
    }
    
    // Add Reaction Annotation
    sbReaction.getAnnotation().appendNoRDFAnnotation("<celldesigner:extension>\n");
    sbReaction.getAnnotation().appendNoRDFAnnotation(String.format("<celldesigner:name>%s</celldesigner:name>\n",
      sbReaction.getName()));
    // TODO: STATE_TRANSITION or UNKNOWN_TRANSITION ? Ersteres in anderen
    // releases.
    sbReaction.getAnnotation().appendNoRDFAnnotation("<celldesigner:reactionType>STATE_TRANSITION</celldesigner:reactionType>\n");
    
    sbReaction.getAnnotation().appendNoRDFAnnotation("<celldesigner:baseReactants>\n");
    for (SpeciesReference s : sbReaction.getListOfReactants()) {
      if (s!=null && s.isSetSpeciesInstance()) {
        sbReaction.getAnnotation().appendNoRDFAnnotation(String.format("<celldesigner:baseReactant species=\"%s\" alias=\"%s\"/>\n",
          s.getSpeciesInstance().getId(),"cd_sa"+ s.getSpeciesInstance().getId()));
        
        // Write annotation for SpeciesReference
        if (!s.isSetAnnotation()) {
          Annotation rAnnot = new Annotation("");
          rAnnot.setAbout("");
          s.setAnnotation(rAnnot);
          if (!containsCellDesignerNS(s)) {
            s.getAnnotation().addDeclaredNamespace("xmlns:celldesigner", "http://www.sbml.org/2001/ns/celldesigner");
            s.setNamespace("xmlns:celldesigner=http://www.sbml.org/2001/ns/celldesigner");
          }
        }
        s.getAnnotation().appendNoRDFAnnotation(String.format("<celldesigner:extension>\n<celldesigner:alias>%s</celldesigner:alias>\n</celldesigner:extension>\n",
          "cd_sa"+ s.getSpeciesInstance().getId()));
      }
    }
    sbReaction.getAnnotation().appendNoRDFAnnotation("</celldesigner:baseReactants>\n");
    
    sbReaction.getAnnotation().appendNoRDFAnnotation("<celldesigner:baseProducts>\n");
    for (SpeciesReference s : sbReaction.getListOfProducts()) {
      if (s!=null && s.isSetSpeciesInstance()) {
        sbReaction.getAnnotation().appendNoRDFAnnotation(String.format("<celldesigner:baseProduct species=\"%s\" alias=\"%s\"/>\n",
          s.getSpeciesInstance().getId(),"cd_sa"+ s.getSpeciesInstance().getId()));
        // Write annotation for SpeciesReference
        if (!s.isSetAnnotation()) {
          Annotation rAnnot = new Annotation("");
          rAnnot.setAbout("");
          s.setAnnotation(rAnnot);
          if (!containsCellDesignerNS(s)) {
            s.getAnnotation().addDeclaredNamespace("xmlns:celldesigner", "http://www.sbml.org/2001/ns/celldesigner");
            s.setNamespace("xmlns:celldesigner=http://www.sbml.org/2001/ns/celldesigner");
          }
        }
        s.getAnnotation().appendNoRDFAnnotation(String.format("<celldesigner:extension>\n<celldesigner:alias>%s</celldesigner:alias>\n</celldesigner:extension>\n",
          "cd_sa"+ s.getSpeciesInstance().getId()));
      }
    }
    sbReaction.getAnnotation().appendNoRDFAnnotation("</celldesigner:baseProducts>\n");
    
    sbReaction.getAnnotation().appendNoRDFAnnotation("<celldesigner:connectScheme connectPolicy=\"direct\" rectangleIndex=\"0\">\n");
    sbReaction.getAnnotation().appendNoRDFAnnotation("<celldesigner:listOfLineDirection>\n");
    sbReaction.getAnnotation().appendNoRDFAnnotation("<celldesigner:lineDirection index=\"0\" value=\"unknown\"/>\n");
    sbReaction.getAnnotation().appendNoRDFAnnotation("</celldesigner:listOfLineDirection>\n");
    sbReaction.getAnnotation().appendNoRDFAnnotation("</celldesigner:connectScheme>\n");
    sbReaction.getAnnotation().appendNoRDFAnnotation("<celldesigner:line width=\"1.0\" color=\"ff000000\"/>\n");
    
    sbReaction.getAnnotation().appendNoRDFAnnotation("<celldesigner:listOfModification>\n");
    for (ModifierSpeciesReference s : sbReaction.getListOfModifiers()) {
      sbReaction.getAnnotation().appendNoRDFAnnotation(
        String.format("<celldesigner:modification type=\"CATALYSIS\" modifiers=\"%s\" aliases=\"%s\" targetLineIndex=\"-1,0\">\n", // original: -1,2
          s.getSpeciesInstance().getId(),"cd_sa"+ s.getSpeciesInstance().getId()));
      sbReaction.getAnnotation().appendNoRDFAnnotation("<celldesigner:connectScheme connectPolicy=\"direct\">\n");
      sbReaction.getAnnotation().appendNoRDFAnnotation("<celldesigner:listOfLineDirection>\n");
      sbReaction.getAnnotation().appendNoRDFAnnotation("<celldesigner:lineDirection index=\"0\" value=\"unknown\"/>\n");
      sbReaction.getAnnotation().appendNoRDFAnnotation("</celldesigner:listOfLineDirection>\n");
      sbReaction.getAnnotation().appendNoRDFAnnotation("</celldesigner:connectScheme>\n");
      sbReaction.getAnnotation().appendNoRDFAnnotation("<celldesigner:line width=\"1.0\" color=\"ff000000\"/>\n");
      sbReaction.getAnnotation().appendNoRDFAnnotation("</celldesigner:modification>\n");
      
      // Write annotation for ModifierSpeciesReference
      
      // It happens, that a modifier occurs in multiple reactions. Take care of this here.
      String currentAnnotation = s.getAnnotation().getNonRDFannotation().toXMLString();
      if (currentAnnotation!=null &&
          !s.getAnnotation().getNonRDFannotation().toXMLString().contains("<celldesigner:alias>")) {
        s.getAnnotation().appendNoRDFAnnotation(String.format(
          "<celldesigner:extension>\n<celldesigner:alias>%s</celldesigner:alias>\n</celldesigner:extension>\n",
          "cd_sa" + s.getSpeciesInstance().getId()));
      }
      
      // Write further annotations for the Modifying species.
      final String loKey = "<celldesigner:listOfCatalyzedReactions>";
      // The key, we want to add
      final String newKey = String.format("<celldesigner:catalyzed reaction=\"%s\"/>\n",sbReaction.getId());
      currentAnnotation = s.getSpeciesInstance().getAnnotation().getNonRDFannotation().toXMLString();
      int pos = -1;
      if (currentAnnotation != null) {
        pos = currentAnnotation.indexOf(loKey);
        if (pos >= 0) {
          currentAnnotation = currentAnnotation.substring(0, pos + loKey.length()) + newKey + currentAnnotation.substring(pos + loKey.length() + 1);
          s.getSpeciesInstance().getAnnotation().setNonRDFAnnotation(currentAnnotation);
        }
      }
      if (pos < 0) {
        s.getSpeciesInstance().getAnnotation().appendNoRDFAnnotation(
          loKey+"\n" + newKey + "</celldesigner:listOfCatalyzedReactions>\n");
      }
      
    }
    sbReaction.getAnnotation().appendNoRDFAnnotation("</celldesigner:listOfModification>\n");
    
    sbReaction.getAnnotation().appendNoRDFAnnotation("</celldesigner:extension>\n");
  }
  
  /**
   * Adds cell designer annotations to the given species
   * 
   * HAS TO BE CALLED AFTER EVERY SPECIES IS CONVERTED TO jSBML!
   * 
   * Uses spec.getName() ! Be careful, the species CD Extension tag is NOT
   * closed.
   * @throws XMLStreamException
   */
  private void addCellDesignerAnnotationToSpecies(NamedSBase spec, Entry e) throws XMLStreamException {
    // TODO: Sind die defaults so richtig? was bedeutet z.B. cd:activity?
    EntryType t = e.getType();
    boolean isGroupNode = AbstractKEGGtranslator.isGroupNode(e); // genes = group in kgml v<0.7
    
    if (!containsCellDesignerNS(spec)) {
      spec.getAnnotation().addDeclaredNamespace("xmlns:celldesigner", "http://www.sbml.org/2001/ns/celldesigner");
      spec.addDeclaredNamespace("xmlns:celldesigner", "http://www.sbml.org/2001/ns/celldesigner");
    }
    
    // Add to Species Annotation list
    StringBuffer target;
    if (isGroupNode) {
      target = CDloComplexSpeciesAliases;
    } else {
      target = CDloSpeciesAliases;
    }
    
    // Warning: prefix "cd_sa" is also hardcoded in addCDAtoReaction!
    target.append("<celldesigner:"
        + (isGroupNode ? "complexSpeciesAlias" : "speciesAlias")
        + " id=\"cd_sa" + spec.getId() + "\" species=\"" + spec.getId()+"\"");
    
    // If this is a child of a group (complex) node, reflect this here.
    if (!isGroupNode && e.getParentNode()!=null) {
      Entry parent = e.getParentNode();
      if (parent.getCustom()!=null && parent.getCustom() instanceof Species) {
        target.append(" complexSpeciesAlias=\"");
        target.append("cd_sa"+((Species)parent.getCustom()).getId());
        target.append('\"');
      }
    }
    target.append(">\n");
    
    
    // CellDesigner REQUIRES attributes like "bounds", so we have to
    // create a new graphics object, if it's not already available.
    Graphics g = (e.hasGraphics()?e.getGraphics():new Graphics(e));
    target.append("<celldesigner:activity>inactive</celldesigner:activity>\n");
    
    target.append(String.format("<celldesigner:bounds x=\"%d\" y=\"%d\" w=\"%d\" h=\"%d\"/>\n",
      g.getX(), g.getY(), g.getWidth(), g.getHeight()));
    target.append("<celldesigner:view state=\"usual\"/>\n");
    
    if (isGroupNode) {
      target.append("<celldesigner:backupSize w=\"0.0\" h=\"0.0\"/>\n");
      target.append("<celldesigner:backupView state=\"none\"/>\n");
    }
    
    // Add usual- and brief view
    for (int i = 1; i <= 2; i++) {
      if (i == 1) {
        target.append("<celldesigner:usualView>\n");
      } else {
        target.append("<celldesigner:briefView>\n");
      }
      target.append("<celldesigner:innerPosition x=\"0.0\" y=\"0.0\"/>\n");
      target.append(String.format("<celldesigner:boxSize width=\"%d\" height=\"%d\"/>\n",
        e.hasGraphics() ? e.getGraphics().getWidth() : 90, e.hasGraphics() ? e.getGraphics().getHeight() : 25));
      target.append("<celldesigner:singleLine width=\"" + (isGroupNode ? "2.0" : (i == 1 ? "1.0" : "0.0")) + "\"/>\n");
      
      String col = "FFFFFF";
      if (g.isSetBGcolor()) {
        col = g.getBgcolor().replace("#", "").toLowerCase();
      }
      target.append(String.format("<celldesigner:paint color=\""
          + (i == 1 ? "ff" : "3f")
          + "%s\" scheme=\"Color\"/>\n", col));
      if (i == 1) {
        target.append("</celldesigner:usualView>\n");
      } else {
        target.append("</celldesigner:briefView>\n");
      }
    }
    
    target.append("<celldesigner:info state=\"empty\" angle=\"0.0\"/>\n");
    target.append("</celldesigner:"+ (isGroupNode ? "complexSpeciesAlias" : "speciesAlias")+ ">\n");
    
    // Add to type specific annotation
    String type = "";
    String reference = "";
    if (t.equals(EntryType.ortholog) || t.equals(EntryType.enzyme) || t.equals(EntryType.gene)) {
      // A Protein. (EntryType.gene => KeggDoc says
      // "the node is a gene PRODUCT (mostly a protein)")
      CDloProteins.append(String.format("<celldesigner:protein id=\"cd_pr%s\" name=\"%s\" type=\"GENERIC\"/>\n",
        spec.getId(), spec.getId()));
      type = "PROTEIN";
      reference = "<celldesigner:proteinReference>cd_pr" + spec.getId() + "</celldesigner:proteinReference>";
    } else if (isGroupNode) { // t.equals(EntryType.group)
      type = "COMPLEX";
      reference = "<celldesigner:name>"+ NameToCellDesignerName(spec.getName())+ "</celldesigner:name>";
    } else if (t.equals(EntryType.compound)) {
      type = "SIMPLE_MOLECULE";
      reference = "<celldesigner:name>"+ NameToCellDesignerName(spec.getName())+ "</celldesigner:name>";
    } else if (t.equals(EntryType.map) || t.equals(EntryType.other)) {
      type = "UNKNOWN";
      reference = "<celldesigner:name>"+ NameToCellDesignerName(spec.getName())+ "</celldesigner:name>";
    }
    
    // Add Species Annotation
    spec.getAnnotation().appendNoRDFAnnotation("<celldesigner:extension>\n");
    spec.getAnnotation().appendNoRDFAnnotation("<celldesigner:positionToCompartment>inside</celldesigner:positionToCompartment>\n");
    spec.getAnnotation().appendNoRDFAnnotation("<celldesigner:speciesIdentity>\n");
    spec.getAnnotation().appendNoRDFAnnotation(String.format("<celldesigner:class>%s</celldesigner:class>\n",type));
    spec.getAnnotation().appendNoRDFAnnotation(reference + "\n");
    spec.getAnnotation().appendNoRDFAnnotation("</celldesigner:speciesIdentity>\n");
    
    /*
     * DON'T WRITE END TAG HERE. Catalysts write additional data in
     * "addCellDesignerAnnotationToReaction".
     * spec.getAnnotation().appendNoRDFAnnotation
     * ("</celldesigner:extension>\n");
     */
  }
  
  
  /**
   * CellDesigner has special encodings for space, minus, alpha, etc. in the
   * id attribute. This function reformats the given string, replacing original
   * with these special attributes.
   * 
   * @param name - input string to reformat.
   * @return String with special CD attributes (e.g., " " => "_space_").
   */
  private static String NameToCellDesignerName(String name) {
    name = name.trim().replace(" ", "_space_").replace("-", "_minus_")
        .replace("alpha", "_alpha_").replace("beta", "_beta_").replace("gamma", "_gamma_").replace("delta", "_delta_").replace("epsilon  ", "_epsilon_")
        .replace("ALPHA", "_ALPHA_").replace("BETA", "_BETA_").replace("GAMMA", "_GAMMA_").replace("DELTA", "_DELTA_").replace("EPSILON  ", "_EPSILON_");
    
    return (name);
  }
  
  /**
   * Adds cell designer annotations to all species
   * 
   * HAS TO BE CALLED AFTER EVERY SPECIES IS CONVERTED TO jSBML!
   * @param p
   * @throws XMLStreamException
   */
  public void addCellDesignerAnnotationToAllSpecies(Pathway p) throws XMLStreamException {
    ArrayList<Entry> entries = p.getEntries();
    Set<String> alreadyProcessed = new HashSet<String>();
    for (Entry entry : entries) {
      if (entry.getCustom()!=null && entry.getCustom() instanceof Species) {
        if (alreadyProcessed.add(((NamedSBase) entry.getCustom()).getId()) ) {
          addCellDesignerAnnotationToSpecies((Species) entry.getCustom(), entry);
        }
      }
    }
  }
  
  
  /**
   * Initializes the model and document for cellDesigner annotations.
   * 
   * @param model
   * @param doc
   */
  public void initCellDesignerAnnotations(Model model, SBMLDocument doc) {
    // XXX: Probably the more correct way. But currently, extensions get completely ignored.
    // Annotation cdAnnot = new Annotation();
    // cdAnnot.setAbout("");
    // addCellDesignerAnnotationPrefixToModel(p, cdAnnot);
    // annot.addExtension("xmlns:celldesigner=http://www.sbml.org/2001/ns/celldesigner", cdAnnot);
    
    String cellDesignerNameSpace = "xmlns:celldesigner=http://www.sbml.org/2001/ns/celldesigner";
    if ((model.getDeclaredNamespaces() == null)
        || (model.getDeclaredNamespaces().get("xmlns:celldesigner") != null)
        && (model.getDeclaredNamespaces().get("xmlns:celldesigner") == "http://www.sbml.org/2001/ns/celldesigner")) {
      model.addDeclaredNamespace("xmlns:celldesigner", cellDesignerNameSpace);
    }
    if (!containsCellDesignerNS(model)) {
      model.getAnnotation().addDeclaredNamespace("xmlns:celldesigner", "http://www.sbml.org/2001/ns/celldesigner");
      doc.addDeclaredNamespace("xmlns:celldesigner", "http://www.sbml.org/2001/ns/celldesigner"); // xmlns:celldesigner  }
    }
  }
  
  /**
   * 
   * @param sb
   * @return
   */
  public static boolean containsCellDesignerNS(SBase sb) {
    if ((sb == null) || (sb.getAnnotation() == null) || (sb.getAnnotation().getDeclaredNamespaces() == null)) {
      return false;
    }
    return sb.getAnnotation().getDeclaredNamespaces().hasPrefix("xmlns:celldesigner");
  }
  
}

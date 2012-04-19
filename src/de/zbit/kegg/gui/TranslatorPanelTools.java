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
 * Copyright (C) 2010-2012 by the University of Tuebingen, Germany.
 *
 * KEGGtranslator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.kegg.gui;

import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;

import de.zbit.graph.gui.TranslatorGraphLayerPanel;
import de.zbit.graph.gui.TranslatorPanel;
import de.zbit.graph.gui.options.GraphBackgroundImageProvider;
import de.zbit.graph.gui.options.TranslatorPanelOptions;
import de.zbit.gui.GUITools;
import de.zbit.kegg.ext.KEGGTranslatorPanelOptions;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.util.prefs.SBPreferences;

/**
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class TranslatorPanelTools {
  
  
  /**
   * 
   * @param inputFile
   * @param outputFormat
   * @param translationResult
   */
  public static TranslatorPanel<?> createPanel(final File inputFile, final Format outputFormat, ActionListener translationResult) {
    TranslatorPanel<?> panel = null;
    
    switch (outputFormat) {
      case SBML: case SBML_QUAL: case SBML_CORE_AND_QUAL: /*case LaTeX: */
        panel = new TranslatorSBMLPanel(inputFile, outputFormat, translationResult);
        break;
        
      case GraphML: case GML: case JPG: case GIF: case YGF: case TGF: /* case SVG:*/
        panel = new TranslatorGraphPanel(inputFile, outputFormat, translationResult);
        break;
        
      case SBGN:
        panel = new TranslatorSBGNPanel(inputFile, translationResult);
        break;
        
      case BioPAX_level2: case BioPAX_level3:
        panel = new TranslatorBioPAXPanel(inputFile, outputFormat, translationResult);
        break;
        
      default:
        GUITools.showErrorMessage(null, "Unknown output Format: '" + outputFormat + "'.");
    } 
    
    if (panel!=null && (panel instanceof TranslatorGraphLayerPanel)) {
      setupBackgroundImage((TranslatorGraphLayerPanel<?>) panel);
    }
    return panel;
  }
  
  public static TranslatorPanel<?> createPanel(final String pathwayID, final Format outputFormat, ActionListener translationResult) {
    TranslatorPanel<?> panel = null;
    
    switch (outputFormat) {
      case SBML: case SBML_QUAL: case SBML_CORE_AND_QUAL: /*case LaTeX: */
        panel = new TranslatorSBMLPanel(pathwayID, outputFormat, translationResult);
        break;
        
      case GraphML: case GML: case JPG: case GIF: case YGF: case TGF: /* case SVG:*/
        panel = new TranslatorGraphPanel(pathwayID, outputFormat, translationResult);
        break;
        
      case SBGN:
        panel = new TranslatorSBGNPanel(pathwayID, translationResult);
        break;
        
      case BioPAX_level2: case BioPAX_level3:
        panel = new TranslatorBioPAXPanel(pathwayID, outputFormat, translationResult);
        break;
        
      default:
        GUITools.showErrorMessage(null, "Unknwon output Format: '" + outputFormat + "'.");
        return null;
    }
    
    if (panel!=null && (panel instanceof TranslatorGraphLayerPanel)) {
      setupBackgroundImage((TranslatorGraphLayerPanel<?>) panel);
    }
    return panel;
  }
  
  /**
   * Setup the background image as set in the preferences
   * @param pane the pane to add the background image
   * @param translator the translator used for translation
   * @param prefs might be null, else, prefs object for {@link TranslatorPanelOptions}
   * @throws MalformedURLException
   */
  public static void setupBackgroundImage(TranslatorGraphLayerPanel<?> panel) {
    SBPreferences prefs = SBPreferences.getPreferencesFor(KEGGTranslatorPanelOptions.class);
    GraphBackgroundImageProvider provider = null;
    
    if (KEGGTranslatorPanelOptions.SHOW_LOGO_IN_GRAPH_BACKGROUND.getValue(prefs)) {
      provider = GraphBackgroundImageProvider.Factory.createStaticImageProvider(TranslatorUI.getWatermarkLogoResource());
      
    } else if (KEGGTranslatorPanelOptions.SHOW_KEGG_PICTURE_IN_GRAPH_BACKGROUND.getValue(prefs)) {
        Integer brighten = (KEGGTranslatorPanelOptions.BRIGHTEN_KEGG_BACKGROUND_IMAGE.getValue(prefs));
        if (brighten==null || brighten<0) brighten = 0;
        provider = GraphBackgroundImageProvider.Factory.createDynamicTranslatorImageProvider(brighten);
    }
    
    // Setup the provider
    panel.setBackgroundImageProvider(provider);
  }
  
}

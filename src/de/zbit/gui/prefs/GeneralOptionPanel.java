/*
 * $Id: GeneralOptionPanel.java 400 2015-02-01 07:32:30Z draeger $
 * $URL: https://rarepos.cs.uni-tuebingen.de/svn/KEGGconverter/trunk/src/de/zbit/gui/prefs/GeneralOptionPanel.java $
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
package de.zbit.gui.prefs;

import java.io.IOException;

import de.zbit.kegg.KEGGtranslatorOptions;

/**
 * This class is collected by reflection and will lead to a automatic
 * generation of a SWING options dialog for {@link KEGGtranslatorOptions}.
 * 
 * @author Clemens Wrzodek
 * @since 1.0
 * @version $Rev: 400 $
 */
public class GeneralOptionPanel extends PreferencesPanelForKeyProvider {
  private static final long serialVersionUID = 6273038303582557299L;
  
  /**
   * @throws IOException
   */
  public GeneralOptionPanel() throws IOException {
    super("KEGGtranslator options", KEGGtranslatorOptions.class);
  }
  
}

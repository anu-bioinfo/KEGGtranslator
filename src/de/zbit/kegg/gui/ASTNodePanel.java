/*
 * Copyright (c) 2011 Center for Bioinformatics of the University of Tuebingen.
 * 
 * This file is part of KEGGtranslator, a program to convert KGML files from the
 * KEGG database into various other formats, e.g., SBML, GraphML, and many more.
 * Please visit <http://www.ra.cs.uni-tuebingen.de/software/KEGGtranslator> to
 * obtain the latest version of KEGGtranslator.
 * 
 * KEGGtranslator is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * KEGGtranslator is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with KEGGtranslator. If not, see
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package de.zbit.kegg.gui;

import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.MathContainer;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.util.compilers.LaTeX;

import atp.sHotEqn;
import de.zbit.gui.LayoutHelper;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-05-11
 */
public class ASTNodePanel extends JPanel {

    /**
     * Generated serial version identifier.
     */
    private static final long serialVersionUID = -7683674821264614573L;

    /**
     * @param node
     * @throws SBMLException
     * @throws IOException
     */
    public ASTNodePanel(ASTNode node) throws SBMLException, IOException {
	super();
	LayoutHelper lh = new LayoutHelper(this);
	boolean enabled = false;
	JSpinner spinner;
	JTextField tf;
	String name;

	name = node.getParent() == null ? "undefined" : node.getParent()
		.toString();
	tf = new JTextField(name);
	tf.setEditable(enabled);
	lh.add("Parent node", tf, true);

	if (node.getParentSBMLObject() == null) {
	    name = "undefined";
	} else {
	    MathContainer parent = node.getParentSBMLObject();
	    name = parent.getClass().getSimpleName();
	    name += " " + node.getParentSBMLObject().toString();
	}
	tf = new JTextField(name);
	tf.setEditable(enabled);
	lh.add("Parent SBML object", tf, true);

	tf = new JTextField(Integer.toString(node.getNumChildren()));
	tf.setEditable(false);
	lh.add("Number of children", tf, true);

	tf = new JTextField(node.toFormula());
	tf.setEditable(false);
	lh.add("Formula", tf, true);

	JComboBox opt = new JComboBox();
	for (ASTNode.Type t : ASTNode.Type.values()) {
	    opt.addItem(t);
	    if (t.equals(node.getType())) {
		opt.setSelectedItem(t);
	    }
	}
	opt.setEditable(enabled);
	opt.setEnabled(enabled);
	lh.add("Type", opt, true);

	if (node.isRational()) {
	    spinner = new JSpinner(new SpinnerNumberModel(node.getNumerator(),
		-1E10, 1E10, 1));
	    spinner.setEnabled(enabled);
	    lh.add("Numerator", spinner, true);
	    spinner = new JSpinner(new SpinnerNumberModel(
		node.getDenominator(), -1E10, 1E10, 1));
	    spinner.setEnabled(enabled);
	    lh.add("Denominator", spinner, true);
	}

	if (node.isReal()) {
	    spinner = new JSpinner(new SpinnerNumberModel(node.getMantissa(),
		-1E10, 1E10, 1));
	    spinner.setEnabled(enabled);
	    lh.add("Mantissa", spinner, true);
	    spinner = new JSpinner(new SpinnerNumberModel(node.getExponent(),
		-1E10, 1E10, 1));
	    spinner.setEnabled(enabled);
	    lh.add("Exponent", spinner, true);
	}

	if (node.isString()) {
	    tf = new JTextField(node.getName());
	    tf.setEditable(enabled);
	    lh.add("Name", tf, true);
	    if (node.getVariable() != null) {
		lh.add(new SBasePanel(node.getVariable()), 0, lh.getRow() + 1,
		    3, 1);
		lh.add(new JPanel(), 0, lh.getRow() + 1, 3, 1);
	    }
	}

	sHotEqn preview = new sHotEqn(node.compile(new LaTeX()).toString());
	preview.setBorder(BorderFactory.createLoweredBevelBorder());
	JScrollPane scroll = new JScrollPane(preview,
	    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
	    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	scroll.setBorder(BorderFactory.createTitledBorder(" Preview "));
	lh.add(scroll, 0, lh.getRow() + 1, 3, 1);

	setBorder(BorderFactory
		.createTitledBorder(" " + node.getClass().getSimpleName() + " "
			+ node.toString() + " "));
    }

}

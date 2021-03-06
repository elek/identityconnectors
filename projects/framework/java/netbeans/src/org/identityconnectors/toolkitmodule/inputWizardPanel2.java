/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.     
 * 
 * The contents of this file are subject to the terms of the Common Development 
 * and Distribution License("CDDL") (the "License").  You may not use this file 
 * except in compliance with the License.
 * 
 * You can obtain a copy of the License at 
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.toolkitmodule;

import java.awt.Component;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.swing.DefaultListModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;

public class inputWizardPanel2 implements WizardDescriptor.Panel, ListDataListener {

    /**
     * The visual component that displays this panel. If you need to access the
     * component from this class, just use getComponent().
     */
    private inputVisualPanel2 component;
    private final Set<ChangeListener> listeners = new HashSet<ChangeListener>(1);
    private boolean isValid = false;

    // Get the visual component for the panel. In this template, the component
    // is kept separate. This can be more efficient: if the wizard is created
    // but never displayed, or not all panels are displayed, it is better to
    // create only those which really need to be visible.
    public Component getComponent() {
        if (component == null) {
            component = new inputVisualPanel2();
            component.getLstSelectedOps().getModel().addListDataListener(this);
        }
        return component;
    }

    public HelpCtx getHelp() {
        return new HelpCtx("org.identityconnectors.toolkitmodule.about");
    }

    public boolean isValid() {
        return isValid;
    }

    public final void addChangeListener(ChangeListener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }

    public final void removeChangeListener(ChangeListener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }

    protected final void fireChangeEvent() {
        Iterator<ChangeListener> it;
        synchronized (listeners) {
            it = new HashSet<ChangeListener>(listeners).iterator();
        }
        ChangeEvent ev = new ChangeEvent(this);
        while (it.hasNext()) {
            it.next().stateChanged(ev);
        }
    }

    private void change() {
        int i = component.getLstSelectedOps().getModel().getSize();
        if (i > 0) {
            setValid(true);
        } else {
            setValid(false);
        }
    }

    private void setValid(boolean val) {
        if (isValid != val) {
            isValid = val;
            fireChangeEvent();  // must do this to enable next/finish button
        }
    }

    public void intervalAdded(ListDataEvent arg0) {
        change();
    }

    public void intervalRemoved(ListDataEvent arg0) {
        change();
    }

    public void contentsChanged(ListDataEvent arg0) {
        change();
    }

    // You can use a settings object to keep track of state. Normally the
    // settings object will be the WizardDescriptor, so you can use
    // WizardDescriptor.getProperty & putProperty to store information entered
    // by the user.
    public void readSettings(Object settings) {
    }

    public void storeSettings(Object settings) {
        WizardDescriptor wizardDescriptor = (WizardDescriptor) settings;
        DefaultListModel model = (DefaultListModel) component.getLstSelectedOps().getModel();
        StringBuilder sb = new StringBuilder();
        for(Object o : model.toArray()) {
            if(sb.length() > 0) {
                sb.append(",");
            }
            sb.append(component.getSPIOps().get(o));
        }
        wizardDescriptor.putProperty("selected.ops", sb.toString());
    }
}


/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009-2010, Rasto Levrinc
 * Copyright (C) 2009-2010, LINBIT HA-Solutions GmbH.
 *
 * DRBD Management Console is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * DRBD Management Console is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with drbd; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package drbd.gui.resources;

import drbd.gui.Browser;
import drbd.gui.ClusterBrowser;
import drbd.gui.SpringUtilities;
import drbd.data.Host;
import drbd.data.PtestData;
import drbd.data.ClusterStatus;
import drbd.data.ConfigData;
import drbd.data.CRMXML;
import drbd.utilities.UpdatableItem;
import drbd.utilities.ButtonCallback;
import drbd.utilities.Tools;
import drbd.utilities.CRM;
import drbd.utilities.MyMenuItem;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.SwingUtilities;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.SpringLayout;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.CountDownLatch;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import EDU.oswego.cs.dl.util.concurrent.Mutex;

/**
 * This class describes a connection between two heartbeat services.
 * It can be order, colocation or both.
 */
public class PcmkRscSetsInfo extends HbConnectionInfo {
    /** Cache for the info panel. */
    private JComponent infoPanel = null;
    /** Placeholders. */
    private final Set<ConstraintPHInfo> constraintPHInfos =
                                          new LinkedHashSet<ConstraintPHInfo>();
    /** Selected placeholder. */
    private ConstraintPHInfo selectedConstaintPHInfo = null;
    /** constraints lock. */
    private final Mutex mConstraintPHLock = new Mutex();

    /** Prepares a new <code>PcmkRscSetsInfo</code> object. */
    public PcmkRscSetsInfo(final Browser browser) {
        super(browser);
    }

    /** Prepares a new <code>PcmkRscSetsInfo</code> object. */
    public PcmkRscSetsInfo(final Browser browser, final ConstraintPHInfo cphi) {
        this(browser);
        try {
            mConstraintPHLock.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        constraintPHInfos.add(cphi);
        mConstraintPHLock.release();
    }

    /** Adds a new rsc set colocation. */
    public final void addColocation(final String colId,
                                    final ConstraintPHInfo cphi) {
        try {
            mConstraintPHLock.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        constraintPHInfos.add(cphi);
        mConstraintPHLock.release();
        addColocation(colId, null, null);
    }

    /** Adds a new rsc set order. */
    public final void addOrder(final String ordId,
                               final ConstraintPHInfo cphi) {
        try {
            mConstraintPHLock.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        constraintPHInfos.add(cphi);
        mConstraintPHLock.release();
        addOrder(ordId, null, null);
    }

    /** Returns info panel. */
    public final JComponent getInfoPanel(
                                    final ConstraintPHInfo constraintPHInfo) {
        selectedConstaintPHInfo = constraintPHInfo;
        return super.getInfoPanel();
    }

    /** Returns panal with user visible info. */
    protected JPanel getLabels(final HbConstraintInterface c) {
        final JPanel panel = getParamPanel(c.getName());
        panel.setLayout(new SpringLayout());
        final int rows = 1;
        final int height = Tools.getDefaultInt("Browser.LabelFieldHeight");
        c.addLabelField(panel,
                        Tools.getString("ClusterBrowser.HeartbeatId"),
                        c.getService().getHeartbeatId(),
                        ClusterBrowser.SERVICE_LABEL_WIDTH,
                        ClusterBrowser.SERVICE_FIELD_WIDTH,
                        height);
        SpringUtilities.makeCompactGrid(panel, rows, 2, /* rows, cols */
                                        1, 1,        /* initX, initY */
                                        1, 1);       /* xPad, yPad */
        return panel;
    }

    /** Applies changes to the placeholders. Called from one connection to a
     * placeholder. */
    public final Map<CRMXML.RscSet, Map<String, String>> getAllAttributes(
                                        final Host dcHost,
                                        final CRMXML.RscSet appliedRscSet,
                                        final Map<String, String> appliedAttrs,
                                        final boolean isColocation,
                                        final boolean testOnly) {
        final Map<CRMXML.RscSet, Map<String, String>> rscSetsAttrs =
                       new LinkedHashMap<CRMXML.RscSet, Map<String, String>>();
        final List<ConstraintPHInfo> allCphis = getAllConstrainPHInfos();
        if (isColocation) {
            for (final ConstraintPHInfo cphi : allCphis) {
                for (final Boolean first : new Boolean[]{false, true}) {
                    cphi.getAttributes(isColocation, first, rscSetsAttrs);
                }
            }
        } else {
            for (int i = allCphis.size() - 1; i >= 0; i--) {
                for (final Boolean first : new Boolean[]{true, false}) {
                    allCphis.get(i).getAttributes(isColocation, first, rscSetsAttrs);
                }
            }
        }
        rscSetsAttrs.put(appliedRscSet, appliedAttrs);
        return rscSetsAttrs;
    }

    /** Returns copy of all constraint placeholders. */
    private List<ConstraintPHInfo> getAllConstrainPHInfos() {
        final Map<String, ServiceInfo> idToInfoHash =
             getBrowser().getNameToServiceInfoHash(ConstraintPHInfo.NAME);
        final List<ConstraintPHInfo> allCphis =
                                            new ArrayList<ConstraintPHInfo>();
        if (idToInfoHash != null) {
            for (final String id : idToInfoHash.keySet()) {
                final ConstraintPHInfo cphi =
                                   (ConstraintPHInfo) idToInfoHash.get(id);
                allCphis.add(cphi);
            }
        }
        return allCphis;
    }

    /** Applies changes to the placeholders. */
    public final void apply(final Host dcHost, final boolean testOnly) {
        super.apply(dcHost, testOnly);
        final Map<String, ServiceInfo> idToInfoHash =
             getBrowser().getNameToServiceInfoHash(ConstraintPHInfo.NAME);
        final List<ConstraintPHInfo> allCphis = getAllConstrainPHInfos();
        try {
            mConstraintPHLock.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        final Map<ServiceInfo, ServiceInfo> parentToChild =
                            new HashMap<ServiceInfo, ServiceInfo>();
        for (final ConstraintPHInfo cphi : constraintPHInfos) {
            final List<ServiceInfo> cphiParents =
                            getBrowser().getHeartbeatGraph().getParents(cphi);
            boolean startComparing = false;
            for (final ConstraintPHInfo withCphi : allCphis) {
                if (cphi == withCphi) {
                    startComparing = true;
                    continue;
                }
                if (!startComparing) {
                    continue;
                }
                final List<ServiceInfo> withCphiChildren =
                         getBrowser().getHeartbeatGraph().getChildren(withCphi);
                if (Tools.serviceInfoListEquals(cphiParents,
                                                withCphiChildren)) {
                    parentToChild.put(cphi, withCphi);
                    break;
                }
            }
        }
        final List<CRMXML.RscSet> rscSetsCol = new ArrayList<CRMXML.RscSet>();
        final List<CRMXML.RscSet> rscSetsOrd = new ArrayList<CRMXML.RscSet>();
        for (final ConstraintPHInfo cphi : constraintPHInfos) {
            if (cphi.getService().isNew()) {
                //cphi.apply(dcHost, testOnly);
                final List<CRMXML.RscSet> sets =
                 cphi.addConstraintWithPlaceholder(
                  getBrowser().getHeartbeatGraph().getChildrenAndParents(cphi),
                  getBrowser().getHeartbeatGraph().getParents(cphi),
                  false,
                  false,
                  dcHost,
                  false,
                  testOnly);
                rscSetsCol.add(sets.get(0)); /* col1 */
                rscSetsOrd.add(0, sets.get(3)); /* ord2 */
                ConstraintPHInfo parent = cphi;
                if (parentToChild.containsKey((ServiceInfo) parent)) {
                    List<CRMXML.RscSet> childSets = null;
                    while (parentToChild.containsKey((ServiceInfo) parent)) {
                        final ConstraintPHInfo child =
                            (ConstraintPHInfo) parentToChild.get((
                                                        ServiceInfo) parent);
                        if (child.getService().isNew()) {
                            //child.apply(dcHost, testOnly);
                            childSets =
                             child.addConstraintWithPlaceholder(
                              getBrowser().getHeartbeatGraph()
                                          .getChildrenAndParents(child),
                              getBrowser().getHeartbeatGraph()
                                          .getParents(child),
                              false,
                              false,
                              dcHost,
                              false,
                              testOnly);
                            rscSetsCol.add(childSets.get(0)); /* col1 */
                            rscSetsOrd.add(0, childSets.get(3)); /* ord2 */
                            //if (!testOnly) {
                            //    child.getService().setNew(false);
                            //}
                        }
                        parent = child;
                    }
                    if (childSets != null) {
                        rscSetsCol.add(childSets.get(1)); /* col2 */
                        rscSetsOrd.add(0, childSets.get(2)); /* ord1 */
                    }
                    
                } else {
                    rscSetsCol.add(sets.get(1)); /* col2 */
                    rscSetsOrd.add(0, sets.get(2)); /* ord2 */
                }
            }
        }
        mConstraintPHLock.release();
        final Map<String, String> attrs =
                                      new LinkedHashMap<String, String>();
        attrs.put(CRMXML.SCORE_STRING, CRMXML.INFINITY_STRING);
        String colId = null;
        String ordId = null;
        final Map<CRMXML.RscSet, Map<String, String>> rscSetsColAttrs =
                       new LinkedHashMap<CRMXML.RscSet, Map<String, String>>();
        for (final CRMXML.RscSet colSet : rscSetsCol) {
            if (colId == null && colSet != null) {
                colId = colSet.getId();
            }
            rscSetsColAttrs.put(colSet, null);
        }
        final Map<CRMXML.RscSet, Map<String, String>> rscSetsOrdAttrs =
                       new LinkedHashMap<CRMXML.RscSet, Map<String, String>>();
        for (final CRMXML.RscSet ordSet : rscSetsOrd) {
            if (ordId == null && ordSet != null) {
                ordId = ordSet.getId();
            }
            rscSetsOrdAttrs.put(ordSet, null);
        }
        final boolean createCol = true;
        final boolean createOrd = true;
        CRM.setRscSet(dcHost,
                      colId,
                      createCol,
                      ordId,
                      createOrd,
                      rscSetsColAttrs,
                      rscSetsOrdAttrs,
                      attrs,
                      testOnly);
    }

    /** Check order and colocation constraints. */
    public final boolean checkResourceFields(final String param,
                                             final String[] params) {
        boolean oneIsNew = false;
        try {
            mConstraintPHLock.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        for (final ConstraintPHInfo cphi : constraintPHInfos) {
            if (cphi.getService().isNew()
                && !getBrowser().getHeartbeatGraph().getChildrenAndParents(
                                                            cphi).isEmpty()) {
                oneIsNew = true;
            }
        }
        mConstraintPHLock.release();
        //TODO: have to chech changed and correct separately
        return super.checkResourceFields(param, params) || oneIsNew;
    }
}

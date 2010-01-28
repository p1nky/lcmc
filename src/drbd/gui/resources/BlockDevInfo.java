/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009-2010, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2009-2010, Rasto Levrinc
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

import drbd.Exceptions;
import drbd.gui.Browser;
import drbd.gui.HostBrowser;
import drbd.gui.ClusterBrowser;
import drbd.gui.DrbdGraph;
import drbd.utilities.MyMenu;
import drbd.utilities.MyMenuItem;
import drbd.utilities.UpdatableItem;
import drbd.utilities.Tools;
import drbd.utilities.DRBD;
import drbd.utilities.ButtonCallback;
import drbd.gui.GuiComboBox;
import drbd.data.Host;
import drbd.data.Subtext;
import drbd.data.Cluster;
import drbd.data.DRBDtestData;
import drbd.data.resources.BlockDevice;

import java.awt.Dimension;
import java.awt.Component;
import java.awt.Font;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.concurrent.CountDownLatch;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;

/**
 * This class holds info data for a block device.
 */
public class BlockDevInfo extends EditableInfo {
    /** DRBD resource in which this block device is member. */
    private DrbdResourceInfo drbdResourceInfo;
    /** Map from paremeters to the fact if the last entered value was
     * correct. */
    private final Map<String, Boolean> paramCorrectValueMap =
                                            new HashMap<String, Boolean>();
    /** Cache for the info panel. */
    private JComponent infoPanel = null;
    /** Extra options panel. */
    private final JPanel extraOptionsPanel = new JPanel();
    /** Keyword that denotes flexible meta-disk. */
    private static final String DRBD_MD_TYPE_FLEXIBLE = "Flexible";
    /** Internal parameter name of drbd meta-disk. */
    private static final String DRBD_MD_PARAM         = "DrbdMetaDisk";
    /** Internal parameter name of drbd meta-disk index. */
    private static final String DRBD_MD_INDEX_PARAM   = "DrbdMetaDiskIndex";
    /** Internal parameter name of drbd network interface. */
    private static final String DRBD_NI_PARAM         = "DrbdNetInterface";
    /** Internal parameter name of drbd network interface port. */
    private static final String DRBD_NI_PORT_PARAM    = "DrbdNetInterfacePort";
    /** Harddisc icon. */
    private static final ImageIcon HARDDISC_ICON = Tools.createImageIcon(
                                   Tools.getDefault("DrbdGraph.HarddiscIcon"));
    /** No harddisc icon. */
    private static final ImageIcon NO_HARDDISC_ICON = Tools.createImageIcon(
                                 Tools.getDefault("DrbdGraph.NoHarddiscIcon"));
    /** Meta-disk subtext. */
    private static final Subtext METADISK_SUBTEXT =
                                          new Subtext("meta-disk", Color.BLUE);
    /** Swap subtext. */
    private static final Subtext SWAP_SUBTEXT =
                                          new Subtext("swap", Color.BLUE);
    /** Mounted subtext. */
    private static final Subtext MOUNTED_SUBTEXT =
                                          new Subtext("mounted", Color.BLUE);
    /** String length after the cut. */
    private static final int MAX_RIGHT_CORNER_STRING_LENGTH = 28;

    /**
     * Prepares a new <code>BlockDevInfo</code> object.
     *
     * @param name
     *      name that will be shown in the tree
     * @param blockDevice
     *      bock device
     */
    public BlockDevInfo(final String name,
                        final BlockDevice blockDevice,
                        final Browser browser) {
        super(name, browser);
        setResource(blockDevice);
    }

    /**
     * Returns browser object of this info.
     */
    protected final HostBrowser getBrowser() {
        return (HostBrowser) super.getBrowser();
    }

    /**
     * Sets info panel of this block devices. TODO: explain why.
     */
    public final void setInfoPanel(final JComponent infoPanel) {
        this.infoPanel = infoPanel;
    }

    /**
     * Remove this block device.
     *
     * TODO: check this
     */
    public final void removeMyself(final boolean testOnly) {
        getBlockDevice().setValue(DRBD_NI_PARAM, null);
        getBlockDevice().setValue(DRBD_NI_PORT_PARAM, null);
        getBlockDevice().setValue(DRBD_MD_PARAM, null);
        getBlockDevice().setValue(DRBD_MD_INDEX_PARAM, null);
        super.removeMyself(testOnly);
        infoPanel = null;
        Tools.unregisterExpertPanel(extraOptionsPanel);
    }

    /**
     * Returns object of the other block device that is connected via drbd
     * to this block device.
     */
    public final BlockDevInfo getOtherBlockDevInfo() {
        return drbdResourceInfo.getOtherBlockDevInfo(this);
    }

    /**
     * Returns host on which is this block device.
     */
    public final Host getHost() {
        return getBrowser().getHost();
    }

    /**
     * Returns block device icon for the menu.
     */
    public final ImageIcon getMenuIcon(final boolean testOnly) {
        return HostBrowser.BD_ICON;
    }

    /**
     * Returns info of this block device as string.
     */
    public final String getInfo() {
        final StringBuffer ret = new StringBuffer(120);
        ret.append("Host            : ");
        ret.append(getHost().getName());
        ret.append("\nDevice          : ");
        ret.append(getBlockDevice().getName());
        ret.append("\nMeta disk       : ");
        ret.append(getBlockDevice().isDrbdMetaDisk());
        ret.append("\nSize            : ");
        ret.append(getBlockDevice().getBlockSize());
        ret.append(" blocks");
        if (getBlockDevice().getMountedOn() == null) {
            ret.append("\nnot mounted");
        } else {
            ret.append("\nMounted on      : ");
            ret.append(getBlockDevice().getMountedOn());
            ret.append("\nType            : ");
            ret.append(getBlockDevice().getFsType());
            if (getUsed() >= 0) {
                ret.append("\nUsed:           : ");
                ret.append(getUsed());
                ret.append('%');
            }
        }
        if (getBlockDevice().isDrbd()) {
            ret.append("\nConnection state: ");
            ret.append(getBlockDevice().getConnectionState());
            ret.append("\nNode state      : ");
            ret.append(getBlockDevice().getNodeState());
            ret.append("\nDisk state      : ");
            ret.append(getBlockDevice().getDiskState());
            ret.append('\n');
        }
        return ret.toString();
    }

    /**
     * Returns tool tip for this block device.
     */
    public final String getToolTipForGraph(final boolean testOnly) {
        final StringBuffer tt = new StringBuffer(60);

        if (getBlockDevice().isDrbd()) {
            tt.append("<b>");
            tt.append(drbdResourceInfo.getDevice());
            tt.append("</b> (");
            tt.append(getBlockDevice().getName());
            tt.append(')');
        } else {
            tt.append("<b>");
            tt.append(getBlockDevice().getName());
            tt.append("</b>");
        }
        tt.append("</b>");
        if (getBlockDevice().isDrbdMetaDisk()) {
            tt.append(" (Meta Disk)\n");
            for (final BlockDevice mb
                         : getBlockDevice().getMetaDiskOfBlockDevices()) {
                tt.append("&nbsp;&nbsp;of ");
                tt.append(mb.getName());
                tt.append('\n');
            }

        }

        if (getBlockDevice().isDrbd()) {
            if (getHost().isDrbdStatus()) {
                String cs = getBlockDevice().getConnectionState();
                String st = getBlockDevice().getNodeState();
                String ds = getBlockDevice().getDiskState();
                if (cs == null) {
                    cs = "not available";
                }
                if (st == null) {
                    st = "not available";
                }
                if (ds == null) {
                    ds = "not available";
                }

                tt.append("\n<table><tr><td><b>cs:</b></td><td>");
                tt.append(cs);
                tt.append("</td></tr><tr><td><b>ro:</b></td><td>");
                tt.append(st);
                tt.append("</td></tr><tr><td><b>ds:</b></td><td>");
                tt.append(ds);
                tt.append("</td></tr></table>");
            } else {
                tt.append('\n');
                tt.append(Tools.getString("HostBrowser.Hb.NoInfoAvailable"));
            }
        }
        return tt.toString();
    }

    /**
     * Creates config for one node.
     */
    public final String drbdNodeConfig(final String resource,
                                       final String drbdDevice)
            throws Exceptions.DrbdConfigException {

        if (drbdDevice == null) {
            throw new Exceptions.DrbdConfigException(
                                    "Drbd device not defined for host "
                                    + getHost().getName()
                                    + " (" + resource + ")");
        }
        if (getBlockDevice().getDrbdNetInterfaceWithPort() == null) {
            throw new Exceptions.DrbdConfigException(
                                    "Net interface not defined for host "
                                    + getHost().getName()
                                    + " (" + resource + ")");
        }
        if (getBlockDevice().getName() == null) {
            throw new Exceptions.DrbdConfigException(
                                    "Block device not defined for host "
                                    + getHost().getName()
                                    + " (" + resource + ")");
        }

        final StringBuffer config = new StringBuffer(120);
        config.append("\ton ");
        config.append(getHost().getName());
        config.append(" {\n\t\tdevice\t");
        config.append(drbdDevice);
        config.append(";\n\t\tdisk\t");
        config.append(getBlockDevice().getName());
        config.append(";\n\t\taddress\t");
        config.append(getBlockDevice().getDrbdNetInterfaceWithPort(
                                        getComboBoxValue(DRBD_NI_PARAM),
                                        getComboBoxValue(DRBD_NI_PORT_PARAM)));
        config.append(";\n\t\t");
        config.append(getBlockDevice().getMetaDiskString(
                                       getComboBoxValue(DRBD_MD_PARAM),
                                       getComboBoxValue(DRBD_MD_INDEX_PARAM)));
        config.append(";\n\t}\n");
        return config.toString();
    }

    public final void selectMyself() {
        super.selectMyself();
    }

    public final void setDrbd(final boolean drbd) {
        getBlockDevice().setDrbd(drbd);
    }

    protected final String getSection(final String param) {
        return getBlockDevice().getSection(param);
    }

    protected final Object[] getPossibleChoices(final String param) {
        return getBlockDevice().getPossibleChoices(param);
    }

    protected final Object getDefaultValue(final String param) {
        return "<select>";
    }

    public final int getNextVIPort() {
        int port = Tools.getDefaultInt("HostBrowser.DrbdNetInterfacePort") - 1;
        for (final String portString : getBrowser().getDrbdVIPortList()) {
            final int p = Integer.valueOf(portString);
            if (p > port) {
                port = p;
            }
        }
        return port;
    }

    public final void setDefaultVIPort(final int port) {
        final String value = Integer.toString(port);
        getBlockDevice().setValue(DRBD_NI_PORT_PARAM, value);
        getBrowser().getDrbdVIPortList().add(value);
    }

    protected final GuiComboBox getParamComboBox(final String param,
                                                 final String prefix,
                                                 final int width) {
        GuiComboBox gcb;
        if (DRBD_NI_PORT_PARAM.equals(param)) {
            final List<String> drbdVIPorts = new ArrayList<String>();
            String defaultPort = getBlockDevice().getValue(param);
            if (defaultPort == null) {
                defaultPort = getBlockDevice().getDefaultValue(param);
            }
            drbdVIPorts.add(defaultPort);
            int i = 0;
            int index = Tools.getDefaultInt("HostBrowser.DrbdNetInterfacePort");
            while (i < 10) {
                final String port = Integer.toString(index);
                if (!getBrowser().getDrbdVIPortList().contains(port)) {
                    drbdVIPorts.add(port);
                    i++;
                }
                index++;
            }
            String regexp = null;
            if (isInteger(param)) {
                regexp = "^\\d*$";
            }
            gcb = new GuiComboBox(
                       defaultPort,
                       drbdVIPorts.toArray(new String[drbdVIPorts.size()]),
                       null,
                       regexp,
                       width,
                       null);
            gcb.setValue(defaultPort);
            paramComboBoxAdd(param, prefix, gcb);
            gcb.setEnabled(true);
            gcb.setAlwaysEditable(true);
        } else if (DRBD_MD_INDEX_PARAM.equals(param)) {
            gcb = super.getParamComboBox(param, prefix, width);
            gcb.setAlwaysEditable(true);
        } else {
            gcb = super.getParamComboBox(param, prefix, width);
            gcb.setEditable(false);
        }
        return gcb;
    }

    protected final boolean checkParam(final String param, String value) {
        boolean ret = true;
        if (value == null) {
            value = "";
        }
        if ("".equals(value) && isRequired(param)) {
            ret = false;
        } else if (DRBD_MD_PARAM.equals(param)) {
            final boolean internal = "internal".equals(value);
            final GuiComboBox ind = paramComboBoxGet(DRBD_MD_INDEX_PARAM,
                                                     null);
            final GuiComboBox indW = paramComboBoxGet(DRBD_MD_INDEX_PARAM,
                                                      "wizard");
            if (internal) {
                ind.setValue(DRBD_MD_TYPE_FLEXIBLE);
                if (indW != null) {
                    indW.setValue(DRBD_MD_TYPE_FLEXIBLE);
                }
            }
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ind.setEnabled(!internal);
                }
            });
            if (indW != null) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        indW.setEnabled(!internal);
                    }
                });
            }
        } else if (DRBD_NI_PORT_PARAM.equals(param)) {
            if (getBrowser().getDrbdVIPortList().contains(value)
                && !value.equals(getBlockDevice().getValue(param))) {
                ret = false;
            }
            final Pattern p = Pattern.compile(".*\\D.*");
            final Matcher m = p.matcher(value);
            if (m.matches()) {
                ret = false;
            }
        } else if (DRBD_MD_INDEX_PARAM.equals(param)) {
            if (getBrowser().getDrbdVIPortList().contains(value)
                && !value.equals(getBlockDevice().getValue(param))) {
                ret = false;
            }
            final Pattern p = Pattern.compile(".*\\D.*");
            final Matcher m = p.matcher(value);
            if (m.matches() && !DRBD_MD_TYPE_FLEXIBLE.equals(value)) {
                ret = false;
            }
        }
        paramCorrectValueMap.remove(param);
        paramCorrectValueMap.put(param, ret);
        return ret;
    }

    protected final boolean isRequired(final String param) {
        return true;
    }

    protected final boolean isInteger(final String param) {
        if (DRBD_NI_PORT_PARAM.equals(param)) {
            return true;
        }
        return false;
    }

    protected final boolean isTimeType(final String param) {
        /* not required */
        return false;
    }


    protected final boolean isCheckBox(final String param) {
        return false;
    }

    protected final String getParamType(final String param) {
        return null;
    }

    protected final Object[] getParamPossibleChoices(final String param) {
        if (DRBD_NI_PARAM.equals(param)) {
            /* net interfaces */
            StringInfo defaultNetInterface = null;
            String netInterfaceString =
                                    getBlockDevice().getValue(DRBD_NI_PARAM);
            if (netInterfaceString == null
                || netInterfaceString.equals("")) {
                defaultNetInterface =
                    new StringInfo(
                        Tools.getString("HostBrowser.DrbdNetInterface.Select"),
                        null,
                        getBrowser());
                netInterfaceString = defaultNetInterface.toString();
                getBlockDevice().setDefaultValue(DRBD_NI_PARAM,
                                                 netInterfaceString);
            }
            return getNetInterfaces(defaultNetInterface,
                                getBrowser().getNetInterfacesNode().children());
        } else if (DRBD_MD_PARAM.equals(param)) {
            /* meta disk */
            final StringInfo internalMetaDisk =
                    new StringInfo(Tools.getString(
                                        "HostBrowser.MetaDisk.Internal"),
                                   "internal",
                                   getBrowser());
            final String defaultMetaDiskString = internalMetaDisk.toString();
            getBrowser().lockBlockDevInfos();
            final Info[] blockDevices = getAvailableBlockDevicesForMetaDisk(
                                internalMetaDisk,
                                getName(),
                                getBrowser().getBlockDevicesNode().children());
            getBrowser().unlockBlockDevInfos();

            getBlockDevice().setDefaultValue(DRBD_MD_PARAM,
                                             defaultMetaDiskString);
            return blockDevices;
        } else if (DRBD_MD_INDEX_PARAM.equals(param)) {

            String defaultMetaDiskIndex = getBlockDevice().getValue(
                                                       DRBD_MD_INDEX_PARAM);
            if ("internal".equals(defaultMetaDiskIndex)) {
                defaultMetaDiskIndex =
                         Tools.getString("HostBrowser.MetaDisk.Internal");
            }

            String[] indeces = new String[11];
            int index = 0;
            if (defaultMetaDiskIndex == null) {
                defaultMetaDiskIndex = DRBD_MD_TYPE_FLEXIBLE;
            } else if (!DRBD_MD_TYPE_FLEXIBLE.equals(defaultMetaDiskIndex)) {
                index = Integer.valueOf(defaultMetaDiskIndex) - 5;
                if (index < 0) {
                    index = 0;
                }
            }

            indeces[0] = DRBD_MD_TYPE_FLEXIBLE;
            for (int i = 1; i < 11; i++) {
                indeces[i] = Integer.toString(index);
                index++;
            }

            getBlockDevice().setDefaultValue(DRBD_MD_INDEX_PARAM,
                                             DRBD_MD_TYPE_FLEXIBLE);
            return indeces;
        }
        return null;
    }

    protected final String getParamDefault(final String param) {
        return getBlockDevice().getDefaultValue(param);
    }

    protected final String getParamPreferred(final String param) {
        return getBlockDevice().getPreferredValue(param);
    }

    protected final boolean checkParamCache(final String param) {
        if (paramCorrectValueMap.get(param) == null) {
            return false;
        }
        return paramCorrectValueMap.get(param).booleanValue();
    }

    protected final Object[] getNetInterfaces(final Info defaultValue,
                                              final Enumeration e) {
        final List<Object> list = new ArrayList<Object>();

        if (defaultValue != null) {
            list.add(defaultValue);
        }

        while (e.hasMoreElements()) {
            final Info i =
              (Info) ((DefaultMutableTreeNode) e.nextElement()).getUserObject();
            list.add(i);
        }
        return list.toArray(new Object[list.size()]);
    }

    protected final Info[] getAvailableBlockDevicesForMetaDisk(
                                                 final Info defaultValue,
                                                 final String serviceName,
                                                 final Enumeration e) {
        final List<Info> list = new ArrayList<Info>();
        final String savedMetaDisk = getBlockDevice().getValue(DRBD_MD_PARAM);

        if (defaultValue != null) {
            list.add(defaultValue);
        }

        while (e.hasMoreElements()) {
            final BlockDevInfo bdi =
              (BlockDevInfo) ((DefaultMutableTreeNode) e.nextElement())
                                                            .getUserObject();
            final BlockDevice bd = bdi.getBlockDevice();
            if (bd.toString().equals(savedMetaDisk)
                || (!bd.isDrbd() && !bd.isUsedByCRM() && !bd.isMounted())) {
                list.add(bdi);
            }
        }
        return list.toArray(new Info[list.size()]);
    }

    public final void attach(final boolean testOnly) {
        DRBD.attach(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    public final void detach(final boolean testOnly) {
        DRBD.detach(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    public final void connect(final boolean testOnly) {
        DRBD.connect(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    public final void disconnect(final boolean testOnly) {
        DRBD.disconnect(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    public final void pauseSync(final boolean testOnly) {
        DRBD.pauseSync(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    public final void resumeSync(final boolean testOnly) {
        DRBD.resumeSync(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    public final void drbdUp(final boolean testOnly) {
        DRBD.up(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    /**
     * Sets this drbd block device to the primary state.
     */
    public final void setPrimary(final boolean testOnly) {
        DRBD.setPrimary(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    /**
     * Sets this drbd block device to the secondary state.
     */
    public final void setSecondary(final boolean testOnly) {
        DRBD.setSecondary(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    /**
     * Initializes drbd block device.
     */
    public final void initDrbd(final boolean testOnly) {
        DRBD.initDrbd(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    public final void makeFilesystem(final String filesystem,
                                     final boolean testOnly) {
        DRBD.makeFilesystem(getHost(),
                            getDrbdResourceInfo().getDevice(),
                            filesystem,
                            testOnly);
    }

    public final void forcePrimary(final boolean testOnly) {
        DRBD.forcePrimary(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    public final void invalidateBD(final boolean testOnly) {
        DRBD.invalidate(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    public final void discardData(final boolean testOnly) {
        DRBD.discardData(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    public final void resizeDrbd(final boolean testOnly) {
        DRBD.resize(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    public final JPanel getGraphicalView() {
        if (getBlockDevice().isDrbd()) {
            drbdResourceInfo.getDrbdInfo().setSelectedNode(this);
        }
        return getBrowser().getDrbdGraph().getDrbdInfo().getGraphicalView();
    }

    protected final void setTerminalPanel() {
        if (getHost() != null) {
            Tools.getGUIData().setTerminalPanel(getHost().getTerminalPanel());
        }
    }

    public final JComponent getInfoPanel() {
        //setTerminalPanel();
        return getInfoPanelBD();
    }

    public final String[] getParametersFromXML() {
        final String[] params = {
                            DRBD_NI_PARAM,
                            DRBD_NI_PORT_PARAM,
                            DRBD_MD_PARAM,
                            DRBD_MD_INDEX_PARAM,
                          };
        return params;
    }

    public final void apply(final boolean testOnly) {
        if (!testOnly) {
            final String[] params = getParametersFromXML();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    applyButton.setEnabled(false);
                }
            });
            if (getBlockDevice().getMetaDisk() != null) {
                getBlockDevice().getMetaDisk().removeMetadiskOfBlockDevice(
                                                             getBlockDevice());
            }
            getBrowser().getDrbdVIPortList().remove(
                               getBlockDevice().getValue(DRBD_NI_PORT_PARAM));

            storeComboBoxValues(params);

            getBrowser().getDrbdVIPortList().add(
                                getBlockDevice().getValue(DRBD_NI_PORT_PARAM));
            final Object o = paramComboBoxGet(DRBD_MD_PARAM, null).getValue();
            if (Tools.isStringInfoClass(o)) {
                getBlockDevice().setMetaDisk(null); /* internal */
            } else {
                final BlockDevice metaDisk =
                                        ((BlockDevInfo) o).getBlockDevice();
                getBlockDevice().setMetaDisk(metaDisk);
            }
            checkResourceFields(null, params);
        }
    }

    public final JComponent getInfoPanelBD() {
        if (infoPanel != null) {
            return infoPanel;
        }
        final BlockDevInfo thisClass = this;
        final ButtonCallback buttonCallback = new ButtonCallback() {
            private volatile boolean mouseStillOver = false;

            /**
             * Whether the whole thing should be enabled.
             */
            public final boolean isEnabled() {
                return true;
            }

            public final void mouseOut() {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = false;
                final DrbdGraph drbdGraph = getBrowser().getDrbdGraph();
                drbdGraph.stopTestAnimation(applyButton);
                applyButton.setToolTipText(null);
            }

            public final void mouseOver() {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = true;
                applyButton.setToolTipText(Tools.getString(
                                         "ClusterBrowser.StartingDRBDtest"));
                applyButton.setToolTipBackground(Tools.getDefaultColor(
                                  "ClusterBrowser.Test.Tooltip.Background"));
                Tools.sleep(250);
                if (!mouseStillOver) {
                    return;
                }
                mouseStillOver = false;
                final CountDownLatch startTestLatch = new CountDownLatch(1);
                final DrbdGraph drbdGraph = getBrowser().getDrbdGraph();
                drbdGraph.startTestAnimation(applyButton, startTestLatch);
                getBrowser().drbdtestLockAcquire();
                thisClass.setDRBDtestData(null);
                apply(true);
                final Map<Host,String> testOutput =
                                         new LinkedHashMap<Host, String>();
                try {
                    drbdResourceInfo.getDrbdInfo().createDrbdConfig(true);
                    for (final Host h
                                    : getHost().getCluster().getHostsArray()) {
                        DRBD.adjust(h, "all", true);
                        testOutput.put(h, DRBD.getDRBDtest());
                    }
                } catch (Exceptions.DrbdConfigException dce) {
                    Tools.appError("config failed");
                }
                final DRBDtestData dtd = new DRBDtestData(testOutput);
                applyButton.setToolTipText(dtd.getToolTip());
                thisClass.setDRBDtestData(dtd);
                getBrowser().drbdtestLockRelease();
                startTestLatch.countDown();
            }
        };
        initApplyButton(buttonCallback);

        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(HostBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(HostBrowser.STATUS_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setBackground(HostBrowser.PANEL_BACKGROUND);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        extraOptionsPanel.setBackground(HostBrowser.PANEL_BACKGROUND);
        extraOptionsPanel.setLayout(new BoxLayout(extraOptionsPanel,
                                                  BoxLayout.Y_AXIS));
        extraOptionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        /* Actions */
        final JMenuBar mb = new JMenuBar();
        mb.setBackground(HostBrowser.PANEL_BACKGROUND);
        final JMenu serviceCombo = getActionsMenu();
        updateMenus(null);
        mb.add(serviceCombo);

        if (getBlockDevice().isDrbd()) {
            buttonPanel.add(mb, BorderLayout.EAST);
            final String[] params = getParametersFromXML();

            addParams(optionsPanel,
                      extraOptionsPanel,
                      params,
                      Tools.getDefaultInt("HostBrowser.DrbdDevLabelWidth"),
                      Tools.getDefaultInt("HostBrowser.DrbdDevFieldWidth"));


            /* apply button */
            applyButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    final Thread thread = new Thread(new Runnable() {
                        public void run() {
                            apply(false);
                            try {
                                drbdResourceInfo.getDrbdInfo()
                                              .createDrbdConfig(false);
                            } catch (Exceptions.DrbdConfigException e) {
                                Tools.appError("config failed");
                            }
                        }
                    });
                    thread.start();
                }
            });
            addApplyButton(buttonPanel);

            applyButton.setEnabled(checkResourceFields(null, params));

            /* expert mode */
            Tools.registerExpertPanel(extraOptionsPanel);
        }

        /* info */
        final Font f = new Font("Monospaced", Font.PLAIN, 12);
        final JPanel riaPanel = new JPanel();
        riaPanel.setBackground(HostBrowser.PANEL_BACKGROUND);
        riaPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        riaPanel.add(super.getInfoPanel());
        mainPanel.add(riaPanel);

        mainPanel.add(optionsPanel);
        mainPanel.add(extraOptionsPanel);
        final JPanel newPanel = new JPanel();
        newPanel.setBackground(HostBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
        newPanel.add(buttonPanel);
        newPanel.add(new JScrollPane(mainPanel));
        newPanel.add(Box.createVerticalGlue());
        infoPanel = newPanel;
        return infoPanel;
    }

    public final boolean selectAutomaticallyInTreeMenu() {
        // TODO: dead code?
        return infoPanel == null;
    }

    /**
     * Sets drbd resource for this block device.
     */
    public final void setDrbdResourceInfo(
                                final DrbdResourceInfo drbdResourceInfo) {
        this.drbdResourceInfo = drbdResourceInfo;
    }

    /**
     * Returns drbd resource info in which this block device is member.
     */
    public final DrbdResourceInfo getDrbdResourceInfo() {
        return drbdResourceInfo;
    }

    /**
     * @return block device resource object.
     */
    public final BlockDevice getBlockDevice() {
        return (BlockDevice) getResource();
    }

    /**
     * Removes this block device from drbd data structures.
     */
    public final void removeFromDrbd() {
        getBrowser().getDrbdVIPortList().remove(
                                getBlockDevice().getValue(DRBD_NI_PORT_PARAM));
        setDrbd(false);
        setDrbdResourceInfo(null);
    }

    /**
     * Returns short description of the parameter.
     */
    protected final String getParamShortDesc(final String param) {
        return Tools.getString(param);
    }

    /**
     * Returns long description of the parameter.
     */
    protected final String getParamLongDesc(final String param) {
        return Tools.getString(param + ".Long");
    }

    /**
     * Returns 'add drbd resource' menu item.
     */
    private MyMenuItem addDrbdResourceMenuItem(final BlockDevInfo oBdi,
                                               final boolean testOnly) {
        final BlockDevInfo thisClass = this;
        return new MyMenuItem(oBdi.toString()) {
            private static final long serialVersionUID = 1L;
            public void action() {
                final DrbdInfo drbdInfo =
                                    getBrowser().getDrbdGraph().getDrbdInfo();
                setInfoPanel(null);
                oBdi.setInfoPanel(null);
                drbdInfo.addDrbdResource(null,
                                         null,
                                         thisClass,
                                         oBdi,
                                         true,
                                         testOnly);
            }
        };
    }

    /**
     * Creates popup for the block device.
     */
    public final List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final BlockDevInfo thisClass = this;
        if (!getBlockDevice().isDrbd() && !getBlockDevice().isAvailable()) {
            /* block devices are not available */
            return null;
        }
        final boolean testOnly = false;
        final MyMenu repMenuItem = new MyMenu(
                        Tools.getString("HostBrowser.Drbd.AddDrbdResource")) {
            private static final long serialVersionUID = 1L;

            public final boolean enablePredicate() {
                return drbdResourceInfo == null
                       && getHost().isConnected()
                       && getHost().isDrbdLoaded();
            }

            public void update() {
                super.update();
                removeAll();
                Cluster cluster = getHost().getCluster();
                Host[] otherHosts = cluster.getHostsArray();
                for (final Host oHost : otherHosts) {
                    if (oHost == getHost()) {
                        continue;
                    }
                    MyMenu hostMenu = new MyMenu(oHost.getName()) {
                        private static final long serialVersionUID = 1L;

                        public final boolean enablePredicate() {
                            return oHost.isConnected()
                                   && oHost.isDrbdLoaded();
                        }

                        public final void update() {
                            super.update();
                            removeAll();
                            List<BlockDevInfo> blockDevInfos =
                                        oHost.getBrowser().getBlockDevInfos();
                            List<BlockDevInfo> blockDevInfosS =
                                                new ArrayList<BlockDevInfo>();
                            for (final BlockDevInfo oBdi : blockDevInfos) {
                                if (oBdi.getName().equals(
                                             getBlockDevice().getName())) {
                                    blockDevInfosS.add(0, oBdi);
                                } else {
                                    blockDevInfosS.add(oBdi);
                                }
                            }

                            for (final BlockDevInfo oBdi : blockDevInfosS) {
                                if (oBdi.getDrbdResourceInfo() == null
                                    && oBdi.getBlockDevice().isAvailable()) {
                                    add(addDrbdResourceMenuItem(oBdi,
                                                                testOnly));
                                }
                                if (oBdi.getName().equals(
                                            getBlockDevice().getName())) {
                                    addSeparator();
                                }
                            }
                        }
                    };
                    hostMenu.update();
                    add(hostMenu);
                }
            }
        };
        items.add(repMenuItem);
        registerMenuItem(repMenuItem);
        /* attach / detach */
        final MyMenuItem attachMenu =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.Detach"),
                           NO_HARDDISC_ICON,
                           Tools.getString("HostBrowser.Drbd.Detach.ToolTip"),

                           Tools.getString("HostBrowser.Drbd.Attach"),
                           HARDDISC_ICON,
                           Tools.getString("HostBrowser.Drbd.Attach.ToolTip")) {
                private static final long serialVersionUID = 1L;

                public boolean predicate() {
                    return !getBlockDevice().isDrbd()
                           || getBlockDevice().isAttached();
                }

                public boolean enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return false;
                    }
                    return !getBlockDevice().isSyncing();
                }

                public void action() {
                    if (this.getText().equals(
                                Tools.getString("HostBrowser.Drbd.Attach"))) {
                        attach(testOnly);
                    } else {
                        detach(testOnly);
                    }
                }
            };
        final ClusterBrowser cb = getBrowser().getClusterBrowser();
        if (cb != null) {
            final ClusterBrowser.DRBDMenuItemCallback attachItemCallback =
                            cb.new DRBDMenuItemCallback(attachMenu, getHost()) {
                public void action(final Host host) {
                    if (isDiskless(false)) {
                        attach(true);
                    } else {
                        detach(true);
                    }
                }
            };
            addMouseOverListener(attachMenu, attachItemCallback);
        }
        items.add(attachMenu);
        registerMenuItem(attachMenu);

        /* connect / disconnect */
        final MyMenuItem connectMenu =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.Disconnect"),
                           null,
                           null,
                           Tools.getString("HostBrowser.Drbd.Connect"),
                           null,
                           null) {
                private static final long serialVersionUID = 1L;

                public boolean predicate() {
                    return isConnectedOrWF(testOnly);
                }

                public boolean enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return false;
                    }
                    return !getBlockDevice().isSyncing()
                        || ((getBlockDevice().isPrimary()
                            && getBlockDevice().isSyncSource())
                            || (getOtherBlockDevInfo().getBlockDevice().
                                                                isPrimary()
                                && getBlockDevice().isSyncTarget()));
                }

                public void action() {
                    if (this.getText().equals(
                            Tools.getString("HostBrowser.Drbd.Connect"))) {
                        connect(testOnly);
                    } else {
                        disconnect(testOnly);
                    }
                }
            };
        if (cb != null) {
            final ClusterBrowser.DRBDMenuItemCallback connectItemCallback =
                               cb.new DRBDMenuItemCallback(connectMenu,
                                                           getHost()) {
                public void action(final Host host) {
                    if (isConnectedOrWF(false)) {
                        disconnect(true);
                    } else {
                        connect(true);
                    }
                }
            };
            addMouseOverListener(connectMenu, connectItemCallback);
        }
        items.add(connectMenu);
        registerMenuItem(connectMenu);

        /* set primary */
        final MyMenuItem setPrimaryItem =
            new MyMenuItem(Tools.getString(
                              "HostBrowser.Drbd.SetPrimaryOtherSecondary"),
                           null,
                           null,

                           Tools.getString("HostBrowser.Drbd.SetPrimary"),
                           null,
                           null) {
                private static final long serialVersionUID = 1L;

                public boolean predicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return false;
                    }
                    return getBlockDevice().isSecondary()
                         && getOtherBlockDevInfo().getBlockDevice().isPrimary();
                }

                public boolean enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return false;
                    }
                    return getBlockDevice().isSecondary();
                }

                public void action() {
                    BlockDevInfo oBdi = getOtherBlockDevInfo();
                    if (oBdi != null && oBdi.getBlockDevice().isPrimary()) {
                        oBdi.setSecondary(testOnly);
                    }
                    setPrimary(testOnly);
                }
            };
        items.add(setPrimaryItem);
        registerMenuItem(setPrimaryItem);

        /* set secondary */
        final MyMenuItem setSecondaryItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.SetSecondary"),
                           null,
                           Tools.getString(
                                "HostBrowser.Drbd.SetSecondary.ToolTip")) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return false;
                    }
                    return getBlockDevice().isPrimary();
                }

                public void action() {
                    setSecondary(testOnly);
                }
            };
        //enableMenu(setSecondaryItem, false);
        items.add(setSecondaryItem);
        registerMenuItem(setSecondaryItem);

        /* force primary */
        final MyMenuItem forcePrimaryItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.ForcePrimary"),
                           null,
                           null) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return false;
                    }
                    return true;
                }

                public void action() {
                    forcePrimary(testOnly);
                }
            };
        items.add(forcePrimaryItem);
        registerMenuItem(forcePrimaryItem);

        /* invalidate */
        final MyMenuItem invalidateItem =
            new MyMenuItem(
                   Tools.getString("HostBrowser.Drbd.Invalidate"),
                   null,
                   Tools.getString("HostBrowser.Drbd.Invalidate.ToolTip")) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return false;
                    }
                    return !getBlockDevice().isSyncing();
                }

                public void action() {
                    invalidateBD(testOnly);
                }
            };
        items.add(invalidateItem);
        registerMenuItem(invalidateItem);

        /* resume / pause sync */
        final MyMenuItem resumeSyncItem =
            new MyMenuItem(
                       Tools.getString("HostBrowser.Drbd.ResumeSync"),
                       null,
                       Tools.getString("HostBrowser.Drbd.ResumeSync.ToolTip"),

                       Tools.getString("HostBrowser.Drbd.PauseSync"),
                       null,
                       Tools.getString("HostBrowser.Drbd.PauseSync.ToolTip")) {
                private static final long serialVersionUID = 1L;

                public boolean predicate() {
                    return getBlockDevice().isSyncing()
                           && getBlockDevice().isPausedSync();
                }

                public boolean enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return false;
                    }
                    return getBlockDevice().isSyncing();
                }

                public void action() {
                    if (this.getText().equals(
                            Tools.getString("HostBrowser.Drbd.ResumeSync"))) {
                        resumeSync(testOnly);
                    } else {
                        pauseSync(testOnly);
                    }
                }
            };
        items.add(resumeSyncItem);
        registerMenuItem(resumeSyncItem);

        /* resize */
        final MyMenuItem resizeItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.Resize"),
                           null,
                           Tools.getString("HostBrowser.Drbd.Resize.ToolTip")) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return false;
                    }
                    return !getBlockDevice().isSyncing();
                }

                public void action() {
                    resizeDrbd(testOnly);
                }
            };
        items.add(resizeItem);
        registerMenuItem(resizeItem);

        /* discard my data */
        final MyMenuItem discardDataItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.DiscardData"),
                           null,
                           Tools.getString(
                                     "HostBrowser.Drbd.DiscardData.ToolTip")) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return false;
                    }
                    return !getBlockDevice().isSyncing()
                           && !isConnected(testOnly)
                           && !getBlockDevice().isPrimary();
                }

                public void action() {
                    discardData(testOnly);
                }
            };
        items.add(discardDataItem);
        registerMenuItem(discardDataItem);

        /* view log */
        final MyMenuItem viewDrbdLogItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.ViewDrbdLog"),
                           null,
                           null) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return false;
                    }
                    return true;
                }

                public void action() {
                    String device = getDrbdResourceInfo().getDevice();
                    drbd.gui.dialog.DrbdLog l =
                                new drbd.gui.dialog.DrbdLog(getHost(), device);
                    l.showDialog();
                }
            };
        items.add(viewDrbdLogItem);
        registerMenuItem(viewDrbdLogItem);

        return items;
    }

    /**
     * Returns how much of the block device is used.
     */
     public final int getUsed() {
         if (drbdResourceInfo != null) {
             return drbdResourceInfo.getUsed();
         }
         return getBlockDevice().getUsed();
     }

     /**
      * Returns text that appears above the icon.
      */
    public String getIconTextForGraph(final boolean testOnly) {
        if (!getHost().isConnected()) {
            return Tools.getString("HostBrowser.Drbd.NoInfoAvailable");
        }
        if (getBlockDevice().isDrbd()) {
            return getBlockDevice().getNodeState();
        }
        return null;
    }

    /**
     * Returns text that appears in the corner of the drbd graph.
     */
    public Subtext getRightCornerTextForDrbdGraph(final boolean testOnly) {
         if (getBlockDevice().isDrbdMetaDisk()) {
             return METADISK_SUBTEXT;
         } else if (getBlockDevice().isSwap()) {
             return SWAP_SUBTEXT;
         } else if (getBlockDevice().getMountedOn() != null) {
             return MOUNTED_SUBTEXT;
         } else if (getBlockDevice().isDrbd()) {
             String s = getBlockDevice().getName();
             // TODO: cache that
             if (s.length() > MAX_RIGHT_CORNER_STRING_LENGTH) {
                 s = "..." + s.substring(
                               s.length()
                               - MAX_RIGHT_CORNER_STRING_LENGTH + 3,
                               s.length());
             }
             return new Subtext(s, Color.BLUE);
         }
         return null;
    }
    /**
     * Returns whether this device is connected via drbd.
     */
    public final boolean isConnected(final boolean testOnly) {
        final DRBDtestData dtd = getDRBDtestData();
        if (testOnly && dtd != null) {
            return isConnectedTest(dtd) && !isWFConnection(testOnly);
        } else {
            return getBlockDevice().isConnected();
        }
    }

    /**
     * Returns whether this device is connected or wait-for-c via drbd.
     */
    public final boolean isConnectedOrWF(final boolean testOnly) {
        final DRBDtestData dtd = getDRBDtestData();
        if (testOnly && dtd != null) {
            return isConnectedTest(dtd);
        } else {
            return getBlockDevice().isConnectedOrWF();
        }
    }

    /**
     * Returns whether this device is in wait-for-connection state.
     */
    public final boolean isWFConnection(final boolean testOnly) {
        final DRBDtestData dtd = getDRBDtestData();
        if (testOnly && dtd != null) {
            return isConnectedOrWF(testOnly)
                   && isConnectedTest(dtd)
                   && !getOtherBlockDevInfo().isConnectedTest(dtd);
        } else {
            return getBlockDevice().isWFConnection();
        }
    }

    /**
     * Returns whether this device will be disconnected.
     */
    public final boolean isConnectedTest(final DRBDtestData dtd) {
        return dtd.isConnected(getHost(), drbdResourceInfo.getDevice())
               || (!dtd.isDisconnected(getHost(),
                                       drbdResourceInfo.getDevice())
                   && getBlockDevice().isConnectedOrWF());
    }

    /**
     * Returns whether this device is diskless.
     */
    public final boolean isDiskless(final boolean testOnly) {
        final DRBDtestData dtd = getDRBDtestData();
        final DrbdResourceInfo dri = drbdResourceInfo;
        if (testOnly && dtd != null && dri != null) {
            return dtd.isDiskless(getHost(), drbdResourceInfo.getDevice())
                   || (!dtd.isAttached(getHost(),
                                       drbdResourceInfo.getDevice())
                       && getBlockDevice().isDiskless());
        } else {
            return getBlockDevice().isDiskless();
        }
    }

    /**
     * Returns drbd test data.
     */
    public final DRBDtestData getDRBDtestData() {
        final ClusterBrowser b = getBrowser().getClusterBrowser();
        if (b == null) {
            return null;
        }
        return b.getDRBDtestData();
    }

    /**
     * Sets drbd test data.
     */
    public final void setDRBDtestData(final DRBDtestData drbdtestData) {
        final ClusterBrowser b = getBrowser().getClusterBrowser();
        if (b == null) {
            return;
        }
        b.setDRBDtestData(drbdtestData);
    }
}
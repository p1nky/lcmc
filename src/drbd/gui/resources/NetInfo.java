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

import drbd.gui.Browser;
import drbd.gui.HostBrowser;
import drbd.data.resources.NetInterface;
import drbd.utilities.Tools;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;

/**
 * This class holds info data for a net interface.
 */
public class NetInfo extends Info {
    /** Net interface icon. */
    private static final ImageIcon NET_I_ICON = Tools.createImageIcon(
                                   Tools.getDefault("HostBrowser.NetIntIcon"));
    /**
     * Prepares a new <code>NetInfo</code> object.
     */
    public NetInfo(final String name,
                   final NetInterface netInterface,
                   final Browser browser) {
        super(name, browser);
        setResource(netInterface);
    }

    /**
     * Returns browser object of this info.
     */
    protected final HostBrowser getBrowser() {
        return (HostBrowser) super.getBrowser();
    }

    /**
     * Returns info of this net interface, which is updatable.
     */
    public final void updateInfo(final JEditorPane ep) {
        final Runnable runnable = new Runnable() {
            public void run() {
                String text = Tools.execCommand(getBrowser().getHost(),
                                                "/sbin/ifconfig "
                                                + getName(),
                                                null,   /* ExecCallback */
                                                false); /* outputVisible */
                ep.setText("<html><pre>" + text + "</html></pre>");
            }
        };
        final Thread thread = new Thread(runnable);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    /**
     * Returns string representation of the net interface.
     */
    public final String toString() {
        final StringBuffer s = new StringBuffer(getName());
        final String ip = getNetInterface().getIp();
        if (ip != null) {
            s.append(" (" + ip + ")");
        }
        return s.toString();
    }

    /**
     * Returns icon of the net interface for the menu.
     */
    public final ImageIcon getMenuIcon(final boolean testOnly) {
        return NET_I_ICON;
    }

    /**
     * Returns ip of the net interface.
     */
    public final String getStringValue() {
        return getNetInterface().getIp();
    }

    /**
     * Returns net interface resource.
     */
    public final NetInterface getNetInterface() {
        return (NetInterface) getResource();
    }

    /**
     * Returns whether ips equal.
     */
    public final boolean equals(final Object value) {
        if (Tools.isStringClass(value)) {
            // TODO: race is here
            final NetInterface ni = getNetInterface();
            if (value == null) {
                return false;
            }
            if (ni == null) {
                return false;
            }
            final String ip = ni.getIp();
            if (ip == null) {
                return false;
            }
            return ip.equals(value.toString());
        } else {
            return toString().equals(value.toString());
        }
    }

    //public int hashCode() {
    //    return toString().hashCode();
    //}
}
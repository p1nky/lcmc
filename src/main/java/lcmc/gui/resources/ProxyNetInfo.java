/*
 * This file is part of Linux Cluster Management Console
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009-2010, Rasto Levrinc
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with drbd; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.gui.resources;

import lcmc.gui.Browser;
import lcmc.gui.HostBrowser;
import lcmc.data.resources.NetInterface;
import lcmc.data.Host;
import lcmc.utilities.Tools;
import lcmc.utilities.SSH;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;

/**
 * This class holds info data for a net interface on a drbd proxy host.
 */
public final class ProxyNetInfo extends NetInfo {
    final static Host SAME_HOST = null;

    private final Host proxyHost;
    public final static String PROXY_PREFIX = "proxy: ";

    /** Prepares a new <code>NetProxyInfo</code> object. */
    public ProxyNetInfo(final NetInfo netInfo, final Host proxyHost) {
        super(netInfo.getName(),
              netInfo.getNetInterface(),
              netInfo.getBrowser());
        this.proxyHost = proxyHost;
    }

    /** Prepares a new <code>NetProxyInfo</code> object. */
    public ProxyNetInfo(final String name,
                        final NetInterface netInterface,
                        final Browser browser,
                        final Host proxyHost) {
        super(name, netInterface, browser);
        this.proxyHost = proxyHost;
    }

    /** Returns string representation of the net interface. */
    @Override
    public String toString() {
        final String ip = getNetInterface().getIp();
        String proxyHostName = null;
        if (proxyHost != null) {
            proxyHostName = proxyHost.getName();
        }
        return displayString(ip,
                             getBrowser().getHost().getName(),
                             proxyHostName);
    }

    /** Reformat to the form that appears in the GUI. */
    public static String displayString(final String someIp,
                                       final String someHost,
                                       final String someProxyHost) {
        final StringBuilder s = new StringBuilder(PROXY_PREFIX);
        if (someIp != null) {
            s.append(someIp);
        }
        if (!someHost.equals(someProxyHost)) {
            s.append(" on ");
            s.append(someProxyHost);
        }
        return s.toString();
    }

    /** Return proxy host. */
    public Host getProxyHost() {
        return proxyHost;
    }
}

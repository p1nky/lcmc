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
import drbd.gui.ClusterBrowser;
import drbd.gui.GuiComboBox;

import drbd.data.resources.Resource;
import drbd.data.CRMXML;
import drbd.data.ClusterStatus;
import drbd.data.ConfigData;
import drbd.utilities.Tools;

import java.util.Collection;
import java.util.Map;
import javax.swing.JPanel;

/**
 * This class is for resource defaults or rsc_defaults.
 */
public class RscDefaultsInfo extends EditableInfo {
    /**
     * Prepares a new <code>RscDefaultsInfo</code> object and creates
     * new rsc defaults object.
     */
    public RscDefaultsInfo(final String name,
                           final Browser browser) {
        super(name, browser);
        setResource(new Resource(name));
    }

    /**
     * Returns browser object of this info.
     */
    protected final ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /**
     * Sets default parameters with values from resourceNode hash.
     */
    public final void setParameters(final Map<String, String> resourceNode) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        if (crmXML == null) {
            Tools.appError("crmXML is null");
            return;
        }
        /* Attributes */
        final String[] params = getParametersFromXML();
        final ClusterStatus cs = getBrowser().getClusterStatus();
        if (params != null) {
            for (String param : params) {
                String value = resourceNode.get(param);
                final String defaultValue = getParamDefault(param);
                if (value == null) {
                    value = defaultValue;
                }
                if (value == null) {
                    value = "";
                }
                final String oldValue = getParamSaved(param);
                final GuiComboBox cb = paramComboBoxGet(param, null);
                final boolean haveChanged =
                   !Tools.areEqual(value, oldValue)
                   || !Tools.areEqual(defaultValue,
                                      getResource().getDefaultValue(param));
                if (haveChanged) {
                    getResource().setValue(param, value);
                    getResource().setDefaultValue(param, defaultValue);
                    if (cb != null) {
                        cb.setValue(value);
                    }
                }
            }
        }
    }

    /**
     * Returns parameters.
     */
    public final String[] getParametersFromXML() {
        final CRMXML crmXML = getBrowser().getCRMXML();
        final Collection<String> params =
                                   crmXML.getRscDefaultsParameters().keySet();
        return params.toArray(new String[params.size()]);
    }

    /**
     * Returns true if the value of the parameter is ok.
     */
    protected final boolean checkParam(final String param,
                                       final String newValue) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.checkMetaAttrParam(param, newValue);
    }

    /**
     * Returns default value for specified parameter.
     */
    protected final String getParamDefault(final String param) {
        if ("resource-stickiness".equals(param)) {
            return getBrowser().getServicesInfo().getResource().getValue(
                                                "default-resource-stickiness");
        }
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getRscDefaultsDefault(param);
    }

    /**
     * Returns saved value for specified parameter.
     */
    protected final String getParamSaved(final String param) {
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        String value = super.getParamSaved(param);
        if (value == null) {
            value = clStatus.getRscDefaultsParameter(param, false);
            if (value == null) {
                value = getParamPreferred(param);
                if (value == null) {
                    return getParamDefault(param);
                }
            }
        }
        return value;
    }

    /**
     * Returns preferred value for specified parameter.
     */
    protected final String getParamPreferred(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getRscDefaultsPreferred(param);
    }

    /**
     * Returns possible choices for drop down lists.
     */
    protected final Object[] getParamPossibleChoices(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        if (isCheckBox(param)) {
            return crmXML.getRscDefaultsCheckBoxChoices(param);
        } else {
            return crmXML.getRscDefaultsPossibleChoices(param);
        }
    }

    /**
     * Returns short description of the specified parameter.
     */
    protected final String getParamShortDesc(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getRscDefaultsShortDesc(param);
    }

    /**
     * Returns long description of the specified parameter.
     */
    protected final String getParamLongDesc(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getRscDefaultsLongDesc(param);
    }

    /**
     * Returns section to which the specified parameter belongs.
     */
    protected final String getSection(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getRscDefaultsSection(param);
    }

    /** Returns true if the specified parameter is advanced. */
    protected final boolean isAdvanced(final String param) {
        if (!Tools.areEqual(getParamDefault(param),
                            getParamSaved(param))) {
            /* it changed, show it */
            return false;
        }
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.isRscDefaultsAdvanced(param);
    }

    /** Returns access type of this parameter. */
    protected final ConfigData.AccessType getAccessType(String param) {
        return getBrowser().getCRMXML().getRscDefaultsAccessType(param);
    }

    /** Returns true if the specified parameter is required. */
    protected final boolean isRequired(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.isRscDefaultsRequired(param);
    }

    /**
     * Returns true if the specified parameter is meta attribute.
     */
    protected final boolean isMetaAttr(final String param) {
        return true;
    }

    /**
     * Returns true if the specified parameter is integer.
     */
    protected final boolean isInteger(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.isRscDefaultsInteger(param);
    }

    /**
     * Returns true if the specified parameter is of time type.
     */
    protected final boolean isTimeType(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.isRscDefaultsTimeType(param);
    }

    /**
     * Returns whether parameter is checkbox.
     */
    protected final boolean isCheckBox(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.isRscDefaultsBoolean(param);
    }

    /**
     * Returns the type of the parameter according to the OCF.
     */
    protected final String getParamType(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getRscDefaultsType(param);
    }

    /**
     * Returns panel with graph.
     */
    public final JPanel getGraphicalView() {
        return getBrowser().getHeartbeatGraph().getGraphPanel();
    }

    /**
     * Check the fields.
     */
    public final boolean checkResourceFields(final String param,
                                             final String[] params) {
        final ServicesInfo ssi = getBrowser().getServicesInfo();
        return ssi.checkResourceFields(param, ssi.getParametersFromXML());
    }
}
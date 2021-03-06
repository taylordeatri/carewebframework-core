/*
 * #%L
 * carewebframework
 * %%
 * Copyright (C) 2008 - 2016 Regenstrief Institute, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * This Source Code Form is also subject to the terms of the Health-Related
 * Additional Disclaimer of Warranty and Limitation of Liability available at
 *
 *      http://www.carewebframework.org/licensing/disclaimer.
 *
 * #L%
 */
package org.carewebframework.help.viewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import org.carewebframework.common.StrUtil;
import org.carewebframework.help.HelpSearchHit;
import org.carewebframework.help.HelpTopic;
import org.carewebframework.help.HelpViewType;
import org.carewebframework.help.IHelpSearch.IHelpSearchListener;
import org.carewebframework.help.IHelpSet;

import org.zkoss.image.AImage;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Textbox;

/**
 * Tab supporting the help system search function. Consists of a text box into which the user may
 * enter a search expression (including boolean operators) and a list box to display the results of
 * the search.
 */
public class HelpSearchTab extends HelpTab implements ListitemRenderer<HelpSearchHit>, IHelpSearchListener {
    
    private static final long serialVersionUID = 1L;
    
    private Textbox txtSearch;
    
    private Listbox lstSrchResults;
    
    private Label lblNoResultsFound;
    
    private final AImage[] icons = new AImage[3];
    
    private final List<IHelpSet> helpSets = new ArrayList<>();
    
    private double tertile1;
    
    private double tertile2;
    
    private final EventListener<Event> searchListener = new EventListener<Event>() {
        
        @Override
        public void onEvent(Event event) throws Exception {
            @SuppressWarnings("unchecked")
            List<HelpSearchHit> searchResults = (List<HelpSearchHit>) event.getData();
            Collections.sort(searchResults);
            
            if (searchResults.isEmpty()) {
                showMessage("cwf.help.tab.search.noresults");
                return;
            }
            
            double highscore = searchResults.get(0).getConfidence();
            double lowscore = searchResults.get(searchResults.size() - 1).getConfidence();
            double interval = (highscore - lowscore) / 3;
            tertile1 = lowscore + interval;
            tertile2 = tertile1 + interval;
            lstSrchResults.setModel(new ListModelList<>(searchResults));
        }
        
    };
    
    /**
     * Create the help tab for the specified viewer and viewType.
     * 
     * @param viewer The help viewer.
     * @param viewType The view type.
     */
    public HelpSearchTab(HelpViewer viewer, HelpViewType viewType) {
        super(viewer, viewType, "helpSearchTab.zul");
        lstSrchResults.setItemRenderer(this);
    }
    
    /**
     * Sets the focus to the search text box when the tab is selected.
     * 
     * @see org.carewebframework.help.viewer.HelpTab#onSelect()
     */
    @Override
    public void onSelect() {
        super.onSelect();
        txtSearch.select();
        txtSearch.setFocus(true);
    }
    
    /**
     * Sets the currently viewed topic when a search result is selected.
     */
    public void onSelect$lstSrchResults() {
        Listitem item = lstSrchResults.getSelectedItem();
        lstSrchResults.renderItem(item);
        setTopic((HelpTopic) item.getValue());
    }
    
    /**
     * Perform search when user presses enter button.
     */
    public void onOK$txtSearch() {
        onClick$btnSearch();
    }
    
    /**
     * Perform the search and display the results.
     */
    public void onClick$btnSearch() {
        lstSrchResults.setModel((ListModelList<?>) null);
        lstSrchResults.getItems().clear();
        String query = txtSearch.getValue();
        showMessage(null);
        
        if (query != null && query.trim().length() > 0) {
            HelpUtil.getSearchService().search(query, helpSets, this);
        } else {
            showMessage("cwf.help.tab.search.noentry");
        }
    }
    
    /**
     * Returns the icon that represents the specified score. There are three icons available based
     * on within which tertile the score falls.
     * 
     * @param score The relevancy score.
     * @return Image to represent relevancy.
     */
    private AImage toImage(double score) {
        int tertile = score >= tertile2 ? 2 : score >= tertile1 ? 1 : 0;
        AImage aimage = icons[tertile];
        
        if (aimage == null) {
            String img = tertile <= 0 ? "empty" : tertile == 1 ? "half" : "full";
            aimage = HelpUtil.getImageContent(img + ".png");
            icons[tertile] = aimage;
        }
        
        return aimage;
    }
    
    /**
     * Displays the specified message. The list box is hidden if the message is not empty.
     * 
     * @param message Message to display.
     */
    private void showMessage(String message) {
        message = message == null ? null : StrUtil.getLabel(message);
        lblNoResultsFound.setValue(message);
        lblNoResultsFound.setVisible(!StringUtils.isEmpty(message));
        lstSrchResults.setVisible(!lblNoResultsFound.isVisible());
    }
    
    /**
     * Renders the list box contents.
     * 
     * @see org.zkoss.zul.ListitemRenderer#render
     */
    @Override
    public void render(Listitem item, HelpSearchHit qr, int index) throws Exception {
        double score = qr.getConfidence();
        item.setValue(qr.getTopic());
        Listcell lc = new Listcell();
        lc.setImageContent(toImage(score));
        String tt = StrUtil.formatMessage("@cwf.help.tab.search.score", score);
        lc.setTooltiptext(tt);
        item.appendChild(lc);
        lc = new Listcell(qr.getTopic().getLabel());
        item.appendChild(lc);
        lc = new Listcell(qr.getTopic().getSource());
        item.appendChild(lc);
    }
    
    @Override
    public void onSearchComplete(List<HelpSearchHit> results) {
        Executions.schedule(getDesktop(), searchListener, new Event("onSearchComplete", this, results));
    }
    
    public void mergeHelpSet(IHelpSet helpSet) {
        helpSets.add(helpSet);
    }
}

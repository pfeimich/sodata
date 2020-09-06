package ch.so.agi.sodata.client;

import static elemental2.dom.DomGlobal.console;
import static org.jboss.elemento.Elements.*;
import static org.dominokit.domino.ui.style.Unit.px;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.dominokit.domino.ui.button.Button;
import org.dominokit.domino.ui.button.ButtonSize;
import org.dominokit.domino.ui.chips.Chip;
import org.dominokit.domino.ui.dropdown.DropDownMenu;
import org.dominokit.domino.ui.forms.SuggestBox.DropDownPositionDown;
import org.dominokit.domino.ui.forms.SuggestBoxStore;
import org.dominokit.domino.ui.forms.SuggestItem;
import org.dominokit.domino.ui.grid.Column;
import org.dominokit.domino.ui.grid.Row;
import org.dominokit.domino.ui.forms.Select;
import org.dominokit.domino.ui.forms.SelectOption;
import org.dominokit.domino.ui.forms.SuggestBox;
import org.dominokit.domino.ui.icons.Icon;
import org.dominokit.domino.ui.icons.Icons;
import org.dominokit.domino.ui.icons.MdiIcon;
import org.dominokit.domino.ui.lists.ListGroup;
import org.dominokit.domino.ui.modals.ModalDialog;
import org.dominokit.domino.ui.style.Color;
import org.dominokit.domino.ui.style.ColorScheme;
import org.dominokit.domino.ui.style.StyleType;
import org.dominokit.domino.ui.style.Styles;
import org.dominokit.domino.ui.themes.Theme;
import org.dominokit.domino.ui.utils.TextNode;
import org.gwtproject.safehtml.shared.SafeHtmlUtils;
import org.jboss.elemento.HtmlContentBuilder;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.http.client.URL;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.Text;
import com.google.gwt.xml.client.XMLParser;

import ch.so.agi.sodata.shared.Dataset;
import ch.so.agi.sodata.shared.SettingsResponse;
import ch.so.agi.sodata.shared.SettingsService;
import ch.so.agi.sodata.shared.SettingsServiceAsync;
import elemental2.core.Global;
import elemental2.dom.DomGlobal;
import elemental2.dom.Element;
import elemental2.dom.EventListener;
import elemental2.dom.HTMLDivElement;
import elemental2.dom.HTMLElement;
import elemental2.dom.Headers;
import elemental2.dom.RequestInit;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;
import ol.Coordinate;
import ol.Map;
import ol.MapBrowserEvent;
import ol.MapEvent;
import ol.events.Event;

public class AppEntryPoint implements EntryPoint {
    private MyMessages messages = GWT.create(MyMessages.class);
    private final SettingsServiceAsync settingsService = GWT.create(SettingsService.class);
    
    // Application settings
    private String myVar;
    
    // Format settings
    private NumberFormat fmtDefault = NumberFormat.getDecimalFormat();
    private NumberFormat fmtPercent = NumberFormat.getFormat("#0.0");
        
    Dataset[] datasets;
    List<Dataset> datasetList;

    public void onModuleLoad() {
        settingsService.settingsServer(new AsyncCallback<SettingsResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                console.error(caught.getMessage());
                DomGlobal.window.alert(caught.getMessage());
            }

            @Override
            public void onSuccess(SettingsResponse result) {
                myVar = (String) result.getSettings().get("MY_VAR");
                
                // Alle vorhandenen Datensätze anfordern.
                RequestInit requestInit = RequestInit.create();
                Headers headers = new Headers();
                headers.append("Content-Type", "application/x-www-form-urlencoded"); 
                requestInit.setHeaders(headers);

                DomGlobal.fetch("datasets", requestInit)
                .then(response -> {
                    if (!response.ok) {
                        return null;
                    }
                    return response.text();
                })
                .then(json -> {                    
                    datasets = (Dataset[]) Global.JSON.parse(json);
                    datasetList = Arrays.asList(datasets);
                    
                    console.log(datasetList.get(0).getEpsgCode());
                    
                    Collections.sort(datasetList, new Comparator<Dataset>() {
                        @Override
                        public int compare(Dataset o1, Dataset o2) {
                            return o1.getTitle().toLowerCase().compareTo(o2.getTitle().toLowerCase());
                        }
                    });
                    
                    // GUI initialisieren.
                    init();
                    return null;
                }).catch_(error -> {
                    console.log(error);
                    return null;
                });
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void init() {         
        Theme theme = new Theme(ColorScheme.RED);
        theme.apply();
        
        HTMLElement container = div().id("container").element();
          
        HTMLElement logoDiv = div().id("logo").element();
        HTMLElement logoCanton = div().add(img().attr("src", GWT.getHostPageBaseURL() + "Logo.png")
                .attr("alt", "Logo Kanton")).element();
        logoDiv.appendChild(logoCanton);
        container.appendChild(logoDiv);

        container.appendChild(div().id("title").textContent("Geodaten Kanton Solothurn").element());
        
        String infoString = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy "
                + "<a href='https://geoweb.so.ch/geodaten/index.php' target='_blank'>https://geoweb.so.ch/geodaten/index.php</a> eirmod "
                + "tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et "
                + "justo <a href='https://geo.so.ch/geodata' target='_blank'>https://geo.so.ch/geodata</a> "
                + "duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.";
        container.appendChild(div().id("info").innerHtml(SafeHtmlUtils.fromTrustedString(infoString)).element());
        
        
        SuggestBoxStore dynamicStore = new SuggestBoxStore() {

            @Override
            public void filter(String value, SuggestionsHandler suggestionsHandler) {
                if (value.trim().length() == 0) {
                    return;
                }
                
                RequestInit requestInit = RequestInit.create();
                Headers headers = new Headers();
                // TODO: notwendig? Bin auf dem gleichen Server.
                headers.append("Content-Type", "application/x-www-form-urlencoded"); 
                requestInit.setHeaders(headers);

                DomGlobal.fetch("/search?query=" + value.trim().toLowerCase(), requestInit)
                .then(response -> {
                    if (!response.ok) {
                        return null;
                    }
                    return response.text();
                })
                .then(json -> {
                    Dataset[] searchResults = (Dataset[]) Global.JSON.parse(json);
                    List<Dataset> searchResultList = Arrays.asList(datasets);
                    
                    List<SuggestItem<Dataset>> suggestItems = new ArrayList<>();
                    for (Dataset dataset : searchResults) {
                      SuggestItem<Dataset> suggestItem = SuggestItem.create(dataset, dataset.getTitle(), null);
                      suggestItems.add(suggestItem);
                    }
                    suggestionsHandler.onSuggestionsReady(suggestItems);
                    return null;
                }).catch_(error -> {
                    console.log(error);
                    return null;
                });
            }

            @Override
            public void find(Object searchValue, Consumer handler) {
                if (searchValue == null) {
                    return;
                }
                Dataset searchResult = (Dataset) searchValue;
                SuggestItem<Dataset> suggestItem = SuggestItem.create(searchResult, null);
                handler.accept(suggestItem);
            }
        };

        
        SuggestBox suggestBox = SuggestBox.create("Suchbegriff", dynamicStore);
        suggestBox.addLeftAddOn(Icons.ALL.search());
        suggestBox.setAutoSelect(false);
        suggestBox.setFocusColor(Color.RED);
        suggestBox.getInputElement().setAttribute("autocomplete", "off");
        suggestBox.getInputElement().setAttribute("spellcheck", "false");
        DropDownMenu suggestionsMenu = suggestBox.getSuggestionsMenu();
        suggestionsMenu.setPosition(new DropDownPositionDown());

        HTMLElement suggestBoxDiv = div().id("suggestBoxDiv").add(suggestBox).element();
        container.appendChild(div().id("searchPanel").add(div().id("suggestBoxDiv").add(suggestBox)).element());
        

        ListGroup<Dataset> listGroup = ListGroup.<Dataset>create()
                .setBordered(false)
                .setItemRenderer((listGroup1, listItem) -> {
                    HTMLElement datasetLink = a().attr("class", "datasetLink")
                            .add(TextNode.of(listItem.getValue().getTitle())).element();
                    datasetLink.addEventListener("click", event -> {
                        openDatasetDialog(listItem.getValue());
                    });
                    
                    Row datasetRow = Row.create();
                    datasetRow.appendChild(Column.span11().setContent(datasetLink));
                    
                    Button button = Button.createPrimary(Icons.ALL.arrow_forward())
                            .circle().setSize(ButtonSize.SMALL)
                            .setButtonType(StyleType.DANGER)
                            .style()
                            .setBackgroundColor("#ef5350")
                            .get();
                    
                    button.addClickListener(even -> {
                        openDatasetDialog(listItem.getValue());
                    });
                    
                    datasetRow.appendChild(Column.span1().style().setTextAlign("right").get().setContent(button));

                    listItem.appendChild(div()
                            .css("datasetList")
                            .add(datasetRow));                        
                })
                .setItems(datasetList);
        
        container.appendChild(listGroup.element());
        
//        HashMap<String, List<Dataset>> datasetsMap = new HashMap<String, List<Dataset>>();
//        for (Dataset dataset : datasets) {
//            if (!datasetsMap.containsKey(dataset.getId())) {
//                List<Dataset> list = new ArrayList<Dataset>();
//                list.add(dataset);
//                datasetsMap.put(dataset.getId(), list);
//            } else {
//                datasetsMap.get(dataset.getId()).add(dataset);
//            }
//        }
        
        body().add(container);
    }
    
    private void openDatasetDialog(Dataset dataset) {
        ModalDialog modal = ModalDialog.create(dataset.getTitle()).setAutoClose(true);
        modal.style().setMaxHeight("calc(100% - 120px)");
        modal.style().setOverFlowY("auto");

        // Short description
        HTMLElement shortDescription = div().css("modal-body-paragraph").add(TextNode.of(dataset.getShortDescription())).element();
        modal.appendChild(shortDescription);
        
        // Last editing date
        Date date = DateTimeFormat.getFormat("yyyy-MM-dd").parse(dataset.getLastEditingDate());
        HTMLElement lastEditingDate = div().css("modal-body-paragraph").add(TextNode.of(
                "Stand der Daten: " + DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_LONG).format(date)))
                .element();
        modal.appendChild(lastEditingDate);

        // Download data
        Row selectRow = Row.create();        
        Column downloadSelectColumn = Column.span4();
        Column serviceSelectColumn = Column.span4();
        Column metaSelectColumn = Column.span4();
               
        Select downloadSelect = Select.create("Download")
            .appendChild(SelectOption.create("-", "Datenformat wählen"))
            .appendChild(SelectOption.create("xtf", "INTERLIS"))
            .appendChild(SelectOption.create("gpkg", "GeoPackage"))
            .appendChild(SelectOption.create("shp", "Shapefile"))
            .appendChild(SelectOption.create("dxf", "DXF"))
            .setSearchable(false)
            .selectAt(0);
        downloadSelect.setFocusColor(Color.RED);
        downloadSelectColumn.setContent(downloadSelect);
        selectRow.addColumn(downloadSelectColumn);
       
        downloadSelect.addSelectionHandler((option) -> {
           console.log(option.getValue().toString()); 
           String format = option.getValue().toString();
           if (format.equalsIgnoreCase("-")) return;
           Window.open("/dataset/"+dataset.getId()+"/format/"+format, "_blank", null);
        });
        
        Select serviceSelect = Select.create("Services")
                .appendChild(SelectOption.create("-", "Service wählen"))
                .appendChild(SelectOption.create("value10", "WMS"))
                .appendChild(SelectOption.create("value20", "WFS"))
                .appendChild(SelectOption.create("value30", "Data Service"))
                .setSearchable(false)
                .selectAt(0);
        serviceSelect.setFocusColor(Color.RED);
        serviceSelectColumn.setContent(serviceSelect);
        selectRow.addColumn(serviceSelectColumn);

        Select metaSelect = Select.create("Dokumentation")
                .appendChild(SelectOption.create("-", "Format wählen"))
                .appendChild(SelectOption.create("value10", "Online (geocat.ch)"))
                .appendChild(SelectOption.create("value20", "PDF"))
                .setSearchable(false)
                .selectAt(0);
        metaSelect.setFocusColor(Color.RED);
        metaSelectColumn.setContent(metaSelect);
        selectRow.addColumn(metaSelectColumn);
        
        modal.appendChild(selectRow);

        // Show data in map
        String knownWMS = dataset.getKnownWMS();
        HashMap<String,String> queryParams = this.getUrlValues(knownWMS);
        String layers = queryParams.get("LAYERS");
        // TODO: make this optional in gwt-wgc-embed ?
        String layersOpacity = "";
        for (String layer : layers.split(",")) {
            layersOpacity += "1,";
        }
        layersOpacity = layersOpacity.substring(0, layersOpacity.length() - 1);
        String embeddedMap = "<iframe src='https://geo-t.so.ch/api/v1/embed/embed.html?bgLayer=ch.so.agi.hintergrundkarte_sw&layers="+layers+"&layers_opacity="+layersOpacity+"&E=2618000&N=1237800&zoom=5' height='500' style='width: 100%; border:0px solid white;'></iframe>";
        modal.appendChild(div().id("map").css("modal-body-paragraph").innerHtml(SafeHtmlUtils.fromTrustedString(embeddedMap)).element());
        
        
        // TODO service
        
        Row chipRow = Row.create();
        Column chipColumn = Column.span12();
        String[] keywords = dataset.getKeywords().split(",");
        for (String keyword : keywords) {
            chipColumn.appendChild(Chip.create()
                    .setValue(keyword)
                    .setColor(Color.RED_LIGHTEN_1));
        }
        chipRow.appendChild(chipColumn);
        modal.appendChild(div().css("modal-body-paragraph").add(chipRow.element()));
        
        Button closeButton = Button.create("CLOSE").linkify();
        EventListener closeModalListener = (evt) -> modal.close();
        closeButton.addClickListener(closeModalListener);
        modal.appendFooterChild(closeButton);
        modal.large().open();
    }
    
    private HashMap<String, String> getUrlValues(String url) {
        int i = url.indexOf("?");
        HashMap<String, String> paramsMap = new HashMap<String, String>();
        if (i > -1) {
            String searchURL = url.substring(url.indexOf("?") + 1);
            String params[] = searchURL.split("&");

            for (String param : params) {
                String temp[] = param.split("=");
                try {
                    paramsMap.put(temp[0], URL.decodeQueryString(temp[1]));
                } catch (NullPointerException e) {}
            }
        }
        return paramsMap;
    }
    
    private static native void updateURLWithoutReloading(String newUrl) /*-{
        $wnd.history.pushState(newUrl, "", newUrl);
    }-*/;
}
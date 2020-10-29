package ch.so.agi.sodata.client;

import static elemental2.dom.DomGlobal.console;
import static org.jboss.elemento.Elements.*;
import static org.jboss.elemento.EventType.*;
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

import org.dominokit.domino.ui.badges.Badge;
import org.dominokit.domino.ui.breadcrumbs.Breadcrumb;
import org.dominokit.domino.ui.button.Button;
import org.dominokit.domino.ui.button.ButtonSize;
import org.dominokit.domino.ui.chips.Chip;
import org.dominokit.domino.ui.datatable.ColumnConfig;
import org.dominokit.domino.ui.datatable.DataTable;
import org.dominokit.domino.ui.datatable.TableConfig;
import org.dominokit.domino.ui.datatable.plugins.HeaderBarPlugin;
import org.dominokit.domino.ui.datatable.plugins.RecordDetailsPlugin;
import org.dominokit.domino.ui.datatable.store.LocalListDataStore;
import org.dominokit.domino.ui.dropdown.DropDownMenu;
import org.dominokit.domino.ui.forms.SuggestBox.DropDownPositionDown;
import org.dominokit.domino.ui.forms.SuggestBoxStore;
import org.dominokit.domino.ui.forms.SuggestItem;
import org.dominokit.domino.ui.forms.TextBox;
import org.dominokit.domino.ui.grid.Column;
import org.dominokit.domino.ui.grid.Row;
import org.dominokit.domino.ui.forms.Select;
import org.dominokit.domino.ui.forms.SelectOption;
import org.dominokit.domino.ui.forms.SuggestBox;
import org.dominokit.domino.ui.icons.Icon;
import org.dominokit.domino.ui.icons.Icons;
import org.dominokit.domino.ui.icons.MdiIcon;
import org.dominokit.domino.ui.infoboxes.InfoBox;
import org.dominokit.domino.ui.lists.ListGroup;
import org.dominokit.domino.ui.modals.ModalDialog;
import org.dominokit.domino.ui.notifications.Notification;
import org.dominokit.domino.ui.style.Color;
import org.dominokit.domino.ui.style.ColorScheme;
import org.dominokit.domino.ui.style.StyleType;
import org.dominokit.domino.ui.style.Styles;
import org.dominokit.domino.ui.style.WaveColor;
import org.dominokit.domino.ui.tabs.Tab;
import org.dominokit.domino.ui.tabs.TabsPanel;
import org.dominokit.domino.ui.themes.Theme;
import org.dominokit.domino.ui.tree.ToggleTarget;
import org.dominokit.domino.ui.tree.Tree;
import org.dominokit.domino.ui.tree.TreeItem;
import org.dominokit.domino.ui.utils.HasSelectionHandler.SelectionHandler;
import org.dominokit.domino.ui.utils.TextNode;
import org.gwtproject.safehtml.shared.SafeHtmlUtils;
import org.jboss.elemento.HtmlContentBuilder;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.History;
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
import elemental2.dom.CSSProperties;
import elemental2.dom.DomGlobal;
import elemental2.dom.Element;
import elemental2.dom.EventListener;
import elemental2.dom.HTMLDivElement;
import elemental2.dom.HTMLElement;
import elemental2.dom.HTMLIFrameElement;
import elemental2.dom.Headers;
//import elemental2.dom.History;
import elemental2.dom.RequestInit;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;
import ol.Coordinate;
import ol.Feature;
import ol.Map;
import ol.MapBrowserEvent;
import ol.MapEvent;
import ol.events.Event;
import ol.layer.Base;
import ol.layer.Vector;

public class AppEntryPoint implements EntryPoint {
    private MyMessages messages = GWT.create(MyMessages.class);
    private final SettingsServiceAsync settingsService = GWT.create(SettingsService.class);

    // Application settings
    private String myVar;

    // Format settings
    private NumberFormat fmtDefault = NumberFormat.getDecimalFormat();
    private NumberFormat fmtPercent = NumberFormat.getFormat("#0.0");

    private Map map;
//    private HTMLElement mapDiv;
//    private ModalDialog modal;
    private String baseUrl;
    private HTMLElement container;
    private HTMLElement topLevelContent;
    private HTMLElement datasetContent;
    private Dataset[] datasets;
    private List<Dataset> datasetList;

    private LocalListDataStore<Dataset> listStore;
    private DataTable<Dataset> datasetTable;

    private HashMap<String, String> formats = new HashMap<String, String>();

    public void onModuleLoad() {
        formats.put("xtf", "INTERLIS");
        formats.put("shp", "Shapefile");
        formats.put("dxf", "DXF");
        formats.put("gpkg", "GeoPackage");
        formats.put("gtiff", "GeoTIFF");

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

                DomGlobal.fetch("datasets", requestInit).then(response -> {
                    if (!response.ok) {
                        return null;
                    }
                    return response.text();
                }).then(json -> {
                    datasets = (Dataset[]) Global.JSON.parse(json);
                    datasetList = Arrays.asList(datasets);

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
        // TODO Überprüfen, ob es nicht im else vom redirect stehen muss.
        baseUrl = Window.Location.getProtocol() + "//" + Window.Location.getHost() + Window.Location.getPath();

        Theme theme = new Theme(ColorScheme.RED);
        theme.apply();

        container = div().id("container").element();

        HTMLElement logoDiv = div().css("logo").element();
        HTMLElement logoCanton = div()
                .add(img().attr("src", GWT.getHostPageBaseURL() + "Logo.png").attr("alt", "Logo Kanton")).element();
        logoDiv.appendChild(logoCanton);
        container.appendChild(logoDiv);

        topLevelContent = div().id("top-level-content").element();

        // Breadcrumb
        Breadcrumb breadcrumb = Breadcrumb.create().appendChild(Icons.ALL.home(), " Home ", (evt) -> {
            Window.open("https://geo.so.ch/", "_self", null);
        }).appendChild(" Geodaten ", (evt) -> {
        });
        topLevelContent.appendChild(breadcrumb.element());

        topLevelContent.appendChild(div().css("sodata-title").textContent("Geodaten Kanton Solothurn").element());
//        container.appendChild(div().css("sodata-title").textContent("Geodaten Kanton Solothurn").element());

        String infoString = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy "
                + "<a class='generic-link' href='https://geoweb.so.ch/geodaten/index.php' target='_blank'>https://geoweb.so.ch/geodaten/index.php</a> eirmod "
                + "tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et "
                + "justo <a class='generic-link' href='https://geo.so.ch/geodata' target='_blank'>https://geo.so.ch/geodata</a> "
                + "duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.";
        topLevelContent.appendChild(div().css("info").innerHtml(SafeHtmlUtils.fromTrustedString(infoString)).element());
//        container.appendChild(div().id("info").innerHtml(SafeHtmlUtils.fromTrustedString(infoString)).element());

        // Suche
        TextBox textBox = TextBox.create().setLabel("Suchbegriff");
        textBox.addLeftAddOn(Icons.ALL.search());
        textBox.setFocusColor(Color.RED);
        textBox.getInputElement().setAttribute("autocomplete", "off");
        textBox.getInputElement().setAttribute("spellcheck", "false");

        HTMLElement resetIcon = Icons.ALL.close().style().setCursor("pointer").get().element();
        resetIcon.addEventListener("click", new EventListener() {
            @Override
            public void handleEvent(elemental2.dom.Event evt) {
                textBox.clear();
                listStore.setData(Arrays.asList(datasets));
            }
        });
        textBox.addRightAddOn(resetIcon);

        textBox.addEventListener("keyup", event -> {
            if (textBox.getValue().trim().length() == 0) {
                return;
            }

            DomGlobal.fetch("/datasets?query=" + textBox.getValue().toLowerCase()).then(response -> {
                if (!response.ok) {
                    return null;
                }
                return response.text();
            }).then(json -> {
                Dataset[] searchResults = (Dataset[]) Global.JSON.parse(json);
                List<Dataset> searchResultList = Arrays.asList(searchResults);
                listStore.setData(searchResultList);
                return null;
            }).catch_(error -> {
                console.log(error);
                return null;
            });
        });
        topLevelContent.appendChild(div().id("search-panel").add(div().id("suggestbox-div").add(textBox)).element());

        // Liste (sämtlicher) Datensätze
        TableConfig<Dataset> tableConfig = new TableConfig<>();
        tableConfig
                .addColumn(ColumnConfig.<Dataset>create("title", "Titel").setShowTooltip(false).textAlign("left")
                        .setCellRenderer(cell -> TextNode.of(cell.getTableRow().getRecord().getTitle())))
                .addColumn(ColumnConfig.<Dataset>create("lastEditingDate", "Aktualisiert").setShowTooltip(false)
                        .textAlign("left").setCellRenderer(cell -> {
                            Date date = DateTimeFormat.getFormat("yyyy-MM-dd")
                                    .parse(cell.getTableRow().getRecord().getLastEditingDate());
                            String dateString = DateTimeFormat.getFormat("dd.MM.yyyy").format(date);
                            return TextNode.of(dateString);

                        }))
                
                // Icons.ALL.open_in_new()
                
                .addColumn(ColumnConfig.<Dataset>create("metadata", "Metadaten")
                        .setShowTooltip(false)                        
                        .textAlign("left")
                        .setCellRenderer(cell -> a().css("generic-link").attr("href", "https://geocat.ch").textContent("geocat.ch").element()))
                .addColumn(ColumnConfig.<Dataset>create("perimeter", "Gebiet").setShowTooltip(false).textAlign("center")
                        .setCellRenderer(cell -> {
                            if (cell.getRecord().getSubunits() != null) {
                                HTMLElement regionSelectionElement = a().css("generic-link")
                                        .textContent("Gebietsauswahl nötig").element();
                                regionSelectionElement.addEventListener("click", new EventListener() {
                                    @Override
                                    public void handleEvent(elemental2.dom.Event evt) {
                                        openRegionSelectionDialog(cell.getRecord());
                                    }

                                });
                                return regionSelectionElement;

                            } else {
                                return TextNode.of("-");
                            }

                        }))
                .addColumn(ColumnConfig.<Dataset>create("formats", "Daten herunterladen").setShowTooltip(false)
                        .textAlign("left").setCellRenderer(cell -> {
                            HTMLElement badgesElement = div().element();

                            for (String fileStr : cell.getRecord().getFiles()) {
                                if (cell.getRecord().getSubunits() != null) {
                                    badgesElement.appendChild(Badge.create(formats.get(fileStr))
                                            .setBackground(Color.GREY_LIGHTEN_4).style().setMarginRight("10px")
                                            .setMarginTop("5px").setMarginBottom("5px").get().element());
                                } else {
                                    badgesElement.appendChild(a().css("badge-link")
                                            .attr("href",
                                                    "/dataset/" + cell.getRecord().getId() + "_" + fileStr + ".zip")
                                            .add(Badge.create(formats.get(fileStr)).setBackground(Color.GREY_LIGHTEN_2)
                                                    .style().setMarginRight("10px").setMarginTop("5px")
                                                    .setMarginBottom("5px").get().element())
                                            .element());
                                }
                            }

                            return badgesElement;
                        }))
                .addColumn(ColumnConfig.<Dataset>create("services", "Servicelink").setShowTooltip(false)
                        .textAlign("center").setCellRenderer(cell -> {
                            HTMLElement serviceLinkElement = div()
                                    .add(Icons.ALL.information_outline_mdi().style().setCursor("pointer")).element();
                            serviceLinkElement.addEventListener("click", new EventListener() {
                                @Override
                                public void handleEvent(elemental2.dom.Event evt) {
                                    openServiceLinkDialog(cell.getRecord());
                                }
                            });
                            return serviceLinkElement;
                        }));

        // TODO: delete
//        tableConfig.addPlugin(new RecordDetailsPlugin<>(cell -> new DatasetDetail(cell).element()));

        listStore = new LocalListDataStore<>();
        listStore.setData(Arrays.asList(datasets));

        datasetTable = new DataTable<>(tableConfig, listStore);
        datasetTable.setId("dataset-table");
        datasetTable.noStripes();
        datasetTable.noHover();
        datasetTable.load();

        topLevelContent.appendChild(datasetTable.element());

        container.appendChild(topLevelContent);
        body().add(container);
    }

//    private void showDatasetDetail(Dataset dataset) {
//        topLevelContent.hidden = true;
//        
//        History.newItem("dataset=" + dataset.getId());
//        
////        String newUrl = baseUrl + "dataset/" + dataset.getId() + "/format/html";
////        updateURLWithoutReloading(newUrl);
//        
//        datasetContent = div().id("dataset-content").element();
//        
//        // Breadcrumb
//        Breadcrumb breadcrumb = Breadcrumb.create()
//        .appendChild(Icons.ALL.home()," Home ", (evt) -> {
//            Window.open("https://geo.so.ch/", "_blank", null);
//        })
//        .appendChild(" Geodaten ", (evt) -> {
//            showHome();
//        })
//        .appendChild(" "+dataset.getTitle()+" ", (evt) -> {});
//        datasetContent.appendChild(breadcrumb.element());
//        
//        // Title of dataset
//        datasetContent.appendChild(div().css("sodata-title").textContent(dataset.getTitle()).element());
//        
//        // Short description
//        datasetContent.appendChild(div().css("info").textContent(dataset.getShortDescription()).element());
//
//        // Last editing date
//        Date date = DateTimeFormat.getFormat("yyyy-MM-dd").parse(dataset.getLastEditingDate());
//        HTMLElement lastEditingDate = div().css("info").add(TextNode.of(
//                "Stand der Daten: " + DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_LONG).format(date)))
//                .element();
//        datasetContent.appendChild(lastEditingDate);
//        
//        // The row which contains the map and the trees
//        Row mapPlusRow = Row.create();
//        mapPlusRow.setId("map-plus-row");
//        
//        // Show dataset in map
//        Column mapColumn = Column.span10();
//        
//        String knownWMS = dataset.getKnownWMS();
//        HashMap<String,String> queryParams = this.getUrlValues(knownWMS);
//        String layers = queryParams.get("LAYERS");
//        // TODO: make this optional in gwt-wgc-embed ?
//        String layersOpacity = "";
//            layersOpacity += "1,";
//        }
//        layersOpacity = layersOpacity.substring(0, layersOpacity.length() - 1);
//        
//        HTMLElement iframe = iframe().id("map").element();
//        iframe.style.setProperty("width", "100%");
//        iframe.style.setProperty("border", "0px solid white");
//        iframe.style.setProperty("height", "600px");
//        iframe.setAttribute("src", "https://geo-t.so.ch/api/embed/v1/embed.html?bgLayer=ch.so.agi.hintergrundkarte_sw&layers="+layers+"&layers_opacity="+layersOpacity+"&E=2620000&N=1237800&zoom=5");
//        mapColumn.appendChild(iframe);
//        mapPlusRow.appendChild(mapColumn);
//
//        // Data download and services
//        Row fileTreesRow = Row.create();
//        fileTreesRow.setId("file-trees-row");
//
//        Tree downloadTree =
//                Tree.create("Download")
//                    .setToggleTarget(ToggleTarget.ICON)
//                    .appendChild(
//                        TreeItem.create("INTERLIS/XTF", Icons.ALL.file_download_mdi()).removeWaves()
//                            .addClickListener((evt) -> Window.open("/dataset/"+dataset.getId()+"/format/xtf", "_blank", null)))
//                    .appendChild(
//                        TreeItem.create("GeoPackage", Icons.ALL.file_download_mdi()).removeWaves()
//                           .addClickListener((evt) -> Window.open("/dataset/"+dataset.getId()+"/format/gpkg", "_blank", null)))
//                    .appendChild(
//                        TreeItem.create("Shapefile", Icons.ALL.file_download_mdi()).removeWaves()
//                           .addClickListener((evt) -> Window.open("/dataset/"+dataset.getId()+"/format/shp", "_blank", null)))
//                    .appendChild(
//                        TreeItem.create("DXF", Icons.ALL.file_download_mdi()).removeWaves()
//                            .addClickListener((evt) -> Window.open("/dataset/"+dataset.getId()+"/format/dxf", "_blank", null)));
//        Column downloadColumn = Column.span12();
//        downloadColumn.appendChild(downloadTree.element());
//        fileTreesRow.appendChild(downloadColumn);
//        
//        Tree serviceTree =
//                Tree.create("Dienste")
//                    .setToggleTarget(ToggleTarget.ICON)
//                    .appendChild(
//                        TreeItem.create("WMS", Icons.ALL.map()).removeWaves()
//                           .addClickListener((evt) -> Notification.create("WMS").show()))
//                    .appendChild(
//                        TreeItem.create("WFS", Icons.ALL.file_download()).removeWaves()
//                           .addClickListener((evt) -> Notification.create("WFS").show()))
//                    .appendChild(
//                        TreeItem.create("Data service", Icons.ALL.file_download()).removeWaves()
//                            .addClickListener((evt) -> Notification.create("Cloud").show()));
//        Column serviceColumn = Column.span12();
//        serviceColumn.appendChild(serviceTree.element());
//        fileTreesRow.appendChild(serviceColumn);
//
//        Column fileTreesColumn = Column.span2();
//        fileTreesColumn.appendChild(fileTreesRow);
//        
//        mapPlusRow.appendChild(fileTreesColumn);
//        datasetContent.appendChild(mapPlusRow.element());
//       
//        // Keywords        
//        Row chipRow = Row.create();
//        Column chipColumn = Column.span12();
//        String[] keywords = dataset.getKeywords().split(",");
//        for (String keyword : keywords) {
//            chipColumn.appendChild(Chip.create()
//                    .setValue(keyword)
//                    .setColor(Color.GREY_LIGHTEN_1));
//            
//        }
//        chipRow.appendChild(chipColumn);
//        datasetContent.appendChild(div().css("info").add(chipRow.element()).element());
//
//        container.appendChild(datasetContent);        
//    }

    private void openRegionSelectionDialog(Dataset dataset) {
        ModalDialog modal = ModalDialog.create("Gebietsauswahl: " + dataset.getTitle()).setAutoClose(true);
        modal.style().setMaxHeight("calc(100% - 120px)");
        modal.style().setOverFlowY("auto");

        String subunitsWmsLayer = dataset.getSubunits();

        TabsPanel tabsPanel = TabsPanel.create().setColor(Color.RED);
        Tab selectionTab = Tab.create(Icons.ALL.map_outline_mdi(), "AUSWAHL");
        Tab downloadTab = Tab.create(Icons.ALL.file_download_outline_mdi(), "HERUNTERLADEN");
        tabsPanel.appendChild(selectionTab);
        tabsPanel.appendChild(downloadTab);

        HTMLDivElement mapDiv = div().id("map").element();
        modal.getBodyElement().appendChild(div().css("modal-body-paragraph")
                .textContent("Sie können einzelne Datensätze mit einem Klick in die Karte aus- und abwählen. "
                        + "Im Reiter 'Herunterladen' können Sie die Daten anschliessend herunterladen. "
                        + "Wünschen Sie viele Datensätze herunterzuladen ... Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor "
                        + "invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et."));

        selectionTab.appendChild(mapDiv);

        TableConfig<Feature> tableConfig = new TableConfig<>();
        tableConfig.addColumn(ColumnConfig.<Feature>create("title", "Titel").setShowTooltip(false).textAlign("left")
                .setCellRenderer(cell -> TextNode.of(cell.getTableRow().getRecord().get("aname"))));

        LocalListDataStore<Feature> subunitListStore = new LocalListDataStore<>();

        DataTable<Feature> subunitFeatureTable = new DataTable<>(tableConfig, subunitListStore);
        subunitFeatureTable.setId("dataset-table");
        subunitFeatureTable.noStripes();
        subunitFeatureTable.noHover();
        subunitFeatureTable.load();
        downloadTab.appendChild(subunitFeatureTable.element());

        modal.getBodyElement().appendChild(tabsPanel);

        Button closeButton = Button.create("SCHLIESSEN").linkify();
        closeButton.setBackground(Color.RED);
        EventListener closeModalListener = (evt) -> modal.close();
        closeButton.addClickListener(closeModalListener);
        modal.appendFooterChild(closeButton);
        modal.large().open();

        downloadTab.addClickListener(new EventListener() {
            @Override
            public void handleEvent(elemental2.dom.Event evt) {
                Vector vectorLayer = (Vector) getMapLayerById("vector");
                ol.source.Vector vectorSource = vectorLayer.getSource();
                ol.Collection<Feature> features = vectorSource.getFeaturesCollection();

                List<Feature> featuresList = new ArrayList<Feature>();
                for (int i = 0; i < features.getLength(); i++) {
                    Feature feature = features.item(i);
                    feature.set("files", dataset.getFiles());
                    featuresList.add(feature);
                }
                subunitListStore.setData(featuresList);
            }
        });

        map = MapPresets.getBlackAndWhiteMap(mapDiv.id, subunitsWmsLayer);
        closeButton.blur();
    }

    private void openServiceLinkDialog(Dataset dataset) {
        ModalDialog modal = ModalDialog.create("Servicelink").setAutoClose(true);

        modal.appendChild(InfoBox.create(Icons.ALL.map(), "WMS", "https://geo.so.ch/wms").setIconBackground(Color.RED));
        modal.appendChild(InfoBox.create(Icons.ALL.file_download_mdi(), "WFS", "https://geo.so.ch/wfs")
                .setIconBackground(Color.RED));
        modal.appendChild(
                InfoBox.create(Icons.ALL.file_download_mdi(), "Data Service", "https://geo.so.ch/api/data/v1/api/")
                        .setIconBackground(Color.RED));

        Button closeButton = Button.create("SCHLIESSEN").linkify();
        closeButton.setBackground(Color.RED);
        EventListener closeModalListener = (evt) -> modal.close();
        closeButton.addClickListener(closeModalListener);
        modal.appendFooterChild(closeButton);
        modal.open();
        
        closeButton.blur();
    }

//    private void openDatasetDialog(Dataset dataset) {
//        ModalDialog modal = ModalDialog.create(dataset.getTitle()).setAutoClose(true);
//        modal.style().setMaxHeight("calc(100% - 120px)");
//        modal.style().setOverFlowY("auto");
//
//        // Short description
//        HTMLElement shortDescription = div().css("modal-body-paragraph").add(TextNode.of(dataset.getShortDescription())).element();
//        modal.appendChild(shortDescription);
//        
//        // Last editing date
//        Date date = DateTimeFormat.getFormat("yyyy-MM-dd").parse(dataset.getLastEditingDate());
//        HTMLElement lastEditingDate = div().css("modal-body-paragraph").add(TextNode.of(
//                "Stand der Daten: " + DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_LONG).format(date)))
//                .element();
//        modal.appendChild(lastEditingDate);
//
//        // Download data
//        Row selectRow = Row.create();        
//        Column downloadSelectColumn = Column.span4();
//        Column serviceSelectColumn = Column.span4();
//        Column metaSelectColumn = Column.span4();
//               
//        Select downloadSelect = Select.create("Download")
//            .appendChild(SelectOption.create("-", "Datenformat wählen"))
//            .appendChild(SelectOption.create("xtf", "INTERLIS"))
//            .appendChild(SelectOption.create("gpkg", "GeoPackage"))
//            .appendChild(SelectOption.create("shp", "Shapefile"))
//            .appendChild(SelectOption.create("dxf", "DXF"))
//            .setSearchable(false)
//            .selectAt(0);
//        downloadSelect.setFocusColor(Color.RED);
//        downloadSelectColumn.setContent(downloadSelect);
//        selectRow.addColumn(downloadSelectColumn);
//       
//        downloadSelect.addSelectionHandler((option) -> {
//           console.log(option.getValue().toString()); 
//           String format = option.getValue().toString();
//           if (format.equalsIgnoreCase("-")) return;
//           Window.open("/dataset/"+dataset.getId()+"/format/"+format, "_blank", null);
//        });
//        
//        Select serviceSelect = Select.create("Services")
//                .appendChild(SelectOption.create("-", "Service wählen"))
//                .appendChild(SelectOption.create("value10", "WMS"))
//                .appendChild(SelectOption.create("value20", "WFS"))
//                .appendChild(SelectOption.create("value30", "Data Service"))
//                .setSearchable(false)
//                .selectAt(0);
//        serviceSelect.setFocusColor(Color.RED);
//        serviceSelectColumn.setContent(serviceSelect);
//        selectRow.addColumn(serviceSelectColumn);
//
//        Select metaSelect = Select.create("Dokumentation")
//                .appendChild(SelectOption.create("-", "Format wählen"))
//                .appendChild(SelectOption.create("value10", "Online (geocat.ch)"))
//                .appendChild(SelectOption.create("value20", "PDF"))
//                .setSearchable(false)
//                .selectAt(0);
//        metaSelect.setFocusColor(Color.RED);
//        metaSelectColumn.setContent(metaSelect);
//        selectRow.addColumn(metaSelectColumn);
//        
//        modal.appendChild(selectRow);
//
//        // Show data in map
//        String knownWMS = dataset.getKnownWMS();
//        HashMap<String,String> queryParams = this.getUrlValues(knownWMS);
//        String layers = queryParams.get("LAYERS");
//        // TODO: make this optional in gwt-wgc-embed ?
//        String layersOpacity = "";
//        for (String layer : layers.split(",")) {
//            layersOpacity += "1,";
//        }
//        layersOpacity = layersOpacity.substring(0, layersOpacity.length() - 1);
//        String embeddedMap = "<iframe src='https://geo-t.so.ch/api/embed/v1/embed.html?bgLayer=ch.so.agi.hintergrundkarte_sw&layers="+layers+"&layers_opacity="+layersOpacity+"&E=2618000&N=1237800&zoom=5' height='500' style='width: 100%; border:0px solid white;'></iframe>";
//        modal.appendChild(div().id("map").css("modal-body-paragraph").innerHtml(SafeHtmlUtils.fromTrustedString(embeddedMap)).element());
//
//        Row chipRow = Row.create();
//        Column chipColumn = Column.span12();
//        String[] keywords = dataset.getKeywords().split(",");
//        for (String keyword : keywords) {
//            chipColumn.appendChild(Chip.create()
//                    .setValue(keyword)
//                    .setColor(Color.RED_LIGHTEN_1));
//        }
//        chipRow.appendChild(chipColumn);
//        modal.appendChild(div().css("modal-body-paragraph").add(chipRow.element()));
//        
//        Button closeButton = Button.create("CLOSE").linkify();
//        EventListener closeModalListener = (evt) -> modal.close();
//        closeButton.addClickListener(closeModalListener);
//        modal.appendFooterChild(closeButton);
//        modal.large().open();
//    }

//    private HashMap<String, String> getUrlValues(String url) {
//        int i = url.indexOf("?");
//        HashMap<String, String> paramsMap = new HashMap<String, String>();
//        if (i > -1) {
//            String searchURL = url.substring(url.indexOf("?") + 1);
//            String params[] = searchURL.split("&");
//
//            for (String param : params) {
//                String temp[] = param.split("=");
//                try {
//                    paramsMap.put(temp[0], URL.decodeQueryString(temp[1]));
//                } catch (NullPointerException e) {}
//            }
//        }
//        return paramsMap;
//    }

    // Get Openlayers map layer by id.
    private Base getMapLayerById(String id) {
        ol.Collection<Base> layers = map.getLayers();
        for (int i = 0; i < layers.getLength(); i++) {
            Base item = layers.item(i);
            try {
                String layerId = item.get("id");
                if (layerId == null) {
                    continue;
                }
                if (layerId.equalsIgnoreCase(id)) {
                    return item;
                }
            } catch (Exception e) {
                console.log(e.getMessage());
                console.log("should not reach here");
            }
        }
        return null;
    }

    private static native void updateURLWithoutReloading(String newUrl) /*-{
                                                                        console.log("fubar");
                                                                        $wnd.history.pushState(newUrl, "", newUrl);
                                                                        }-*/;
}
# sodata

## TODO
- ~~history~~
- ~~bookmarkable~~
- ~~Modal via URL (z.B localhost:8080/ch.so.awjf.forstkreise)~~
- ~~ilidata.xml~~
- Lucene Suche
- ~~group by Amt~~
- dataset link mit korrektem name der Datei
- Gebietseinteilung
- Detail zum Datensatz
- Link zur Karte (siehe Mockup)
- Raster vs. Vektorsubunits
- versionierte Datensätze?
- ...

## Development

First Terminal:
```
./mvnw clean spring-boot:run
```

Second Terminal:
```
./mvnw gwt:generate-module gwt:codeserver
```

Or simple devmode (which worked better for java.xml.bind on client side):
```
./mvnw gwt:generate-module gwt:devmode 
```

Build fat jar and docker image:
```
GITHUB_RUN_NUMBER=9999 mvn clean package
```

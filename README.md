# btn-common
Fellesbibliotek for btn-kafka produsenter og konsumenter. BTN er forkortelse for 
Beskjed Til NAV og dreier seg om kommunikasjonen mellom bruker og NAV som går via
MinInnboks og Modia.

## Konsumenter og produsenter som inngår
* **btn-brukermelding-producer**: Tar meldinger fra MinInnboks og legger på 
Kafka
* **btn-brukermelding-oppgave**: Henter melding fra Kafka, kaller på 
oppgave-backend, oppretter oppgave og knytter medling til oppgaveid
* **btn-brukermelding-oppgave-retry**: En retry konsument som henter ut
meldinger som feilet på å opprette oppgave og prøver på nytt. Gir opp etter
X antall forsøk

## Kjøre opp lokalt
* Kjør Kafka lokalt
```
$ docker run --rm -p 2181:2181 -p 3030:3030 -p 8081-8083:8081-8083 \
       -p 9581-9585:9581-9585 -p 9092:9092 -e ADV_HOST=localhost \
       landoop/fast-data-dev:latest
```
* Lag topics
  * btn-brukermelding
  * btn-brukermelding-oppgave
  * btn-opprett-oppgave-retry
  * btn-multiple-failing
  * btn-melding-til-database
```
$ docker run --rm -it --net=host landoop/fast-data-dev kafka-topics --zookeeper localhost:2181 \
       --create --topic btn-brukermelding --replication-factor 1 --partitions 1
```
* Start opp alle applikasjoner
* Send en testmelding
```
$ curl --header "Content-Type: application/json" --request POST \
  --data '{"message": "testmessage", "timestamp": 74293847}' \
  http://localhost:7070
```
* Sjekk http://localhost:3030 for å se hvordan det gikk
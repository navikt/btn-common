# btn-common
Fellesbibliotek for btn-kafka produsenter og konsumenter. BTN er forkortelse for 
Beskjed Til NAV og dreier seg om kommunikasjonen mellom bruker og NAV som går via
MinInnboks og Modia.

## Kjøre opp lokalt
* Kjør Kafka lokalt
```
$ docker run --rm -p 2181:2181 -p 3030:3030 -p 8081-8083:8081-8083 \
       -p 9581-9585:9581-9585 -p 9092:9092 -e ADV_HOST=localhost \
       landoop/fast-data-dev:latest
```
* Lag topics
  * aapen-btn-meldingSendt
  * privat-btn-meldingLest
  * privat-btn-meldingJournalfoert
  * privat-btn-meldingArkivert
  * privat-btn-meldingOperasjonVellykket
  * privat-btn-opprettOppgaveFeilet
  * privat-btn-avsluttOppgaveFeilet
  * privat-btn-opprettVarselFeilet
  * privat-btn-stoppRevarselFeilet
  * privat-btn-journalfoeringFeilet
  * privat-btn-deadLetter
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

## Opprette / redigere topics i Kafka-clusterne
Det finnes et Swagger-grensesnitt som du kan finne [her](https://confluence.adeo.no/display/AURA/Kafka#Kafka-TopicogSikkerhetskonfigurasjon).
Det er opprettet en fil `kafka/oneshot.json` som du kan poste i swagger-grensesnittet
for å opprette eller redigere topics.

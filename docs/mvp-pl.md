# Aplikacja - BrandPulse (MVP)

## Główny problem

Małe i średnie sieci usługowe (np. restauracje, hotele, salony beauty) - zwłaszcza te z wieloma lokalizacjami lub
kanałami kontaktu - mają trudność z monitorowaniem, analizą i szybkim reagowaniem na opinie klientów publikowane w
różnych miejscach (Google, Facebook, Trustpilot itp.). Ręczne przetwarzanie jest czasochłonne, opóźnia reakcję na
negatywne komentarze i utrudnia wyciąganie wniosków strategicznych dotyczących jakości obsługi oraz reputacji marki.

## Jak projekt rozwiązuje ten problem

**BrandPulse** automatyzuje cały proces: od agregacji opinii z wielu źródeł, przez analizę sentymentu i identyfikację
problemów, po syntetyczne wnioski.\
Dzięki temu firmy mogą szybciej reagować na negatywne opinie, budować pozytywny wizerunek i podejmować decyzje oparte
na danych - bez angażowania dużego zespołu analityczno-marketingowego.

## Najmniejszy zestaw funkcjonalności

- Landing page z opisem usługi i opcją logowania.
- Prosty system kont użytkowników do przechowywania danych.
- Ręczne tworzenie profilu firmy (nazwa, branża) i wskazanie źródeł opinii do monitorowania (Google/Facebook/Trustpilot).
- Zarządzanie źródłami monitorowania (dodawanie, edycja, usuwanie linków do profili firmowych na Google/Facebook/Trustpilot).
- Dashboard z listą opinii per źródło (źródło, data, ocena, treść).
- Analiza sentymentu (pozytywny/negatywny/neutralny) wykonana przez AI dla każdej opinii, widoczna np. w formie ikony lub etykiety.
- Agregacja sentymentu na poziomie źródła (udział procentowy: X% pozytywnych, Y% neutralnych, Z% negatywnych).
- Tekstowe podsumowanie AI dla źródła (np. „75% opinii pozytywnych; klienci chwalą szybkość obsługi, ale skarżą się na ceny”).
- Automatyczne pobieranie opinii ze skonfigurowanych źródeł: jednorazowy import po konfiguracji, następnie aktualizacja raz dziennie.

## Co NIE wchodzi w zakres MVP

- Automatyczne generowanie i publikowanie odpowiedzi na opinie - na start skupiamy się na analizie i prezentacji danych.
- Rekomendacje naprawcze AI - wymagają danych zwrotnych; poza pierwszą wersją.
- Zaawansowana analityka (trendy, wykresy, porównania okresowe) - ograniczamy się do podstawowego podsumowania sentymentu.
- Integracje z systemami zewnętrznymi (CRM, ticketing) - warstwa integracyjna w późniejszym etapie.
- Zaawansowane raporty - na początku wystarczy prosty pulpit; eksport CSV w późniejszym etapie.
- Uprawnienia ról i złożone struktury organizacyjne - start z jedną rolą (właściciel konta).
- Alerty w czasie rzeczywistym (e-mail/Slack) - w MVP przegląd ręczny.
- Wsparcie wielu języków - zaczynamy od jednego (polski) dla uproszczenia modeli AI.
- Publiczne API - do rozważenia po potwierdzeniu popytu.
- Aplikacje mobilne - responsywna aplikacja webowa bez natywnych aplikacji.
- Źródła opinii inne niż Google/Facebook/Trustpilot - ograniczenie do trzech platform na potrzeby weryfikacji konceptu.

## Kryteria sukcesu

- Time to Value: 90% użytkowników konfiguruje pierwsze źródło danych i widzi zagregowane opinie w ciągu 10 minut od rejestracji.
- Dokładność analizy sentymentu: min. 75% zgodności automatycznej kategoryzacji (pozytywny/negatywny/neutralny) z oceną manualną na próbie 100 losowych opinii.
- ompletność agregacji danych: poprawne pobranie i prezentacja opinii z co najmniej 3 różnych źródeł dla 80% skonfigurowanych firm, bez błędów integracji.
- Aktywacja i retencja: 35% firm loguje się co najmniej 3 razy w ciągu pierwszych 4 tygodni od pomyślnej konfiguracji pierwszego źródła.
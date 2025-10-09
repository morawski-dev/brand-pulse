# Dokument wymagań produktu (PRD) - BrandPulse

## 1. Przegląd produktu

BrandPulse to aplikacja webowa typu SaaS (Software as a Service) zaprojektowana dla małych i średnich sieci usługowych (np. restauracje, hotele, salony beauty), aby zautomatyzować proces monitorowania i analizy opinii klientów. Aplikacja agreguje opinie z wielu źródeł (Google, Facebook, Trustpilot), analizuje ich sentyment za pomocą sztucznej inteligencji i przedstawia syntetyczne wnioski w przejrzystym dashboardzie. Celem wersji MVP jest dostarczenie kluczowej wartości - szybkiego dostępu do zagregowanych danych i podstawowej analityki sentymentu, co pozwoli firmom oszczędzić czas, szybciej reagować na kryzysy wizerunkowe i podejmować lepsze decyzje biznesowe w oparciu o głos klienta. Model freemium, z darmowym monitoringiem jednego źródła, ma na celu obniżenie bariery wejścia i szybkie zbudowanie bazy użytkowników.

## 2. Problem użytkownika

Małe i średnie firmy usługowe, zwłaszcza te posiadające wiele lokalizacji lub działające w wielu kanałach online, napotykają na znaczące trudności w efektywnym zarządzaniu swoją reputacją. Główny problem polega na rozproszeniu opinii klientów w internecie (Google, Facebook, Trustpilot itp.). Ręczne śledzenie, gromadzenie i analizowanie tych opinii jest procesem niezwykle czasochłonnym i podatnym na błędy. Prowadzi to do opóźnień w reakcji na negatywne komentarze, co może eskalować problemy i trwale zaszkodzić wizerunkowi marki. Ponadto, brak zagregowanych danych uniemożliwia identyfikację powtarzających się problemów i wyciąganie strategicznych wniosków dotyczących jakości obsługi, co utrudnia rozwój i doskonalenie usług.

## 3. Wymagania funkcjonalne

### 3.1. Zarządzanie Kontem i Onboarding

* Użytkownik może założyć konto za pomocą adresu e-mail i hasła.
* System logowania i odzyskiwania hasła.
* Po pierwszym zalogowaniu użytkownik jest prowadzony przez prosty przewodnik (onboarding) w celu dodania pierwszej "Marki" i skonfigurowania pierwszego źródła opinii.
* Jedno konto użytkownika (login) może zarządzać jedną "Marką" w ramach MVP.

### 3.2. Zarządzanie Źródłami Opinii

* Użytkownik może ręcznie dodać źródła opinii do monitorowania, podając link do profilu firmy na platformach: Google, Facebook, Trustpilot.
* Interfejs pozwala na edycję i usuwanie skonfigurowanych źródeł.
* Priorytetem dla MVP jest bezbłędna integracja z Google (przez API). Integracje z Facebookiem i Trustpilot są celami dodatkowymi (stretch goals), z możliwym wykorzystaniem web scrapingu jako rozwiązania tymczasowego.

### 3.3. Agregacja i Aktualizacja Danych

* Po dodaniu nowego źródła, system automatycznie importuje opinie opublikowane w ciągu ostatnich 90 dni.
* Dane są odświeżane automatycznie raz na dobę (zadanie CRON o 3:00 CET).
* Użytkownik ma możliwość ręcznego wywołania odświeżenia danych dla wszystkich źródeł, nie częściej niż raz na 24 godziny (w oknie toczącym się).

### 3.4. Dashboard i Wizualizacja Danych

* Główny widok aplikacji to dashboard prezentujący listę wszystkich zagregowanych opinii.
* Nad listą opinii znajduje się sekcja podsumowująca, która prezentuje zagregowaną ocenę, procentowy rozkład sentymentu oraz tekstowe podsumowanie AI.
* Każda opinia na liście zawiera: źródło, datę publikacji, ocenę gwiazdkową, treść oraz ikonę/etykietę sentymentu (pozytywny/negatywny/neutralny) przypisanego przez AI.
* Użytkownik może filtrować listę opinii niezależnie po:
    * Źródle (np. tylko Google).
    * Sencymencie AI (pozytywny, negatywny, neutralny).
    * Ocenie gwiazdkowej (np. 1-2 gwiazdki).
* Interfejs zawiera listę rozwijaną do przełączania się między widokiem zagregowanym ("Wszystkie lokalizacje") a widokami dla poszczególnych źródeł.
* Zaprojektowane zostaną "puste stany" (empty states) na czas importu danych oraz dla nowych użytkowników przed dodaniem pierwszego źródła.

### 3.5. Analiza AI i Interakcja Użytkownika

* Każda nowa opinia jest automatycznie analizowana w celu określenia sentymentu.
* System generuje krótkie, tekstowe podsumowanie dla każdego źródła (np. „75% opinii pozytywnych; klienci chwalą szybkość obsługi, ale skarżą się na ceny”).
* Użytkownik ma możliwość ręcznej zmiany (korekty) sentymentu przypisanego opinii przez AI.

### 3.6. Model Biznesowy i Angażowanie

* Aplikacja działa w modelu freemium: darmowy plan pozwala na stałe monitorowanie jednego źródła opinii.
* Próba dodania drugiego źródła wyświetla komunikat informujący o przyszłych planach płatnych (bez możliwości zakupu w MVP).
* System automatycznie wysyła cotygodniowy raport e-mail do użytkownika z podsumowaniem nowych opinii i kluczowych metryk, zachęcając do powrotu do aplikacji.

### 3.7. Wymagania Niefunkcjonalne

* Czas ładowania dashboardu z danymi musi być krótszy niż 4 sekundy.
* Aplikacja musi wspierać dwie ostatnie stabilne wersje przeglądarek: Chrome, Firefox i Safari.
* Aplikacja musi być responsywna (RWD) i użyteczna na urządzeniach mobilnych.
* Wersja MVP wspiera wyłącznie język polski.

## 4. Granice produktu

### 4.1. Funkcjonalności wyłączone z MVP

* Automatyczne generowanie i publikowanie odpowiedzi na opinie.
* Rekomendacje naprawcze generowane przez AI.
* Zaawansowana analityka (trendy w czasie, wykresy, porównania okresowe).
* Integracje z systemami zewnętrznymi (CRM, systemy ticketowe).
* Generowanie i eksportowanie raportów (np. do CSV/PDF).
* System ról i uprawnień (w MVP istnieje tylko jedna rola - właściciel konta).
* Alerty i powiadomienia w czasie rzeczywistym (e-mail/Slack).
* Wsparcie dla wielu języków.
* Publiczne API dla deweloperów.
* Dedykowane aplikacje mobilne (iOS/Android).
* Integracja ze źródłami opinii innymi niż Google, Facebook i Trustpilot.

### 4.2. Nierozwiązane kwestie i ryzyka

* Koszty API: Nie przeprowadzono szczegółowej analizy kosztów związanych z wykorzystaniem oficjalnych API (zwłaszcza Google My Business API), co może wpłynąć na rentowność modelu freemium w przyszłości.
* Ryzyko związane z Web Scrapingiem: Nie przeprowadzono dogłębnej analizy ryzyka technicznego (podatność na zmiany w strukturze stron) ani prawnego (zgodność z regulaminami serwisów Facebook i Trustpilot) dla awaryjnej metody pozyskiwania danych.

## 5. Historyjki użytkowników

### Zarządzanie Kontem

* ID: US-001
* Tytuł: Rejestracja nowego użytkownika
* Opis: Jako nowy użytkownik, chcę móc założyć konto za pomocą adresu e-mail i hasła, aby uzyskać dostęp do aplikacji.
* Kryteria akceptacji:
    1. Formularz rejestracji zawiera pola: e-mail, hasło, powtórz hasło.
    2. Walidacja po stronie klienta i serwera sprawdza poprawność formatu e-mail i zgodność haseł.
    3. Po pomyślnej rejestracji, użytkownik jest automatycznie zalogowany i przekierowany do ekranu onboardingu.
    4. W przypadku błędu (np. zajęty e-mail), wyświetlany jest czytelny komunikat.

* ID: US-002
* Tytuł: Logowanie do systemu
* Opis: Jako zarejestrowany użytkownik, chcę móc zalogować się do aplikacji przy użyciu mojego e-maila i hasła, aby uzyskać dostęp do swojego dashboardu.
* Kryteria akceptacji:
    1. Strona logowania zawiera pola: e-mail, hasło oraz przycisk "Zaloguj".
    2. Po pomyślnym zalogowaniu, użytkownik jest przekierowany do głównego dashboardu.
    3. W przypadku podania błędnych danych, wyświetlany jest stosowny komunikat.

### Onboarding i Konfiguracja

* ID: US-003
* Tytuł: Konfiguracja pierwszego źródła
* Opis: Jako nowy użytkownik, po pierwszym zalogowaniu, chcę być przeprowadzony przez proces dodania mojego pierwszego źródła opinii, aby jak najszybciej zobaczyć wartość aplikacji.
* Kryteria akceptacji:
    1. Po rejestracji pojawia się ekran onboardingu, który prosi o dodanie pierwszego źródła.
    2. Użytkownik może wybrać typ źródła (Google, Facebook, Trustpilot).
    3. Użytkownik wkleja link do profilu swojej firmy.
    4. Po zatwierdzeniu, system rozpoczyna import danych historycznych (ostatnie 90 dni) i wyświetla informację o postępie.

### Dashboard i Analiza

* ID: US-004
* Tytuł: Przeglądanie zagregowanych opinii
* Opis: Jako menedżer, chcę widzieć wszystkie opinie z moich podłączonych źródeł na jednej liście, abym mógł szybko zorientować się w ogólnej sytuacji.
* Kryteria akceptacji:
    1. Dashboard domyślnie wyświetla opinie ze wszystkich źródeł, posortowane od najnowszych.
    2. Każdy element listy pokazuje: treść opinii (skróconą z opcją rozwinięcia), autora, datę, ocenę gwiazdkową, ikonę źródła i etykietę sentymentu AI.
    3. Nad listą widoczne są zagregowane metryki: średnia ocena, procentowy rozkład sentymentu i podsumowanie tekstowe AI.

* ID: US-005
* Tytuł: Filtrowanie negatywnych opinii
* Opis: Jako właściciel firmy, chcę jednym kliknięciem przefiltrować opinie, aby zobaczyć tylko te negatywne (ocena 1-2 gwiazdki), abym mógł priorytetowo na nie odpowiedzieć.
* Kryteria akceptacji:
    1. Na dashboardzie znajdują się kontrolki do filtrowania.
    2. Użytkownik może wybrać filtr "Ocena" i zaznaczyć np. "1 gwiazdka" i "2 gwiazdki".
    3. Lista opinii natychmiast aktualizuje się, pokazując tylko te, które spełniają kryteria.
    4. Zastosowane filtry są wyraźnie widoczne.

* ID: US-006
* Tytuł: Przełączanie widoku między lokalizacjami
* Opis: Jako menedżer marketingu sieci, chcę móc łatwo przełączać się między widokiem zagregowanym ("Wszystkie lokalizacje") a widokiem dla pojedynczego źródła, aby porównać ogólny sentyment z sytuacją w konkretnej placówce.
* Kryteria akceptacji:
    1. Na górze strony znajduje się rozwijane menu (dropdown) z listą skonfigurowanych źródeł oraz opcją "Wszystkie lokalizacje".
    2. Wybranie konkretnego źródła z listy odświeża dashboard, pokazując dane (listę opinii i podsumowanie AI) tylko dla tego źródła.
    3. Wybranie "Wszystkie lokalizacje" przywraca widok zagregowany.

### Interakcja i Zarządzanie

* ID: US-007
* Tytuł: Ręczna korekta sentymentu
* Opis: Jako użytkownik, chcę mieć możliwość poprawienia błędnie oznaczonego przez AI sentymentu (np. ironiczna opinia oznaczona jako pozytywna), aby moje statystyki były bardziej dokładne.
* Kryteria akceptacji:
    1. Przy każdej opinii na liście znajduje się opcja (np. ikona edycji przy etykiecie sentymentu) pozwalająca na zmianę.
    2. Po kliknięciu pojawia się możliwość wyboru nowego sentymentu (pozytywny/negatywny/neutralny).
    3. Po zapisaniu zmiany, etykieta na liście opinii zostaje zaktualizowana, a zagregowane statystyki na dashboardzie przeliczone na nowo.

* ID: US-008
* Tytuł: Ręczne odświeżanie danych
* Opis: Jako niecierpliwy użytkownik, chcę mieć możliwość ręcznego uruchomienia pobierania nowych opinii, abym nie musiał czekać na automatyczną synchronizację o 3 w nocy.
* Kryteria akceptacji:
    1. Na dashboardzie znajduje się przycisk "Odśwież dane".
    2. Przycisk jest aktywny, jeśli od ostatniego ręcznego odświeżenia minęły co najmniej 24 godziny.
    3. Po kliknięciu aktywnego przycisku, system rozpoczyna pobieranie nowych danych, a przycisk staje się nieaktywny (wyszarzony) z informacją, kiedy będzie ponownie dostępny.

### Model Freemium

* ID: US-009
* Tytuł: Ograniczenie darmowego planu
* Opis: Jako użytkownik darmowego planu, po dodaniu jednego źródła, przy próbie dodania kolejnego chcę otrzymać informację o limitach i przyszłych planach płatnych, abym zrozumiał zasady modelu biznesowego.
* Kryteria akceptacji:
    1. Użytkownik z darmowym planem ma jedno aktywne źródło.
    2. Przycisk "Dodaj nowe źródło" jest widoczny.
    3. Po kliknięciu przycisku, zamiast formularza dodawania, pojawia się modal/okno z informacją o tym, że darmowy plan obejmuje jedno źródło i że wkrótce pojawią się plany płatne umożliwiające monitorowanie większej liczby źródeł.

## 6. Metryki sukcesu

* Time to Value: 90% użytkowników pomyślnie konfiguruje pierwsze źródło danych i widzi zagregowane opinie w ciągu 10 minut od rejestracji.
    * Sposób pomiaru: Analityka wewnątrz aplikacji, śledzenie czasu między zdarzeniem `user_registered` a `first_source_configured_successfully`.
* Dokładność analizy sentymentu: Minimum 75% zgodności automatycznej kategoryzacji (pozytywny/negatywny/neutralny) z oceną manualną.
    * Sposób pomiaru: Wewnętrzny audyt na losowej próbie 100 opinii, przeprowadzany co tydzień przez zespół produktowy.
* Kompletność agregacji danych: Poprawne pobranie i prezentacja opinii z co najmniej 3 różnych profili firmowych (dla kont testowych) dla 80% skonfigurowanych źródeł, bez błędów integracji.
    * Sposób pomiaru: Wewnętrzne testy i monitoring logów błędów związanych z API i scraperami.
* Aktywacja i retencja: 35% firm, które pomyślnie skonfigurowały źródło, loguje się co najmniej 3 razy w ciągu pierwszych 4 tygodni.
    * Sposób pomiaru: Śledzenie aktywności użytkowników (liczba sesji na użytkownika w zdefiniowanym okresie).
* Wskażnik aktywacji planu darmowego: 60% nowo zarejestrowanych użytkowników pomyślnie konfiguruje co najmniej jedno źródło w ciągu 7 dni od rejestracji.
    * Sposób pomiaru: Analiza kohortowa nowych użytkowników w narzędziu analitycznym.
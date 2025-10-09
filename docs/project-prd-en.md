# Product Requirements Document (PRD) - BrandPulse

## 1. Product Overview

BrandPulse is a SaaS (Software as a Service) web application designed for small and medium-sized service chains (e.g., restaurants, hotels, beauty salons) to automate the process of monitoring and analyzing customer reviews. The application aggregates reviews from multiple sources (Google, Facebook, Trustpilot), analyzes their sentiment using artificial intelligence, and presents synthetic conclusions on a clear dashboard. The goal of the MVP is to deliver the core value—quick access to aggregated data and basic sentiment analysis—which will allow companies to save time, react faster to brand reputation crises, and make better business decisions based on the voice of the customer. The freemium model, with free monitoring of one source, aims to lower the barrier to entry and quickly build a user base.

## 2. User Problem

Small and medium-sized service companies, especially those with multiple locations or operating across various online channels, face significant difficulties in effectively managing their reputation. The main problem is the dispersion of customer reviews across the internet (Google, Facebook, Trustpilot, etc.). Manually tracking, collecting, and analyzing these reviews is an extremely time-consuming and error-prone process. This leads to delays in responding to negative comments, which can escalate problems and permanently damage the brand's image. Furthermore, the lack of aggregated data makes it impossible to identify recurring issues and draw strategic conclusions about service quality, which hinders development and service improvement.

## 3. Functional Requirements

### 3.1. Account Management and Onboarding

* The user can create an account using an email address and password.
* Login and password recovery system.
* After the first login, the user is guided through a simple onboarding process to add their first "Brand" and configure the first review source.
* One user account (login) can manage one "Brand" within the MVP.

### 3.2. Review Source Management

* The user can manually add review sources to monitor by providing a link to the company's profile on Google, Facebook, and Trustpilot.
* The interface allows for editing and deleting configured sources.
* The priority for the MVP is a flawless integration with Google (via API). Integrations with Facebook and Trustpilot are stretch goals, with web scraping as a potential temporary solution.

### 3.3. Data Aggregation and Updates

* After adding a new source, the system automatically imports reviews published within the last 90 days.
* Data is refreshed automatically once a day (CRON job at 3:00 AM CET).
* The user has the option to manually trigger a data refresh for all sources, no more than once per 24 hours (on a rolling basis).

### 3.4. Dashboard and Data Visualization

* The main view of the application is a dashboard presenting a list of all aggregated reviews.
* Above the review list, there is a summary section that displays the aggregated rating, sentiment distribution percentage, and a text summary by AI.
* Each review on the list includes: source, publication date, star rating, content, and an icon/label for the sentiment (positive/negative/neutral) assigned by AI.
* The user can filter the review list independently by:
    * Source (e.g., only Google).
    * AI Sentiment (positive, negative, neutral).
    * Star Rating (e.g., 1-2 stars).
* The interface includes a dropdown menu to switch between the aggregated view ("All locations") and views for individual sources.
* "Empty states" will be designed for the data import process and for new users before they add their first source.

### 3.5. AI Analysis and User Interaction

* Each new review is automatically analyzed to determine its sentiment.
* The system generates a short, text-based summary for each source (e.g., "75% positive reviews; customers praise the speed of service but complain about prices").
* The user has the ability to manually change (correct) the sentiment assigned to a review by the AI.

### 3.6. Business Model and Engagement

* The application operates on a freemium model: the free plan allows for the continuous monitoring of one review source.
* An attempt to add a second source displays a message informing about future paid plans (without the option to purchase in the MVP).
* The system automatically sends a weekly email report to the user with a summary of new reviews and key metrics, encouraging them to return to the app.

### 3.7. Non-Functional Requirements

* The dashboard loading time with data must be less than 4 seconds.
* The application must support the two latest stable versions of Chrome, Firefox, and Safari browsers.
* The application must be responsive (RWD) and usable on mobile devices.
* The MVP version supports only the Polish language.

## 4. Product Boundaries

### 4.1. Features Out of Scope for MVP

* Automatic generation and publishing of responses to reviews.
* Corrective action recommendations generated by AI.
* Advanced analytics (trends over time, charts, periodic comparisons).
* Integrations with external systems (CRM, ticketing systems).
* Generating and exporting reports (e.g., to CSV/PDF).
* Roles and permissions system (in the MVP, there is only one role - account owner).
* Real-time alerts and notifications (email/Slack).
* Support for multiple languages.
* Public API for developers.
* Dedicated mobile applications (iOS/Android).
* Integration with review sources other than Google, Facebook, and Trustpilot.

### 4.2. Unresolved Issues and Risks

* API Costs: A detailed cost analysis for using official APIs (especially the Google My Business API) has not been conducted, which may affect the profitability of the freemium model in the future.
* Web Scraping Risks: A thorough analysis of the technical risks (susceptibility to changes in page structure) and legal risks (compliance with the terms of service of Facebook and Trustpilot) for the backup data scraping method has not been performed.

## 5. User Stories

### Account Management

* ID: US-001
* Title: New User Registration
* Description: As a new user, I want to be able to create an account using an email and password to gain access to the application.
* Acceptance Criteria:
    1.  The registration form includes fields: email, password, confirm password.
    2.  Client-side and server-side validation checks the correctness of the email format and password match.
    3.  Upon successful registration, the user is automatically logged in and redirected to the onboarding screen.
    4.  In case of an error (e.g., email already taken), a clear message is displayed.

* ID: US-002
* Title: System Login
* Description: As a registered user, I want to be able to log in to the application using my email and password to access my dashboard.
* Acceptance Criteria:
    1.  The login page includes fields: email, password, and a "Log In" button.
    2.  Upon successful login, the user is redirected to the main dashboard.
    3.  If incorrect credentials are provided, an appropriate message is displayed.

### Onboarding and Configuration

* ID: US-003
* Title: Configuring the first source
* Description: As a new user, after my first login, I want to be guided through the process of adding my first review source to see the value of the application as quickly as possible.
* Acceptance Criteria:
    1.  After registration, an onboarding screen appears, prompting the user to add the first source.
    2.  The user can select the source type (Google, Facebook, Trustpilot).
    3.  The user pastes the link to their company's profile.
    4.  After confirmation, the system begins importing historical data (last 90 days) and displays progress information.

### Dashboard and Analysis

* ID: US-004
* Title: Viewing aggregated reviews
* Description: As a manager, I want to see all reviews from my connected sources in a single list, so I can quickly get an overview of the general situation.
* Acceptance Criteria:
    1.  The dashboard defaults to displaying reviews from all sources, sorted by newest first.
    2.  Each list item shows: review content (shortened with an option to expand), author, date, star rating, source icon, and AI sentiment label.
    3.  Above the list, aggregated metrics are visible: average rating, sentiment distribution percentage, and AI text summary.

* ID: US-005
* Title: Filtering negative reviews
* Description: As a business owner, I want to filter reviews with a single click to see only the negative ones (1-2 star rating), so I can prioritize responding to them.
* Acceptance Criteria:
    1.  Filtering controls are available on the dashboard.
    2.  The user can select the "Rating" filter and check, for example, "1 star" and "2 stars".
    3.  The review list updates immediately to show only reviews that meet the criteria.
    4.  The applied filters are clearly visible.

* ID: US-006
* Title: Switching the view between locations
* Description: As a marketing manager of a chain, I want to easily switch between the aggregated view ("All locations") and the view for a single source, to compare the overall sentiment with the situation at a specific branch.
* Acceptance Criteria:
    1.  At the top of the page, there is a dropdown menu with a list of configured sources and an "All locations" option.
    2.  Selecting a specific source from the list refreshes the dashboard, showing data (review list and AI summary) only for that source.
    3.  Selecting "All locations" restores the aggregated view.

### Interaction and Management

* ID: US-007
* Title: Manual sentiment correction
* Description: As a user, I want to be able to correct a sentiment incorrectly marked by the AI (e.g., a sarcastic review marked as positive), so that my statistics are more accurate.
* Acceptance Criteria:
    1.  Each review in the list has an option (e.g., an edit icon next to the sentiment label) to change it.
    2.  After clicking, an option to select a new sentiment (positive/negative/neutral) appears.
    3.  After saving the change, the label in the review list is updated, and the aggregated statistics on the dashboard are recalculated.

* ID: US-008
* Title: Manual data refresh
* Description: As an impatient user, I want to be able to manually trigger the fetching of new reviews, so I don't have to wait for the automatic synchronization at 3 AM.
* Acceptance Criteria:
    1.  There is a "Refresh Data" button on the dashboard.
    2.  The button is active if at least 24 hours have passed since the last manual refresh.
    3.  After clicking the active button, the system starts fetching new data, and the button becomes inactive (grayed out) with information on when it will be available again.

### Freemium Model

* ID: US-009
* Title: Free plan limitation
* Description: As a free plan user, after adding one source, when I try to add another, I want to receive information about the limits and future paid plans, so I understand the business model.
* Acceptance Criteria:
    1.  A free plan user has one active source.
    2.  The "Add New Source" button is visible.
    3.  After clicking the button, instead of the add form, a modal/window appears with information that the free plan includes one source and that paid plans allowing for more sources will be available soon.

## 6. Success Metrics

* Time to Value: 90% of users successfully configure their first data source and see aggregated reviews within 10 minutes of registration.
    * How to measure: In-app analytics, tracking the time between the `user_registered` event and the `first_source_configured_successfully` event.
* Sentiment Analysis Accuracy: A minimum of 75% agreement between automatic categorization (positive/negative/neutral) and manual evaluation.
    * How to measure: Internal audit on a random sample of 100 reviews, conducted weekly by the product team.
* Data Aggregation Completeness: Successful fetching and presentation of reviews from at least 3 different company profiles (for test accounts) for 80% of configured sources, without integration errors.
    * How to measure: Internal testing and monitoring of error logs related to APIs and scrapers.
* Activation and Retention: 35% of companies that have successfully configured a source log in at least 3 times within the first 4 weeks.
    * How to measure: User activity tracking (number of sessions per user in a defined period).
* Free Plan Activation Rate: 60% of newly registered users successfully configure at least one source within 7 days of registration.
    * How to measure: Cohort analysis of new users in an analytics tool.
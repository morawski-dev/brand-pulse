# Application - BrandPulse (MVP)

## Main Problem

Small and medium-sized service networks (e.g., restaurants, hotels, beauty salons) - especially those with multiple locations or
communication channels - struggle to monitor, analyze, and quickly respond to customer reviews published across different platforms
(Google, Facebook, Trustpilot, etc.). Manual processing is time-consuming, delays reactions to negative comments, and makes it
difficult to draw strategic conclusions about service quality and brand reputation.

## How the Project Solves This Problem

**BrandPulse** automates the entire process: from aggregating reviews from multiple sources, through sentiment analysis and issue
identification, to generating concise insights.\
This enables companies to respond more quickly to negative feedback,build a positive brand image, and make data-driven decisions without
the need for a large analytical or marketing team.

## Minimum Viable Feature Set

-   Landing page with service description and login option.
-   Simple user account system for storing data.
-   Manual company profile creation (name, industry) and indication of review sources to monitor (Google/Facebook/Trustpilot).
-   Management of monitored sources (adding, editing, removing links to company profiles on Google/Facebook/Trustpilot).
-   Dashboard with a list of reviews per source (source, date, rating, content).
-   **AI-based sentiment analysis** (positive/negative/neutral) for each review, displayed as an icon or label.
-   Sentiment aggregation at the source level (percentage breakdown: X% positive, Y% neutral, Z% negative).
-   **AI-generated textual summary** for each source (e.g., "75% of reviews are positive; customers praise service speed but complain
    about pricing").
-   Automatic review fetching from configured sources: initial import after setup, followed by daily updates.

## Out of MVP Scope

-   Automatic generation and publishing of review responses - initial focus is on data analysis and presentation.
-   AI-generated corrective recommendations - require feedback data; postponed beyond the first version.
-   Advanced analytics (trends, charts, period comparisons) - limited to basic sentiment summaries.
-   Integrations with external systems (CRM, ticketing) - integration layer to be added later.
-   Advanced reports - a simple dashboard is sufficient initially; CSV export planned later.
-   Role-based permissions and complex organizational structures - starting with a single role (account owner).
-   Real-time alerts (email/Slack) - manual review only in MVP.
-   Multi-language support - starting with one language (Polish) to simplify AI models.
-   Public API - to be considered after confirming demand.
-   Mobile applications - responsive web app only, no native apps.
-   Review sources other than Google/Facebook/Trustpilot - limited to three platforms for concept validation.

## Success Criteria

-   **Time to Value:** 90% of users configure their first data source and see aggregated reviews within 10 minutes of registration.
-   **Sentiment Analysis Accuracy:** At least 75% agreement between automatic classification (positive/negative/neutral) and manual
    evaluation on a sample of 100 random reviews.
-   **Data Aggregation Completeness:** Correct retrieval and display of reviews from at least 3 different sources for 80% of configured
    companies, without integration errors.
-   **Activation and Retention:** 35% of companies log in at least 3 times within the first 4 weeks after successfully configuring their
    first data source.

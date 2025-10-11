# BrandPulse

> Monitor and analyze customer reviews from multiple sources with AI-powered sentiment analysis

**BrandPulse** is a SaaS web application that helps small and medium-sized service businesses (restaurants, hotels, beauty salons, etc.) aggregate, monitor, and analyze customer reviews from multiple platforms including Google, Facebook, and Trustpilot. The system uses AI to perform sentiment analysis and provides actionable insights through an intuitive dashboard.

## Key Features

- **Multi-Source Review Aggregation**: Collect reviews from Google, Facebook, and Trustpilot in one place
- **AI-Powered Sentiment Analysis**: Automatic classification of reviews as positive, negative, or neutral
- **Smart Summarization**: AI-generated text summaries for each review source
- **Interactive Dashboard**: Filter reviews by source, sentiment, and star rating
- **Manual Corrections**: Override AI sentiment classifications when needed
- **Automated Data Refresh**: Daily synchronization at 3:00 AM CET + manual refresh option
- **Freemium Model**: Free tier allows monitoring of one review source

## Tech Stack

### Backend
- **Java 21** + **Spring Boot 3.5.6**
- **PostgreSQL 16** - Primary database
- **Spring Data JPA** + **Hibernate** - ORM and data persistence
- **Spring Security** - Authentication and authorization
- **Liquibase** - Database schema migrations
- **Quartz Scheduler** - Scheduled jobs for review synchronization
- **Spring Mail** - Weekly email reports
- **Spring Cache** (Caffeine) - Performance optimization
- **Lombok** - Reduce boilerplate code

### Frontend (Planned)
- **Next.js 14** - React framework
- **TypeScript 5** - Type safety
- **Tailwind CSS 3** - Styling
- **Shadcn/ui** - Component library

### AI Integration
- **Openrouter.ai** - Access to multiple AI models (OpenAI, Anthropic, Google)
- Used for sentiment classification and text summarization

### Infrastructure
- **Docker Compose** - Local development environment
- **GitHub Actions** - CI/CD (planned)
- **AWS** - Production deployment (planned)

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java 21** or higher ([Download](https://adoptium.net/))
- **Maven 3.9+** (included via Maven Wrapper)
- **Docker** and **Docker Compose** ([Download](https://docs.docker.com/get-docker/))
- **Git**

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/brand-pulse.git
cd brand-pulse
```

### 2. Start PostgreSQL Database

```bash
# Start PostgreSQL container
docker-compose up -d

# Verify it's running
docker-compose ps

# View logs (optional)
docker-compose logs -f postgres
```

Database connection details:
- **Host**: localhost:5432
- **Database**: brandpulse
- **Username**: brandpulse_user
- **Password**: brandpulse_pass

### 3. Build the Backend

```bash
# Build the project
./backend/mvnw clean install -f backend/pom.xml
```

### 4. Run the Application

```bash
# Start the Spring Boot application
./backend/mvnw spring-boot:run -f backend/pom.xml
```

The backend will start on **http://localhost:8080**

### 5. Verify Installation

```bash
# Actuator endpoints
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP",
  "timestamp": "2025-10-11T...",
  "service": "BrandPulse API",
  "version": "0.0.1-SNAPSHOT"
}
```

## Running Tests

```bash
# Run all tests
./backend/mvnw test -f backend/pom.xml

# Run a specific test class
./backend/mvnw test -Dtest=ClassName -f backend/pom.xml

# Run a specific test method
./backend/mvnw test -Dtest=ClassName#methodName -f backend/pom.xml

# Package without tests
./backend/mvnw clean package -DskipTests -f backend/pom.xml
```

## Project Structure

```
brand-pulse/
├── backend/                    # Spring Boot backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/morawski/dev/backend/
│   │   │   │   ├── config/         # Configuration classes
│   │   │   │   ├── controller/     # REST controllers
│   │   │   │   ├── dto/            # Data Transfer Objects
│   │   │   │   ├── entity/         # JPA entities
│   │   │   │   ├── repository/     # Spring Data repositories
│   │   │   │   ├── service/        # Business logic
│   │   │   │   ├── exception/      # Custom exceptions
│   │   │   │   ├── security/       # Security components
│   │   │   │   ├── scheduler/      # Scheduled jobs
│   │   │   │   └── util/           # Utilities
│   │   │   └── resources/
│   │   │       ├── db/changelog/   # Liquibase migrations
│   │   │       └── application.properties
│   │   └── test/                   # Unit and integration tests
│   ├── pom.xml                     # Maven dependencies
│   └── mvnw                        # Maven Wrapper
├── frontend/                       # Next.js frontend (planned)
├── docs/                           # Project documentation
│   ├── project-prd-en.md          # Product Requirements
│   ├── mvp-en.md                  # MVP definition
│   └── tech-stack-en.md           # Tech stack details
├── docker-compose.yml             # Local development environment
├── CLAUDE.md                      # AI assistant guidance
└── README.md                      # This file
```

## Configuration

The main configuration file is located at `backend/src/main/resources/application.properties`.

### Key Configuration Areas:

- **Database**: PostgreSQL connection settings
- **Security**: CORS, JWT settings (placeholders)
- **Mail**: SMTP configuration for email reports
- **Scheduler**: Thread pool for background jobs
- **Cache**: Caffeine cache settings (10-minute TTL)
- **AI**: OpenRouter.ai API configuration

### Environment-Specific Configuration:

The application uses Spring profiles. Current active profile: `dev`

To use a different profile:
```bash
./backend/mvnw spring-boot:run -Dspring.profiles.active=prod -f backend/pom.xml
```

## API Documentation

### Available Endpoints:

**Actuator Endpoints:**
- `GET /actuator/health` - Detailed health information
- `GET /actuator/info` - Application information
- `GET /actuator/prometheus` - Prometheus metrics

### Swagger UI (Planned)

Once implemented, API documentation will be available at:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`


## Success Metrics (MVP)

- **Time to Value**: 90% of users configure first source within 10 minutes
- **Sentiment Accuracy**: 75% AI sentiment analysis accuracy
- **Activation Rate**: 60% of users configure a source within 7 days
- **Retention**: 35% retention (3+ logins in first 4 weeks)

## Contributing

This is a private project currently in development. Contribution guidelines will be added once the MVP is complete.

## License

This project is proprietary software. All rights reserved.

## Support

For issues and questions, please contact the development team or create an issue in the GitHub repository.

---

**Built with ❤️ for small and medium-sized businesses**

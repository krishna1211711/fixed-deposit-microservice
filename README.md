# Banking Fixed Deposit Microservice

A comprehensive microservices-based banking application for managing Fixed Deposits (FDs). 

## Architecture
```text
[ Angular Web UI ] ---> [ API Gateway ] ---> [ FD Microservice ] ---> [ MySQL DB ]
                                       ---> [ Report Service ]  ---> [ MySQL DB ]
```

## Technology Stack
| Component | Technology |
|---|---|
| Frontend | Angular 17, Node 20 |
| Backend API | Spring Boot 3.2.x, Java 17 |
| API Gateway | Spring Cloud Gateway |
| Database | MySQL 8.0 |
| Reports | Python 3.10, Pandas, ReportLab |
| Containerization| Docker, Docker Compose |
| Orchestration | Kubernetes |

## Prerequisites
- Java 17
- Node 20
- Python 3.10
- MySQL 8
- Docker and Docker Compose
- kubectl & minikube (for Kubernetes)

## Quick Start

### 1. Local Setup
1. Create MySQL database: `CREATE DATABASE fd_bank_db;`
2. Start FD Microservice: `cd fd-microservice && mvn spring-boot:run`
3. Start Angular UI: `cd fd-angular-ui && npm install && ng serve`
4. Access at http://localhost:4200

### 2. Docker Compose Setup
Run: `docker-compose up --build`
Access UI at http://localhost:4200 and API Gateway at http://localhost:9090.

### 3. Kubernetes Setup
Run: `kubectl apply -f k8s/`
The application will be deployed to the `fd-banking` namespace.

## Default Credentials
- **Username**: admin
- **Password**: admin123
- Roles: ADMIN
You can also register new users via the UI.

## API Documentation
Swagger UI available at: http://localhost:8080/swagger-ui.html

## Environment Variables
| Variable | Description |
|---|---|
| DB_HOST | Database host address |
| DB_PORT | Database port |
| DB_NAME | Database name |
| DB_USERNAME | DB user |
| DB_PASSWORD | DB password |
| JWT_SECRET | 256-bit secret key for JWT signing |

## API Endpoints
| Module | Endpoint | Methods | Description |
|---|---|---|---|
| Auth | /api/auth/** | POST | Authentication and Registration |
| FD Accounts | /api/fd-accounts | GET, POST | Manage FD accounts |
| Users | /api/users | GET | User management |

## Database Schema Summary
- `users`: Stores user credentials and roles.
- `fd_products`: Fixed deposit product definitions and interest rates.
- `fd_accounts`: Customer fixed deposit accounts.

## Git Commit Recommendations
- feat: Add FD account creation with initial deposit
- feat: Implement compound interest calculation engine
- feat: Add batch schedulers for interest accrual and maturity
- feat: Add Angular FD dashboard with i18n support
- infra: Add Docker and Kubernetes deployment manifests
- docs: Add comprehensive README and API documentation

## Azure Deployment Commands
```bash
az group create --name fd-rg --location eastus
az acr create --resource-group fd-rg --name fdbankacr --sku Basic
az acr build --registry fdbankacr --image fd-service:latest ./fd-microservice
az webapp create --resource-group fd-rg --plan fd-plan --name fd-service-app --container-image-name fdbankacr.azurecr.io/fd-service:latest
az webapp config appsettings set --resource-group fd-rg --name fd-service-app --settings DB_HOST=...
```

## Team Credits
- Placeholder for team members

## License
MIT License

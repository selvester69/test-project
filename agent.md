# Product Service Development - Agent Task Instructions

## Overview

Create a comprehensive product service with full CRUD operations, advanced searching, and filtering capabilities. Follow these task-wise instructions to implement a robust, scalable solution.

<!-- ## Task 1: Project Setup and Architecture Planning

### Agent Instructions

- **Analyze Requirements**: Determine the technology stack based on requirements (Node.js/Express, Python/FastAPI, Java/Spring, etc.)
- **Database Selection**: Choose appropriate database (PostgreSQL for relational, MongoDB for document-based)
- **Architecture Pattern**: Implement layered architecture (Controller → Service → Repository/DAO → Database)
- **Project Structure**: Create organized folder structure with separation of concerns
- **Environment Setup**: Configure development environment with necessary tools and dependencies

### Deliverables

- Project folder structure
- Package.json/requirements.txt with dependencies
- Environment configuration files
- Database connection setup
- Basic server configuration

## Task 2: Data Model Design

### Agent Instructions

- **Define Product Schema**: Create comprehensive product data model including:
  - id (primary key)
  - name (string, required)
  - description (text)
  - price (decimal/float)
  - category_id (foreign key)
  - sku (unique string)
  - stock_quantity (integer)
  - status (enum: active, inactive, discontinued)
  - tags (array/JSON)
  - created_at (timestamp)
  - updated_at (timestamp)
  - metadata (JSON for extensible attributes)

- **Category Model** (if needed):
  - id, name, description, parent_category_id

- **Database Schema**: Create migration scripts or database schema
- **Indexing Strategy**: Plan indexes for search and filter fields
- **Validation Rules**: Define data validation constraints

### Deliverables

- Database migration files
- Model/Entity classes
- Validation schemas
- Database indexes setup

## Task 3: CRUD Operations Implementation

### Agent Instructions

#### CREATE Operation

- **Endpoint**: POST /api/products
- **Validation**: Implement input validation (required fields, data types, business rules)
- **Duplicate Check**: Ensure SKU uniqueness
- **Error Handling**: Return appropriate HTTP status codes and error messages
- **Response Format**: Return created product with generated ID and timestamps

#### READ Operations

- **Single Product**: GET /api/products/:id
- **All Products**: GET /api/products (with pagination)
- **Error Handling**: Handle not found cases (404)
- **Data Transformation**: Format response data consistently

#### UPDATE Operation

- **Endpoint**: PUT /api/products/:id (full update) and PATCH /api/products/:id (partial update)
- **Validation**: Validate input data and business rules
- **Optimistic Locking**: Implement version control if needed
- **Audit Trail**: Track update timestamps
- **Response**: Return updated product data

#### DELETE Operation

- **Endpoint**: DELETE /api/products/:id
- **Soft Delete Option**: Consider implementing soft delete (status = 'deleted')
- **Cascade Rules**: Handle related data appropriately
- **Confirmation**: Return success status

### Deliverables

- Controller methods for all CRUD operations
- Service layer business logic
- Repository/DAO layer for database operations
- Input validation middleware
- Error handling middleware
- Unit tests for each operation -->

<!-- ## Task 4: Search Functionality Implementation

### Agent Instructions

#### Basic Search

- **Endpoint**: GET /api/products/search?q={query}
- **Search Fields**: Implement search across name, description, SKU, tags
- **Search Types**:
  - Exact match
  - Partial match (LIKE/contains)
  - Full-text search (if supported by database)
  - Case-insensitive search

#### Advanced Search

- **Multi-field Search**: Allow searching specific fields
- **Search Operators**: Implement AND, OR logic
- **Fuzzy Search**: Consider implementing approximate string matching
- **Search Ranking**: Order results by relevance

#### Search Performance

- **Database Indexes**: Ensure proper indexing on searchable fields
- **Query Optimization**: Optimize database queries
- **Caching**: Implement search result caching for popular queries

### Deliverables

- Search controller and service methods
- Database queries optimized for search
- Search result ranking algorithm
- Search performance tests
- API documentation for search endpoints

## Task 5: Filtering System Implementation

### Agent Instructions

#### Filter Categories

- **Price Range**: min_price, max_price parameters
- **Category Filter**: category_id or category_name
- **Status Filter**: status (active, inactive, discontinued)
- **Stock Filter**: in_stock (boolean), min_stock, max_stock
- **Date Range**: created_after, created_before, updated_after, updated_before
- **Tag Filter**: tags (array or comma-separated)
- **Custom Attributes**: Support filtering by metadata fields

#### Filter Implementation

- **Query Parameters**: GET /api/products?category_id=1&min_price=10&max_price=100
- **Multiple Values**: Support multiple values for same filter (category_id=1,2,3)
- **Filter Combination**: Implement AND logic between different filters
- **Dynamic Query Building**: Build database queries dynamically based on provided filters
- **Validation**: Validate filter parameters

#### Advanced Filtering

- **Range Filters**: Support range operations (between, greater than, less than)
- **Array Filters**: Handle array fields (tags contains, tags in)
- **Null Filters**: Support filtering for null/empty values
- **Negation**: Support NOT operations

### Deliverables

- Filter parsing and validation logic
- Dynamic query builder
- Filter combination logic
- Comprehensive filter tests
- Filter documentation

## Task 6: Pagination and Sorting

### Agent Instructions

#### Pagination

- **Parameters**: page (default: 1), limit/per_page (default: 20, max: 100)
- **Response Format**: Include total_count, total_pages, current_page, has_next, has_previous
- **Offset Calculation**: Implement proper offset calculation
- **Performance**: Optimize for large datasets

#### Sorting

- **Parameters**: sort_by (field name), sort_order (asc/desc)
- **Multi-field Sorting**: Support multiple sort criteria
- **Default Sorting**: Implement sensible default sorting (e.g., by created_at desc)
- **Sortable Fields**: Restrict sorting to indexed/appropriate fields

#### Combined Implementation

- **URL Format**: GET /api/products?page=2&limit=10&sort_by=price&sort_order=asc
- **Integration**: Combine with search and filtering
- **Performance**: Ensure efficient database queries

### Deliverables

- Pagination helper functions
- Sorting logic implementation
- Combined query builder for search, filter, sort, and paginate
- Response formatter for paginated results -->

## Task 7: API Documentation and Testing

### Agent Instructions

#### API Documentation

- **OpenAPI/Swagger**: Create comprehensive API documentation
- **Endpoint Documentation**: Document all endpoints with parameters, responses, examples
- **Schema Documentation**: Document request/response schemas
- **Error Codes**: Document all possible error responses
- **Authentication**: Document any authentication requirements

#### Testing Strategy

- **Unit Tests**: Test individual functions and methods
- **Integration Tests**: Test API endpoints end-to-end
- **Test Data**: Create test fixtures and seed data
- **Edge Cases**: Test boundary conditions and error scenarios
- **Performance Tests**: Test with large datasets

#### Quality Assurance

- **Code Review**: Implement code review process
- **Linting**: Set up code linting and formatting
- **Security Review**: Check for common security vulnerabilities
- **Performance Monitoring**: Implement logging and monitoring

### Deliverables

- Complete API documentation
- Comprehensive test suite
- Test coverage report
- Performance benchmarks
- Security audit checklist

## Task 8: Performance Optimization and Scalability

### Agent Instructions

#### Database Optimization

- **Query Optimization**: Analyze and optimize slow queries
- **Index Strategy**: Review and optimize database indexes
- **Connection Pooling**: Implement database connection pooling
- **Query Caching**: Implement query result caching

#### Application Performance

- **Response Caching**: Cache frequently requested data
- **Compression**: Implement response compression
- **Async Processing**: Use asynchronous operations where appropriate
- **Memory Management**: Optimize memory usage

#### Scalability Considerations

- **Horizontal Scaling**: Design for multiple server instances
- **Load Balancing**: Consider load balancing strategies
- **Database Sharding**: Plan for database scaling
- **Microservices**: Consider service decomposition if needed

### Deliverables

- Performance optimization report
- Caching implementation
- Scalability documentation
- Load testing results
- Monitoring and alerting setup

<!-- ## Task 9: Security and Validation

### Agent Instructions

#### Input Validation

- **Schema Validation**: Implement strict input validation
- **SQL Injection Prevention**: Use parameterized queries
- **XSS Prevention**: Sanitize output data
- **CSRF Protection**: Implement CSRF tokens if needed

#### Authentication and Authorization

- **API Keys**: Implement API key authentication
- **JWT Tokens**: Consider JWT for stateless authentication
- **Role-Based Access**: Implement user roles and permissions
- **Rate Limiting**: Implement API rate limiting

#### Security Best Practices

- **HTTPS**: Ensure all communications use HTTPS
- **Security Headers**: Implement security headers
- **Audit Logging**: Log all data modifications
- **Data Privacy**: Implement data privacy controls

### Deliverables

- Security implementation
- Authentication system
- Authorization middleware
- Security testing results
- Audit logging system -->

<!-- ## Task 10: Deployment and Monitoring

### Agent Instructions

#### Deployment Strategy

- **Environment Setup**: Configure production environment
- **Database Migration**: Plan database deployment strategy
- **Configuration Management**: Manage environment-specific configurations
- **Backup Strategy**: Implement database backup procedures

#### Monitoring and Logging

- **Application Logging**: Implement comprehensive logging
- **Error Tracking**: Set up error monitoring
- **Performance Monitoring**: Monitor API performance
- **Health Checks**: Implement health check endpoints

#### Maintenance

- **Documentation**: Create deployment and maintenance documentation
- **Troubleshooting**: Create troubleshooting guides
- **Update Procedures**: Document update and rollback procedures

### Deliverables

- Deployment scripts
- Monitoring dashboard
- Logging configuration
- Maintenance documentation
- Backup and recovery procedures -->

## Success Criteria

The product service implementation will be considered successful when it meets these criteria:

1. **Functionality**: All CRUD operations work correctly
2. **Search**: Fast and accurate search across multiple fields
3. **Filtering**: Flexible filtering with multiple criteria
4. **Performance**: Response times under 200ms for typical queries
5. **Scalability**: Can handle 1000+ concurrent requests
6. **Security**: Passes security audit with no critical vulnerabilities
7. **Documentation**: Complete API documentation with examples
8. **Testing**: 90%+ test coverage
9. **Monitoring**: Full observability of system health and performance
10. **Maintainability**: Clean, well-structured, and documented code

<!-- ## Implementation Order

Follow this sequence for optimal development flow:

1. Project Setup → Data Model → Basic CRUD
2. Search Implementation → Filtering System
3. Pagination/Sorting → Performance Optimization
4. Security Implementation → Testing
5. Documentation → Deployment → Monitoring

Each task should be completed and tested before moving to the next phase. -->

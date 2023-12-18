# DIPLOMA PROJECT

## api

| address            | request | action      |
|--------------------|---------|-------------|
| /api/v1/users      | get     | getAllUsers |
| /api/v1/users/{id} | get     | getUserById |
| /api/v1/users      | post    | addUser     |
| /api/v1/users      | patch   | updateUser  |
| /api/v1/users/{id} | delete  |  deleteUser |

## database relations

#### user <-manyToOne-> user_role <-ontToMany-> role

## database docker command

#### docker run --name pg-db -p <port>:<port> -e POSTGRES_PASSWORD=<password> -d -v "path":/docker-entrypoint-initdb.d postgres
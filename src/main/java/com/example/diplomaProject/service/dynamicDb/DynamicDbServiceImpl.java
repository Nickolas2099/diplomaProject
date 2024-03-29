package com.example.diplomaProject.service.dynamicDb;

import com.example.diplomaProject.domain.api.allDbData.GetDbDataResp;
import com.example.diplomaProject.domain.api.allDbData.TableResp;
import com.example.diplomaProject.domain.api.constructor.QueryReq;
import com.example.diplomaProject.domain.api.SwitchDbReq;
import com.example.diplomaProject.domain.api.constructor.QueryResp;
import com.example.diplomaProject.domain.constant.Code;
import com.example.diplomaProject.domain.dto.ConnDbDto;
import com.example.diplomaProject.domain.dto.Table;
import com.example.diplomaProject.domain.entity.Action;
import com.example.diplomaProject.domain.entity.ConnDb;
import com.example.diplomaProject.domain.entity.DynamicField;
import com.example.diplomaProject.domain.entity.DynamicTable;
import com.example.diplomaProject.domain.mapper.connDb.ConnDbMapper;
import com.example.diplomaProject.domain.mapper.dynamicDb.FieldMapper;
import com.example.diplomaProject.domain.mapper.dynamicDb.ResultMapper;
import com.example.diplomaProject.domain.mapper.dynamicDb.TableMapper;
import com.example.diplomaProject.domain.response.Response;
import com.example.diplomaProject.domain.response.SuccessResponse;
import com.example.diplomaProject.domain.response.error.Error;
import com.example.diplomaProject.domain.response.error.ErrorResponse;
import com.example.diplomaProject.repository.ActionRepository;
import com.example.diplomaProject.repository.TableRepository;
import com.example.diplomaProject.repository.UserRepository;
import com.example.diplomaProject.service.connectedDB.ConnectedDbService;
import com.example.diplomaProject.service.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Slf4j
@RequiredArgsConstructor
public class DynamicDbServiceImpl implements DynamicDbService {

    private final ConnectedDbService connectedDbService;
    private final ConnDbMapper connDbMapper;
    private final ActionRepository actionRepository;
    private final TableRepository tableRepository;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public static SessionFactory dynamicSessionFactory;

    @Deprecated
    @Override
    public ResponseEntity<Response> switchDb(SwitchDbReq req) {
        try {
            SuccessResponse response = (SuccessResponse)connectedDbService.getByTitle(req.getTitle()).getBody();
            ConnDb connDb = connDbMapper.toEntity((ConnDbDto)response.getData());
//            dbName = getDatabaseName(connDb.getUrl());
            dynamicSessionFactory = getSessionFactory(connDb);
            return new ResponseEntity<>(SuccessResponse.builder().build(), HttpStatus.OK);
        }catch (Exception ex) {
            log.error(ex.toString());
            return new ResponseEntity<>(ErrorResponse.builder().error(Error.builder()
                    .code(Code.INVALID_VALUE).message("база с названием: `" + req.getTitle() + "` не найдена")
                    .build()).build(), HttpStatus.BAD_REQUEST);
        }
    }

    private String getDatabaseName(String url) throws URISyntaxException {
        URI uri = new URI(url.substring(5));
        return uri.getPath().substring(1);
//        String[] parts = url.split("/");
//            return parts[parts.length - 1];
    }

    @Deprecated
    @Override
    public ResponseEntity<Response> getAll(String dbTitle) {

        ConnDb connDb = connDbMapper.toEntity(
                (ConnDbDto) ((SuccessResponse<ConnDbDto>)connectedDbService.getByTitle(dbTitle).getBody()).getData()
        );
        List<Table> tables =
                (List<Table>)
                        ((SuccessResponse<List<Table>>)connectedDbService.getTablesByDbTitle(dbTitle ).getBody()).getData();

        SessionFactory sessionFactory = getSessionFactory(connDb);

        try(Session session = sessionFactory.openSession()) {

            Transaction tx = session.beginTransaction();
            GetDbDataResp resp = new GetDbDataResp();
            resp.setTables(new ArrayList<>());
            for(Table table : tables) {
                resp.getTables().add(TableResp.builder().title(table.getUserTitle()).rows(
                         session.createNativeQuery("SELECT * FROM " + table.getTechTitle())
                                .setResultTransformer(new ResultMapper()).list()
                ).build());
            }
            tx.commit();
            session.close();
            return new ResponseEntity<>(SuccessResponse.builder().data(resp).build(), HttpStatus.OK);
        } catch (Exception ex) {
            log.error(ex.toString());
            return new ResponseEntity<>(ErrorResponse.builder().error(Error.builder()
                    .code(Code.INTERNAL_SERVER_ERROR).message("Error getting data from the database")
                    .build()).build(), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public ResponseEntity<Response> checkConnection(ConnDbDto connDb) {

            SessionFactory sessionFactory = getSessionFactory(connDbMapper.toEntity(connDb));
            Session session = null;
            try {
                session = sessionFactory.openSession();
            } catch (HibernateException e) {
                e.printStackTrace();
                return new ResponseEntity<>(ErrorResponse.builder().error(Error.builder()
                        .message("Connection error").code(Code.INVALID_VALUE)
                        .build()).build(), HttpStatus.BAD_REQUEST);
            } finally {
                if (session != null) {
                    session.close();
                }
            }
            return new ResponseEntity<>(SuccessResponse.builder().build(), HttpStatus.OK);

    }

    @Override
    public ResponseEntity<Response> handleDb(ConnDbDto connDbDto) {
        ConnDb connDb = connDbMapper.toEntity(connDbDto);
        SessionFactory sessionFactory = getSessionFactory(connDb);
        switch (connDb.getDbType()) {
            case MYSQL -> handleMySql(sessionFactory, connDb);
            case POSTGRESQL -> handlePostgres(sessionFactory, connDb);
            default -> {
                return new ResponseEntity<>(ErrorResponse.builder().error(Error.builder()
                        .code(Code.INVALID_VALUE).message("Data base type is not supported").build()).build(),
                        HttpStatus.BAD_REQUEST);
            }
        }

        return new ResponseEntity<>(SuccessResponse.builder().build(), HttpStatus.OK);
    }

    private void handleMySql(SessionFactory sessionFactory, ConnDb connDb) {

        try {
            Session session = sessionFactory.openSession();
            Transaction tx = session.beginTransaction();
            String databaseName = getDatabaseName(connDb.getUrl());
            List<DynamicTable> tables =
                    session.createNativeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
                                    "WHERE TABLE_SCHEMA = '" + databaseName + "' AND TABLE_TYPE = 'BASE TABLE';")
                            .setResultTransformer(new TableMapper()).list();
            for(DynamicTable table : tables) {
                table.setFields(
                        session.createNativeQuery("SELECT COLUMN_NAME, DATA_TYPE " +
                                        "FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '" + table.getTechTitle() + "';")
                                .setResultTransformer(new FieldMapper()).list());
            }
            //define userTitles of tables & fields if they're null
            if(tables.get(0).getUserTitle() == null || tables.get(0).getFields().get(0).getTechTitle() == null) {
                for(DynamicTable table : tables) {
                    table.setUserTitle(table.getTechTitle());
                    for(DynamicField field : table.getFields()) {
                        field.setUserTitle(field.getTechTitle());
                    }
                }
            }

            connDb.setTables(tables);
            for(DynamicTable table : tables) {
                table.setConnDb(connDb);
                for(DynamicField field : table.getFields()) {
                    field.setTable(table);
                }
            }
            connectedDbService.add(connDb);
            tx.commit();
        } catch (Exception ex) {
            log.error(ex.toString());
        } finally {
            sessionFactory.close();
        }
    }

    public void handlePostgres(SessionFactory sessionFactory, ConnDb connDb) {
            try {
                Session session = sessionFactory.openSession();
                Transaction tx = session.beginTransaction();
//                String databaseName = getDatabaseName(connDb.getUrl());
                List<DynamicTable> tables =
                        session.createNativeQuery("SELECT table_name FROM information_schema.tables " +
                                        "WHERE table_schema = 'public' AND table_type = 'BASE TABLE';")
                                .setResultTransformer(new TableMapper()).list();
                for(DynamicTable table : tables) {
                    table.setFields(
                            session.createNativeQuery("SELECT column_name, data_type " +
                                            "FROM information_schema.columns WHERE table_name = '" + table.getTechTitle() + "';")
                                    .setResultTransformer(new FieldMapper()).list());
                }
                if(tables.get(0).getUserTitle() == null || tables.get(0).getFields().get(0).getTechTitle() == null) {
                    for(DynamicTable table : tables) {
                        table.setUserTitle(table.getTechTitle());
                        for(DynamicField field : table.getFields()) {
                            field.setUserTitle(field.getTechTitle());
                        }
                    }
                }

                tx.commit();
                connDb.setTables(tables);
                for(DynamicTable table : tables) {
                    table.setConnDb(connDb);
                    for(DynamicField field : table.getFields()) {
                        field.setTable(table);
                    }
                }
                log.info("TABLES: {}", tables);

                connectedDbService.add(connDb);
            } catch (Exception ex) {
                log.error(ex.toString());
            } finally {
                sessionFactory.close();
            }
    }

    @Override
    public ResponseEntity<Response> selectFromDb(QueryReq req, String authorization) {

        List<QueryResp> resp;
        String sql;
        String tableTitle = req.getTable();
        ResponseEntity<Response> response = connectedDbService.getByTitle(req.getDbTitle());
        if(response.getBody() instanceof ErrorResponse) {
            return response;
        } else {
            SessionFactory sessionFactory = getSessionFactory(connDbMapper.toEntity(
                    (ConnDbDto)((SuccessResponse)response.getBody()).getData())
            );
            sql = getSqlQuery(req);
            try {
                Session session = sessionFactory.openSession();
                Transaction tx = session.beginTransaction();

                resp = session.createNativeQuery(sql).setResultTransformer(new ResultMapper()).list();
                if(resp == null) {
                    throw new Exception();
                }
                tx.commit();

            } catch (Exception ex) {
                log.error("ERROR: {}", ex.getMessage());
                return new ResponseEntity<>(ErrorResponse.builder()
                        .error(Error.builder().code(Code.INTERNAL_SERVER_ERROR).message("query hasn't been assembled").build())
                        .build(), HttpStatus.BAD_REQUEST);
            } finally {
                sessionFactory.close();
            }
        }
        this.saveAction(authorization, sql, tableTitle);
        return new ResponseEntity<>(SuccessResponse.builder().data(resp).build(), HttpStatus.OK);
    }

    private void saveAction(String authorization, String sql, String tableTitle) {

        String jwt = authorization.substring(7);
        actionRepository.save(Action.builder()
                .queryText(sql)
                .table(tableRepository.getFirstByTechTitle(tableTitle))
                .user(userRepository.findByLogin(jwtService.extractUsername(jwt))
                        .orElseThrow(NoSuchElementException::new))
                .build());
    }


    private SessionFactory getSessionFactory(ConnDb dataBase) {
        Configuration config = new Configuration();

        String driver = "";
        switch (dataBase.getDbType()) {
            case MYSQL ->  driver = "com.mysql.cj.jdbc.Driver";
            case POSTGRESQL -> driver = "org.postgresql.Driver";
        }

        config.setProperty("hibernate.connection.driver_class", driver);
        config.setProperty("hibernate.connection.url", dataBase.getUrl());
        config.setProperty("hibernate.connection.username", dataBase.getUsername());
        config.setProperty("hibernate.connection.password", dataBase.getPassword());

        return config.buildSessionFactory();
    }

    private String getSqlQuery(QueryReq req) {
        StringBuilder sql = new StringBuilder("SELECT ");

        //add fields
        for(int i = 0; i < req.getFields().size()-1; i++) {
            sql.append(req.getFields().get(i));
            sql.append(", ");
        }
        sql.append(req.getFields().get(req.getFields().size()-1));
        sql.append(" ");

        //add FROM
        sql.append("FROM ");
        sql.append(req.getTable());
        sql.append(" ");

        //add filter
        if(req.getFilters() != null) {
            sql.append("WHERE ");
            for(int i = 0; i < req.getFilters().size()-1; i++) {

                sql.append(req.getFilters().get(i).getField());
                sql.append(" ");
                sql.append(req.getFilters().get(i).getCondition());

                if(req.getFilters().get(i).getConditionField() != null) {
                    sql.append(" ");
                    sql.append(req.getFilters().get(i).getConditionField());
                }
                sql.append(" AND");
            }
            sql.append(req.getFilters().get(req.getFilters().size()-1).getField());
            sql.append(" ");
            sql.append(req.getFilters().get(req.getFilters().size()-1).getCondition());
            sql.append(" ");

            if(req.getFilters().get(req.getFilters().size()-1).getConditionField() != null) {
                sql.append(req.getFilters().get(req.getFilters().size()-1).getConditionField());
                sql.append(" ");
            }
        }

        //add GROUP BY
        if(req.getGroups() != null) {
            sql.append("GROUP BY ");
            for(int i = 0; i < req.getGroups().size()-1; i++) {
                sql.append(req.getGroups().get(i));
                sql.append(", ");
            }
            sql.append(req.getGroups().get(req.getGroups().size()-1));
            sql.append(" ");
        }

        //add ORDER BY
        if(req.getSorts() != null) {
            sql.append("ORDER BY ");
            for(int i = 0; i < req.getSorts().size()-1; i++) {
                sql.append(req.getSorts().get(i).getField());
                sql.append(" ");
                sql.append(req.getSorts().get(i).getSortType());
                sql.append(", ");
            }
            sql.append(req.getSorts().get(req.getSorts().size()-1).getField());
            sql.append(" ");
            sql.append(req.getSorts().get(req.getSorts().size()-1).getSortType());
            sql.append(" ");
        }

        //add LIMIT
        if(req.getLimit() != null) {
            sql.append("LIMIT ");
            sql.append(req.getLimit());
        }

        log.info("ASSEMBLED sql command: `{}`", sql);

        return sql.toString();
    }

}
